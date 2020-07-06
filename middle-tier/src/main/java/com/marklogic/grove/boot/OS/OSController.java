package com.marklogic.grove.boot.OS;

import com.marklogic.hub.DataHub;
import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.envision.deploy.DeployHubService;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.hub.deploy.util.HubDeployStatusListener;
import com.marklogic.hub.dhs.DhsDeployer;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.impl.*;
import com.marklogic.hub.step.MarkLogicStepDefinitionProvider;
import com.marklogic.hub.step.StepDefinition;
import com.marklogic.hub.step.StepDefinitionProvider;
import com.marklogic.hub.step.StepDefinition.StepDefinitionType;
import com.marklogic.hub.step.impl.Step;
import com.marklogic.hub.step.impl.WriteStepRunner;
import com.marklogic.hub.util.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.marklogic.grove.boot.AbstractController;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import org.springframework.http.HttpEntity;
import org.apache.commons.io.FileUtils;
import java.time.Instant;

import org.springframework.web.bind.annotation.RequestParam;

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
		final File dhsConfigFile = new File(dhfDir + "/gradle-dhs.properties");
		config.put("dhsConfigFileExists", dhsConfigFile.exists());

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
	public String runIngestStepsDMSK()  {

		//TODO This return variable has no value
		final String output = "";

		stepDefinitionProvider = new MarkLogicStepDefinitionProvider(hubConfig.newStagingClient(null));

		// get Ingestion flows
		final List<Flow> flows = flowManager.getFlows();

		for(final Flow flow : flows) {
			System.out.println("FR got flow: " + flow.getName());
			final Map<String, Step> steps = flow.getSteps();
			Integer i = 0;
			for (final Entry<String, Step> step: steps.entrySet()){
				final Step theStep = step.getValue();
				i++;
				System.out.println("FR starting Step: " + i);
				if (theStep.getStepDefinitionType().toString() == "ingestion") {
					final StepDefinition stepDef = stepDefinitionProvider.getStepDefinition(theStep.getStepDefinitionName(), theStep.getStepDefinitionType());
					final WriteStepRunner stepRunner = new WriteStepRunner(hubConfig);
					stepRunner.withFlow(flow).withStep(i.toString());
					System.out.println("FR Step: " + stepDef.getName());
					if(theStep.getBatchSize() != 0) {
						batchSize = theStep.getBatchSize();
					}
					else if(flow.getBatchSize() != 0) {
						batchSize = flow.getBatchSize();
					}
					else if(stepDef!=null && stepDef.getBatchSize() != 0) {
						batchSize = stepDef.getBatchSize();
					}
					stepRunner.withBatchSize(batchSize);

					if(theStep.getThreadCount() != 0) {
						threadCount = theStep.getThreadCount();
					}
					else if(flow.getThreadCount() != 0) {
						threadCount = flow.getThreadCount();
					}
					else if(stepDef != null && stepDef.getThreadCount() !=0 ){
						threadCount = stepDef.getThreadCount();
					}

					stepRunner.withThreadCount(threadCount);

					if(theStep.getOptions().get("sourceDatabase") != null) {
						sourceDatabase = ((TextNode)theStep.getOptions().get("sourceDatabase")).asText();
					}
					else if(stepDef.getOptions().get("sourceDatabase") != null) {
						sourceDatabase = ((TextNode)stepDef.getOptions().get("sourceDatabase")).asText();
					}
					else {
						sourceDatabase = hubConfig.getDbName(DatabaseKind.STAGING);
					}
					stepRunner.withSourceClient(hubConfig.newStagingClient(sourceDatabase));

					if(theStep.getOptions().get("targetDatabase") != null) {
						targetDatabase = ((TextNode)theStep.getOptions().get("targetDatabase")).asText();
					}
					else if(stepDef.getOptions().get("targetDatabase") != null) {
						targetDatabase = ((TextNode)stepDef.getOptions().get("targetDatabase")).asText();
					}
					else {
						if(StepDefinitionType.INGESTION.equals(step.getValue().getStepDefinitionType())) {
							targetDatabase = hubConfig.getDbName(DatabaseKind.STAGING);
						}
						else {
							targetDatabase = hubConfig.getDbName(DatabaseKind.FINAL);
						}
					}

					stepRunner.withDestinationDatabase(targetDatabase);

					//For ingest flow, set stepDef.
					if(StepDefinitionType.INGESTION.equals(theStep.getStepDefinitionType())) {
						((WriteStepRunner)stepRunner).withStepDefinition(stepDef);
					}
					//set up runner for running
					Map<String, Object> optsMap ;
					if(flow.getOverrideOptions() != null) {
						optsMap = new HashMap<>(flow.getOverrideOptions());
					}
					else {
						optsMap = new HashMap<>();
					}
					System.out.println("FR Running Step: " + stepDef);
					stepRunner.withOptions(optsMap).withJobId(UUID.randomUUID().toString());
					stepRunner.run();
					stepRunner.awaitCompletion();
				}
			}
		}

		return output;
	}

	@RequestMapping(value = "/runIngestSteps", method = RequestMethod.GET)
	public void runFlows()  {
		//TODO This return variable has no value
		final String output = "";
		// get Ingestion flows
		final List<Flow> flows = flowManager.getFlows();

		for(final Flow flow : flows) {
			try {
			runFlow(flow.getName());
			}catch(final Error e){
				System.out.println("Cannot run flow " + flow.getName());
			}
		}
	}

	// runs all flow steps except for ingestion
	@RequestMapping(value = "/runFlowsWithoutIngestionSteps", method = RequestMethod.GET)
	public String runFlowsWithoutIngestionSteps()  {

		// read the DHS properties file
		final String dhfDir = hubConfig.getHubProject().getProjectDirString();
		String output = "";
		final Properties prop = new Properties();

		final ProcessBuilder builder = new ProcessBuilder();

		try (InputStream input = new FileInputStream(dhfDir + "/gradle-dhs.properties")) {
			prop.load(input);

			// get Ingestion flows
			final ArrayList<String> arrFlows = new ArrayList<String>();
			final List<Flow> flows = flowManager.getFlows();

			for(final Flow flow : flows) {
				System.out.println("DGB got flow: " + flow.getName());
				final Map<String, Step> steps = flow.getSteps();
				final ArrayList<String> arrSteps = new ArrayList<>();


				int i = 0;
				for (final Map.Entry<String, Step> step: steps.entrySet()){
					i++;
					if (!step.getValue().getStepDefinitionType().toString().equals("ingestion")) {
						arrSteps.add(Integer.toString(i));
					}
				}
				System.out.println("DGB flow: " + flow.getName() + " steps: " + String.join("," , arrSteps) );

				final RunFlowResponse runNonIngestSteps = fr.runFlow(flow.getName(), String.join("," , arrSteps));

				fr.awaitCompletion();
				output += " " + flow.getName();
			}

		} catch (final IOException e){
			System.out.println("Cannot read gradle-dhs.properties");
		}

		return "Processed: " + output;
	}


	private	HubDeployStatusListener getListener() {
		// callback for status updates
		HubDeployStatusListener listener;
		listener = new HubDeployStatusListener() {
			@Override
			public void onStatusChange(final int percentComplete, final String message) {
				//TODO access view's progress indicator
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
		//FlowRunner flowRunner = new FlowRunnerImpl(hubConfig.getHost(), hubConfig.getMlUsername(), hubConfig.getMlPassword());
		FlowRunner flowRunner = new FlowRunnerImpl(hubConfig);
		System.out.println("Running flow: " + flowName);
		FlowInputs inputs = new FlowInputs(flowName);
		RunFlowResponse response = flowRunner.runFlow(inputs);
		flowRunner.awaitCompletion();
		System.out.println("Response: " + response);
	}

	static void runFlow(FlowRunner flowRunner, String inputFilePath) {
		final String flowName = "ingestion_mapping_mastering-flow";
		FlowInputs inputs = new FlowInputs(flowName);
		// This is needed so that an absolute file path is used
		inputs.setInputFilePath(inputFilePath);

		System.out.println("Running flow: " + flowName);
		RunFlowResponse response = flowRunner.runFlow(inputs);
		flowRunner.awaitCompletion();
		System.out.println("Response: " + response);
	}

}


