package com.marklogic.r2m;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.SessionState;
import com.marklogic.client.io.Format;
import java.io.Reader;
import java.util.stream.Stream;

import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface bulkLoaderDS {
    /**
     * Creates a bulkLoaderDS object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static bulkLoaderDS on(DatabaseClient db) {
        final class bulkLoaderDSImpl implements bulkLoaderDS {
            private BaseProxy baseProxy;

            private bulkLoaderDSImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/dataservices/bulkLoader/");
            }
            @Override
            public SessionState newSessionState() {
              return baseProxy.newSessionState();
            }

            @Override
            public Reader bulkLoadDocs(SessionState session, Reader endpointState, Reader workUnit, Stream<Reader> input) {
              return BaseProxy.XmlDocumentType.toReader(
                baseProxy
                .request("bulkLoader.xqy", BaseProxy.ParameterValuesKind.MULTIPLE_NODES)
                .withSession("session", baseProxy.newSessionState(), true)
                .withParams(
                    BaseProxy.documentParam("endpointState", true, BaseProxy.XmlDocumentType.fromReader(endpointState)),
                    BaseProxy.documentParam("workUnit", true, BaseProxy.JsonDocumentType.fromReader(workUnit)),
                    BaseProxy.documentParam("input", true, BaseProxy.TextDocumentType.fromReader(input)))
                .withMethod("POST")
                .responseSingle(true, Format.XML)
                );
            }
        }

        return new bulkLoaderDSImpl(db);
    }
    /**
     * Creates an object to track a session for a set of operations
     * that require session state on the database server.
     *
     * @return	an object for session state
     */
    SessionState newSessionState();

  /**
   * Invokes the bulkLoadDocs operation on the database server
   *
   * @param session	provides input
   * @param endpointState	provides input
   * @param workUnit	provides input
   * @param input	provides input
   * @return	as output
   */
    Reader bulkLoadDocs(SessionState session, Reader endpointState, Reader workUnit, Stream<Reader> input);

}
