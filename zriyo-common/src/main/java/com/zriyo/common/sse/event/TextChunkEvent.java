package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 文本片段事件 (流式输出)
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextChunkEvent extends SseEvent {

    /**
     * 文本片段内容
     */
    private String content;

    /**
     * 是否为第一个片段
     */
    private Boolean isFirst;

    /**
     * 是否为最后一个片段
     */
    private Boolean isLast;

    /**
     * 片段索引
     */
    private Integer index;

    public TextChunkEvent(String traceId, String eventId, String content) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "text_chunk";
        this.timestamp = Instant.now();
        this.content = content;
    }
}
