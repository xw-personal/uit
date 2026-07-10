package com.uit.api.common;

import java.util.Map;

import org.springframework.stereotype.Component;

import dev.langchain4j.agentic.observability.AfterAgentToolExecution;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.observability.BeforeAgentToolExecution;
import reactor.core.publisher.Sinks;

@Component
public class AgentProgressListenerFactory {
    public AgentListener forSink(String userId,Sinks.Many<Object> sink){
        return new AgentListener() {
            public void beforeAgentInvocation(AgentRequest agentRequest) {
                sink.tryEmitNext(Map.of(
                    "phase", "agent_start",
                    "agent", agentRequest.agentName(),
                    "userId", userId
                ));
            }
            public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
                sink.tryEmitNext(Map.of(
                    "phase", "tool_call",
                    "agent", beforeAgentToolExecution.agentInstance().name(),
                    "tool", beforeAgentToolExecution.toolExecution().request().name(),
                    "args", beforeAgentToolExecution.toolExecution().request().arguments(),
                    "userId", userId
                ));
            }
            public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
                sink.tryEmitNext(Map.of(
                    "phase", "tool_result",
                    "agent", afterAgentToolExecution.agentInstance().name(),
                    "tool", afterAgentToolExecution.toolExecution().request().name(),
                    "args", afterAgentToolExecution.toolExecution().request().arguments(),
                    "userId", userId
                ));
            }
            
            public void afterAgentInvocation(AgentResponse agentResponse) {
                sink.tryEmitNext(Map.of(
                    "phase", "agent_done",
                    "agent", agentResponse.agentName(),
                    "output",agentResponse.output(),
                    "userId", userId
                ));
            }
            public void onAgentInvocationError(AgentInvocationError agentInvocationError) {
                sink.tryEmitNext(Map.of(
                    "phase", "agent_error",
                    "agent", agentInvocationError.agentName(),
                    "error", agentInvocationError.error().getMessage(),
                    "userId", userId
                ));
            }
            public boolean inheritedBySubagents() {
                return true;
            }
        };
    }
}
