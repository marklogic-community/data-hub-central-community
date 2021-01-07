'use strict';
const rdt = require('/envision/redaction-lib.sjs');

function transform(context, params, content) {
	return rdt.redact(content);
}

exports.transform = transform;
