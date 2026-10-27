package br.com.fiap.nutribot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.com.fiap.nutribot.model.Food;
import br.com.fiap.nutribot.model.MealAnalysis;

@Service
public class MealAnalysisService {

    private static final String SYSTEM = """
            Você é a NutriBot, nutricionista virtual da Clínica Equilíbrio.
            Receba a descrição de uma refeição e estime os alimentos, a quantidade em gramas
            e as calorias de cada um, usando porções típicas da culinária brasileira quando
            a quantidade não for informada.
            Calcule também o total de calorias e os totais de proteína, carboidratos e gordura,
            em gramas. Nunca devolva protein, carbs ou fat zerados.
            Quando houver equivalente na tabela abaixo, use EXATAMENTE o nome da tabela.
            Todos os valores numéricos devem ser números, nunca texto.
            Alimentos da tabela: %s
            """;

    private final ChatClient chatClient;
    private final FoodService foodService;

    public MealAnalysisService(ChatClient.Builder builder, FoodService foodService) {
        this.chatClient = builder.build();
        this.foodService = foodService;
    }

    public MealAnalysis analyze(String description) {
        MealAnalysis raw;
        try {
            raw = ask(description);
        } catch (Exception e) {
            raw = ask(description); // JSON torto: tenta de novo
        }

        List<MealAnalysis.Item> rawItems = raw.items() == null ? List.of() : raw.items();
        List<MealAnalysis.Item> items = new ArrayList<>();
        double calories = 0, protein = 0, carbs = 0, fat = 0;
        int matched = 0;

        for (MealAnalysis.Item item : rawItems) {
            Optional<Food> food = findFood(item.food());
            if (food.isPresent()) {
                Food f = food.get();
                double factor = item.grams() / 100.0;
                double kcal = round(f.getCalories() * factor);
                items.add(new MealAnalysis.Item(item.food(), item.grams(), kcal));
                calories += kcal;
                protein += f.getProtein() * factor;
                carbs += f.getCarbs() * factor;
                fat += f.getFat() * factor;
                matched++;
            } else {
                items.add(item); // alimento fora da tabela: mantém a estimativa do modelo
                calories += item.calories();
            }
        }

        // macros da tabela só se TODOS os itens casaram; senão usa a estimativa do modelo
        boolean allMatched = matched > 0 && matched == rawItems.size();
        return new MealAnalysis(
                items,
                round(calories),
                allMatched ? round(protein) : raw.protein(),
                allMatched ? round(carbs) : raw.carbs(),
                allMatched ? round(fat) : raw.fat());
    }

    private MealAnalysis ask(String description) {
        String nomes = foodService.search(null).stream()
                .map(Food::getName)
                .collect(Collectors.joining(", "));
        return chatClient.prompt()
                .system(SYSTEM.formatted(nomes))
                .user(description)
                .call()
                .entity(MealAnalysis.class);
    }

    private Optional<Food> findFood(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String busca = name.trim();
        // a tabela não tem "bife"; o corte equivalente é a alcatra grelhada
        if (busca.toLowerCase().startsWith("bife")) busca = "Alcatra grelhada";
        try {
            return Optional.of(foodService.findByName(busca));
        } catch (ResponseStatusException e) {
            String firstWord = busca.split("\\s+")[0];
            try {
                return Optional.of(foodService.findByName(firstWord));
            } catch (ResponseStatusException e2) {
                return Optional.empty();
            }
        }
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}