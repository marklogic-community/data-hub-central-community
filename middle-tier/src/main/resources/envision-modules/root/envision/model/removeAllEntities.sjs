declareUpdate();

var user

for (let uri of cts.uriMatch(`/entities/${user || ''}*.entity.json`, null, cts.collectionQuery('http://marklogic.com/entity-services/models'))) {
  xdmp.documentDelete(uri)
}

true
