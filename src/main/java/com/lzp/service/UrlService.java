package com.lzp.service;

import com.lzp.dto.CreateShortUrlRequest;
import com.lzp.dto.ShortUrlResponse;

/**
 * 短链接服务接口
 */
public interface UrlService {

    /**
     * 创建短链接
     * @param request 请求参数（长URL + 可选自定义短码）
     * @return 短链接响应
     */
    ShortUrlResponse createShortUrl(CreateShortUrlRequest request);

    /**
     * 根据短码获取原始URL（用于重定向）
     * @param shortCode 短码
     * @return 原始长URL
     */
    String getOriginalUrl(String shortCode);

    /**
     * 查询短链接信息
     * @param shortCode 短码
     * @return 短链接详情
     */
    ShortUrlResponse getUrlInfo(String shortCode);
}
