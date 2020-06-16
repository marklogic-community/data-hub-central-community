'use strict'

const ents = require('/envision/entities.sjs');

var qtext;
var searchQuery;
var facetName;

let query = fn.head(xdmp.unquote(searchQuery)).root
ents.getValues(qtext, query, facetName)
