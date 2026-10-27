package br.com.fiap.nutribot.controller;

import java.time.LocalDate;
import java.util.List;

import br.com.fiap.nutribot.dto.MealAnalyzeRequest;
import br.com.fiap.nutribot.dto.MealEntryRequest;
import br.com.fiap.nutribot.dto.MealEntryResponse;
import br.com.fiap.nutribot.model.MealAnalysis;
import br.com.fiap.nutribot.service.MealAnalysisService;
import br.com.fiap.nutribot.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final MealAnalysisService mealAnalysisService;

    @GetMapping
    public List<MealEntryResponse> findByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return mealService.findByDate(date).stream()
                .map(MealEntryResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealEntryResponse create(@RequestBody @Valid MealEntryRequest request) {
        return MealEntryResponse.from(mealService.create(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        mealService.delete(id);
    }

    @PostMapping("/analyze")
    public MealAnalysis analyze(@RequestBody @Valid MealAnalyzeRequest request) {
        return mealAnalysisService.analyze(request.description());
    }
}