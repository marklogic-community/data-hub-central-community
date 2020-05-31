package com.marklogic.dhf.config;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.ext.modulesloader.ssl.SimpleX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A reusable database client connection.
 *
 * @author Drew Wanczowski
 */
@Configuration
class MarkLogicDatabaseClientConfig {

    private final Logger logger = LoggerFactory.getLogger(MarkLogicDatabaseClientConfig.class);

    private final MarkLogicConfiguration marklogicConfiguration;

    @Autowired
    MarkLogicDatabaseClientConfig(MarkLogicConfiguration marklogicConfiguration) {
        this.marklogicConfiguration = marklogicConfiguration;
    }

    @Bean
    public DatabaseClient getDatabaseClient() {

        logger.info("Creating connection to MarkLogic at " + this.marklogicConfiguration.getHost() + ":" + this.marklogicConfiguration.getPort());

        DatabaseClientFactory.SecurityContext securityContext;
        securityContext = this.marklogicConfiguration.getAuth().equals("digest") ? new DatabaseClientFactory.DigestAuthContext(
                this.marklogicConfiguration.getUser(),
                this.marklogicConfiguration.getPassword()
        ) : new DatabaseClientFactory.BasicAuthContext(
                this.marklogicConfiguration.getUser(),
                this.marklogicConfiguration.getPassword()
        );

        if (this.marklogicConfiguration.isSsl()) {
            securityContext.setSSLHostnameVerifier(DatabaseClientFactory.SSLHostnameVerifier.ANY);
            securityContext = securityContext.withSSLContext(SimpleX509TrustManager.newSSLContext(), new SimpleX509TrustManager());
        }

        return DatabaseClientFactory.newClient(
                this.marklogicConfiguration.getHost(),
                this.marklogicConfiguration.getPort(),
                this.marklogicConfiguration.getDatabase(),
                securityContext,
                DatabaseClient.ConnectionType.GATEWAY
        );
    }

}