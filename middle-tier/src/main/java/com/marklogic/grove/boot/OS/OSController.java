package com.marklogic.grove.boot.OS;

import com.marklogic.envision.deploy.DeployHubService;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.deploy.util.HubDeployStatusListener;
import com.marklogic.hub.dhs.DhsDeployer;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.hub.impl.EntityManagerImpl;
import com.marklogic.hub.impl.FlowManagerImpl;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.hub.util.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/os/")

public class OSController extends AbstractController {

	private final FlowManagerImpl flowManager;
	private final EntityManagerImpl entityManager;
	private final DeployHubService deployService;

	@Autowired
	OSController(HubConfigImpl hubConfig, FlowManagerImpl flowManager, EntityManagerImpl entityManager, DeployHubService deployService) {
		super(hubConfig);
		this.flowManager = flowManager;
		this.entityManager = entityManager;
		this.deployService = deployService;
	}

	// returns JSON (or string) containining details of the DH project config
	@RequestMapping(value = "/getDHprojectConfig", method = RequestMethod.GET)
	public String getDHprojectConfig()  {
		ArrayList<JSONObject> config = new ArrayList<>();

		final JSONObject dirObj = new JSONObject();
		final String dhfDir = getHubConfig().getHubProject().getProjectDirString();
		dirObj.put("prop", "Project");
		dirObj.put("val", dhfDir);

		config.add(dirObj);

		final JSONObject hostObj = new JSONObject();
		final String myHostName = getHubConfig().getHost();
		hostObj.put("prop", "Host");
		hostObj.put("val", myHostName);

		config.add(hostObj);

		// get flows
		final JSONObject flowsObj = new JSONObject();
		final ArrayList<String> arrFlows = new ArrayList<>();
		final List<Flow> flows = flowManager.getFlows();
		for (final Flow flow : flows) {
			arrFlows.add(flow.getName());
		}
		flowsObj.put("prop", "Flows");
		flowsObj.put("val", arrFlows.toString() );

		config.add(flowsObj);

		// get Entities
		final JSONObject entitiesObj = new JSONObject();
		final ArrayList<String> arrEntities = new ArrayList<>();
		final List<HubEntity> entities = entityManager.getEntities();
		for (final HubEntity entity : entities) {
			arrEntities.add(entity.getInfo().getTitle());
		}
		entitiesObj.put("prop", "Entities");
		entitiesObj.put("val", arrEntities.toString() );

		config.add(entitiesObj);

		return config.toString();
	}

	@RequestMapping(value = "/runFlows", method = RequestMethod.POST)
	public String runFlows() {
		StringBuilder output = new StringBuilder();
		// get Ingestion flows
		final List<Flow> flows = flowManager.getFlows();

		for (final Flow flow : flows) {
			try {
				_runFlow(flow.getName());
				output.append("Ran flow ").append(flow.getName()).append(" ");
			} catch (final Error e) {
				System.out.println("Cannot run flow " + flow.getName());
				output.append("Error running flow ").append(flow.getName()).append(" ");
			}
		}
		return output.toString();
	}

	@RequestMapping(value = "/getFlowNames", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ArrayList<String> getFlowNames() {
		// get flows
		ArrayList<String> arrFlows = new ArrayList<>();

		final List<Flow> flows = flowManager.getFlows();
		for (final Flow flow : flows) {
			arrFlows.add(flow.getName());
		}
		return arrFlows;
	}

	@RequestMapping(value = "/runFlow", method = RequestMethod.POST)
	public String runFlow(final String flowName)  {
		String output;
		try {
				_runFlow(flowName);
				output = "Ran flow " + flowName;
			}catch(final Error e){
				System.out.println("Cannot run flow " + flowName);
				output = "Error running flow " + flowName;
			}
		return output;
	}

	private HubDeployStatusListener getListener() {
		// callback for status updates
		HubDeployStatusListener listener;
		listener = new HubDeployStatusListener() {
			@Override
			public void onStatusChange(final int percentComplete, final String message) {
				// TODO access view's progress indicator
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
		// if is provisioned environment we are deploying to the cloud
		if (getHubConfig().getIsProvisionedEnvironment()) {
			try {
				this.deployToDHS();
			} catch (final Exception e) {
				System.out.println("Cannot deploy to DHS");
			}
		} else {
			try {
				this.deployToNonProvisioned();
			} catch (final Exception e) {
				System.out.println("Cannot deploy to non-provisioned environment");
			}
		}
		return "Hub deployed.";
	}

	// dispatch to DHSDeployer
	private void deployToDHS() {
		final DhsDeployer dhsDeployer = new DhsDeployer();

		HubConfigImpl hubConfig = getHubConfig();
		dhsDeployer.deployAsSecurityAdmin(hubConfig);
		dhsDeployer.deployAsDeveloper(hubConfig);
	}

	private boolean deployToNonProvisioned() {
		boolean result = false;
		final HubDeployStatusListener listener = getListener();
		try {
			result = deployService.deployHubInstall(listener);
		} catch (final Error error) {
			error.printStackTrace();
		}
		return result;
	}

	private void _runFlow(final String flowName) {
		final FlowRunner flowRunner = new FlowRunnerImpl(getHubConfig());
		System.out.println("Running flow: " + flowName);
		final FlowInputs inputs = new FlowInputs(flowName);
		final RunFlowResponse response = flowRunner.runFlow(inputs);
		flowRunner.awaitCompletion();
		System.out.println("Response: " + response);
	}
}


