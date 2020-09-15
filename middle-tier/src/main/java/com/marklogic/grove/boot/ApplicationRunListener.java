package com.marklogic.grove.boot;

import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.envision.installer.InstallService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;

@Profile("!test")
public class ApplicationRunListener implements SpringApplicationRunListener {

    public ApplicationRunListener(SpringApplication application, String[] args) { }

    @Override
    public void starting() {}

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {}

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {}

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {}

    @Override
    public void started(ConfigurableApplicationContext context) {
    	EnvisionConfig envisionConfig = context.getBean(EnvisionConfig.class);
    	if (envisionConfig.autoInstall) {
			boolean forceInstall = context.getEnvironment().getProperty("forceInstall", "false").equals("true");
			context.getBean(InstallService.class).install(forceInstall);
		}
        String port = context.getEnvironment().getProperty("local.server.port");
        System.out.println("Web UI is Ready and Listening on port " + port + ".\n");
        System.out.println("Open your browser to http://localhost:" + port + ".\t(We recommend you use Chrome or FireFox.)");
    }

    @Override
    public void running(ConfigurableApplicationContext context) { }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        throw new RuntimeException(exception);
    }
}
