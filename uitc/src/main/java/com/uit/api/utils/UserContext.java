package com.uit.api.utils;

import com.uit.api.entry.UserInfo;

public class UserContext {


    private static final ThreadLocal<UserInfo> userContext = new ThreadLocal<>();

    public static void setUser(UserInfo user) {

        userContext.set(user);
    }

    public static UserInfo getUser() {
        return userContext.get();
    }

    public static void clear() {
        userContext.remove(); // 必须清除！
    }


}
