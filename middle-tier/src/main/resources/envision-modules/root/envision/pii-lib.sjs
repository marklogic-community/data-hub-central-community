const sec = require('/MarkLogic/security');

function updatePii(oldPii, newPii) {
	const removeUs = oldPii.filter(a => !newPii.find(b => b['path-expression'] === a['path-expression']))
	const addUs = newPii.filter(a => !oldPii.find(b => b['path-expression'] === a['path-expression']))

	xdmp.invokeFunction(function() {
		declareUpdate();

		removeUs.forEach(pii => {
			const securityPathNamespaces = piiNamespacesToSecurityXML(pii['path-namespace']);
			sec.unprotectPath(pii['path-expression'], securityPathNamespaces);
		})
	}, { database: xdmp.securityDatabase() })

	xdmp.invokeFunction(function() {
		declareUpdate();

		removeUs.forEach(pii => {
			const securityPathNamespaces = piiNamespacesToSecurityXML(pii['path-namespace']);
			sec.removePath(pii['path-expression'], securityPathNamespaces);
		})
	}, { database: xdmp.securityDatabase() })

	xdmp.invokeFunction(function() {
		declareUpdate();
		addUs.forEach(pii => {
			const perm = pii.permission;
			const securityPathNamespaces = piiNamespacesToSecurityXML(pii['path-namespace']);
			sec.protectPath(
				pii['path-expression'],
				securityPathNamespaces,
				xdmp.permission(perm['role-name'], perm['capability'], 'element'))
		})
	}, { database: xdmp.securityDatabase() })
}

const globalNamespaces = [{prefix: 'es', 'namespace-uri': 'http://marklogic.com/entity-services'}];

function piiNamespacesToSecurityXML(piiNamespaces) {
	const pathNamespaces = Sequence.from(piiNamespaces).toArray();
	const globalNamespacesToAdd = globalNamespaces
		.filter((gns) => !pathNamespaces.find((ns) => ns.prefix === gns.prefix));
	return pathNamespaces.concat(globalNamespacesToAdd).map((pathNamespace) => {
		return sec.securityPathNamespace(pathNamespace['prefix'], pathNamespace['namespace-uri']);
	});

}

module.exports.updatePii = module.amp(updatePii);
