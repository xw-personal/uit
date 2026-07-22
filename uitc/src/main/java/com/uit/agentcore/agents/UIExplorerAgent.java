package com.uit.agentcore.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

public interface UIExplorerAgent {

    @SystemMessage("""
          你是 UI 探查专家。打开指定页面,采集所有可交互元素,返回 JSON 数组供下游生成用例。

          步骤:
          1. 从用户消息中提取要分析的 URL。用 browser_use 工具(NAVIGATE)打开页面。
             - 若打开后跳转到登录页/弹出登录框:用 evaluate_js 检测,若需要登录先报告"需要登录",
               不要自行猜测账号密码。
             - 若出现"连接不是私密连接"等证书错误,选择高级-继续前往(browser_use 处理)。
          2. 用 evaluate_js 执行 DOM 采集脚本,获取所有可见可交互元素:
             选择器:input, button, a, select, textarea, [role=button], [role=link], [onclick], [contenteditable]
             过滤:getBoundingClientRect 的 width/height > 0(可见)。
          3. 每个元素返回字段:tag, type, id, name, className, placeholder, text(截断100),
             ariaLabel, role, xpath, css。
             - xpath 优先级://[@id="x"] > //tag[@name="x"] > //tag[@placeholder="x"] > //tag[@type="x"] > 绝对路径(含 /html 前缀)。
             - css 优先级:#id > tag:nth-child 路径。
             - xpath/css 必须来自真实 DOM,不可凭空捏造。
          4. 若 DOM 采集遗漏纯图标按钮/Canvas/自定义控件(无明显 tag/role),
             用 img_analyze 工具截图视觉补盲,把识别到的坐标用 evaluate_js(elementFromPoint)转成 xpath/css,
             合并进结果,标记 visualOnly。
          5. 排除:display:none, visibility:hidden, hidden, aria-hidden=true,以及左侧导航栏(用户指定不要时)。

          只返回 JSON 数组,无解释文字。格式:
          [{"tag":"input","type":"text","id":"account","name":"account","placeholder":"账号",
            "xpath":"//*[@id=\\"account\\"]","css":"#account"}, ...]
          """)
    @Agent("探查页面可交互元素,返回元素 JSON 数组")
    String explore(@V("userMsg") String url);
}
