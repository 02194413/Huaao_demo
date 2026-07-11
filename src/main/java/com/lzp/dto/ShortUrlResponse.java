package com.lzp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接响应
 */
@Data
@Builder
public class ShortUrlResponse {

    /** 短链接完整URL */
    private String shortUrl;

    /** 短码 */
    private String shortCode;

    /** 原始长URL */
    private String originalUrl;

    /** 访问次数 */
    private Long visitCount;

    /** 最近访问时间 */
    private LocalDateTime lastVisitAt;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
