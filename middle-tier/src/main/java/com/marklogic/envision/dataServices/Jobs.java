package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface Jobs {
    /**
     * Creates a Jobs object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static Jobs on(DatabaseClient db) {
        final class JobsImpl implements Jobs {
            private BaseProxy baseProxy;

            private JobsImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/jobs/");
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode getJobs(String flowName) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getJobs.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("flowName", false, BaseProxy.StringType.fromString(flowName)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode deleteJob(String jobId) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("deleteJob.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("jobId", false, BaseProxy.StringType.fromString(jobId)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new JobsImpl(db);
    }

  /**
   * Invokes the getJobs operation on the database server
   *
   * @param flowName	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getJobs(String flowName);

  /**
   * Invokes the deleteJob operation on the database server
   *
   * @param jobId	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode deleteJob(String jobId);

}
