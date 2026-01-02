package com.zriyo.aicodemother.model.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageData {
    private String type;
    private String data;


    public static MessageData doneOf() {
        return MessageData.builder().type(StreamMessageTypeEnum.AI_DONE.getValue()).data(StreamMessageTypeEnum.AI_DONE.getText()).build();
    }



}
