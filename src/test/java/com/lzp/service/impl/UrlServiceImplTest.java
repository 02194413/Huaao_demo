package com.lzp.service.impl;

import com.lzp.common.BusinessException;
import com.lzp.dto.CreateShortUrlRequest;
import com.lzp.dto.ShortUrlResponse;
import com.lzp.entity.UrlMapping;
import com.lzp.mapper.UrlMappingMapper;
import com.lzp.service.VisitCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UrlServiceImplTest {

    @Mock
    private UrlMappingMapper urlMappingMapper;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private VisitCountService visitCountService;

    @InjectMocks
    private UrlServiceImpl urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "domain", "http://localhost:8080");
        ReflectionTestUtils.setField(urlService, "codeLength", 6);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== createShortUrl ====================

    @Test
    void createShortUrl_newUrl_shouldGenerateShortCode() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://www.example.com");

        when(urlMappingMapper.selectByOriginalUrl(anyString())).thenReturn(null);
        when(urlMappingMapper.insert(any(UrlMapping.class))).thenAnswer(inv -> {
            UrlMapping m = inv.getArgument(0);
            m.setId(1L);
            return 1;
        });
        when(urlMappingMapper.updateById(any(UrlMapping.class))).thenReturn(1);

        ShortUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("https://www.example.com", response.getOriginalUrl());
        assertNotNull(response.getShortCode());
        assertEquals(6, response.getShortCode().length());
        assertTrue(response.getShortUrl().startsWith("http://localhost:8080/"));

        // Redis 缓存格式: id:url
        verify(valueOperations).set(
                eq(response.getShortCode()),
                eq("1|https://www.example.com"),
                any(Duration.class)
        );
    }

    @Test
    void createShortUrl_existingUrl_shouldReturnExisting() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://www.example.com");

        UrlMapping existing = new UrlMapping();
        existing.setId(1L);
        existing.setShortCode("abc123");
        existing.setOriginalUrl("https://www.example.com");
        existing.setVisitCount(10L);
        existing.setCreatedAt(LocalDateTime.now());

        when(urlMappingMapper.selectByOriginalUrl(anyString())).thenReturn(existing);

        ShortUrlResponse response = urlService.createShortUrl(request);

        assertEquals("abc123", response.getShortCode());
        assertEquals(10L, response.getVisitCount());
        verify(urlMappingMapper, never()).insert(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_customCode_shouldUseCustomCode() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://www.example.com");
        request.setCustomCode("mycode");

        when(urlMappingMapper.selectByOriginalUrl(anyString())).thenReturn(null);
        when(urlMappingMapper.selectByShortCode("mycode")).thenReturn(null);
        when(urlMappingMapper.insert(any(UrlMapping.class))).thenReturn(1);

        ShortUrlResponse response = urlService.createShortUrl(request);

        assertEquals("mycode", response.getShortCode());
    }

    @Test
    void createShortUrl_duplicateCustomCode_shouldThrowException() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://www.example.com");
        request.setCustomCode("taken");

        when(urlMappingMapper.selectByOriginalUrl(anyString())).thenReturn(null);
        UrlMapping conflict = new UrlMapping();
        conflict.setShortCode("taken");
        when(urlMappingMapper.selectByShortCode("taken")).thenReturn(conflict);

        assertThrows(BusinessException.class, () -> urlService.createShortUrl(request));
    }

    // ==================== getOriginalUrl ====================

    @Test
    void getOriginalUrl_cacheHit_shouldReturnFromCache() {
        when(valueOperations.get("abc123")).thenReturn("1|https://cached.example.com");

        String result = urlService.getOriginalUrl("abc123");

        assertEquals("https://cached.example.com", result);
        verify(urlMappingMapper, never()).selectByShortCode(anyString());
        verify(visitCountService).increment(1L);
    }

    @Test
    void getOriginalUrl_cacheHit_directUrl_shouldStillWork() {
        // 纯URL格式（无 | 分隔符）直接返回
        when(valueOperations.get("plain")).thenReturn("https://plain.example.com");

        String result = urlService.getOriginalUrl("plain");

        assertEquals("https://plain.example.com", result);
    }

    @Test
    void getOriginalUrl_cacheMiss_shouldQueryDb() {
        when(valueOperations.get("abc123")).thenReturn(null);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode("abc123");
        mapping.setOriginalUrl("https://www.example.com");
        when(urlMappingMapper.selectByShortCode("abc123")).thenReturn(mapping);

        String result = urlService.getOriginalUrl("abc123");

        assertEquals("https://www.example.com", result);
        // 回写 Redis（id|url 格式）
        verify(valueOperations).set(eq("abc123"), eq("1|https://www.example.com"), any(Duration.class));
        verify(visitCountService).increment(1L);
    }

    @Test
    void getOriginalUrl_nonexistentCode_shouldThrowException() {
        when(valueOperations.get("notexist")).thenReturn(null);
        when(urlMappingMapper.selectByShortCode("notexist")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> urlService.getOriginalUrl("notexist"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getOriginalUrl_expired_shouldThrowException() {
        when(valueOperations.get("expired")).thenReturn(null);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode("expired");
        mapping.setOriginalUrl("https://www.example.com");
        mapping.setExpireAt(LocalDateTime.now().minusDays(1));
        when(urlMappingMapper.selectByShortCode("expired")).thenReturn(mapping);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> urlService.getOriginalUrl("expired"));
        assertEquals(410, ex.getCode());
    }

    // ==================== getUrlInfo ====================

    @Test
    void getUrlInfo_existingCode_shouldReturnInfo() {
        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode("abc123");
        mapping.setOriginalUrl("https://www.example.com");
        mapping.setVisitCount(50L);
        mapping.setLastVisitAt(LocalDateTime.now().minusHours(1));
        mapping.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(urlMappingMapper.selectByShortCode("abc123")).thenReturn(mapping);

        ShortUrlResponse response = urlService.getUrlInfo("abc123");

        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals(50L, response.getVisitCount());
    }

    @Test
    void getUrlInfo_nonexistentCode_shouldThrowException() {
        when(urlMappingMapper.selectByShortCode("notexist")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> urlService.getUrlInfo("notexist"));
        assertEquals(404, ex.getCode());
    }
}
