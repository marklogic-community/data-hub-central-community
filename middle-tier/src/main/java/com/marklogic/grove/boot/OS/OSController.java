package com.marklogic.grove.boot.os;

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
import com.marklogic.hub.step.StepDefinition;
import com.marklogic.hub.step.impl.Step;
import com.marklogic.hub.util.json.JSONObject;
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

import java.util.*;
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
	//private HubConfig hubConfig;
	@Autowired
	HubConfigImpl hubConfig;

	@Autowired
	DataHubImpl datahub;

	@Autowired
	FlowManagerImpl flowManager;
	//	FlowManager flowManager;

	@Autowired
	EntityManagerImpl entityManager;

	@Autowired
	FlowRunner fr;

	// runs a gradle task
	@RequestMapping(value = "/gradle", method = RequestMethod.POST)
	public String getGradle(@RequestBody ObjectNode searchRequest) {
		String dhfDir = hubConfig.getHubProject().getProjectDirString();
		String task = new String();

		task = "tasks" ; // default value
		if (searchRequest.has("task")) {
			task = searchRequest.get("task").asText();

		}

		ProcessBuilder builder = new ProcessBuilder();
		if (isWindows) {
			// Not tested
			builder.command("cmd.exe", "/c", "cd " + dhfDir + " & gradlew " + task );
		} else {
			// task = "tasks"; // debug to do a gradle tasks
			builder.command("sh", "-c", "cd " + dhfDir + "; sh gradlew " + task);
		}

		String output = runProcess( builder );

		return "Done with " + task + " - "  + output;
	}
	private String runProcess (ProcessBuilder builder) {
		// see https://www.baeldung.com/java-lang-processbuilder-api
		System.out.println ("Start of runProcess");
		String output = new String();

		try {
			Process process = builder.start();
			if( process.getErrorStream().read() != -1 ){
				// convert process.getErrorStream() to a string
				StringWriter writer = new StringWriter();
				IOUtils.copy(process.getErrorStream(), writer);
				output = "Errors " + writer.toString();
			} else {
				StringWriter writer = new StringWriter();
				IOUtils.copy(process.getInputStream(), writer);
				output = "Output " + writer.toString();
			}


		} catch(IOException e) {
			e.printStackTrace();
		}
		System.out.println ("End of runProcess");
		return output;
	};

	// returns JSON (or string) containining details of the DH project config
	@RequestMapping(value = "/getDHprojectConfig", method = RequestMethod.GET)
	public String getDHprojectConfig()  {
		JSONObject config = new JSONObject();

		// Does grade-dhs.properties exists in the datahub project dir?
		String dhfDir = hubConfig.getHubProject().getProjectDirString();
		File dhsConfigFile = new File(dhfDir + "/gradle-dhs.properties");
		config.put("dhsConfigFileExists", dhsConfigFile.exists());

		// get flows
		ArrayList<String> arrFlows = new ArrayList<String>();

		List<Flow> flows = flowManager.getFlows();
		for(Flow flow : flows) {
			System.out.println("DGB got flow: " + flow.getName());
			arrFlows.add(flow.getName());
		}
		config.put("flows", arrFlows.toString());

		// get Entities
		ArrayList<String> arrEntities = new ArrayList<String>();
		List<HubEntity> entities = entityManager.getEntities();
		for(HubEntity entity : entities) {
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
	public void setGradleProps(HttpEntity<String> httpEntity) {

		String dhsConfig = httpEntity.getBody();
		String dhfDir = hubConfig.getHubProject().getProjectDirString();

		File dhsPropertiesFile = new File(dhfDir + "/gradle-dhs.properties");

		if (dhsPropertiesFile.exists() ) {
			System.out.println("Creating backup copy of gradle-dhs.properties");
			File safeCopy = new File(dhfDir + "/gradle-dhs.properties." + Instant.now().getEpochSecond() );

			try {
				FileUtils.moveFile(dhsPropertiesFile, safeCopy);
			} catch (IOException e) {
				System.out.println("Error making backup copy of gradle-dhs.properties ");
				e.printStackTrace();
			}
		}

		try {
			FileUtils.write(dhsPropertiesFile, dhsConfig);
		}  catch (IOException e) {
			System.out.println("Error creating gradle-dhs.properties");
			e.printStackTrace();
		}

		return ;
	}


	// runs ingestion steps using MLCP/DMSDK?
	@RequestMapping(value = "/runIngestSteps", method = RequestMethod.GET)
	public String runIngestSteps()  {

		// read the DHS properties file
		String dhfDir = hubConfig.getHubProject().getProjectDirString();
		String output = "";
		Properties prop = new Properties();

		ProcessBuilder builder = new ProcessBuilder();

		try (InputStream input = new FileInputStream(dhfDir + "/gradle-dhs.properties")) {
			prop.load(input);

			// get Ingestion flows
			ArrayList<String> arrFlows = new ArrayList<String>();
			List<Flow> flows = flowManager.getFlows();



			for(Flow flow : flows) {
				System.out.println("DGB got flow: " + flow.getName());
				Map<String, Step> steps = flow.getSteps();
				Integer i = 0;
				for (Map.Entry<String, Step> step: steps.entrySet()){
					i++;
					if (step.getValue().getStepDefinitionType().toString() == "ingestion") {

						String mlcp = getMLCPCommand(prop, flow, step.getValue(), i);

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

		} catch (IOException e){
			System.out.println("Cannot read gradle-dhs.properties");
		}

		return output;
	}
	// runs all flow steps except for ingestion
	@RequestMapping(value = "/runFlowsWithoutIngestionSteps", method = RequestMethod.GET)
	public String runFlowsWithoutIngestionSteps()  {

		// read the DHS properties file
		String dhfDir = hubConfig.getHubProject().getProjectDirString();
		String output = "";
		Properties prop = new Properties();

		ProcessBuilder builder = new ProcessBuilder();

		try (InputStream input = new FileInputStream(dhfDir + "/gradle-dhs.properties")) {
			prop.load(input);

			// get Ingestion flows
			ArrayList<String> arrFlows = new ArrayList<String>();
			List<Flow> flows = flowManager.getFlows();

			for(Flow flow : flows) {
				System.out.println("DGB got flow: " + flow.getName());
				Map<String, Step> steps = flow.getSteps();
				ArrayList<String> arrSteps = new ArrayList<>();


				int i = 0;
				for (Map.Entry<String, Step> step: steps.entrySet()){
					i++;
					if (!step.getValue().getStepDefinitionType().toString().equals("ingestion")) {
						arrSteps.add(Integer.toString(i));
					}
				}
				System.out.println("DGB flow: " + flow.getName() + " steps: " + String.join("," , arrSteps) );

				RunFlowResponse runNonIngestSteps = fr.runFlow(flow.getName(), String.join("," , arrSteps));

				fr.awaitCompletion();
				output += " " + flow.getName();
			}

		} catch (IOException e){
			System.out.println("Cannot read gradle-dhs.properties");
		}

		return "Processed: " + output;
	}

	private String getMLCPCommand(Properties prop,  Flow flow,  Step step, int stepNumber) {
		Map<String, Object> stepOptions = step.getOptions();
		JsonNode jsonFileLocs = step.getFileLocations();

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
			public void onStatusChange(int percentComplete, String message) {

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

		HubDeployStatusListener listener =  getListener() ;

		System.out.println("DGB start");

		String dhfDir = hubConfig.getHubProject().getProjectDirString();

		System.out.println("DGB got dir: " + dhfDir);

		Properties prop = new Properties();
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

			DhsDeployer dhsDeployer = new DhsDeployer();

			dhsDeployer.deployAsSecurityAdmin(hubConfig);
			dhsDeployer.deployAsDeveloper(hubConfig);

		} catch (IOException e) {
			System.out.println("Cannot read gradle-dhs.properties");
		}


	}

}


