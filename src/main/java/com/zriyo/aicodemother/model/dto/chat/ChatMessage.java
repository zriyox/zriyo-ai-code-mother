package com.zriyo.aicodemother.model.dto.chat;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话历史 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @Column("id")
    @NotNull(message = "id不能为空")
    private Long id;


    /**
     * 消息
     */
    @NotBlank(message = "消息不能为空")
    private String message;

    /**
     * user/ai
     */
    @Column("messageType")
    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    /**
     * 应用id
     */
    @Column("appId")
    @NotNull(message = "应用id不能为空")
    private Long appId;


    @Column("user_visible")
    private int userVisible;


    private String metaData;
}
