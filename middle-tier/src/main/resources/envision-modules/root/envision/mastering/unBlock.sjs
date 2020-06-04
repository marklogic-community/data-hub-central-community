'use strict';

declareUpdate();

var uris;
uris = uris.toObject();

const matcher = require("/com.marklogic.smart-mastering/matcher.xqy");

xdmp.invokeFunction(function() {
	declareUpdate();
	matcher.allowMatch(uris[0], uris[1]);
});

const blocks = xdmp.invokeFunction(function() {
	return uris.reduce((obj, uri) => {
		obj[uri] = matcher.getBlocks(uri)
		return obj
	}, {});
});
blocks;
