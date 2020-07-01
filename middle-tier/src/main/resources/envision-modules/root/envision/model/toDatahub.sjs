/**
 * Given an Envision model (model.json), create Entity Services
 * entities for each entity in the model.
 */
const model = require('/envision/model.sjs');
let entities = {};

// first create the models
Object.keys(model.nodes).forEach(key => {
	let node = model.nodes[key];

	if (node.type === 'entity') {
		let primaryKey = null;
		let properties = {};
		let required = {};
		let pii = {};
		let elementRangeIndex = {};
		let rangeIndex = {};
		let wordLexicon = {};
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
				properties[p.name] = {
					datatype: "array",
					description: p.description,
					items: {
						datatype: p.type,
						collation: p.collation || "http://marklogic.com/collation/codepoint"
					}
				};
			}
			else {
				properties[p.name] = {
					datatype: p.type,
					description: p.description,
					collation: p.collation || "http://marklogic.com/collation/codepoint"
				};
			}
		});
		let definition = {
			"primaryKey": primaryKey,
			"required": Object.keys(required),
			"pii": Object.keys(pii),
			"elementRangeIndex": Object.keys(elementRangeIndex),
			"rangeIndex": Object.keys(rangeIndex),
			"wordLexicon": Object.keys(wordLexicon),
			"properties": properties
		};
		entities[key] = {
			"info": {
				"title": node.entityName,
				"version": node.version || "0.0.1",
				"baseUri": node.baseUri || "http://marklogic.com/envision/",
				"description": node.description
			},
			"definitions": {}
		};
		entities[key].definitions[node.entityName] = definition;
	}
});

// now connect the entities according to the edge definitions in the model
Object.keys(model.edges).forEach(key => {
	let edge = model.edges[key];

	let nameFrom = model.getName(edge.from);
	let nameTo = model.getName(edge.to);
	if (model.nodes[edge.from] && model.nodes[edge.from].type === 'entity' && model.nodes[edge.to] && model.nodes[edge.to].type === 'entity') {
		if (edge.cardinality === '1:1') {
			entities[edge.from].definitions[nameFrom].properties[edge.label] = {
				"$ref": "#/definitions/" + nameTo
			};
		}
		else {
			entities[edge.from].definitions[nameFrom].properties[edge.label] = {
				"datatype": "array",
				"items": {
					"$ref": "#/definitions/" + nameTo
				}
			};
		}
	}

	Object.keys(model.edges).forEach(key => {
		let edge = model.edges[key];
		if (model.nodes[edge.from].type === 'entity' && model.nodes[edge.to].type === 'entity') {
			const defs = entities[edge.to].definitions
			if (defs) {
				for (let key in defs) {
					entities[edge.from].definitions[key] = defs[key]
				}
			}
		}
	})
});

entities;
