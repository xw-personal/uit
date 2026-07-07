package com.uit.api.service.impl;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.uit.api.entry.User;
import com.uit.api.handler.BusinessException;
import com.uit.api.mapper.UserMapper;
import com.uit.api.service.UserService;
import com.uit.api.utils.JwtUtils;
import com.uit.api.vo.UserLoginVO;

import io.github.robsonkades.uuidv7.UUIDv7;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper,PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserLoginVO login(User user) {
        User u = userMapper.getByAccount(user.getAccount());
        if (u == null){
            throw new BusinessException(403,"用户不存在");
        }
        if (!passwordEncoder.matches(user.getPassword(), u.getPassword())){
            throw new BusinessException(403,"用户名或密码不存在");
        }
        log.info("user: "+u);
        String token = JwtUtils.generateToken(u.getId().toString(), u.getUsername());
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setToken(token);
        userLoginVO.setUsername(u.getUsername());
        userLoginVO.setUserId(u.getId().toString());
        return userLoginVO;
    }

    public void register(User user){
        UUID uuid = UUIDv7.randomUUID();
        user.setId(uuid);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.save(user);
    }

}
