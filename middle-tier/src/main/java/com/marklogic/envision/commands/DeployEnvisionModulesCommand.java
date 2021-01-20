package com.marklogic.envision.commands;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.command.AbstractCommand;
import com.marklogic.appdeployer.command.CommandContext;
import com.marklogic.appdeployer.command.SortOrderConstants;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.file.*;
import com.marklogic.client.ext.modulesloader.impl.AssetFileLoader;
import com.marklogic.client.ext.modulesloader.impl.DefaultModulesFinder;
import com.marklogic.client.ext.modulesloader.impl.DefaultModulesLoader;
import com.marklogic.client.ext.tokenreplacer.DefaultTokenReplacer;
import com.marklogic.client.ext.tokenreplacer.TokenReplacer;

import java.util.Map;
import java.util.Properties;

public class DeployEnvisionModulesCommand extends AbstractCommand {
	private final DatabaseClient modulesClient;
	private final boolean isMultitenant;

	private Throwable caughtException;



	public DeployEnvisionModulesCommand(DatabaseClient modulesClient, boolean isMultitenant) {
		this.modulesClient = modulesClient;
		this.isMultitenant = isMultitenant;
		setExecuteSortOrder(SortOrderConstants.LOAD_MODULES - 1);
	}

	@Override
	public void execute(CommandContext context) {
		AssetFileLoader assetFileLoader = new AssetFileLoader(modulesClient);
		prepareAssetFileLoader(assetFileLoader, context);

		DefaultModulesLoader modulesLoader = new DefaultModulesLoader(assetFileLoader);
		modulesLoader.setModulesManager(null);
		modulesLoader.addFailureListener((throwable, client) -> {
			// ensure we throw the first exception
			if (caughtException == null) {
				caughtException = throwable;
			}
		});
		if (caughtException == null) {
			modulesLoader.loadModules("classpath*:/envision-modules", new DefaultModulesFinder(), modulesClient);

			// for multi-tenant installs we need to replace some DHF triggers for entities. this allows us to support
			// per-user entities
			if (isMultitenant) {
				modulesLoader.loadModules("classpath*:/multitenant-modules", new DefaultModulesFinder(), modulesClient);
			}
		}

		if (caughtException != null) {
			throw new RuntimeException(caughtException);
		}
	}

	protected void prepareAssetFileLoader(AssetFileLoader loader, CommandContext context) {
		AppConfig appConfig = context.getAppConfig();

		Integer batchSize = appConfig.getModulesLoaderBatchSize();
		if (batchSize != null) {
			loader.setBatchSize(batchSize);
		}

		JarDocumentFileReader jarDocumentFileReader = new JarDocumentFileReader();
		jarDocumentFileReader.addDocumentFileProcessor(new CacheBusterDocumentFileProcessor());
		jarDocumentFileReader.addDocumentFileProcessor(new TokenReplacerDocumentFileProcessor(buildModuleTokenReplacer(appConfig)));
		jarDocumentFileReader.addDocumentFileProcessor(new CollectionsDocumentFileProcessor("envision-module"));
		jarDocumentFileReader.addDocumentFileProcessor(new PermissionsDocumentFileProcessor(appConfig.getModulePermissions()));
		loader.setDocumentFileReader(jarDocumentFileReader);
	}

	private TokenReplacer buildModuleTokenReplacer(AppConfig appConfig) {
		DefaultTokenReplacer r = new DefaultTokenReplacer();
		final Map<String, String> customTokens = appConfig.getCustomTokens();
		if (customTokens != null && !customTokens.isEmpty()) {
			r.addPropertiesSource(() -> {
				Properties p = new Properties();
				p.putAll(customTokens);
				return p;
			});
		}

		return r;
	}
}
