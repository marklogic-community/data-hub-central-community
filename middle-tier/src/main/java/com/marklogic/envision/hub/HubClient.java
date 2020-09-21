package com.marklogic.envision.hub;

import com.marklogic.client.DatabaseClient;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.ManageClient;

public interface HubClient {

	/**
	 * @return the name of the MarkLogic user associated with this client
	 */
	String getUsername();

	DatabaseClient getStagingClient();

	DatabaseClient getFinalClient();

	DatabaseClient getJobsClient();

	DatabaseClient getModulesClient();

	DatabaseClient getStagingSchemasClient();

	DatabaseClient getFinalSchemasClient();

	String getDbName(DatabaseKind kind);

	ManageClient getManageClient();

	HubConfigImpl getHubConfig();
}
