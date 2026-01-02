// com.zriyo.aicodemother.model.dto.CodeGenRoute.java
package com.zriyo.aicodemother.model.dto;

import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;

public class CodeGenRoute {
    private CodeGenTypeEnum type;

    // 必须有 getter/setter
    public CodeGenTypeEnum getType() {
        return type;
    }

    public void setType(CodeGenTypeEnum type) {
        this.type = type;
    }
}
