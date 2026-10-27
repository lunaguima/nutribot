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
            @ToolParam(description = "Nome simples do alimento no singular, ex: tapioca, ovo, arroz") String nomeAlimento) {
        try {
            String limpo = limparNomeAlimento(nomeAlimento);
            return foodService.findByName(limpo);
        } catch (ResponseStatusException e) {
            return "Alimento não encontrado na tabela da clínica: " + nomeAlimento;
        }
    }

    @Tool(description = "Consulta o resumo do dia de hoje do paciente: calorias consumidas, meta diária e quantas calorias ainda pode comer hoje.")
    public SummaryResponse resumoDoDia(
            @ToolParam(description = "Data ou período, envie sempre 'hoje'") String data) {
        return mealService.summary(LocalDate.now());
    }

    @Tool(description = "Registra uma refeição consumida pelo paciente no diário alimentar de hoje.")
    public String registrarRefeicao(
            @ToolParam(description = "Nome do alimento no singular, ex: ovo, tapioca, banana") String nomeAlimento,
            @ToolParam(description = "Quantidade em gramas, ex: 100.0") Double gramas,
            @ToolParam(description = "Refeição: BREAKFAST, LUNCH, DINNER ou SNACK") String tipoRefeicao) {
        try {
            double peso = (gramas != null && gramas > 0) ? gramas : 100.0;
            String nomeLimpo = limparNomeAlimento(nomeAlimento);
            MealType mealType = converterMealType(tipoRefeicao);

            MealEntry entry;
            try {
                entry = mealService.log(nomeLimpo, peso, mealType);
            } catch (ResponseStatusException ex) {
                // Se veio "ovo cozido" ou "ovos" e não achou de primeira, pega só a primeira palavra no singular ("ovo")
                String primeiraPalavra = nomeLimpo.split("\\s+")[0].replaceAll("(?i)s$", "");
                entry = mealService.log(primeiraPalavra, peso, mealType);
            }

            return String.format("Refeição registrada com sucesso no diário: %.0fg de %s no %s (%.0f kcal).",
                    entry.getGrams(), entry.getFood().getName(), entry.getMealType(), entry.calories());
        } catch (Exception e) {
            return "Erro ao registrar: " + e.getMessage();
        }
    }

    private String limparNomeAlimento(String raw) {
        if (raw == null || raw.isBlank()) return "Ovo";
        String limpo = raw.replaceAll("(?i)\\b(\\d+|dois|duas|um|uma|tres|três|gramas|g|de|no|na)\\b", "").trim();
        if (limpo.toLowerCase().startsWith("ovos")) {
            limpo = limpo.replaceFirst("(?i)ovos", "ovo");
        }
        return limpo.isBlank() ? raw.trim() : limpo;
    }

    private MealType converterMealType(String tipo) {
        if (tipo == null) return MealType.BREAKFAST;
        String t = tipo.toUpperCase();
        if (t.contains("LUNCH") || t.contains("ALMO")) return MealType.LUNCH;
        if (t.contains("DINNER") || t.contains("JANT")) return MealType.DINNER;
        if (t.contains("SNACK") || t.contains("LANCH")) return MealType.SNACK;
        return MealType.BREAKFAST;
    }
}