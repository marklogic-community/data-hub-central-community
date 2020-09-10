var token;
let valid = false;
let error = null
const user = fn.head(cts.search(cts.andQuery([
	cts.collectionQuery('envision-user'),
	cts.jsonPropertyValueQuery('resetToken', token)
])))

if (user) {
	if (user.resetTokenExpiry >= fn.currentDate()) {
		valid = true;
	}
	else {
		valid = false;
		error = 'Token expired'
	}
}

const resp = {
	valid: valid,
	error: error
}
resp
