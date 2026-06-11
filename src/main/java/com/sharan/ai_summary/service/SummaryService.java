package com.sharan.ai_summary.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharan.ai_summary.dto.SummaryResponse;
import com.sharan.ai_summary.entity.Summary;
import com.sharan.ai_summary.repository.SummaryRepository;

@Service
public class SummaryService {

    public static final String CACHE_NAME = "summaries";

    private final GeminiService geminiService;
    private final SummaryRepository summaryRepository;
    private final CacheManager cacheManager;

    public SummaryService(GeminiService geminiService, SummaryRepository summaryRepository,CacheManager cacheManager) {
        this.geminiService = geminiService;
        this.summaryRepository = summaryRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public SummaryResponse summarize(String text) {
        String hash = sha256(text);
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if(cache != null){
            Cache.ValueWrapper wrapper = cache.get(hash);
            if(wrapper !=null && wrapper.get()!=null){
                SummaryResponse cached = (SummaryResponse) wrapper.get();
                return new SummaryResponse(
                    cached.getId(), cached.getSummary(), cached.getModel(), 
                    true, cached.getCreatedAt()
                );
            }
        }

        var existing = summaryRepository.findByContentHash(hash);
        if (existing.isPresent()) {
            Summary s = existing.get();
            SummaryResponse response = new SummaryResponse(
                s.getId(), s.getSummaryText(), s.getModel(), true, s.getCreatedAt()
            );
            if(cache!=null) cache.put(hash,response);
            return new SummaryResponse(
                response.getId(), response.getSummary(), response.getModel(), 
                true, response.getCreatedAt()
            );
        }

        String summaryText = geminiService.summarize(text);

        Summary summary = new Summary();
        summary.setSourceType("TEXT");
        summary.setSourceContent(text);
        summary.setSummaryText(summaryText);
        summary.setModel("gemini-2.5-flash");
        summary.setContentHash(hash);

        Summary saved = summaryRepository.save(summary);
        SummaryResponse response = new SummaryResponse(
            saved.getId(), saved.getSummaryText(), saved.getModel(), false, saved.getCreatedAt()
        );
        if (cache != null)cache.put(hash, response);
        return response;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}