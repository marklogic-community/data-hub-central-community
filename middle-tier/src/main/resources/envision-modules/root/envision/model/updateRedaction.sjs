const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE
const config = require('/envision/config.sjs');
const model = require('/envision/model.sjs').model();
const currentUser = xdmp.getCurrentUser()

let ruleCollectionName = 'redactionRule'
if (config.isMultiTenant) {
	ruleCollectionName = `redactionRule4${currentUser}`
}

// if the entity has Redaction, create a redaction rule, if not, delete any Redaction rules
function createRedactionRule(entityName, propertyName, isRedacted) {
	if (!isRedacted) {
		return null;
	}

	let ruleUri = ''
	if (config.isMultiTenant) {
		ruleUri = `/rules/pii/${currentUser}/${entityName}-${propertyName}.json`
	}
	else {
		ruleUri = `/rules/pii/${entityName}-${propertyName}.json`
	}

	return {
		rule: {
			description: "Redact " + entityName ,
			path: "/envelope/instance/" + entityName + "/" + propertyName,
			method: { function: "redact-regex" },
			options: {
				pattern : "^[\u0001-\uE007F].*",
				replacement :  "### Redacted ###"
			}
		}
	}
}

function getRuleUri(rule) {
	const parts = rule.rule.path.split('/')
	const entityName = parts[3]
	const propertyName = parts[4]
	let ruleUri = ''
	if (config.isMultiTenant) {
		ruleUri = `/rules/pii/${currentUser}/${entityName}-${propertyName}.json`
	}
	else {
		ruleUri = `/rules/pii/${entityName}-${propertyName}.json`
	}
	return ruleUri
}

function createRules() {
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

const oldRules = xdmp.invokeFunction(function() {
	return cts.search(cts.collectionQuery(ruleCollectionName))
}, { database: xdmp.schemaDatabase(xdmp.database(finalDB)) })
.toArray()
.map(rule => rule.toObject())
const newRules = createRules()

const removeUs = oldRules.filter(a => !newRules.find(b => b.rule.path === a.rule.path))
const addUs = newRules.filter(a => !oldRules.find(b => b.rule.path === a.rule.path))

xdmp.invokeFunction(function() {
	declareUpdate()
	removeUs.forEach(rule => {
		const uri = getRuleUri(rule)
		if (fn.exists(cts.doc(uri))) {
			xdmp.documentDelete(uri)
		}
	})
}, { database: xdmp.schemaDatabase(xdmp.database(finalDB)) })

xdmp.invokeFunction(function() {
	declareUpdate()
	addUs.forEach(rule => {
		const uri = getRuleUri(rule)
		xdmp.documentInsert(
			uri,
			rule,
			{
				permissions : [
					xdmp.defaultPermissions(),
					xdmp.permission("envision", "read"),
					xdmp.permission("envision", "update")
				],
				collections : [ ruleCollectionName ]
			}
		)
	})
}, { database: xdmp.schemaDatabase(xdmp.database(finalDB)) })

true
