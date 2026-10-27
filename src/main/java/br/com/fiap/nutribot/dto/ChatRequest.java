package br.com.fiap.nutribot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "conversationId is required")
        String conversationId,

        @NotBlank(message = "message is required")
        String message
) {}