package com.shengding.shengdingllm.interfaces.impl;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ResponseManager {

    private final AtomicReference<String> response = new AtomicReference<>("");
    private final CountDownLatch latch = new CountDownLatch(1);

    public void handleUpdate(Object callbackParam, Map<String, Object> update) {
        boolean isEnd = (Boolean) update.get("done");
        String text = (String) update.get("content");

        if (text != null) {
            response.set(text);
        }

        if (isEnd) {
            latch.countDown(); // Signal that the response is complete.
        }
    }

    public String getResponse() throws InterruptedException {
        latch.await(); // Wait for the response to be completed.
        return response.get();
    }
}