var uris;

const matcher = require("/com.marklogic.smart-mastering/matcher.xqy");

const blocks = uris.toObject().reduce((obj, uri) => {
  obj[uri] = matcher.getBlocks(uri)
  return obj
}, {});

blocks;
