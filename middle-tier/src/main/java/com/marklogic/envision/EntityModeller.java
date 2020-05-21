package com.marklogic.envision;

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
                baseProxy = new BaseProxy(dbClient, "/entities/model/");
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
            public Boolean removeAllEntities() {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("removeAllEntities.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
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
   * 
   * @return	as output
   */
    Boolean removeAllEntities();

  /**
   * Invokes the fromDatahub operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode fromDatahub();

  /**
   * Invokes the toDatahub operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode toDatahub();

}
