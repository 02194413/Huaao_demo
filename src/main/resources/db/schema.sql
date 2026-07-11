DROP TABLE IF EXISTS `url_mapping`;
CREATE TABLE `url_mapping` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `short_code`    VARCHAR(10)  NOT NULL COMMENT '短码(Base62编码)',
    `original_url`  TEXT         NOT NULL COMMENT '原始长URL',
    `visit_count`   BIGINT       NOT NULL DEFAULT 0 COMMENT '总访问次数',
    `last_visit_at` DATETIME     NULL COMMENT '最近访问时间',
    `expire_at`     DATETIME     NULL COMMENT '过期时间(可选)',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`),
    KEY `idx_original_url` (`original_url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接映射表';
