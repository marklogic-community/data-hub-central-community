var models;

const entityNames = [].concat(...models.toObject().map(m => Object.keys(m.nodes)));

const unmodeledEntities = fn.collection('http://marklogic.com/entity-services/models')
	.toArray()
	.map(e => e.toObject().info.title.toLowerCase())
	.filter(e => !entityNames.find(m => m === e))

unmodeledEntities.length > 0
