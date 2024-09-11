package com.shengding.shengdingllm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootstrapApplication.class, args);
//        AiModelSettingService aiModelSettingService1 = new AiModelSettingService();
//        aiModelSettingService1.initLLMServiceList();
    }

}
