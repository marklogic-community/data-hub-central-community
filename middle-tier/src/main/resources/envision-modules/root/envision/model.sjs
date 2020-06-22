/**
 * This module provides a wrapper around the Envision Model.json
 * to provide extra functionality
 */
function getModel() {
	let model = cts.doc('model.json');
	if (model) {
		model = model.toObject();
	}
	else {
		return {};
	}

	let names = {};
	Object.keys(model.nodes).forEach(key => {
		let node = model.nodes[key];
		names[key] = node.entityName
	});

	/**
	 * given a lowercase entity name, returns the proper name
	 */
	model.getName = function(name) {
		return names[name];
	}

	return model;
}

module.exports = getModel();
