package com.uit.agentcore;

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


    public static SequenceAgents SequenceAgentServiceBuilder(ChatModel chatModel,AgentListener listener){
        UIExplorerAgent uiExplorer = AgenticServices
                .agentBuilder(UIExplorerAgent.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();
        
        CaseDesignerAgent caseDesigner = AgenticServices
                .agentBuilder(CaseDesignerAgent.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();
        
        ScriptAgent script = AgenticServices
                .agentBuilder(ScriptAgent.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        return AgenticServices
            .sequenceBuilder(SequenceAgents.class) // 传入接口类型
            .subAgents(uiExplorer, caseDesigner, script)
            .outputKey("story")
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
