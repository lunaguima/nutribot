package br.com.fiap.nutribot.model;

import java.util.List;

public record MealAnalysis(
        List<Item> items,
        double totalCalories,
        double protein,
        double carbs,
        double fat) {

    public record Item(String food, double grams, double calories) {
    }
}