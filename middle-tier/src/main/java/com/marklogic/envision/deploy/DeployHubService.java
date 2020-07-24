package com.marklogic.envision.deploy;

import com.marklogic.hub.DataHub;
import com.marklogic.hub.deploy.util.HubDeployStatusListener;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
public class DeployHubService {

	@Autowired
	private DataHub dataHub;

	public boolean deployHubInstall(HubDeployStatusListener listener) {
        try {
            dataHub.install(listener);
            return true;
        } catch(Throwable e) {
            e.printStackTrace();
            listener.onStatusChange(-1, getStackTrace(e));
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
