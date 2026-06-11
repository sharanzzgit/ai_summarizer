package com.sharan.ai_summary.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sharan.ai_summary.dto.SummarizeRequest;
import com.sharan.ai_summary.dto.SummaryResponse;
import com.sharan.ai_summary.service.SummaryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class SummaryController {
    
    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService){
        this.summaryService = summaryService;
    }

    @PostMapping("/summarize")
    public SummaryResponse summarize(@Valid @RequestBody SummarizeRequest request){
        return summaryService.summarize(request.getText());
    }
}
