package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 文件生成/更新事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileEvent extends SseEvent {

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件类型 (extension)
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long size;

    /**
     * 操作类型 (created, updated, deleted)
     */
    private String action;

    /**
     * 文件内容摘要
     */
    private String summary;

    public FileEvent(String traceId, String eventId, String path, String action) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = action.equals("created") ? "file_created" : "file_updated";
        this.timestamp = Instant.now();
        this.path = path;
        this.action = action;
    }
}
