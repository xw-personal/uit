package com.uit.agentcore.agents;

import java.util.List;
import java.util.Map;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CaseDesignerAgent {

    @SystemMessage("""
          你是测试用例设计专家。根据页面可交互元素清单,生成数据驱动的测试套件。
          规则:
          - steps[].target 必须从给定元素的 css/xpath 选取,不可凭空捏造。
          - dataLibrary 按业务分组,组内分 effective/invalid/boundary 三类,每条 DataItem 带 value 和 expected。
          - case.dataGroup 声明用哪个数据组;steps.value 和 assertions.expected 用 ${group.field} 引用当前数据项。
          - action: navigate/fill/click/select/check/press/wait
          - matcher: contains/equals/visible/notVisible
          - depends_on 是依赖的 case id 列表;precondition 是状态描述(如"已登录")。
          只返回 JSON,符合 TestSuite schema。
          """)
    @Agent("根据页面元素生成数据驱动测试套件")
    TestSuite design(@UserMessage String elementsJson);


    public record TestSuite(
        Module module,
        Map<String, DataGroup> dataLibrary,    // key = 业务分组名,如 "search"
        List<TestCase> cases
    ) {}

    public record Module(String name, String url) {}

    public record DataGroup(
        List<DataItem> effective,               // 有效等价类
        List<DataItem> invalid,                 // 无效等价类
        List<DataItem> boundary                 // 边界值
    ) {}

    public record DataItem(String value, String expected) {}   // 数据 + 它的预期,绑死

    public record TestCase(
        String id,
        String title,
        String type,                            // FUNCTIONAL/BOUNDARY/EXCEPTION
        String priority,                        // P0/P1/P2
        List<String> depends_on,                // 依赖的 case id
        String precondition,                    // 状态前置
        String dataGroup,                       // 引用 dataLibrary 的 key
        List<Step> steps,
        List<Assertion> assertions
    ) {}

    public record Step(
        int no,
        String description,                     // 文档:人类可读
        String action,                          // 脚本动作
        String target,                          // 元素定位
        String value,                           // ${group.field} 或字面量
        String waitTime                             // 操作后等待
    ) {}

    public record Assertion(
        String target,                          // "url" 或选择器
        String matcher,                         // contains/equals/visible/notVisible
        String expected                         // ${group.field} 或字面量
    ) {}

  }

