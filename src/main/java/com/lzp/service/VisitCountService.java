package com.lzp.service;

import com.lzp.mapper.UrlMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 访问计数服务（异步更新）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitCountService {

    private final UrlMappingMapper urlMappingMapper;

    @Async
    public void increment(Long id) {
        try {
            urlMappingMapper.incrementVisitCount(id);
        } catch (Exception e) {
            log.error("更新访问计数失败: id={}", id, e);
        }
    }
}
