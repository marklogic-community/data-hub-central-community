const sec = require('/MarkLogic/security');

function updatePii(oldPii, newPii) {
	const removeUs = oldPii.filter(a => !newPii.find(b => b['path-expression'] === a['path-expression']))
	const addUs = newPii.filter(a => !oldPii.find(b => b['path-expression'] === a['path-expression']))

	xdmp.invokeFunction(function() {
		declareUpdate();

		removeUs.forEach(pii => {
			sec.unprotectPath(pii['path-expression'], pii['path-namespace']);
		})
	}, { database: xdmp.securityDatabase() })

	xdmp.invokeFunction(function() {
		declareUpdate();

		removeUs.forEach(pii => {
			sec.removePath(pii['path-expression'], pii['path-namespace']);
		})
	}, { database: xdmp.securityDatabase() })

	xdmp.invokeFunction(function() {
		declareUpdate();

		addUs.forEach(pii => {
			const perm = pii.permission
			sec.protectPath(
				pii['path-expression'],
				pii['path-namespace'],
				xdmp.permission(perm['role-name'], perm['capability'], 'element'))
		})
	}, { database: xdmp.securityDatabase() })
}

module.exports.updatePii = module.amp(updatePii);
