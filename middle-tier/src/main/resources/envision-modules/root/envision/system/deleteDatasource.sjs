declareUpdate();

var collections;
collections = collections.toObject();

collections.forEach(collection => xdmp.collectionDelete(collection));

true;
