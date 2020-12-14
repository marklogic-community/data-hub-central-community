package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.dataServices.Flows;
import com.marklogic.envision.hub.HubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GetSampleDocTest extends BaseTest {

	@BeforeEach
	void setUp() throws IOException {
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		installDoc(hubClient.getStagingClient(), "getSamples/envelopedJSON.json", "/envelopedJSON.json", "Test");
		installDoc(hubClient.getStagingClient(), "getSamples/envelopedXML.xml", "/envelopedXML.xml", "Test");
		installDoc(hubClient.getStagingClient(), "getSamples/JSON.json", "/JSON.json", "Test");
		installDoc(hubClient.getStagingClient(), "getSamples/xml.xml", "/xml.xml", "Test");
	}

	@Test
	void testEnvelopedJSON() throws Exception {
		JsonNode actual = Flows.on(getNonAdminHubClient().getStagingClient()).getSample("/envelopedJSON.json", objectMapper.readTree("{}"));
		System.out.println(objectMapper.writeValueAsString(actual));
		jsonAssertEquals(getResource("getSamples/output/envelopedJSON.json"), actual);
	}

	@Test
	void testJSON() throws Exception {
		JsonNode actual = Flows.on(getNonAdminHubClient().getStagingClient()).getSample("/JSON.json", objectMapper.readTree("{}"));
		System.out.println(objectMapper.writeValueAsString(actual));
		jsonAssertEquals(getResource("getSamples/output/envelopedJSON.json"), actual);
	}

	@Test
	void testEnvelopedXMLNoNS() throws Exception {
		JsonNode actual = Flows.on(getNonAdminHubClient().getStagingClient()).getSample("/envelopedXML.xml", objectMapper.readTree("{}"));
		System.out.println(objectMapper.writeValueAsString(actual));
		jsonAssertEquals(getResource("getSamples/output/envelopedXML.json"), actual);
	}

	@Test
	void testEnvelopedXMLWithNS() throws Exception {
		JsonNode actual = Flows.on(getNonAdminHubClient().getStagingClient()).getSample("/envelopedXML.xml", objectMapper.readTree("{\"blah\": \"http://fake-ns\"}"));
		System.out.println(objectMapper.writeValueAsString(actual));
		jsonAssertEquals(getResource("getSamples/output/envelopedXMLNamespace.json"), actual);
	}

	@Test
	void testXMLNoNS() throws Exception {
		JsonNode actual = Flows.on(getNonAdminHubClient().getStagingClient()).getSample("/xml.xml", objectMapper.readTree("{}"));
		System.out.println(objectMapper.writeValueAsString(actual));
		jsonAssertEquals(getResource("getSamples/output/xml.json"), actual);
	}

	@Test
	void testXMLWithNS() throws Exception {
		JsonNode actual = Flows.on(getNonAdminHubClient().getStagingClient()).getSample("/xml.xml", objectMapper.readTree("{\"blah\": \"http://fake-ns\"}"));
		System.out.println(objectMapper.writeValueAsString(actual));
		jsonAssertEquals(getResource("getSamples/output/xmlNamespace.json"), actual);
	}

}
