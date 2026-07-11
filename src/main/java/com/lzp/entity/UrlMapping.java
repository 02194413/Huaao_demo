package com.lzp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接映射实体
 */
@Data
@TableName("url_mapping")
public class UrlMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 短码(Base62编码) */
    private String shortCode;

    /** 原始长URL */
    private String originalUrl;

    /** 总访问次数 */
    private Long visitCount;

    /** 最近访问时间 */
    private LocalDateTime lastVisitAt;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
