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
            public com.fasterxml.jackson.databind.JsonNode getFlows() {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getFlows.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new FlowsImpl(db);
    }

  /**
   * Invokes the getFlows operation on the database server
   *
   * 
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getFlows();

}
