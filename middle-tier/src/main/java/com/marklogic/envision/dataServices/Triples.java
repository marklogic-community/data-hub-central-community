package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface Triples {
    /**
     * Creates a Triples object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static Triples on(DatabaseClient db) {
        final class TriplesImpl implements Triples {
            private BaseProxy baseProxy;

            private TriplesImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/triples/");
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode browseTriples(String qtext, Integer page, Integer subjectsPerPage, Integer linksPerSubject, String sort, String dedup) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("browseTriples.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("qtext", true, BaseProxy.StringType.fromString(qtext)),
                    BaseProxy.atomicParam("page", false, BaseProxy.IntegerType.fromInteger(page)),
                    BaseProxy.atomicParam("subjectsPerPage", false, BaseProxy.IntegerType.fromInteger(subjectsPerPage)),
                    BaseProxy.atomicParam("linksPerSubject", true, BaseProxy.IntegerType.fromInteger(linksPerSubject)),
                    BaseProxy.atomicParam("sort", false, BaseProxy.StringType.fromString(sort)),
                    BaseProxy.atomicParam("dedup", false, BaseProxy.StringType.fromString(dedup)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode getRelated(String item, String itemId, Boolean isIRI, String qtext, String predicate, Integer maxRelated, Boolean filterText) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("getRelated.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("item", false, BaseProxy.StringType.fromString(item)),
                    BaseProxy.atomicParam("itemId", false, BaseProxy.StringType.fromString(itemId)),
                    BaseProxy.atomicParam("isIRI", false, BaseProxy.BooleanType.fromBoolean(isIRI)),
                    BaseProxy.atomicParam("qtext", true, BaseProxy.StringType.fromString(qtext)),
                    BaseProxy.atomicParam("predicate", true, BaseProxy.StringType.fromString(predicate)),
                    BaseProxy.atomicParam("maxRelated", false, BaseProxy.IntegerType.fromInteger(maxRelated)),
                    BaseProxy.atomicParam("filterText", false, BaseProxy.BooleanType.fromBoolean(filterText)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new TriplesImpl(db);
    }

  /**
   * Invokes the browseTriples operation on the database server
   *
   * @param qtext	provides input
   * @param page	provides input
   * @param subjectsPerPage	provides input
   * @param linksPerSubject	provides input
   * @param sort	provides input
   * @param dedup	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode browseTriples(String qtext, Integer page, Integer subjectsPerPage, Integer linksPerSubject, String sort, String dedup);

  /**
   * Invokes the getRelated operation on the database server
   *
   * @param item	provides input
   * @param itemId	provides input
   * @param isIRI	provides input
   * @param qtext	provides input
   * @param predicate	provides input
   * @param maxRelated	provides input
   * @param filterText	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode getRelated(String item, String itemId, Boolean isIRI, String qtext, String predicate, Integer maxRelated, Boolean filterText);

}
