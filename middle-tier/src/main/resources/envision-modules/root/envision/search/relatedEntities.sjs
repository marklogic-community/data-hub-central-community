const ents = require('/envision/entities.sjs');
const modelWrapper = require('/envision/model.sjs');

var model;
var uri;
var label;
var page;
var pageLength;

model = modelWrapper(model);
const connectionLimit = pageLength
const labels = [label]
const results = ents.getEntities(model, [uri], { connectionLimit, labels })
results;
