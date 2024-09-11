//package com.shengding.shengdingllm;
//
//
//import com.shengding.shengdingllm.interfaces.api.KimiLLMService;
//import com.shengding.shengdingllm.utils.ResponseManager;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//@SpringBootTest
//@RunWith(SpringRunner.class)
//class ShengdingllmApplicationTests {
//
//    @Test
//    void contextLoads() {
//
//        ResponseManager manager = new ResponseManager();
//        String finalResponse = null;
//
//        new KimiLLMService().sendPrompt("你好", manager::handleUpdate, manager);
//        try {
//            finalResponse = manager.getResponse();
//            System.out.println(finalResponse);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//}
