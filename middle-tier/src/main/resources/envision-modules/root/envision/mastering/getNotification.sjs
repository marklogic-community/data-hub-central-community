'use strict'

const mastering = require('/envision/mastering.sjs')

var uri

const doc = cts.doc(uri)
let notification = null
if (fn.head(doc)) {
	notification = mastering.getNotification(uri, doc)
}

notification
