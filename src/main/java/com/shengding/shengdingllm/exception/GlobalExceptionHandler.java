package com.shengding.shengdingllm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 捕获客户端错误 (4xx)
    @ExceptionHandler(HttpClientErrorException.class)
    public ModelAndView handleClientError(HttpClientErrorException ex, RedirectAttributes redirectAttributes) {
        return handleRedirectHomePage();
    }

    // 捕获服务端错误 (5xx)
    @ExceptionHandler(HttpServerErrorException.class)
    @ResponseBody
    public R handleServerError(HttpServerErrorException ex) {
        log.error("Server Error: ", ex);
        return R.failed("999999",ex.getMessage());
    }

    @ExceptionHandler(BaseException.class)
    @ResponseBody
    public R handleServerError(BaseException ex) {
        log.error("Server Error: ", ex);
        return R.failed(ex.getCode(),ex.getMessage());
    }

    // 捕获其他错误
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R handleOtherExceptions(Exception ex) {
        log.error("Server Error: ", ex);
        return R.failed("999999",ex.getMessage());
    }

    // 跳转到首页的方法
    private ModelAndView handleRedirectHomePage() {
        return new ModelAndView("redirect:/");
    }
}
