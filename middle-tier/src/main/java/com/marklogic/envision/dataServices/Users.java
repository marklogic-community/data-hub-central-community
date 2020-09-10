package com.marklogic.envision.dataServices;

// IMPORTANT: Do not edit. This file is generated.

import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractWriteHandle;


import com.marklogic.client.DatabaseClient;

import com.marklogic.client.impl.BaseProxy;

/**
 * Provides a set of operations on the database server
 */
public interface Users {
    /**
     * Creates a Users object for executing operations on the database server.
     *
     * The DatabaseClientFactory class can create the DatabaseClient parameter. A single
     * client object can be used for any number of requests and in multiple threads.
     *
     * @param db	provides a client for communicating with the database server
     * @return	an object for session state
     */
    static Users on(DatabaseClient db) {
        final class UsersImpl implements Users {
            private BaseProxy baseProxy;

            private UsersImpl(DatabaseClient dbClient) {
                baseProxy = new BaseProxy(dbClient, "/envision/users/");
            }

            @Override
            public Boolean addResetToken(String email, String token, java.util.Date expiry) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("addResetToken.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("email", false, BaseProxy.StringType.fromString(email)),
                    BaseProxy.atomicParam("token", false, BaseProxy.StringType.fromString(token)),
                    BaseProxy.atomicParam("expiry", false, BaseProxy.DateTimeType.fromDate(expiry)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean testLogin() {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("testLogin.sjs", BaseProxy.ParameterValuesKind.NONE)
                .withSession()
                .withParams(
                    )
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean userExists(String email) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("userExists.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("email", false, BaseProxy.StringType.fromString(email)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean updatePassword(String email, String password) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("updatePassword.sjs", BaseProxy.ParameterValuesKind.MULTIPLE_ATOMICS)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("email", false, BaseProxy.StringType.fromString(email)),
                    BaseProxy.atomicParam("password", false, BaseProxy.StringType.fromString(password)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean createUser(com.fasterxml.jackson.databind.JsonNode user) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("createUser.sjs", BaseProxy.ParameterValuesKind.SINGLE_NODE)
                .withSession()
                .withParams(
                    BaseProxy.documentParam("user", false, BaseProxy.JsonDocumentType.fromJsonNode(user)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public Boolean validateToken(String token) {
              return BaseProxy.BooleanType.toBoolean(
                baseProxy
                .request("validateToken.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("token", false, BaseProxy.StringType.fromString(token)))
                .withMethod("POST")
                .responseSingle(false, null)
                );
            }


            @Override
            public com.fasterxml.jackson.databind.JsonNode validateResetToken(String token) {
              return BaseProxy.JsonDocumentType.toJsonNode(
                baseProxy
                .request("validateResetToken.sjs", BaseProxy.ParameterValuesKind.SINGLE_ATOMIC)
                .withSession()
                .withParams(
                    BaseProxy.atomicParam("token", false, BaseProxy.StringType.fromString(token)))
                .withMethod("POST")
                .responseSingle(false, Format.JSON)
                );
            }

        }

        return new UsersImpl(db);
    }

  /**
   * Invokes the addResetToken operation on the database server
   *
   * @param email	provides input
   * @param token	provides input
   * @param expiry	provides input
   * @return	as output
   */
    Boolean addResetToken(String email, String token, java.util.Date expiry);

  /**
   * Invokes the testLogin operation on the database server
   *
   * 
   * @return	as output
   */
    Boolean testLogin();

  /**
   * Invokes the userExists operation on the database server
   *
   * @param email	provides input
   * @return	as output
   */
    Boolean userExists(String email);

  /**
   * Invokes the updatePassword operation on the database server
   *
   * @param email	provides input
   * @param password	provides input
   * @return	as output
   */
    Boolean updatePassword(String email, String password);

  /**
   * Invokes the createUser operation on the database server
   *
   * @param user	provides input
   * @return	as output
   */
    Boolean createUser(com.fasterxml.jackson.databind.JsonNode user);

  /**
   * Invokes the validateToken operation on the database server
   *
   * @param token	provides input
   * @return	as output
   */
    Boolean validateToken(String token);

  /**
   * Invokes the validateResetToken operation on the database server
   *
   * @param token	provides input
   * @return	as output
   */
    com.fasterxml.jackson.databind.JsonNode validateResetToken(String token);

}
