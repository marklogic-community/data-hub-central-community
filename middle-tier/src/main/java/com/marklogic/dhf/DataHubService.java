package com.marklogic.dhf;

import java.util.List;

import com.marklogic.dhf.config.MarkLogicConfiguration;
import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Data Hub Service allows for the processing of content already loaded into the MarkLogic Database.
 *
 * @author Drew Wanczowski
 */
@Service
public class DataHubService {

    private final Logger logger = LoggerFactory.getLogger(DataHubService.class);

    private final FlowRunner flowRunner;

    private final MarkLogicConfiguration markLogicConfiguration;

    @Autowired
    public DataHubService(MarkLogicConfiguration markLogicConfiguration) {
        this.markLogicConfiguration = markLogicConfiguration;
        this.flowRunner = new FlowRunnerImpl(
                this.markLogicConfiguration.getHost(),
                this.markLogicConfiguration.getUser(),
                this.markLogicConfiguration.getPassword());
    }

    public void runFlow(String flowName, List<String> steps, String jobId) {
        FlowInputs inputs = new FlowInputs(flowName);
        inputs.setJobId(jobId);
        inputs.setSteps(steps);

        logger.info("Running flow: " + flowName);
        RunFlowResponse response = flowRunner.runFlow(inputs);
        flowRunner.awaitCompletion();
        logger.info("Data Hub Response: " + response.toJson());
    }
}