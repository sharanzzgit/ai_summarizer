package com.sharan.ai_summary.service;

import com.sharan.ai_summary.dto.SummaryResponse;
import com.sharan.ai_summary.entity.Summary;
import com.sharan.ai_summary.repository.SummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class SummaryServiceTest {
    
    @Mock private GeminiService geminiService;
    @Mock private SummaryRepository summaryRepository;
    @Mock private CacheManager cacheManager;

    @InjectMocks private SummaryService summaryService;

    private Cache cache;
    private final String sampleText = "This is some sample text long enough to summarize meaningfully.";

    @BeforeEach
    void setup(){
        cache = mock(Cache.class);
        when(cacheManager.getCache("summaries")).thenReturn(cache);
    }

    @Test
    void returnsCachedResponse_whenRedisHit() {
        SummaryResponse cached = new SummaryResponse(
            1L, "cached summary", "gemini-2.5-flash", false, Instant.now()
        );
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(wrapper.get()).thenReturn(cached);
        when(cache.get(anyString())).thenReturn(wrapper);

        SummaryResponse result = summaryService.summarize(sampleText);

        assertThat(result.isCached()).isTrue();
        assertThat(result.getSummary()).isEqualTo("cached summary");
        verify(summaryRepository, never()).findByContentHash(anyString());
        verify(geminiService, never()).summarize(anyString());
    }

    @Test
    void retursDbResponse_andWarmsCache_whenRedisMiss_butDbHit() {
        when(cache.get(anyString())).thenReturn(null);

        Summary existing = new Summary();
        existing.setId(42L);
        existing.setSummaryText("from database");
        existing.setModel("gemini-2.5-flash");
        existing.setCreatedAt(Instant.now());
        when(summaryRepository.findByContentHash(anyString())).thenReturn(Optional.of(existing));

        SummaryResponse result = summaryService.summarize(sampleText);

        assertThat(result.isCached()).isTrue();
        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getSummary()).isEqualTo("from database");
        verify(cache, times(1)).put(anyString(),any(SummaryResponse.class));
        verify(geminiService, never()).summarize(anyString());
    }

    @Test
    void callsGemini_savesCache_whenFullMiss(){
        when(cache.get(anyString())).thenReturn(null);
        when(summaryRepository.findByContentHash(anyString())).thenReturn(Optional.empty());
        when(geminiService.summarize(anyString())).thenReturn("fresh summary getting from gemini");

        Summary saved = new Summary();
        saved.setId(100L);
        saved.setSummaryText("fresh summary from gemini");
        saved.setModel("gemini-2.5-flash");
        saved.setCreatedAt(Instant.now());
        when(summaryRepository.save(any(Summary.class))).thenReturn(saved);

        SummaryResponse result = summaryService.summarize(sampleText);

        assertThat(result.isCached()).isFalse();
        assertThat(result.getSummary()).isEqualTo("fresh summary from gemini");
        verify(geminiService, times(1)).summarize(sampleText);
        verify(summaryRepository, times(1)).save(any(Summary.class));
        verify(cache, times(1)).put(anyString(), any(SummaryResponse.class));
    }
}
