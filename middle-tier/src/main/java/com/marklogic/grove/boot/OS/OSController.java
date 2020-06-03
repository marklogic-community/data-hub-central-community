package com.marklogic.grove.boot.OS;

import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.command.Command;
import com.marklogic.hub.FlowManager;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubProject;
import com.marklogic.hub.StepDefinitionManager;
import com.marklogic.hub.deploy.HubAppDeployer;
import com.marklogic.hub.deploy.util.HubDeployStatusListener;
import com.marklogic.hub.dhs.DhsDeployer;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.impl.*;
import com.marklogic.hub.step.MarkLogicStepDefinitionProvider;
import com.marklogic.hub.step.StepDefinition;
import com.marklogic.hub.step.StepDefinitionProvider;
import com.marklogic.hub.step.StepRunner;
import com.marklogic.hub.step.StepDefinition.StepDefinitionType;
import com.marklogic.hub.step.impl.Step;
import com.marklogic.hub.step.impl.WriteStepRunner;
import com.marklogic.hub.util.json.JSONObject;
import com.marklogic.dhf.DataHubService;
import com.marklogic.dhf.DataMovementService;
import com.marklogic.dhf.config.MarkLogicConfiguration;
import com.marklogic.dhf.config.ServerTransformConfig;
import com.marklogic.hub.impl.StepDefinitionManagerImpl;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.marklogic.grove.boot.AbstractController;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sound.midi.SysexMessage;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import java.util.concurrent.Executors;


import org.springframework.http.HttpEntity;
import org.apache.commons.io.FileUtils;
import java.time.Instant;

import org.springframework.web.bind.annotation.RequestParam;

import com.marklogic.hub.entity.HubEntity;

import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/api/os/")

public class OSController extends AbstractController {

	// private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

	boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	
	//TODO	superclass has private HubConfig hubConfig you can access:
	//super.getHubConfig()
	@Autowired
	HubConfigImpl hubConfig; // = super.getHubConfig();

	@Autowired
	DataHubImpl datahub;

	@Autowired
	FlowManagerImpl flowManager;
	//	FlowManager flowManager;

	@Autowired
	EntityManagerImpl entityManager;

	@Autowired
	FlowRunner fr;

	//support for hub step runner
	private int batchSize = 100;
    private int threadCount = 4;
    private String sourceDatabase;
    private String targetDatabase;
	private StepDefinitionProvider stepDefinitionProvider;

	// runs a gradle task
	@RequestMapping(value = "/gradle", method = RequestMethod.POST)
	public String getGradle(@RequestBody final ObjectNode searchRequest) {
		final String dhfDir = super.getHubConfig().getHubProject().getProjectDirString();
		String task = new String();

		task = "tasks" ; // default value
		if (searchRequest.has("task")) {
			task = searchRequest.get("task").asText();

		}

		final ProcessBuilder builder = new ProcessBuilder();
		if (isWindows) {
			// Not tested
			builder.command("cmd.exe", "/c", "cd " + dhfDir + " & gradlew " + task );
		} else {
			// task = "tasks"; // debug to do a gradle tasks
			builder.command("sh", "-c", "cd " + dhfDir + "; sh gradlew " + task);
		}

		final String output = runProcess( builder );

		return "Done with " + task + " - "  + output;
	}
	private String runProcess (final ProcessBuilder builder) {
		// see https://www.baeldung.com/java-lang-processbuilder-api
		System.out.println ("Start of runProcess");
		String output = new String();

		try {
			final Process process = builder.start();
			if( process.getErrorStream().read() != -1 ){
				// convert process.getErrorStream() to a string
				final StringWriter writer = new StringWriter();
				IOUtils.copy(process.getErrorStream(), writer);
				output = "Errors " + writer.toString();
			} else {
				final StringWriter writer = new StringWriter();
				IOUtils.copy(process.getInputStream(), writer);
				output = "Output " + writer.toString();
			}


		} catch(final IOException e) {
			e.printStackTrace();
		}
		System.out.println ("End of runProcess");
		return output;
	};

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

	// called when creating the gradle-dhs.properties file
	@RequestMapping(value = "/setGradleProps", method = RequestMethod.POST)

	//public String setGradleProps(@RequestBody ObjectNode searchRequest) {
	public void setGradleProps(final HttpEntity<String> httpEntity) {

		final String dhsConfig = httpEntity.getBody();
		final String dhfDir = hubConfig.getHubProject().getProjectDirString();

		final File dhsPropertiesFile = new File(dhfDir + "/gradle-dhs.properties");

		if (dhsPropertiesFile.exists() ) {
			System.out.println("Creating backup copy of gradle-dhs.properties");
			final File safeCopy = new File(dhfDir + "/gradle-dhs.properties." + Instant.now().getEpochSecond() );

			try {
				FileUtils.moveFile(dhsPropertiesFile, safeCopy);
			} catch (final IOException e) {
				System.out.println("Error making backup copy of gradle-dhs.properties ");
				e.printStackTrace();
			}
		}

		try {
			FileUtils.write(dhsPropertiesFile, dhsConfig);
		}  catch (final IOException e) {
			System.out.println("Error creating gradle-dhs.properties");
			e.printStackTrace();
		}

		return ;
	}


