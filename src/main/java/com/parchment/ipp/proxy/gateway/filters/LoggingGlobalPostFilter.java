package com.parchment.ipp.proxy.gateway.filters;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingGlobalPostFilter implements GlobalFilter, Ordered {


    private static final Set<String> LOGGABLE_CONTENT_TYPES = new HashSet<>(
            Arrays.asList(MediaType.APPLICATION_JSON_VALUE.toLowerCase(),
                    MediaType.APPLICATION_JSON_VALUE.toLowerCase(), MediaType.TEXT_PLAIN_VALUE,
                    MediaType.TEXT_XML_VALUE, "application/ipp"));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Set<URI> uris = exchange.getAttributeOrDefault(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, Collections.emptySet());
        String originalUri = uris.isEmpty() ? exchange.getRequest().getURI().toString() : uris.iterator().next().toString();        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI routeUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        log.info("Incoming request " + originalUri + " is routed to id: " + route.getId()
                + ", uri:" + routeUri);

        ServerHttpRequestDecorator requestMutated = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                Logger requestLogger = new Logger(getDelegate());
                if (LOGGABLE_CONTENT_TYPES.contains(String.valueOf(getHeaders().getContentType()).toLowerCase())) {
                    return super.getBody().map(ds -> {
                        requestLogger.appendBody(ds.asByteBuffer());
                        return ds;
                    }).doFinally((s) -> requestLogger.log());
                } else {
                    requestLogger.log();
                    return super.getBody();
                }
            }
        };

        var responseMutated = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                var responseLogger = new Logger(getDelegate());
                if (LOGGABLE_CONTENT_TYPES.contains(String.valueOf(getHeaders().getContentType()).toLowerCase())) {
                    return join(body).flatMap(db -> {
                        responseLogger.appendBody(db.asByteBuffer());
                        responseLogger.log();
                        return getDelegate().writeWith(Mono.just(db));
                    });
                } else {
                    responseLogger.log();
                    return getDelegate().writeWith(body);
                }
            }
        };
        return chain.filter(exchange.mutate().request(requestMutated).response(responseMutated).build());
    }

    private Mono<? extends DataBuffer> join(Publisher<? extends DataBuffer> dataBuffers) {
        Assert.notNull(dataBuffers, "'dataBuffers' must not be null");
        return Flux.from(dataBuffers).collectList().filter((list) -> !list.isEmpty())
                .map((list) -> list.get(0).factory().join(list))
                .doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static class Logger {

        private StringBuilder sb = new StringBuilder();

        Logger(ServerHttpResponse response) {
            sb.append("\n");
            sb.append("---- Response -----").append("\n");
            sb.append("Headers      :").append(response.getHeaders().toSingleValueMap()).append("\n");
            sb.append("Status code  :").append(response.getStatusCode()).append("\n");
        }

        Logger(ServerHttpRequest request) {
            sb.append("\n");
            sb.append("---- Request -----").append("\n");
            sb.append("Headers      :").append(request.getHeaders().toSingleValueMap()).append("\n");
            sb.append("Method       :").append(request.getMethod()).append("\n");
            sb.append("Client       :").append(request.getRemoteAddress()).append("\n");
            sb.append("Address      :").append(request.getRemoteAddress().getHostName()).append("\n");
        }


        void appendBody(ByteBuffer byteBuffer) {
            sb.append("Body         :").append(StandardCharsets.UTF_8.decode(byteBuffer)).append("\n");
        }

        void log() {
            sb.append("-------------------").append("\n");
            log.info(sb.toString());
        }

    }
}