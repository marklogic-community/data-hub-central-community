var token;

const user = fn.head(cts.search(cts.andQuery([
	cts.collectionQuery('envision-user'),
	cts.jsonPropertyValueQuery('token', token),
	cts.jsonPropertyValueQuery('validated', false)
])))
