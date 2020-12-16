const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE
const config = require('/envision/config.sjs');
const currentUser = xdmp.getCurrentUser()

var oldModel;
var newModel;
if (oldModel) {
	oldModel = oldModel.toObject();
}
newModel = newModel.toObject();

// if the entity has Redaction, create a redaction rule, if not, delete any Redaction rules
function createRedactionRule(entityName, propertyName, isRedacted) {
	if (!isRedacted) {
		return null;
	}

	let ruleUri = ''
	let ruleCollectionName = ''
	if (config.isMultiTenant) {
		ruleUri = `/rules/pii/${currentUser}/${entityName}-${propertyName}.json`
		ruleCollectionName = `redactionRule4${currentUser}`
	}
	else {
		ruleUri = `/rules/pii/${entityName}-${propertyName}.json`
		ruleCollectionName = 'redactionRule'
	}

	return {
		uri: ruleUri,
		collection: ruleCollectionName,
		rule: {
			description: "Redact " + entityName ,
			path: "/envelope/instance/" + entityName + "/" + propertyName,
			method: { function: "redact-regex" },
			options: {
				pattern : "^[\u0001-\uE007F].*",
				replacement :  "### PII Redacted ###"
			}
		}
	}
}

function createRules(model) {
	let rules = []
	Object.values(model.nodes).forEach(node => {
		node.properties.forEach(p => {
			const rule = createRedactionRule(node.entityName, p.name, p.isRedacted)
			if (rule) {
				rules.push(rule)
			}
		})
	})
	return rules
}

const oldRules = oldModel ? createRules(oldModel) : []
const newRules = createRules(newModel)

const removeUs = oldRules.filter(a => !newRules.find(b => b.uri === a.uri))
const addUs = newRules.filter(a => !oldRules.find(b => b.uri === a.uri))

xdmp.invokeFunction(function() {
	declareUpdate()
	removeUs.forEach(rule => {
		if (fn.exists(cts.doc(rule.uri))) {
			xdmp.documentDelete(rule.uri)
		}
	})
}, { database: xdmp.schemaDatabase(xdmp.database(finalDB)) })

xdmp.invokeFunction(function() {
	declareUpdate()
	addUs.forEach(rule => {
		const newRule = {
			rule: rule.rule
		}
		xdmp.documentInsert(
			rule.uri,
			newRule,
			{
				permissions : [
					xdmp.defaultPermissions(),
					xdmp.permission("envision", "read"),
					xdmp.permission("envision", "update")
				],
				collections : [ rule.collection ]
			}
		)
	})
}, { database: xdmp.schemaDatabase(xdmp.database(finalDB)) })

true
