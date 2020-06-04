'use strict';

var uris;
uris = uris.toObject();

const mastering = require('/envision/mastering.sjs')

xdmp.invokeFunction(function() {
	declareUpdate();
	mastering.unblock(uris);
});

const blocks = xdmp.invokeFunction(function() {
	return mastering.getBlocks(uris);
});
blocks;
