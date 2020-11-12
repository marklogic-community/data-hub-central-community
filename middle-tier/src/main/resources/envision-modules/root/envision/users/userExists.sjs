'use strict';

var email;

fn.count(cts.search(cts.andQuery([
	cts.collectionQuery('envision-user'),
	cts.jsonPropertyValueQuery('email', email)
]))) === 1;
