package com.example.restproxy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

@RestController
public class ProxyController {

    @Value("${target.base.url:http://localhost:8080}")
    private String targetBaseUrl;

    @Autowired
    private WebClient webClient;

    @RequestMapping("/proxy-api/**")
    public Mono<ResponseEntity<String>> proxy(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String proxyPath = path.replaceFirst("/proxy-api", "/3rd-server");
        String targetUrl = targetBaseUrl + proxyPath;
        HttpMethod method = request.getMethod();

        // 构建 WebClient 请求（无论是否有 body）
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.method(method)
                .uri(targetUrl)
                .headers(h -> {
                    h.addAll(request.getHeaders());
                });

        WebClient.RequestHeadersSpec<?> bodySpec;
        if (hasBody(method)) {
            bodySpec = ((WebClient.RequestBodySpec) requestSpec).body(request.getBody(), DataBuffer.class);
        } else {
            bodySpec = requestSpec;
        }

        // 发送请求并处理响应
        return bodySpec
                .retrieve()
                .toEntity(String.class)
                .map(response -> {
                    String modifiedBody = modifyResponseBody(response.getBody());
                    return ResponseEntity
                            .status(response.getStatusCode())
                            .headers(response.getHeaders()) // 防止引用修改
                            .body(modifiedBody);
                })
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(ex.getStatusCode())
                                .body(ex.getResponseBodyAsString()))
                )
                .onErrorResume(Exception.class, ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Proxy error: " + ex.getMessage()))
                );
    }

    // 判断是否为有请求体的 HTTP 方法
    private boolean hasBody(HttpMethod method) {
        return HttpMethod.POST.equals(method) ||
                HttpMethod.PUT.equals(method) ||
                HttpMethod.PATCH.equals(method);
    }

    private String modifyResponseBody(String body) {
        if (body == null) return null;
        return body.replace("}", ",\"extra\":\"added by proxy\"}");
    }
}