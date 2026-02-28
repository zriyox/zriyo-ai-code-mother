package com.zriyo.common.util;

import com.alibaba.fastjson2.JSON;
import com.zriyo.common.sse.event.SseEvent;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * SSE 通用工具类（基于 WebFlux + Flux）
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
public final class SseUtils {

    private SseUtils() {
    }

    public static ServerSentEvent<String> toServerSentEvent(SseEvent event) {
        String data = JSON.toJSONString(event);
        return ServerSentEvent.<String>builder()
                .id(event.getEventId())
                .event(event.getSseEventName())
                .data(data)
                .build();
    }

    public static Flux<ServerSentEvent<String>> toSseFlux(Flux<SseEvent> eventFlux) {
        return eventFlux.map(SseUtils::toServerSentEvent);
    }

    public static Flux<ServerSentEvent<String>> withHeartbeat(
            Flux<ServerSentEvent<String>> source,
            Duration interval
    ) {
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(interval)
                .map(i -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("ping")
                        .build());
        return Flux.merge(source, heartbeat);
    }
}
