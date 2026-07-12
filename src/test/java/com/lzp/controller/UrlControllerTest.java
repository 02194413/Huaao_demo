package com.lzp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lzp.common.BusinessException;
import com.lzp.dto.CreateShortUrlRequest;
import com.lzp.dto.ShortUrlResponse;
import com.lzp.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = UrlController.class,
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class
    }
)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlService urlService;

    @Test
    void shorten_validUrl_shouldReturn200() throws Exception {
        ShortUrlResponse response = ShortUrlResponse.builder()
                .shortUrl("http://localhost:8080/abc123")
                .shortCode("abc123")
                .originalUrl("https://www.example.com")
                .visitCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
        when(urlService.createShortUrl(any())).thenReturn(response);

        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("https://www.example.com");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shortCode").value("abc123"));
    }

    @Test
    void shorten_emptyUrl_shouldReturn400() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shorten_invalidUrlFormat_shouldReturn400() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setUrl("ftp://invalid-protocol.com");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirect_validCode_shouldReturn302() throws Exception {
        when(urlService.getOriginalUrl("abc123")).thenReturn("https://www.example.com");

        mockMvc.perform(get("/abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://www.example.com"));
    }

    @Test
    void redirect_nonexistentCode_shouldReturn400() throws Exception {
        when(urlService.getOriginalUrl("notexist"))
                .thenThrow(new BusinessException(404, "短链接不存在"));

        mockMvc.perform(get("/notexist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void info_validCode_shouldReturn200() throws Exception {
        ShortUrlResponse response = ShortUrlResponse.builder()
                .shortUrl("http://localhost:8080/abc123")
                .shortCode("abc123")
                .originalUrl("https://www.example.com")
                .visitCount(5L)
                .createdAt(LocalDateTime.now())
                .build();
        when(urlService.getUrlInfo("abc123")).thenReturn(response);

        mockMvc.perform(get("/api/info/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.visitCount").value(5));
    }

    @Test
    void info_nonexistentCode_shouldReturn400() throws Exception {
        when(urlService.getUrlInfo("notexist"))
                .thenThrow(new BusinessException(404, "短链接不存在"));

        mockMvc.perform(get("/api/info/notexist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("OK"));
    }
}
