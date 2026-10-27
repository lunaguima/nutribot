package br.com.fiap.nutribot.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import br.com.fiap.nutribot.model.MealAnalysis;

@Service
public class MealAnalysisService {

    private static final String SYSTEM = """
            Você é a NutriBot, nutricionista virtual da Clínica Equilíbrio.
            Receba a descrição de uma refeição e estime os alimentos, a quantidade em gramas
            e as calorias de cada um, usando porções típicas da culinária brasileira quando
            a quantidade não for informada.
            Calcule também o total de calorias e os totais de proteína, carboidratos e gordura,
            em gramas. Nunca devolva protein, carbs ou fat zerados: estime cada um somando
            os alimentos da refeição.
            Use nomes de alimentos em português, no singular quando fizer sentido
            (ex.: "Arroz branco", "Feijão carioca", "Bife grelhado").
            Todos os valores numéricos devem ser números, nunca texto.
            """;

    private final ChatClient chatClient;

    public MealAnalysisService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public MealAnalysis analyze(String description) {
        MealAnalysis analysis;
        try {
            analysis = ask(description);
        } catch (Exception e) {
            // modelos pequenos às vezes devolvem JSON torto: tenta uma segunda vez
            analysis = ask(description);
        }

        List<MealAnalysis.Item> items = analysis.items() == null ? List.of() : analysis.items();

        // não confiamos no modelo para somar: o total é a soma dos itens
        double total = items.stream().mapToDouble(MealAnalysis.Item::calories).sum();

        return new MealAnalysis(
                items,
                Math.round(total * 10) / 10.0,
                analysis.protein(),
                analysis.carbs(),
                analysis.fat());
    }

    private MealAnalysis ask(String description) {
        return chatClient.prompt()
                .system(SYSTEM)
                .user(description)
                .call()
                .entity(MealAnalysis.class);
    }
}