package com.lzp.service.impl;

import com.lzp.common.BusinessException;
import com.lzp.dto.CreateShortUrlRequest;
import com.lzp.dto.ShortUrlResponse;
import com.lzp.entity.UrlMapping;
import com.lzp.mapper.UrlMappingMapper;
import com.lzp.service.UrlService;
import com.lzp.service.VisitCountService;
import com.lzp.util.Base62Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 短链接服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlMappingMapper urlMappingMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final VisitCountService visitCountService;

    @Value("${short-url.domain}")
    private String domain;

    @Value("${short-url.code-length:6}")
    private int codeLength;

    private static final Duration REDIS_TTL = Duration.ofHours(24);

    @Override
    @Transactional
    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        String originalUrl = request.getUrl();

        // 1. 检查是否已存在（避免重复生成）
        UrlMapping existing = urlMappingMapper.selectByOriginalUrl(originalUrl);
        if (existing != null) {
            log.info("URL已存在，返回已有短码: {}", existing.getShortCode());
            return buildResponse(existing);
        }

        // 2. 处理自定义短码
        String shortCode;
        if (StringUtils.hasText(request.getCustomCode())) {
            shortCode = request.getCustomCode();
            // 检查自定义短码是否冲突
            UrlMapping conflict = urlMappingMapper.selectByShortCode(shortCode);
            if (conflict != null) {
                throw new BusinessException("自定义短码已存在，请更换");
            }
        } else {
            shortCode = null;
        }

        // 3. 保存到数据库获取自增ID
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(originalUrl);
        mapping.setShortCode(shortCode != null ? shortCode : "");
        mapping.setVisitCount(0L);
        mapping.setCreatedAt(LocalDateTime.now());
        urlMappingMapper.insert(mapping);

        // 4. 如果没有自定义短码，用自增ID生成
        if (shortCode == null) {
            shortCode = Base62Util.encodeWithPadding(mapping.getId(), codeLength);
            mapping.setShortCode(shortCode);
            urlMappingMapper.updateById(mapping);
        }

        // 5. 缓存到 Redis（格式: id|url，避免URL中冒号冲突）
        redisTemplate.opsForValue().set(shortCode, mapping.getId() + "|" + originalUrl, REDIS_TTL);

        log.info("短链生成成功: {} -> {}", shortCode, originalUrl);
        return buildResponse(mapping);
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        // 1. 先从 Redis 查（格式: id|url）
        String cachedValue = redisTemplate.opsForValue().get(shortCode);
        if (StringUtils.hasText(cachedValue)) {
            int pipeIndex = cachedValue.indexOf('|');
            if (pipeIndex > 0) {
                try {
                    long id = Long.parseLong(cachedValue.substring(0, pipeIndex));
                    visitCountService.increment(id);
                } catch (NumberFormatException e) {
                    log.warn("Redis缓存ID解析失败: {}", cachedValue);
                }
                return cachedValue.substring(pipeIndex + 1);
            }
            return cachedValue;
        }

        // 2. Redis 未命中，查数据库
        UrlMapping mapping = urlMappingMapper.selectByShortCode(shortCode);
        if (mapping == null) {
            throw new BusinessException(404, "短链接不存在");
        }

        // 3. 检查是否过期
        if (mapping.getExpireAt() != null && mapping.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(410, "短链接已过期");
        }

        // 4. 回写 Redis（id|url 格式）
        redisTemplate.opsForValue().set(shortCode,
                mapping.getId() + "|" + mapping.getOriginalUrl(), REDIS_TTL);

        // 5. 异步更新访问计数
        visitCountService.increment(mapping.getId());

        return mapping.getOriginalUrl();
    }

    @Override
    public ShortUrlResponse getUrlInfo(String shortCode) {
        UrlMapping mapping = urlMappingMapper.selectByShortCode(shortCode);
        if (mapping == null) {
            throw new BusinessException(404, "短链接不存在");
        }
        return buildResponse(mapping);
    }

    /**
     * 构建响应对象
     */
    private ShortUrlResponse buildResponse(UrlMapping mapping) {
        return ShortUrlResponse.builder()
                .shortUrl(domain + "/" + mapping.getShortCode())
                .shortCode(mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .visitCount(mapping.getVisitCount())
                .lastVisitAt(mapping.getLastVisitAt())
                .expireAt(mapping.getExpireAt())
                .createdAt(mapping.getCreatedAt())
                .build();
    }
}
