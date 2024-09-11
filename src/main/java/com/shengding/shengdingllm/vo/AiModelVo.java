package com.shengding.shengdingllm.vo;

import lombok.Data;

@Data
public class AiModelVo{

    private Long id;
    private String type;
    private String name;
    private String platform;
    private String setting;
    private String remark;
    private Boolean isEnable;
    private Integer contextWindow;
    private String llmType;
}
