package br.com.fiap.nutribot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    // ChatClient montado uma vez na config (system prompt, memória, tools e RAG)
    private final ChatClient chatClient;

    public String chat(String conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                // a memória separa as conversas pelo conversationId
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}