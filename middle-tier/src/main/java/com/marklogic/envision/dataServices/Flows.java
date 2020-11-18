package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface Flows {
    /**
     * Creates a Flows object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static Flows on(DatabaseClient db) {
        final class FlowsImpl implements Flows {
            private BaseProxy baseProxy;

            private FlowsImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/flows/");
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode newStepInfo() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("newStepInfo.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getFlows(com.fasterxml.jackson.databind.node.ArrayNode flowsToGet) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getFlows.sjs", BaseProxy.ParameterValuesKind.SINGLE_NODE)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("flowsToGet", true, BaseProxy.ArrayType.fromArrayNode(flowsToGet)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getSample(String uri, com.fasterxml.jackson.databind.JsonNode namespaces) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getSample.xqy", BaseProxy.ParameterValuesKind.MULTIPLE_MIXED)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("uri", false, BaseProxy.StringType.fromString(uri)),
                    BaseProxy.documentParam("namespaces", false, BaseProxy.JsonDocumentType.fromJsonNode(namespaces)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public String previewMapping(String mappingName, Integer mappingVersion, String format, String uri) {
              return BaseProxy.StringType.toString(
                baseProxy
                .request("previewMapping.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("mappingName", false, BaseProxy.StringType.fromString(mappingName)),
                    BaseProxy.atomicParam("mappingVersion", false, BaseProxy.IntegerType.fromInteger(mappingVersion)),
                    BaseProxy.atomicParam("format", false, BaseProxy.StringType.fromString(format)),
                    BaseProxy.atomicParam("uri", false, BaseProxy.StringType.fromString(uri)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }

        }

        return new FlowsImpl(db);
    }

  /**
   * Invokes the newStepInfo operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode newStepInfo();

  /**
   * Invokes the getFlows operation on the database server
   *
   * @param flowsToGet	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getFlows(com.fasterxml.jackson.databind.node.ArrayNode flowsToGet);

  /**
   * Invokes the getSample operation on the database server
   *
   * @param uri	provides input
   * @param namespaces	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getSample(String uri, com.fasterxml.jackson.databind.JsonNode namespaces);

  /**
   * Invokes the previewMapping operation on the database server
   *
   * @param mappingName	provides input
   * @param mappingVersion	provides input
   * @param format	provides input
   * @param uri	provides input
   * @return	as output
   */
    String previewMapping(String mappingName, Integer mappingVersion, String format, String uri);

}
