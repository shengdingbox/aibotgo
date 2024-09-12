package com.shengding.shengdingllm.api;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class    IndexWeb {

    /**
     * 主页
     */
    @GetMapping({"", "/"})
    public String index(Model model) throws IOException {
        return "index";
    }
    /**
     * 主页
     */
    @GetMapping("/login.html")
    public String getLog(Model model) {
        return "login";
    }
}
