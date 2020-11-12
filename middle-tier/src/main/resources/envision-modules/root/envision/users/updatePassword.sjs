var email;
var password;

const sec = require('/MarkLogic/security.xqy');

xdmp.invokeFunction(function() {
	declareUpdate();
	sec.userSetPassword(email, password);
}, {
	database: xdmp.securityDatabase()
});

true
