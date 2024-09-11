package com.shengding.shengdingllm.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 封装返回结果对象
 *
 * @author dinghao
 * @date 2021/2/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = -7583882240838909179L;
    private T data;
    private Integer code;
    private String msg;

    public static <T> R<T> succeed(String data) {
        return of((T)data, 200, null);
    }

    public static <T> R<T> succeed(T model, String msg) {
        return of(model, 200, msg);
    }

    public static <T> R<T> succeed(T model, Integer code, String msg) {
        return of(model, code, msg);
    }

    public static <T> R<T> succeed(T model) {
        return of(model, 200, "");
    }
    public static  R succeed() {
        return of(null, 200, "");
    }

    public static <T> R<T> of(T datas, Integer code, String msg) {
        return new R<>(datas, code, msg);
    }


    public static <T> R<T> failed(String msg) {
        return of(null, 500, msg);
    }

    public static <T> R<T> failed(Integer code, String msg) {

        return of(null, code, msg);
    }

    public static <T> R<T> failed(T model, String msg) {
        return of(model, 500, msg);
    }
}
