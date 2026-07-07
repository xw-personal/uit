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
        ElementRef captchaInput,
        ElementRef captchaImage,
        Boolean isSliderCaptcha,  // 滑块类需另走推流
        ElementRef sendCodeButton,
        ElementRef submit
    ) {}
    public record ElementRef(String xpath, String css) {}

}