	// runs ingestion steps using MLCP/DMSDK?
	@RequestMapping(value = "/runIngestStepsMLCP", method = RequestMethod.GET)
	public String runIngestSteps()  {

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
				Integer i = 0;
				for (final Map.Entry<String, Step> step: steps.entrySet()){
					i++;
					if (step.getValue().getStepDefinitionType().toString() == "ingestion") {

						final String mlcp = getMLCPCommand(prop, flow, step.getValue(), i);

						if (isWindows) {
							builder.command("cmd.exe", "/c", ".... todo" );
						} else {
							builder.command("sh", "-c", "mlcp.sh  " + mlcp);
						}
						output += "***************************************************";
						output += "**** running MLCP for flow: " + flow.getName() + " step: " + step.getValue().getName();
						output += "***************************************************";
						output += runProcess( builder );


						// DMDSK approach

					}
				}
			}

		} catch (final IOException e){
			System.out.println("Cannot read gradle-dhs.properties");
		}

		return output;
	}

//DMSDK approach to ingestion
//TODO --
//file uploader file being uploaded, name of flow/step
//write to tmp
//kick off nifi/etc to run ingest/harmonize
//S3 upload?
//Rest service in spring boot- will collect all files in tmp
//real way - write own custom ui for uploadin 1-n file
//automagically determine file type
//could do in naive way- json, xml, csv
//no need to specify a flow, just target collection-
//custom ui for loading data flow

	@RequestMapping(value = "/runIngestSteps", method = RequestMethod.GET)
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

	private String getMLCPCommand(final Properties prop,  final Flow flow,  final Step step, final int stepNumber) {
		final Map<String, Object> stepOptions = step.getOptions();
		final JsonNode jsonFileLocs = step.getFileLocations();

		String mlcp = " ";
		mlcp = mlcp.concat(" import -mode \"local\"");
		mlcp = mlcp.concat( " -restrict_hosts \"true\"");
		mlcp = mlcp.concat(" -ssl \"true\" ");
		mlcp = mlcp.concat(" -generate_uri \"true\" ");

		mlcp = mlcp.concat(" -transform_module \"/data-hub/5/transforms/mlcp-flow-transform.sjs\"");
		mlcp = mlcp.concat(" -transform_namespace \"http://marklogic.com/data-hub/mlcp-flow-transform\"");


		mlcp = mlcp.concat( " -host \"" + prop.getProperty("mlHost") + "\"");
		mlcp = mlcp.concat( " -port \"" + prop.getProperty("mlAppServicesPort") + "\"");
		mlcp = mlcp.concat( " -username \"" + prop.getProperty("mlUsername") + "\"");
		mlcp = mlcp.concat( " -password \"" + prop.getProperty("mlPassword") + "\"");

		mlcp = mlcp.concat( " -input_file_path " + jsonFileLocs.get("inputFilePath") );

		if (jsonFileLocs.get("inputFileType").toString().equals( "\"csv\"") ){
			mlcp = mlcp.concat( " -input_file_type \"delimited_text\" " );
		}

		if (jsonFileLocs.get("outputURIReplacement").toString().length() > 2) {
			mlcp = mlcp.concat(" -output_uri_replace " + jsonFileLocs.get("outputURIReplacement"));
		}
		mlcp = mlcp.concat( " -delimiter " + jsonFileLocs.get("separator") );

		String collectionList = stepOptions.get("collections").toString();
		collectionList = collectionList.substring(1, collectionList.length() - 1); // strip off first and last [ ]
		collectionList = collectionList.replaceAll("\",\"", ","); //  Change from "BrandAOrders","DavidAdditionalCollection"  to "BrandAOrders,DavidAdditionalCollection"


		mlcp = mlcp.concat( " -output_collections  " + collectionList);
		//mlcp = mlcp.concat( " -output_permissions  " + stepOptions.get("permissions").toString());
		mlcp = mlcp.concat( " -output_permissions  " + "\"rest-reader,read,rest-writer,update\"");

		mlcp = mlcp.concat( " -document_type  " + stepOptions.get("outputFormat").toString());

		mlcp = mlcp.concat(" -transform_param \"flow-name=" + flow.getName() + ",step=" + stepNumber + "\"" );

		System.out.println( "mlcp.sh " + mlcp);
		return mlcp;
	}



	private	HubDeployStatusListener getListener() {
		// callback for status updates
		HubDeployStatusListener listener;
		listener = new HubDeployStatusListener() {
			@Override
			public void onStatusChange(final int percentComplete, final String message) {

			}

			@Override
			public void onError() {
				System.out.println("DGB in error");
			}
		};
		return listener;
	}


	@RequestMapping(value = "/deployToDHS", method = RequestMethod.GET)
	public void deployToDHS() {
		// Deploy entities and flows to DHS
		// Does the equivalent of ./gradlew hubDeploy -PenvironmentName=dhs -i

		final HubDeployStatusListener listener =  getListener() ;

		System.out.println("DGB start");

		final String dhfDir = hubConfig.getHubProject().getProjectDirString();

		System.out.println("DGB got dir: " + dhfDir);

		final Properties prop = new Properties();
		try (InputStream input = new FileInputStream(dhfDir + "/gradle-dhs.properties")) {
			prop.load(input);
			hubConfig.setHost(prop.getProperty("mlHost"));
			hubConfig.setMlUsername(prop.getProperty("mlUsername"));
			hubConfig.setMlPassword(prop.getProperty("mlPassword"));

			hubConfig.withPropertiesFromEnvironment("dhs");
			hubConfig.resetHubConfigs();

			hubConfig.refreshProject();

			hubConfig.setAuthMethod(DatabaseKind.STAGING, "basic");
			hubConfig.setSimpleSsl(DatabaseKind.STAGING, true);

			System.out.println("DGB hubConfig: " + hubConfig.getInfo().toString() );

			final DhsDeployer dhsDeployer = new DhsDeployer();

			dhsDeployer.deployAsSecurityAdmin(hubConfig);
			dhsDeployer.deployAsDeveloper(hubConfig);

		} catch (final IOException e) {
			System.out.println("Cannot read gradle-dhs.properties");
		}


	}

}


