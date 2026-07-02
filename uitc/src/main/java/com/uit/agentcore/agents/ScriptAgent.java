package com.uit.agentcore.agents;

import dev.langchain4j.agentic.Agent;

public interface ScriptAgent {

    @Agent("")
    String execute(String script);
}
