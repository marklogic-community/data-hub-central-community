/**
 * This module provides a wrapper around the Envision Model.json
 * to provide extra functionality
 */
function getModel() {
	const uri = '/envision/' + xdmp.getCurrentUser() +'/currentModel.json';
	let model = fn.head(cts.doc(uri));
	if (model) {
		model = model.toObject();
		return model;
	}

	return null;
}

function getEnhancedModel() {
	let model = getModel();
	if (!model) return null;

	let names = {};
	Object.keys(model.nodes).forEach(key => {
		let node = model.nodes[key];
		names[key] = node.entityName;
		names[`${node.baseUri}${key}`] = node.entityName;
	});

	/**
	 * given a lowercase entity name, returns the proper name
	 */
	model.getName = function(name) {
		return names[name];
	}

	return model;
}

module.exports.model = getModel();
module.exports.enhancedModel = getEnhancedModel();
