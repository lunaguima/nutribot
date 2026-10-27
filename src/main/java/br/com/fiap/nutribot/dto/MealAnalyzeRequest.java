package br.com.fiap.nutribot.dto;

import jakarta.validation.constraints.NotBlank;

public record MealAnalyzeRequest(
        @NotBlank(message = "description is required")
        String description
) {}