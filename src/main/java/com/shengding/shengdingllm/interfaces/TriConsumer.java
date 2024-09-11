package com.shengding.shengdingllm.interfaces;

@FunctionalInterface
public interface TriConsumer<T, U> {
    void accept(T t, U u);
}
