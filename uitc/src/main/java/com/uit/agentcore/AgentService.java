package com.uit.agentcore;

import java.util.List;
import java.util.Map;

import com.uit.agentcore.agents.CaseDesignerAgent;
import com.uit.agentcore.agents.ExploerAnalyzer;
import com.uit.agentcore.agents.ScriptAgent;
import com.uit.agentcore.agents.SequenceAgents;
import com.uit.agentcore.agents.UIExplorerAgent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;


public class AgentService {


    public static SequenceAgents SequenceAgentServiceBuilder(ChatModel chatModel,AgentListener listener,Map<String,List<Object>> tools){
        UIExplorerAgent uiExplorer = AgenticServices
                .agentBuilder(UIExplorerAgent.class)
                .chatModel(chatModel)
                .tools(tools.get("UIExplorerTools").toArray(new Object[0]))
                .outputKey("elements")
                .build();
        
        CaseDesignerAgent caseDesigner = AgenticServices
                .agentBuilder(CaseDesignerAgent.class)
                .chatModel(chatModel)
                // .tools(tools.get("CaseDesignerTools"))
                .outputKey("cases")
                .build();
        
        ScriptAgent script = AgenticServices
                .agentBuilder(ScriptAgent.class)
                .chatModel(chatModel)
                // .tools(tools.get("uiExplorerTools"))
                .outputKey("script")
                .build();

        return AgenticServices
            .sequenceBuilder(SequenceAgents.class) // 传入接口类型
            .subAgents(uiExplorer, caseDesigner, script)
            .outputKey("userMsg")
            .listener(listener)
            .build();
    }


    public static ExploerAnalyzer analyzer(ChatModel chatModel){
        return AiServices
                .builder(ExploerAnalyzer.class)
                .chatModel(chatModel)
                .build();
    }

}
