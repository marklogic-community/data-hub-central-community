'use strict'

var uri;

const mastering = require('/envision/mastering.sjs')
const model = cts.doc('model.json').root;
const labels = Object.values(model.nodes).reduce((prev, cur) => {
	prev[cur.entityName] = cur.labelField
	return prev;
}, {});

const doc = cts.doc(uri);
const notification = mastering.getNotification(uri, doc);
notification;
