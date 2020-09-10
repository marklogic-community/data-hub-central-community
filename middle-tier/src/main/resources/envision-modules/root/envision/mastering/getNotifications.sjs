'use strict'

const mastering = require('/envision/mastering.sjs')

var qtext;
var page;
var pageLength;
var sort;

let start = ((page - 1) * pageLength) + 1;
const collections = cts.collectionMatch("sm-*-notification")
	.toArray()
	.concat(cts.collectionMatch("sm-notification").toArray());

const queries = [cts.collectionQuery(collections)]

if (qtext && qtext.length > 0) {
	queries.push(cts.wordQuery(qtext));
}
const query = cts.andQuery(queries);
const unreadQuery = cts.andQuery([
	cts.elementValueQuery(fn.QName('http://marklogic.com/smart-mastering', 'status'), 'unread', 'exact')
].concat(queries))
const total = cts.estimate(query)
const totalUnread = cts.estimate(unreadQuery)
const docs = fn.subsequence(cts.search(query), start, pageLength).toArray();
const notifications = docs.map(doc => {
	const uri = fn.baseUri(doc);
	return mastering.getNotification(uri, doc);
});

const response = {
	query: query,
	page: page,
	total: total,
	totalUnread: totalUnread,
	pageLength: pageLength,
	notifications: notifications
};

response;
