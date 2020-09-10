const config = require('/com.marklogic.hub/config.sjs');

let databases = {
	staging: config.STAGINGDATABASE,
	final: config.FINALDATABASE
}

let collections = {
	staging: xdmp.invokeFunction(() =>
		cts.collections()
			.toArray()
			.filter(c => !fn.startsWith(c, 'http://marklogic.com')),
		{ database: xdmp.database(databases.staging)}
	),
	final: xdmp.invokeFunction(() =>
		cts.collections()
			.toArray()
			.filter(c => !fn.startsWith(c, 'http://marklogic.com')),
		{ database: xdmp.database(databases.final)}
	)
}

let result = {
	databases: databases,
	collections: collections
}
result
