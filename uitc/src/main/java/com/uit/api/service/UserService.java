package com.uit.api.service;

import com.uit.api.entry.User;
import com.uit.api.vo.UserLoginVO;

public interface UserService {

    UserLoginVO login(User user);

    void register(User user);
}   
