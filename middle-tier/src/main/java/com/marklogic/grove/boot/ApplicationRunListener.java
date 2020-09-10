package com.marklogic.grove.boot;

import com.marklogic.envision.installer.InstallService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

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
		context.getBean(InstallService.class).install();
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
