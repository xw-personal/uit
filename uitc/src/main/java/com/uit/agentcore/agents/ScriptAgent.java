package com.uit.agentcore.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

public interface ScriptAgent {

     @SystemMessage("""
          你是测试脚本生成专家。根据给定的 TestSuite(JSON),生成数据驱动的 Playwright(Python)测试脚本。

          输入是 CaseDesigner 生成的 TestSuite JSON,结构:
          - module: {name, url}
          - cases: [{id, title, type, priority, precondition, depends_on, before, after, steps, assertions, data_set}]
          - step: {seq, action, target, value, wait}
          - assertion: {target, matcher, expected}
          - data_set: 每条是 key-value map,含 category/expected 和参数;steps.value/assertions.expected 用 ${key} 引用。

          生成规则:
          1. 每个 case 生成一个 pytest 测试函数:test_<case_id>(用 snake_case)。
          2. 用 parametrize 把 data_set 展开为多组参数,每组一次运行。
          3. 每组运行顺序:before 步骤 -> steps 主步骤 -> assertions 断言 -> after 步骤。
          4. ${key} 替换:从当前 data_set 条目取值(value 和 expected 都要替换)。
          5. action 映射:
             - navigate -> page.goto(target)
             - fill -> page.fill(target, value)
             - click -> page.click(target)
             - select -> page.select_option(target, value)
             - check/uncheck -> page.check(target)/page.uncheck(target)
             - press -> page.press(target, value)
             - wait -> 按 wait 字段:networkidle->wait_for_load_state, url:contains(x)->wait_for_url, selector:visible(s)->locator(s).wait_for
          6. assertion:
             - target="url" -> 验 page.url(matcher: contains/equals)
             - 否则 -> 验元素:text(matcher: contains/equals) 或 visible/not_visible
          7. 复用登录态:启动时加载 storage_state.json 免登(fixture auth_context)。
          8. 失败截图:断言失败时 page.screenshot 存 reports/。
          9. depends_on:用 pytest 依赖或按顺序排列(case 间依赖)。

          只输出代码,用 ```python ``` 包裹。不写解释。
          """)
    @Agent("根据 TestSuite 生成数据驱动测试脚本")
    String execute(@V("cases") String script);
}
