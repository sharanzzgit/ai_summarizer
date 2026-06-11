package com.sharan.ai_summary.dto;

import java.time.Instant;

public class SummaryResponse {

    private Long id;
    private String summary;
    private String model;
    private boolean cached;
    private Instant createdAt;

    public SummaryResponse() {}

    public SummaryResponse(Long id, String summary, String model, boolean cached, Instant createdAt) {
        this.id = id;
        this.summary = summary;
        this.model = model;
        this.cached = cached;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}