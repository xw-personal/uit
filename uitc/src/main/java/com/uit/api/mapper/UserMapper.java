package com.uit.api.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.uit.api.entry.User;

@Mapper
public interface UserMapper {


    @Select("select * from users where account=#{account}")
    User getByAccount(String account);

    @Insert("insert into users(id,username,account,password) values(#{id},#{username},#{account},#{password})")
    void save(User user);
}
