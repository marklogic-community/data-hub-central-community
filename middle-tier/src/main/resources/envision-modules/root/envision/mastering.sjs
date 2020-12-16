const jobDB = require('/com.marklogic.hub/config.sjs').JOBDATABASE
const matcher = require("/com.marklogic.smart-mastering/matcher.xqy");
const model = require('/envision/model.sjs').model();

function getLabels() {
	const labels = model ? Object.values(model.nodes).reduce((prev, cur) => {
		prev[cur.entityName] = cur.labelField
		return prev;
	}, {}) : {};
	return labels;
}

function getNotificationFlowInfo(uri) {
	const prov = fn.head(xdmp.invokeFunction(() => {
		const doc = fn.head(cts.search(cts.elementValueQuery(xs.QName('location'), uri)))
		if (doc) {
			const prov = doc.root
			const assocs = prov.xpath('//*:wasAssociatedWith/*:agent/@*:ref/string()')
			return assocs.toArray()
		}
		return []
	}, {
		database: xdmp.database(jobDB)
	}))

	const flows = fn.collection(['http://marklogic.com/data-hub/flow']).toArray()
		.filter(flow => !xdmp.nodeUri(flow).match('/default-'))
		.map(flow => flow.toObject())

	const flow = flows.find(flow => prov.indexOf(flow.name) >= 0)
	if (flow) {
		const steps = Object.values(flow.steps).map(step => step.name)
		const flowName = flow.name
		const stepName = steps.find(step => prov.indexOf(step) >= 0)
		const stepNumber = Object.keys(flow.steps).find(key => prov.indexOf(flow.steps[key].name) >= 0);
		const res = {
			flowName,
			stepName,
			stepNumber
		}
		return res;
	}
	return null;
}

function getEntityType(doc) {
	return doc.xpath('*:envelope/*:instance/*:info/*:title/string()');
}

function getMergedDoc(uris) {
	const collections = cts.collectionMatch("sm-*-mastered")
	const query = cts.andQuery(
		uris.map(uri => {
			return cts.orQuery([
				cts.elementValueQuery(fn.QName('http://marklogic.com/smart-mastering', 'document-uri'), uri, 'exact'),
				cts.jsonPropertyValueQuery('document-uri', uri, 'exact')
			])
		})
	)

	return fn.head(cts.search(
		cts.andQuery([
			cts.collectionQuery(collections),
			cts.orQuery([
				cts.elementQuery(fn.QName('http://marklogic.com/entity-services', 'headers'), query),
				cts.jsonPropertyScopeQuery('headers', query)
			])
		])
	))
}

function isBlocked(uris) {
	const blocks = getBlocks(uris);
	const blocked = uris.reduce((isBlocked, uri) => {
		const otherUris = uris.filter(u => u !== uri);
		const uriBlocks = blocks[uri]
		let result = isBlocked;
		if (uriBlocks) {
      const filteredRes = uriBlocks.filter(u => otherUris.includes(u))
			result = isBlocked && (filteredRes.length == uris.length - 1);
		}
		else {
			result = false;
		}
		return result;
	}, true);
	return {
		blocks: blocks,
		blocked: blocked
	}
}

function getNotification(uri, doc) {
	const labels = getLabels();
	const flowInfo = getNotificationFlowInfo(uri);
	const r = doc.root;
	const uris = r.xpath('*:document-uris/*:document-uri/string()').toObject()
		.map(uri => {
			if (fn.docAvailable(uri)) {
				return uri;
			}
			else {
				return fn.head(cts.uriMatch(`*${uri}*`))
			}
		});
	const blocked = isBlocked(uris);
	let mergedDoc = getMergedDoc(uris);
	let merged = {};
	if (mergedDoc) {
		mergedDoc = mergedDoc.root;
		merged = {
			uri: xdmp.nodeUri(mergedDoc),
			doc: mergedDoc
		};
	}
  return {
    meta: {
      dateTime: r.xpath('*:meta/*:dateTime/string()'),
      user: r.xpath('*:meta/*:user/string()'),
      uri: uri,
			status: r.xpath('*:meta/*:status/string()'),
			merged: !!mergedDoc,
			blocks: blocked.blocks,
			blocked: blocked.blocked
		},
		flowInfo: flowInfo || {},
    thresholdLabel: r.xpath('*:threshold-label/string()'),
		uris: uris,
		merged: merged,
    labels: uris.reduce((prev, cur) => {
			let doc = cts.doc(cur);
			if (doc) {
				doc = doc.root;
				const entity = getEntityType(doc);
				const labelField = labels[entity];
				prev[cur] = doc.xpath(`//*:${labelField}`);
			}
      return prev;
    }, {})
  }
}

function updateStatus(uri, status) {
	return matcher.updateNotificationStatus(uri, status)
}

function unblock(uris) {
	return matcher.allowMatch(uris[0], uris[1]);
}

function blockMatches(uris) {
	return matcher.blockMatches(uris);
}

function getBlocks(uris) {
	return uris.reduce((obj, uri) => {
		obj[uri] = matcher.getBlocks(uri).toObject()
		return obj
	}, {});
}

exports.getNotificationFlowInfo = getNotificationFlowInfo;
exports.getNotification = getNotification;
exports.updateStatus = updateStatus;
exports.unblock = unblock;
exports.blockMatches = blockMatches;
exports.getBlocks = getBlocks;
exports.isBlocked = isBlocked;
exports.getMergedDoc = getMergedDoc;
