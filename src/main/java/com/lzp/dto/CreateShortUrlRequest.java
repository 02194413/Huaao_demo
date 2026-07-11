package com.lzp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建短链接请求
 */
@Data
public class CreateShortUrlRequest {

    @NotBlank(message = "原始URL不能为空")
    @Pattern(regexp = "^https?://.*$", message = "URL必须以http://或https://开头")
    private String url;

    /** 自定义短码（可选） */
    private String customCode;
}
