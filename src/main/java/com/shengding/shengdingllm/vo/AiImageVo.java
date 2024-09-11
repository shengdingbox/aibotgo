package com.shengding.shengdingllm.vo;


import lombok.Data;

@Data
public class AiImageVo{

    private Long userId;
    private String uuid;
    private String AiModelName;
    private String prompt;
    private String generateSize;
    private String generateQuality;
    private Integer generateNumber;
    private String originalImage;
    private String maskImage;
    private String respImagesPath;
    private String generatedImages;
    private Integer interactingMethod;
    private Integer processStatus;
}
