package com.uit.agentcore.agents;

import dev.langchain4j.agentic.Agent;

public interface CaseDesignerAgent {

    @Agent("")
    String design(String url);
}
