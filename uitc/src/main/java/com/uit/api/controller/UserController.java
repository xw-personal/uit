package com.uit.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.api.entry.Result;
import com.uit.api.entry.User;
import com.uit.api.service.UserService;
import com.uit.api.vo.UserLoginVO;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("login")
    public Result<UserLoginVO> login(@RequestBody User user) {
        UserLoginVO userLoginVO = userService.login(user);
        return Result.success(userLoginVO);
    }
    
    @PostMapping("register")
    public Result register(@RequestBody User user) {
        userService.register(user);
        return Result.success();
    }
    
}
