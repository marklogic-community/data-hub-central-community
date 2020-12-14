/**
 * Given an Envision model , create Entity Services
 * entities for each entity in the model.
 *
 */

const model = require('/envision/model.sjs').enhancedModel;
const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE
const stagingDB = require('/com.marklogic.hub/config.sjs').STAGINGDATABASE
const config = require('/envision/config.sjs');
const entityNames = Object.values(model.nodes).map(n => n.entityName)

let entities = {};

// if the entity has PII, create a redaction rule, if not, delete any PII rules
function ruleDefinition(entityName, propertyName, isPII) {
	return {
		piiRule: function piiRule() {
			declareUpdate()

			const ruleName = entityName + "-" + propertyName

			if (isPII){
				if (! fn.exists(cts.doc("/rules/pii/" + ruleName + ".json")) ) {

					xdmp.documentInsert("/rules/pii/" + xdmp.getCurrentUser() + "/" + ruleName + ".json",
						{ "rule": {
								"description": "Redact " + entityName ,
								"path": "/envelope/instance/" + entityName + "/" + propertyName,
								"method": { "function": "redact-regex" },
								"options": {
									"pattern" : "^[\u0001-\uE007F].*",
									"replacement" :  "### PII Redacted ###"
								}
							}
						},
						{
							permissions : xdmp.defaultPermissions(),
							collections : ["piiRules"]
						}
					)
				}
			} else {
				if (fn.exists(cts.doc("/rules/pii/" + ruleName + ".json")) ) {
					xdmp.documentDelete( "/rules/pii/" + ruleName + ".json")
				}
			}
		}
	}
}

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
			if (entityNames.indexOf(p.type) >= 0) {
				properties[p.name] = {
					datatype: "array",
					description: p.description,
					items: {
						"$ref": `#/definitions/${p.type}`
					}
				}
				definitions = {
					...definitions,
					...buildDefinitions(model.nodes[p.type.toLowerCase()])
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
			// if the entity has PII, create a redaction rule, if not, delete any PII rules
			function ruleDefinition(entityName, propertyName, isPII, ruleUri, ruleCollectionName) {
				return {
					piiRule: function piiRule() {
						declareUpdate()

						if (isPII){
							if (! fn.exists(cts.doc(ruleUri)) ) {
								xdmp.documentInsert(ruleUri,
									{ "rule": {
											"description": "Redact " + entityName ,
											"path": "/envelope/instance/" + entityName + "/" + propertyName,
											"method": { "function": "redact-regex" },
											"options": {
												"pattern" : "^[\u0001-\uE007F].*",
												"replacement" :  "### PII Redacted ###"
											}
										}
									},
									{
										permissions : [xdmp.defaultPermissions(), xdmp.permission("envision", "read"), xdmp.permission("envision", "update")],
										collections : [ruleCollectionName]
									}
								)
							}
						} else {
							if (fn.exists(cts.doc(ruleUri)) ) {
								xdmp.documentDelete( ruleUri )
							}
						}
					}
				}
			}
			let ruleUri = ""
			let ruleCollectionName = ""
			if (config.isMultiTenant) {
				ruleUri = "/rules/pii/" + xdmp.getCurrentUser() + "/" + node.entityName + "-" + p.name + ".json"
				ruleCollectionName = "piiRule4" + xdmp.getCurrentUser()
			} else {
				ruleUri = "/rules/pii/"  + node.entityName + "-" + p.name + ".json"
				ruleCollectionName = "piiRule"
			}

			const invokeRule = ruleDefinition (node.entityName, p.name, p.isPii, ruleUri, ruleCollectionName)
			xdmp.invokeFunction(invokeRule.piiRule,
				{ "database" : xdmp.schemaDatabase(xdmp.database(finalDB)) }
			);
			// note - thought about adding a role to staging as well. However, this uses a different way of reading
			// docs and doesn't use entities.sjs where the redaction code is applied

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
		let baseUri = node.baseUri || "http://marklogic.com/envision/"
		if (!baseUri.endsWith('/')) {
			baseUri += '/'
		}
		else if (entityNames.indexOf(p.type) >= 0) {
			properties[p.name] = {
				"$ref": `#/definitions/${p.type}`
			}
			definitions = {
				...definitions,
				...buildDefinitions(model.nodes[p.type.toLowerCase()])
			}
		}
		else {
			properties[p.name] = {
				datatype: p.type,
				description: p.description,
				collation: p.collation || "http://marklogic.com/collation/codepoint"
			};
		}

		const invokeRule = ruleDefinition (node.entityName, p.name, p.isPii)
		xdmp.invokeFunction(invokeRule.piiRule,
			{ "database" : xdmp.schemaDatabase(xdmp.database(finalDB)) }
		);
	});

	definitions[node.entityName] = {
		"primaryKey": primaryKey,
		"required": Object.keys(required),
		"pii": Object.keys(pii),
		"elementRangeIndex": Object.keys(elementRangeIndex),
		"rangeIndex": Object.keys(rangeIndex),
		"wordLexicon": Object.keys(wordLexicon),
		"properties": properties
	};

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
			};
		}
	});
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

entities;
