'use strict'

const admin = require('/MarkLogic/admin.xqy');
const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE

var config = admin.getConfiguration()
const idx = fn.head(admin.databaseGetRangeElementIndexes(config, xdmp.database(finalDB)))
if (idx) {
  idx.xpath('.[namespace-uri=""]/localname/string()').toArray()
} else {
  []
}
