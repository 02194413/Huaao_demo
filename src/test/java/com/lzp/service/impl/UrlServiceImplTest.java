package com.lzp.service.impl;

import com.lzp.common.BusinessException;
import com.lzp.dto.CreateShortUrlRequest;
import com.lzp.dto.ShortUrlResponse;
import com.lzp.entity.UrlMapping;
import com.lzp.mapper.UrlMappingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

        // 验证 Redis 缓存写入
        verify(valueOperations).set(
                eq(response.getShortCode()),
                eq("https://www.example.com"),
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
        assertEquals("https://www.example.com", response.getOriginalUrl());
        assertEquals(10L, response.getVisitCount());
        // 不会生成新的插入
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
        when(valueOperations.get("abc123")).thenReturn("https://cached.example.com");

        String result = urlService.getOriginalUrl("abc123");

        assertEquals("https://cached.example.com", result);
        // 不会查询数据库
        verify(urlMappingMapper, never()).selectByShortCode(anyString());
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
        // 应该回写 Redis
        verify(valueOperations).set(eq("abc123"), eq("https://www.example.com"), any(Duration.class));
    }

    @Test
    void getOriginalUrl_nonexistentCode_shouldThrowException() {
        when(valueOperations.get("notexist")).thenReturn(null);
        when(urlMappingMapper.selectByShortCode("notexist")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> urlService.getOriginalUrl("notexist"));
        assertEquals(404, ex.getCode());
        assertEquals("短链接不存在", ex.getMessage());
    }

    @Test
    void getOriginalUrl_expired_shouldThrowException() {
        when(valueOperations.get("expired")).thenReturn(null);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(1L);
        mapping.setShortCode("expired");
        mapping.setOriginalUrl("https://www.example.com");
        mapping.setExpireAt(LocalDateTime.now().minusDays(1)); // 已过期
        when(urlMappingMapper.selectByShortCode("expired")).thenReturn(mapping);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> urlService.getOriginalUrl("expired"));
        assertEquals(410, ex.getCode());
        assertEquals("短链接已过期", ex.getMessage());
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
        assertEquals("https://www.example.com", response.getOriginalUrl());
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
