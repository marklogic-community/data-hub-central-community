'use strict';
declareUpdate();

var email;
var token;
var expiry;

let result = false;
const user = fn.head(cts.search(cts.andQuery([
	cts.collectionQuery('envision-user'),
	cts.jsonPropertyValueQuery('email', email)
])))

if (user) {
	user.resetToken = token;
	user.resetTokenExpiry = expiry;
	xdmp.documentInsert(`/users/${user.userId}.json`, user, { collections: 'envision-user'});
	result = true;
}

result;
