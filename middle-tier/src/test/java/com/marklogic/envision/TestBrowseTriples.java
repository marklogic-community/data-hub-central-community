package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.Triples;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.grove.boot.Application;
import com.marklogic.hub.HubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)

public class TestBrowseTriples extends BaseTest {

	@BeforeEach
	void setUp() throws IOException {
		envisionConfig.setMultiTenant(true);
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);

		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient finalClient = hubClient.getFinalClient();
		installDoc(finalClient, "know/taxonomy.json", "/taxonomy.json", "taxonomy", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "know/doc.json", "/doc.json", "doc", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
	}

	@Test
	void bug144() throws Exception {
		JsonNode resp = Triples.on(getFinalClient()).browseTriples("ID-f27d7d86-1b2f-4352-bd9c-73820ea0755c", 1, 10, 100, "DESC");
		System.out.println(objectMapper.writeValueAsString(resp));
		jsonAssertEquals(getResource("output/know/browseOutput.json"), resp, false);
	}

}
