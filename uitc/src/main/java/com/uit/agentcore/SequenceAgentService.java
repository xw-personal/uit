package com.uit.agentcore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uit.agentcore.agents.CaseDesignerAgent;
import com.uit.agentcore.agents.ScriptAgent;
import com.uit.agentcore.agents.SequenceAgents;
import com.uit.agentcore.agents.UIExplorerAgent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

@Service
public class SequenceAgentService {

    @Autowired
    private ChatModel chatModel;

    public SequenceAgents SequenceAgentServiceBuilder(){
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
            .build();
    }
}
