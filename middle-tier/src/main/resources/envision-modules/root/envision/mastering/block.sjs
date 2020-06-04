'use strict';
var uris;
uris = uris.toObject();

const matcher = require("/com.marklogic.smart-mastering/matcher.xqy");

xdmp.invokeFunction(function() {
	declareUpdate();
	matcher.blockMatches(uris);
})

const blocks = xdmp.invokeFunction(function() {
	return uris.reduce((obj, uri) => {
		obj[uri] = matcher.getBlocks(uri)
		return obj
	}, {});
})
blocks;
