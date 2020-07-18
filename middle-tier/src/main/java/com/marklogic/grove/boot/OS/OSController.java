package com.marklogic.grove.boot.OS;

import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.envision.deploy.DeployHubService;
import com.marklogic.hub.deploy.util.HubDeployStatusListener;
import com.marklogic.hub.dhs.DhsDeployer;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.impl.*;
import com.marklogic.hub.step.StepDefinitionProvider;
import com.marklogic.hub.util.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.marklogic.grove.boot.AbstractController;

import java.io.*;
import java.util.*;

import com.marklogic.hub.entity.HubEntity;

import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/api/os/")

public class OSController extends AbstractController {

	boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

	@Autowired
	HubConfigImpl hubConfig;

	@Autowired
	DataHubImpl datahub;

	@Autowired
	FlowManagerImpl flowManager;

	@Autowired
	EntityManagerImpl entityManager;

	@Autowired
	FlowRunner fr;

	@Autowired
	DeployHubService deployService;

	//support for hub step runner
	private int batchSize = 100;
    private int threadCount = 4;
    private String sourceDatabase;
    private String targetDatabase;
	private StepDefinitionProvider stepDefinitionProvider;

	// returns JSON (or string) containining details of the DH project config
	@RequestMapping(value = "/getDHprojectConfig", method = RequestMethod.GET)
	public String getDHprojectConfig()  {
		final JSONObject config = new JSONObject();

		// Does grade-dhs.properties exists in the datahub project dir?
		final String dhfDir = hubConfig.getHubProject().getProjectDirString();
		config.put("project", dhfDir);

		final String myHostName =  hubConfig.getHost();
		config.put("host", myHostName);
		
		// get flows
		final ArrayList<String> arrFlows = new ArrayList<String>();

		final List<Flow> flows = flowManager.getFlows();
		for(final Flow flow : flows) {
			System.out.println("DGB got flow: " + flow.getName());
			arrFlows.add(flow.getName());
		}
		config.put("flows", arrFlows.toString());

		// get Entities
		final ArrayList<String> arrEntities = new ArrayList<String>();
		final List<HubEntity> entities = entityManager.getEntities();
		for(final HubEntity entity : entities) {
			System.out.println("DGB got entity: " + entity.getInfo().getTitle() );
			arrEntities.add(entity.getInfo().getTitle());
		}
		config.put("entities", arrEntities.toString());


		System.out.println("DGB returning: " + config.toString() );
		return config.toString() ;
	}

	//DMSDK approach to ingestion
	//TODO file uploader for Envision at a url
	//file being uploaded, name of flow/step
	//write to tmp
	//kick off nifi/etc to run ingest/harmonize
	//S3 upload?
	//Rest service in spring boot- will collect all files in tmp
	//real way - write own custom ui for uploadin 1-n file
	//automagically determine file type
	//could do in naive way- json, xml, csv
	//no need to specify a flow, just target collection-
	//custom ui for loading data flow
	@RequestMapping(value = "/runFlows", method = RequestMethod.POST)
	public String runFlows()  {
		//TODO This return variable has no value
		String output = "";
		// get Ingestion flows
		final List<Flow> flows = flowManager.getFlows();

		for (final Flow flow : flows) {
			try {
				runFlow(flow.getName());
				output = "Ran flow " + flow.getName();
			}catch(final Error e){
				System.out.println("Cannot run flow " + flow.getName());
				output = "Error running flow " + flow.getName();
			}
		}
		return output;
	}

	private	HubDeployStatusListener getListener() {
		// callback for status updates
		HubDeployStatusListener listener;
		listener = new HubDeployStatusListener() {
			@Override
			public void onStatusChange(final int percentComplete, final String message) {
				//TODO access view's progress indicator
				System.out.println(message + " complete:" + percentComplete);
			}

			@Override
			public void onError() {
				System.out.println("Deploy controller in error");
			}
		};
		return listener;
	}

	// Deploy entities and flows to DH
	@RequestMapping(value = "/deployToDH", method = RequestMethod.GET)
	public String deployToDH() {
			//if is provisioned environment we are deploying to the cloud
			if(hubConfig.getIsProvisionedEnvironment()){
				try {
					this.deployToDHS();
				} catch (final Exception e) {
					System.out.println("Cannot deploy to DHS");
				}
			}else{
				try{
					this.deployToNonProvisioned();
				} catch (final Exception e) {
					System.out.println("Cannot deploy to non-provisioned environment");
				}
			}
			return new String("Hub deployed.");
	}

	//dispatch to DHSDeployer
	private void deployToDHS() {
		DhsDeployer dhsDeployer = new DhsDeployer();

		dhsDeployer.deployAsSecurityAdmin(hubConfig);
		dhsDeployer.deployAsDeveloper(hubConfig);
	}

	private boolean deployToNonProvisioned() {
		boolean result = false;
		HubDeployStatusListener listener = getListener();
		try {
			result = deployService.deployHubInstall(listener);
		} catch (Error error) {
			error.printStackTrace();
		}
		return result;
	}

	private void runFlow(String flowName) {
		FlowRunner flowRunner = new FlowRunnerImpl(hubConfig);
		System.out.println("Running flow: " + flowName);
		FlowInputs inputs = new FlowInputs(flowName);
		RunFlowResponse response = flowRunner.runFlow(inputs);
		flowRunner.awaitCompletion();
		System.out.println("Response: " + response);
	}
}


