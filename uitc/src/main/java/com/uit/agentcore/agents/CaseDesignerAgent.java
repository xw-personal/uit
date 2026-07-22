package com.uit.agentcore.agents;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CaseDesignerAgent {

    @SystemMessage("""
          你是测试用例设计专家。根据给定的页面可交互元素清单,生成数据驱动的测试套件。
          规则:
          - module: 模块名 name 和 URL。
          - cases: 用例列表,每个 case 含:
            · id: 唯一标识(如 search_001),用于 depends_on 引用
            · title: 业务语言标题(不同 case 不要重名)
            · type: FUNCTIONAL / BOUNDARY / EXCEPTION
            · priority: P0 / P1 / P2
            · precondition: 前置状态(如"已登录"),可选
            · depends_on: 依赖的用例 id 列表,可选
            · before: setup 步骤,每个数据组运行前执行一次,可选,无 seq
            · after: teardown 步骤,每个数据组运行后执行一次,可选,无 seq
            · steps: 主步骤,必须有 seq(从 1 开始)
            · assertions: 断言列表
            · data_set: 数据组,每条一组参数,框架迭代
          - step(before/after/steps 通用)字段:
            · action: navigate / fill / click / select / check / uncheck / press / wait  (是 click,不是 chick)
            · target: 元素选择器(css 或 xpath=...),必须来自给定元素清单,不可凭空捏造;navigate 时 target=URL
            · value: ${param} 引用 data_set,或字面量
            · description: 人类可读描述,可选
            · waitExploer: 操作后等待(networkidle / url:contains(...) / selector:visible(...)),可选
          - assertion 字段:
            · target: "url" 或元素选择器
            · matcher: contains / equals / visible / not_visible
            · expected: ${param} 引用 data_set,或字面量
          - data_set: 每条数据是 key-value map,必须含:
            · category: effective / invalid / boundary
            · expected: 该组数据的预期(和 assertions 的 ${expected} 对应)
            · 其余 key 是参数,和 steps.value 的 ${param} 一一对应(如 keyword、filter)
            · 不要写 test_id(框架自动生成 run id)
          - ${param} 引用:steps.value 和 assertions.expected 中用 ${key} 引用 data_set 当前条目的 key,
            key 必须在 data_set 每条里都存在。
          只返回 JSON,符合 TestSuite schema。
          将 TestSuite 序列化为 JSON 字符串返回(不要返回对象)。
          """)
      @Agent("根据页面元素生成数据驱动测试用例,返回 JSON")
      String design(@V("elements") String elementsJson);

      record TestSuite(Module module, List<TestCase> cases) {}

      record Module(String name, String url) {}

      record TestCase(
          String id,
          String title,
          String type,                 // FUNCTIONAL/BOUNDARY/EXCEPTION
          String priority,             // P0/P1/P2
          String precondition,         // 可空
          @JsonProperty("depends_on") List<String> dependsOn,
          List<Step> before,           // setup,可空
          List<Step> after,            // teardown,可空
          List<Step> steps,
          List<Assertion> assertions,
          @JsonProperty("data_set") List<Map<String, String>> dataSet
      ) {}

      record Step(
          Integer seq,                 // 可空(before/after 步骤无 seq)
          String description,
          String action,
          String target,
          String value,
          String waitExploer
      ) {}

      record Assertion(
          String target,
          String matcher,
          String expected
      ) {}
  }
