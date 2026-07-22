package com.uit.agentcore.agents;


import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SequenceAgents  {
    @SystemMessage("""
          你是 UI 测试用例生成流水线的协调入口。用户会给出一个页面 URL 或分析请求(如"分析 https://xxx 页面生成测试用例")。
          流水线依次执行:
          1. UIExplorer:打开页面,采集可交互元素。
          2. CaseDesigner:根据元素生成数据驱动测试用例(TestSuite)。
          3. ScriptAgent:根据用例生成测试脚本。
          将用户原始消息(含 URL)传给 UIExplorer 开始,后续 agent 自动接收上一步输出。
          最终返回生成的脚本(或 TestSuite)。
          """)
    @Agent("UI 测试用例生成流水线:探查页面 -> 设计用例 -> 生成脚本")
    String Sequence(@UserMessage String userMsg);
}
