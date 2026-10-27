package br.com.fiap.nutribot.service;

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
            em gramas.
            Use nomes de alimentos em português, no singular quando fizer sentido
            (ex.: "Arroz branco", "Feijão carioca", "Bife grelhado").
            Todos os valores numéricos devem ser números, nunca texto.
            """;

    private final ChatClient chatClient;

    public MealAnalysisService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public MealAnalysis analyze(String description) {
        return chatClient.prompt()
                .system(SYSTEM)
                .user(description)
                .call()
                .entity(MealAnalysis.class);
    }
}