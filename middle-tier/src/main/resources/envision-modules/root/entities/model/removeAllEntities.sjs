declareUpdate();

for (let uri of cts.uris(null, null, cts.collectionQuery('http://marklogic.com/entity-services/models'))) {
  xdmp.documentDelete(uri)
}

true
