function modelToES(model) {
	const entityNames = Object.values(model.nodes).map(n => n.entityName);

	let entities = {}

	function buildDefinitions(node) {
		let primaryKey = null;
		let properties = {};
		let required = {};
		let pii = {};
		let elementRangeIndex = {};
		let rangeIndex = {};
		let wordLexicon = {};
		let definitions = {};
		node.properties.forEach(p => {
			if (p.isPrimaryKey) {
				primaryKey = p.name
			}
			if (p.isRequired) {
				required[p.name] = true
			}
			if (p.isPii) {
				pii[p.name] = true
			}
			if (p.isElementRangeIndex) {
				elementRangeIndex[p.name] = true
			}
			if (p.isRangeIndex) {
				rangeIndex[p.name] = true
			}
			if (p.isWordLexicon) {
				wordLexicon[p.name] = true
			}
			if (p.isArray) {
				if (entityNames.indexOf(p.type) >= 0 || p.structureDefinitions) {
					properties[p.name] = {
						datatype: "array",
						description: p.description,
						items: {
							"$ref": `#/definitions/${p.type}`
						}
					}
					let typeNode = model.nodes[p.type.toLowerCase()]
					let builtDefinition = typeNode ? buildDefinitions(typeNode) : (p.structureDefinitions || {})
					definitions = {
						...definitions,
						...builtDefinition
					}
				}
				else {
					properties[p.name] = {
						datatype: "array",
						description: p.description,
						items: {
							datatype: p.type,
							collation: p.collation || "http://marklogic.com/collation/codepoint"
						}
					};
				}
			}
			else if (entityNames.indexOf(p.type) >= 0 || p.isStructured) {
				properties[p.name] = {
					"$ref": `#/definitions/${p.type}`
				}
				let typeNode = model.nodes[p.type.toLowerCase()]
				let builtDefinition = typeNode ? buildDefinitions(typeNode) : (p.structureDefinitions || {})
				definitions = {
					...definitions,
					...builtDefinition
				}
			}
			else {
				properties[p.name] = {
					datatype: p.type,
					description: p.description,
					collation: p.collation || "http://marklogic.com/collation/codepoint"
				};
			}
		})

		definitions[node.entityName] = {
			"primaryKey": primaryKey,
			"required": Object.keys(required),
			"pii": Object.keys(pii),
			"elementRangeIndex": Object.keys(elementRangeIndex),
			"rangeIndex": Object.keys(rangeIndex),
			"wordLexicon": Object.keys(wordLexicon),
			"properties": properties
		}

		return definitions
	}

	if (model.nodes) {
		// first create the models
		Object.keys(model.nodes).forEach(key => {
			let node = model.nodes[key];

			if (node.type === 'entity') {
				let baseUri = node.baseUri || "http://marklogic.com/envision/"
				if (!baseUri.endsWith('/')) {
					baseUri += '/'
				}
				entities[key] = {
					"info": {
						"title": node.entityName,
						"version": node.version || "0.0.1",
						"baseUri": baseUri,
						"description": node.description
					},
					"definitions": buildDefinitions(node)
				}
			}
		})
	}

	if (model.edges) {
		// now connect the entities according to the edge definitions in the model
		Object.keys(model.edges).forEach(key => {
			let edge = model.edges[key];

			let nameFrom = model.getName(edge.from);
			let nameTo = model.getName(edge.to);
			if (model.nodes[edge.from] && model.nodes[edge.from].type === 'entity' && model.nodes[edge.to] && model.nodes[edge.to].type === 'entity') {
				const toEntity = entities[edge.to]
				const baseUri = toEntity.info.baseUri
				const version = toEntity.info.version
				const title = toEntity.info.title
				let isExternal = ((edge.type || 'external') === 'external')
				const ref = isExternal ? `${baseUri}${title}-${version}/${title}` : `#/definitions/${nameTo}`
				if (edge.cardinality === '1:1') {
					entities[edge.from].definitions[nameFrom].properties[edge.label] = {
						"$ref": `${baseUri}${title}-${version}/${title}`
					};
				}
				else {
					entities[edge.from].definitions[nameFrom].properties[edge.label] = {
						"datatype": "array",
						"items": {
							"$ref": ref
						}
					};
				}
			}
		});
	}

	return entities;
}

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

module.exports.model = getModel;
module.exports.enhancedModel = getEnhancedModel;
module.exports.modelToES = modelToES;
