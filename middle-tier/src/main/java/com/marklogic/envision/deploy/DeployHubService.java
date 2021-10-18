package com.marklogic.envision.deploy;

import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.hub.DataHub;
import com.marklogic.hub.impl.DataHubImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
public class DeployHubService {

	private final DataHub dataHub;

	@Autowired
	public DeployHubService(EnvisionConfig envisionConfig) {
		this.dataHub = new DataHubImpl(envisionConfig.getHubConfig());
	}

	public boolean deployHubInstall() {
        try {
            dataHub.install();
            return true;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return false;
	}

	private String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
