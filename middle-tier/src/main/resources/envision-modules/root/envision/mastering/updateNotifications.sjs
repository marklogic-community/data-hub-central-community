'use strict';

var uris;
var status;

uris = uris.toObject();

const mastering = require('/envision/mastering.sjs')

xdmp.invokeFunction(function() {
	declareUpdate();
	uris.forEach(uri => {
		if (fn.docAvailable(uri)) {
			mastering.updateStatus(uri, status)
		}
	});
});

const notifications = xdmp.invokeFunction(function() {
	return uris.map(uri => {
		return mastering.getNotification(uri, cts.doc(uri));
	});
});

notifications;
