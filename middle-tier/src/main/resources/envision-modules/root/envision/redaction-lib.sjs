const rdt = require('/MarkLogic/redaction');
const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE
const config = require('/envision/config.sjs');

function redact(doc) {
	// see if we have any redaction rules to apply
	const thisUserId = xdmp.getCurrentUserid()
	const seqUserRoles = xdmp.useridRoles(thisUserId)
	const arrUserRoleNames = seqUserRoles.toArray().map(roleId => xdmp.roleName(roleId))

	const redactionRolesDocUri = config.isMultiTenant ? '/redactionRules2Roles4' + xdmp.getCurrentUser() + '.json' : '/redactionRules2Roles.json'
	const redactionRuleCollectionName = config.isMultiTenant ? 'redactionRule4' + xdmp.getCurrentUser() : 'redactionRule'
	const redactionRuleCollectionPrefix = config.isMultiTenant ? 'redactionRule4' + xdmp.getCurrentUser() : 'redactionRule'

	let arrRulesToApply = [ redactionRuleCollectionName ]
	let arrRulesNotToApply = []
	// get all rules that could apply
	let seqRuleCollections = xdmp.invokeFunction(function() {
		return cts.collectionMatch(redactionRuleCollectionPrefix + '*')
	}, { database : xdmp.schemaDatabase(xdmp.database(finalDB)) })

	for (let coll of seqRuleCollections ) {
		arrRulesToApply.push(coll)
	}

	//redactionRolesDocUri maps from rules to roles the rules should not apply to.
	let seqRedactionRules2Roles = xdmp.invokeFunction(function() {
		return cts.doc(redactionRolesDocUri)
	}, { database: xdmp.database(finalDB) })

	if (fn.count(seqRedactionRules2Roles) > 0) {
		fn.head(seqRedactionRules2Roles).toObject().rules.forEach((rule) => {
			// each rule is like 'redactionRuleCollection': 'redactionRules', 'rolesThatDoNotUseRedaction': [ 'pii-reader']
			if (rule.rolesThatDoNotUseRedaction.some(r=> arrUserRoleNames.includes(r))) {
				arrRulesNotToApply.push(rule.redactionRuleCollection)
			}
		});

		arrRulesToApply = arrRulesToApply.filter(( el ) => {
			return !arrRulesNotToApply.includes( el.toString() );
		});
	}

	// rdt functions don't like if if there are no rules in a collection, which could be an edge case
	let arrCollectionWithNoDocuments = []
	for (var i = 0; i < arrRulesToApply.length; i++) {
		const collectionCount = xdmp.invokeFunction(function() {
			return fn.count(cts.search(cts.collectionQuery(arrRulesToApply[i])))
		}, { database : xdmp.schemaDatabase(xdmp.database(finalDB)) })

		if (collectionCount == 0) {
			arrCollectionWithNoDocuments.push(arrRulesToApply[i] )
		}
	}

	arrRulesToApply = arrRulesToApply.filter(function( el ) {
		return !arrCollectionWithNoDocuments.includes( el.toString() );
	});

	// iterate over the uris and create 'nodes' for the graph ui
	// do this by opening the docs at each uri
	// then insert some additional metadata about each 'node'

	if (arrRulesToApply.length == 0) {
		xdmp.log('No redaction rules to apply')
	} else {
		xdmp.log('Applying redaction rules ' + arrRulesToApply.toString() )
	}

	if (arrRulesToApply.length > 0) {
		try {
			doc = fn.head( rdt.redact(doc, arrRulesToApply ) )
		} catch (e) {
			xdmp.log('Problem applying redaction rules: ' + e.toString() )
		}
	}

	return doc;
}

module.exports = {
	redact: redact
}
