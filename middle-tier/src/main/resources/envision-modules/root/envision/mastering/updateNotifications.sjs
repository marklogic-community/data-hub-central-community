'use strict';

var uris;
var readStatus;
var mergeStatus;
var blockStatus;

uris = uris.toObject();

const mastering = require('/envision/mastering.sjs')

function updateStatus(uri, elementName, status) {
	if (fn.docAvailable(uri)) {
		const meta = fn.head(cts.doc(uri).root.xpath('/*:notification/*:meta'))
		const statusNode = fn.head(meta.xpath(`*:${elementName}`))

		const x = new NodeBuilder();
		x.startElement (elementName, "http://marklogic.com/smart-mastering");
		x.addText(status);
		x.endElement();
		const newNode = x.toNode();

		if (statusNode) {
			xdmp.nodeReplace(
				statusNode,
				newNode
			)
		}
		else {
			xdmp.nodeInsertChild(meta, newNode)
		}
	}
}

xdmp.invokeFunction(function() {
	declareUpdate();
	uris.forEach(uri => {
		if (fn.docAvailable(uri)) {
			if (readStatus && readStatus !== '') {
				updateStatus(uri, 'status', readStatus)
			}
			if (mergeStatus && mergeStatus !== '') {
				updateStatus(uri, 'merge-status', mergeStatus)
			}
			if (blockStatus && blockStatus !== '') {
				updateStatus(uri, 'block-status', blockStatus)
			}
		}
	});
});

const notifications = xdmp.invokeFunction(function() {
	return uris.map(uri => {
		return mastering.getNotification(uri, cts.doc(uri));
	});
});

notifications;
