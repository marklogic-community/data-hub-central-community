'use strict';

const hubEs = require('/data-hub/5/impl/hub-es.sjs');
const piiLib = require('/envision/pii-lib.sjs');

var oldModel;
var newModel;

oldModel = oldModel.toObject();
newModel = newModel.toObject();

function getPii(model) {
	return hubEs.generateProtectedPathConfig(model).toObject().config['protected-path'];
}

const oldPii = getPii(Object.values(oldModel));
const newPii = getPii(Object.values(newModel));

piiLib.updatePii(oldPii, newPii);

true
