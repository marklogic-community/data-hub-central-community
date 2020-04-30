package com.marklogic.envision;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;


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
                baseProxy = new BaseProxy(dbClient, "/entities/mastering/");
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

        }

        return new MasteringImpl(db);
    }

  /**
   * Invokes the unmerge operation on the database server
   *
   * @param uri	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode unmerge(String uri);

}
