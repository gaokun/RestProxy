package com.example.restproxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @GetMapping("/admin-api/test")
    public String getHelloWorld() {
        return "hello world admin";
    }
}