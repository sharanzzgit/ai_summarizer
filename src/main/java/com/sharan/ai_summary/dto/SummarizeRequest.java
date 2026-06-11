package com.sharan.ai_summary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SummarizeRequest {
    
    @NotBlank(message ="text must not be empty")
    @Size(min=50, max=50000, message="text must be 50-50000 characters")
    private String text;

    public String getText(){return text;}
    public void setText(String text){this.text = text;}

}
