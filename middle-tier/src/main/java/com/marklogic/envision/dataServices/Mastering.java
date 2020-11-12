package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface Mastering {
    /**
     * Creates a Mastering object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static Mastering on(DatabaseClient db) {
        final class MasteringImpl implements Mastering {
            private BaseProxy baseProxy;

            private MasteringImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/mastering/");
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode updateNotifications(com.fasterxml.jackson.databind.JsonNode uris, String status) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("updateNotifications.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_MIXED)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("uris", false, BaseProxy.JsonDocumentType.fromJsonNode(uris)),
                    BaseProxy.atomicParam("status", false, BaseProxy.StringType.fromString(status)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getNotification(String uri) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getNotification.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("uri", false, BaseProxy.StringType.fromString(uri)))
                .withMethod("POST")
                .responseSingle(true, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode block(com.fasterxml.jackson.databind.node.ArrayNode uris) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("block.sjs", BaseProxy.ParameterValuesKind.SINGLE_NODE)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("uris", false, BaseProxy.ArrayType.fromArrayNode(uris)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getNotifications(String qtext, Integer page, Integer pageLength, String sort) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getNotifications.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("qtext", true, BaseProxy.StringType.fromString(qtext)),
                    BaseProxy.atomicParam("page", false, BaseProxy.IntegerType.fromInteger(page)),
                    BaseProxy.atomicParam("pageLength", false, BaseProxy.IntegerType.fromInteger(pageLength)),
                    BaseProxy.atomicParam("sort", true, BaseProxy.StringType.fromString(sort)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode merge(com.fasterxml.jackson.databind.node.ArrayNode uris, String flowName, String stepNumber, Boolean preview, String jobId, Boolean performanceMetrics) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("merge.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_MIXED)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("uris", false, BaseProxy.ArrayType.fromArrayNode(uris)),
                    BaseProxy.atomicParam("flowName", false, BaseProxy.StringType.fromString(flowName)),
                    BaseProxy.atomicParam("stepNumber", true, BaseProxy.StringType.fromString(stepNumber)),
                    BaseProxy.atomicParam("preview", true, BaseProxy.BooleanType.fromBoolean(preview)),
                    BaseProxy.atomicParam("jobId", false, BaseProxy.StringType.fromString(jobId)),
                    BaseProxy.atomicParam("performanceMetrics", true, BaseProxy.BooleanType.fromBoolean(performanceMetrics)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode unmerge(String uri) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("unmerge.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("uri", false, BaseProxy.StringType.fromString(uri)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode unBlock(com.fasterxml.jackson.databind.node.ArrayNode uris) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("unBlock.sjs", BaseProxy.ParameterValuesKind.SINGLE_NODE)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("uris", false, BaseProxy.ArrayType.fromArrayNode(uris)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getBlocks(com.fasterxml.jackson.databind.node.ArrayNode uris) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getBlocks.sjs", BaseProxy.ParameterValuesKind.SINGLE_NODE)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("uris", false, BaseProxy.ArrayType.fromArrayNode(uris)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getHistory(String uri) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getHistory.xqy", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("uri", false, BaseProxy.StringType.fromString(uri)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new MasteringImpl(db);
    }

  /**
   * Invokes the updateNotifications operation on the database server
   *
   * @param uris	provides input
   * @param status	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode updateNotifications(com.fasterxml.jackson.databind.JsonNode uris, String status);

  /**
   * Invokes the getNotification operation on the database server
   *
   * @param uri	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getNotification(String uri);

  /**
   * Invokes the block operation on the database server
   *
   * @param uris	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode block(com.fasterxml.jackson.databind.node.ArrayNode uris);

  /**
   * Invokes the getNotifications operation on the database server
   *
   * @param qtext	provides input
   * @param page	provides input
   * @param pageLength	provides input
   * @param sort	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getNotifications(String qtext, Integer page, Integer pageLength, String sort);

  /**
   * Invokes the merge operation on the database server
   *
   * @param uris	provides input
   * @param flowName	provides input
   * @param stepNumber	provides input
   * @param preview	provides input
   * @param jobId	provides input
   * @param performanceMetrics	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode merge(com.fasterxml.jackson.databind.node.ArrayNode uris, String flowName, String stepNumber, Boolean preview, String jobId, Boolean performanceMetrics);

  /**
   * Invokes the unmerge operation on the database server
   *
   * @param uri	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode unmerge(String uri);

  /**
   * Invokes the unBlock operation on the database server
   *
   * @param uris	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode unBlock(com.fasterxml.jackson.databind.node.ArrayNode uris);

  /**
   * Invokes the getBlocks operation on the database server
   *
   * @param uris	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getBlocks(com.fasterxml.jackson.databind.node.ArrayNode uris);

  /**
   * Invokes the getHistory operation on the database server
   *
   * @param uri	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getHistory(String uri);

}
