package br.com.fiap.nutribot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    public String chat(String conversationId, String message) {
        String answer = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        // Proteção para modelos locais pequenos (ex: llama3.2 3B) que às vezes
        // devolvem tool call em JSON bruto para perguntas simples de memória/conversa:
        if (answer != null && answer.trim().startsWith("{") && answer.contains("\"name\"")) {
            answer = chatClient.prompt()
                    .system("Responda à pergunta do usuário APENAS em português com uma frase amigável e direta, usando o histórico da conversa. É proibido responder em formato JSON.")
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools() // desativa tools apenas neste retry de alucinação JSON
                    .call()
                    .content();
        }

        return answer;
    }
}