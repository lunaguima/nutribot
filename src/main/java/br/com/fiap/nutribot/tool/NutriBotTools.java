package br.com.fiap.nutribot.tool;

import java.time.LocalDate;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import br.com.fiap.nutribot.dto.SummaryResponse;
import br.com.fiap.nutribot.model.MealEntry;
import br.com.fiap.nutribot.model.MealType;
import br.com.fiap.nutribot.service.FoodService;
import br.com.fiap.nutribot.service.MealService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NutriBotTools {

    private final FoodService foodService;
    private final MealService mealService;

    @Tool(description = "Busca as informações nutricionais e calóricas (por 100g) de um alimento pelo nome na tabela da clínica.")
    public Object buscarAlimento(
            @ToolParam(description = "Nome do alimento, ex: tapioca, pão de queijo, ovo, arroz") String nomeAlimento) {
        try {
            String limpo = limparNomeAlimento(nomeAlimento);
            return foodService.findByName(limpo);
        } catch (ResponseStatusException e) {
            return "Alimento não encontrado na tabela da clínica: " + nomeAlimento;
        }
    }

    @Tool(description = "Retorna o resumo do diário nutricional do paciente hoje (calorias consumidas, meta diária e calorias restantes).")
    public SummaryResponse resumoDoDia(
            @ToolParam(description = "Envie sempre 'hoje'") String periodo) {
        return mealService.summary(LocalDate.now());
    }

    @Tool(description = "Registra um alimento consumido no diário alimentar de hoje.")
    public String registrarRefeicao(
            @ToolParam(description = "Nome do alimento, ex: Ovo cozido, Pão de queijo, Banana prata") String nomeAlimento,
            @ToolParam(description = "Quantidade em gramas (ex: 100.0). Se for em unidades como 2 ovos, envie 100.0") Double gramas,
            @ToolParam(description = "Tipo da refeição: BREAKFAST, LUNCH, DINNER ou SNACK") MealType tipoRefeicao) {
        try {
            double peso = (gramas == null || gramas <= 0) ? 100.0 : (gramas <= 10 ? gramas * 50.0 : gramas);
            String nomeLimpo = limparNomeAlimento(nomeAlimento);
            MealType mealType = tipoRefeicao != null ? tipoRefeicao : MealType.BREAKFAST;

            MealEntry entry;
            try {
                entry = mealService.log(nomeLimpo, peso, mealType);
            } catch (ResponseStatusException ex) {
                String primeiraPalavra = nomeLimpo.split("\\s+")[0].replaceAll("(?i)s$", "");
                entry = mealService.log(primeiraPalavra, peso, mealType);
            }

            return String.format("Refeição registrada com sucesso: %.1fg de %s no %s (%.1f kcal)",
                    entry.getGrams(), entry.getFood().getName(), entry.getMealType(), entry.calories());
        } catch (ResponseStatusException e) {
            return "Não foi possível registrar a refeição: " + e.getReason();
        }
    }

    private String limparNomeAlimento(String raw) {
        if (raw == null || raw.isBlank()) return "Ovo";
        String limpo = raw.trim()
                .replaceFirst("(?i)^(\\d+([.,]\\d+)?\\s*((gramas?|g)\\b)?|(um|uma|dois|duas|tr[eê]s)\\b)\\s*(de\\s+)?", "")
                .trim();
        if (limpo.toLowerCase().startsWith("ovos")) {
            limpo = limpo.replaceFirst("(?i)ovos", "ovo");
        }
        return limpo.isBlank() ? raw.trim() : limpo;
    }
}