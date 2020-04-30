const ents = require('/entities.sjs');
var fromId;
var from;
var label;
var to;
var pageLength;
var page;

const connectionLimit = pageLength
const start = ((page - 1) * pageLength);
const labels = [label]
const results = ents.getEntities([fromId], { connectionLimit, labels })
results;
