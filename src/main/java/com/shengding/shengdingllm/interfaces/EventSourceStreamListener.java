package com.shengding.shengdingllm.interfaces;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;

public abstract class EventSourceStreamListener extends EventSourceListener {


    protected Request request;
    protected OkHttpClient okHttpClient;

    public EventSourceStreamListener(OkHttpClient okHttpClient, Request request) {
        this.request = request;
        this.okHttpClient = okHttpClient;
        EventSource.Factory factory = EventSources.createFactory(okHttpClient);
        factory.newEventSource(request, this);
    }


    @Override
    public void onOpen(EventSource eventSource, Response response) {
        super.onOpen(eventSource, response);
    }

    @Override
    public void onClosed(EventSource eventSource) {
        super.onClosed(eventSource);
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (t != null) {
            System.err.println("Error occurred: " + t.getMessage());
        }
        if (response != null) {
            try {
                System.out.println(response.body().source().readUtf8Line());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.err.println("Response code: " + response.code());
        }
        super.onFailure(eventSource, t, response);
    }

    @Override
    public abstract void onEvent(EventSource eventSource,  String id,  String type, String eventData);
}
