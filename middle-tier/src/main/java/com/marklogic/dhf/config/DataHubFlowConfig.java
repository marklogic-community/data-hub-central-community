package com.marklogic.dhf.config;

import java.util.List;

public class DataHubFlowConfig {
    private String flowName;
    private List<String> postLoadSteps;

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public List<String> getPostLoadSteps() {
        return postLoadSteps;
    }

    public void setPostLoadSteps(List<String> postLoadSteps) {
        this.postLoadSteps = postLoadSteps;
    }
}
