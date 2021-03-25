const mappingArtifactLib = require('/data-hub/5/artifacts/mapping.sjs');
const lib = require('/data-hub/5/builtins/steps/mapping/entity-services/lib.sjs');
const es = require('/MarkLogic/entity-services/entity-services');

var mappingName;
var format;
var uri;

const doc = cts.doc(uri);
let mapping = fn.head(mappingArtifactLib.getArtifactNode(mappingName));
let mappingURIforXML = fn.replace(xdmp.nodeUri(mapping), 'json$','xml');
let instance = lib.extractInstance(doc);
const resp = es.mapToCanonical(instance, mappingURIforXML, {'format': format});
xdmp.quote(
	resp,
	{
		indent: 'yes',
		indentUntyped: 'yes',
		omitXmlDeclaration: 'yes'
	}
)
