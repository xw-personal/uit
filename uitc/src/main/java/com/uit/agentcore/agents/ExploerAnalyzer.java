package com.uit.agentcore.agents;

import com.uit.api.common.LoginType;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.Data;

public interface ExploerAnalyzer {
    @SystemMessage("""
        你是登录表单分析助手。给你一个页面的可交互元素清单(JSON 数组,每个元素含
        tag/type/id/name/className/placeholder/text/ariaLabel/xpath/css)。
        请判断登录类型并指认各字段对应的元素。只返回 JSON,符合给定 schema。
        loginType 取值:PASSWORD_ONLY / PASSWORD_CAPTCHA / SMS / QRCODE / SLIDER。
        各 loginType 需要填的字段(其余字段填 null):
        - PASSWORD_ONLY:    username, password, submit
        - PASSWORD_CAPTCHA: username, password, captchaInput, captchaImage, submit
        - SMS:              phone, smsCodeInput, sendCodeButton, submit (无图形验证码时 captchaImage=null)
        - QRCODE:           submit (扫码无需输入框,二维码图可选填 captchaImage)
        - SLIDER:           submit (滑块容器填 captchaImage,isSliderCaptcha=true)

        字段说明:
        - username: 账号输入框(用户名/邮箱/账号)
        - phone: 手机号输入框(type=tel 或 placeholder 含"手机/电话")
        - password: 密码输入框(type=password)
        - captchaInput: 图形验证码输入框
        - captchaImage: 图形验证码图片(或二维码图、滑块容器)
        - smsCodeInput: 短信验证码输入框(placeholder 含"验证码/动态码",且非图形验证码)
        - sendCodeButton: "获取验证码/发送验证码"按钮
        - submit: 登录/提交按钮
        """)
    LoginAnalysis analyze(@UserMessage String elementsJson);
    // 用 dev.langchain4j 的结构化输出:LoginAnalysis 是 record,
    // 配合 @JsonClassDescription 或返回类型推断,AiServices 自动要求 LLM 按 schema 输出
    public record LoginAnalysis(
      LoginType loginType,
      Fields fields
    ) {}
    public record Fields(
        ElementRef username,      // {xpath, css}
        ElementRef password,
        ElementRef phone,
        ElementRef captchaInput,
        ElementRef captchaImage,
        Boolean isSliderCaptcha,  // 滑块类需另走推流
        ElementRef smsCodeInput,
        ElementRef sendCodeButton,
        ElementRef submit
    ) {}
    public record ElementRef(String xpath, String css) {}

}
