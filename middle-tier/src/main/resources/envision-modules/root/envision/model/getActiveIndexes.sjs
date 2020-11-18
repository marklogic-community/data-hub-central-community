'use strict'

const admin = require('/MarkLogic/admin.xqy');
const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE

var config = admin.getConfiguration()
const indexes = admin.databaseGetRangeElementIndexes(config, xdmp.database(finalDB))
const results = []
for (let idx of indexes) {
  if (idx) {
    results.push(idx.xpath('.[namespace-uri=""]/localname/string()'))
  }
}

results
