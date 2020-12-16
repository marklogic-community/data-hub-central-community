package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface EntityModeller {
    /**
     * Creates a EntityModeller object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static EntityModeller on(DatabaseClient db) {
        final class EntityModellerImpl implements EntityModeller {
            private BaseProxy baseProxy;

            private EntityModellerImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/model/");
            }

            @Override
            public Boolean updateRedaction(com.fasterxml.jackson.databind.JsonNode oldModel, com.fasterxml.jackson.databind.JsonNode newModel) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("updateRedaction.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_NODES)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("oldModel", true, BaseProxy.JsonDocumentType.fromJsonNode(oldModel)),
                    BaseProxy.documentParam("newModel", false, BaseProxy.JsonDocumentType.fromJsonNode(newModel)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean needsImport(com.fasterxml.jackson.databind.node.ArrayNode models) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("needsImport.sjs", BaseProxy.ParameterValuesKind.SINGLE_NODE)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("models", false, BaseProxy.ArrayType.fromArrayNode(models)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getActiveIndexes() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getActiveIndexes.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode createTdes() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("createTdes.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public Boolean removeAllEntities(String user) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("removeAllEntities.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("user", true, BaseProxy.StringType.fromString(user)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean updatePii(com.fasterxml.jackson.databind.JsonNode oldModel, com.fasterxml.jackson.databind.JsonNode newModel) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("updatePii.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_NODES)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("oldModel", false, BaseProxy.JsonDocumentType.fromJsonNode(oldModel)),
                    BaseProxy.documentParam("newModel", false, BaseProxy.JsonDocumentType.fromJsonNode(newModel)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode fromDatahub() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("fromDatahub.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getCurrentModel() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getCurrentModel.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(true, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode toDatahub() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("toDatahub.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new EntityModellerImpl(db);
    }

  /**
   * Invokes the updateRedaction operation on the database server
   *
   * @param oldModel	provides input
   * @param newModel	provides input
   * @return	as output
   */
    Boolean updateRedaction(com.fasterxml.jackson.databind.JsonNode oldModel, com.fasterxml.jackson.databind.JsonNode newModel);

  /**
   * Invokes the needsImport operation on the database server
   *
   * @param models	provides input
   * @return	as output
   */
    Boolean needsImport(com.fasterxml.jackson.databind.node.ArrayNode models);

  /**
   * Invokes the getActiveIndexes operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getActiveIndexes();

  /**
   * Invokes the createTdes operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode createTdes();

  /**
   * Invokes the removeAllEntities operation on the database server
   *
   * @param user	provides input
   * @return	as output
   */
    Boolean removeAllEntities(String user);

  /**
   * Invokes the updatePii operation on the database server
   *
   * @param oldModel	provides input
   * @param newModel	provides input
   * @return	as output
   */
    Boolean updatePii(com.fasterxml.jackson.databind.JsonNode oldModel, com.fasterxml.jackson.databind.JsonNode newModel);

  /**
   * Invokes the fromDatahub operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode fromDatahub();

  /**
   * Invokes the getCurrentModel operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getCurrentModel();

  /**
   * Invokes the toDatahub operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode toDatahub();

}
