package br.com.fiap.nutribot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String RETRY_SYSTEM = """
            Você é a NutriBot, assistente virtual da Clínica Equilíbrio.
            Responda APENAS em português, em texto natural (nunca JSON).
            Se a pergunta não for sobre nutrição ou sobre a clínica, recuse educadamente
            e diga que só pode ajudar com nutrição.
            Você não substitui o nutricionista.
            """;

    private final ChatClient chatClient;

    public String chat(String conversationId, String message) {
        String answer = ask(conversationId, message);

        // Proteção para modelos locais pequenos (ex: llama3.2 3B): às vezes devolvem
        // resposta vazia, "{}" ou uma tool call em JSON cru em vez de texto.
        if (isInvalid(answer)) {
            answer = chatClient.prompt()
                    .system(RETRY_SYSTEM)
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools() // sem tools neste retry
                    .call()
                    .content();
        }

        if (isInvalid(answer)) {
            return "Desculpe, não consegui responder agora. Pode reformular sua pergunta sobre nutrição?";
        }
        return answer;
    }

    private String ask(String conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    private boolean isInvalid(String answer) {
        if (answer == null || answer.isBlank()) return true;
        String t = answer.trim();
        return t.startsWith("{") || t.startsWith("[");
    }
}