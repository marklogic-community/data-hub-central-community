package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface SystemUtils {
    /**
     * Creates a SystemUtils object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static SystemUtils on(DatabaseClient db) {
        final class SystemUtilsImpl implements SystemUtils {
            private BaseProxy baseProxy;

            private SystemUtilsImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/system/");
            }

            @Override
            public Boolean deleteDatasource(String collection) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("deleteDatasource.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("collection", false, BaseProxy.StringType.fromString(collection)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode resetSystem() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("resetSystem.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new SystemUtilsImpl(db);
    }

  /**
   * Invokes the deleteDatasource operation on the database server
   *
   * @param collection	provides input
   * @return	as output
   */
    Boolean deleteDatasource(String collection);

  /**
   * Invokes the resetSystem operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode resetSystem();

}
