const model = cts.doc('model.json').root;
const labels = Object.values(model.nodes).reduce((prev, cur) => {
	prev[cur.entityName] = cur.labelField
	return prev;
}, {});

function getNotificationFlowInfo(uri) {
	const prov = fn.head(xdmp.invokeFunction(() => {
		const doc = fn.head(cts.search(cts.elementValueQuery(xs.QName('location'), uri)))
		const prov = doc.root
		const assocs = prov.xpath('//*:wasAssociatedWith/*:agent/@*:ref/string()')
		return assocs.toArray()
	}, {
		database: xdmp.database('data-hub-JOBS')
	}))

	const flows = fn.collection(['http://marklogic.com/data-hub/flow']).toArray()
		.filter(flow => !xdmp.nodeUri(flow).match('/default-'))
		.map(flow => flow.toObject())

	const flow = flows.find(flow => prov.indexOf(flow.name) >= 0)
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

function getNotification(uri, doc) {
	const flowInfo = getNotificationFlowInfo(uri);
	const r = doc.root;
	const uris = r.xpath('*:document-uris/*:document-uri/string()').toArray();
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
			readStatus: r.xpath('*:meta/*:status/string()'),
			mergeStatus: r.xpath('*:meta/*:merge-status/string()'),
			blockStatus: r.xpath('*:meta/*:block-status/string()')
		},
		flowInfo: flowInfo || {},
    thresholdLabel: r.xpath('*:threshold-label/string()'),
		uris: uris,
		merged: merged,
    labels: uris.reduce((prev, cur) => {
      const doc = cts.doc(cur).root;
      const entity = getEntityType(doc);
      const labelField = labels[entity];
      prev[cur] = doc.xpath(`//*:${labelField}`);
      return prev;
    }, {})
  }
}

exports.getNotificationFlowInfo = getNotificationFlowInfo;
exports.getNotification = getNotification
