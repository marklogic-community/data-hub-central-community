'use strict';
declareUpdate();

const sec = require('/MarkLogic/security.xqy');

var user;
user = user.toObject();
const roleName = xdmp.md5(user.email);

xdmp.invokeFunction(function() {
	declareUpdate();
	try {
		const roleId = sec.createRole(
			roleName,
			'a role for ' + user.email,
			['envision'].concat(user.roles || []),
			xdmp.defaultPermissions(null, 'elements'),
			xdmp.defaultCollections()
		);
	}
	catch(ex) {
		console.log(ex, ex.toString());
	}
}, {
	database: xdmp.securityDatabase()
});

xdmp.invokeFunction(function() {
	declareUpdate();
	if (!sec.userExists(user.email)) {
		sec.createUser(
			user.email,
			user.email,
			user.password,
			[roleName],
			[
				xdmp.permission(roleName, 'read', 'element'),
				xdmp.permission(roleName, 'update', 'element')
			],
			xdmp.defaultCollections()
		);
	}
}, {
	database: xdmp.securityDatabase()
});

true;
