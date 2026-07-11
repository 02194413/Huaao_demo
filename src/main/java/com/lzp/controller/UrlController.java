package com.lzp.controller;

import com.lzp.common.Result;
import com.lzp.dto.CreateShortUrlRequest;
import com.lzp.dto.ShortUrlResponse;
import com.lzp.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 短链接 REST 控制器
 */
@Tag(name = "短链接服务", description = "短链接生成、跳转、查询接口")
@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @Operation(summary = "生成短链接", description = "提交长URL，返回对应的短链接")
    @PostMapping("/api/shorten")
    public Result<ShortUrlResponse> shorten(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlService.createShortUrl(request);
        return Result.success("短链接生成成功", response);
    }

    @Operation(summary = "短链接跳转", description = "访问短链接，302重定向到原始URL")
    @GetMapping("/{shortCode}")
    public RedirectView redirect(
            @Parameter(description = "短码") @PathVariable String shortCode) {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        RedirectView redirectView = new RedirectView(originalUrl);
        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }

    @Operation(summary = "查询短链接信息", description = "获取短链接的详细信息和访问统计")
    @GetMapping("/api/info/{shortCode}")
    public Result<ShortUrlResponse> info(@PathVariable String shortCode) {
        ShortUrlResponse response = urlService.getUrlInfo(shortCode);
        return Result.success(response);
    }

    @Operation(summary = "健康检查", description = "服务健康状态")
    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
