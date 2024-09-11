package com.shengding.shengdingllm.cosntant;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatMessageRoleEnum {

    USER("user"),
    SYSTEM("system"),

    ASSISTANT("assistant");

    private final String value;
}
