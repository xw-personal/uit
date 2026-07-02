package com.uit.agentcore.agents;

import dev.langchain4j.agentic.Agent;

public interface UIExplorerAgent {

    @Agent("")
    String explore(String url);
}
