package com.example.restproxy.controller;

import com.example.restproxy.po.Response;
import com.example.restproxy.po.Success;
import com.example.restproxy.po.UserPo;
import org.springframework.web.bind.annotation.*;

@RestController
public class ThirdServerController {

    @GetMapping("/3rd-server/user")
    public UserPo getTest(@RequestHeader("Token") String token) {
        UserPo user = new UserPo();
        user.setName("[3rd-server] Token: " + token);
        user.setAge(28);
        return user;
    }

    @PostMapping("/3rd-server/user")
    public Response postTest(@RequestHeader("Token") String token, @RequestBody UserPo user) {
        System.out.println("POST user: " + user);
        System.out.println("POST token: " + token);
        return new Success();
    }
}