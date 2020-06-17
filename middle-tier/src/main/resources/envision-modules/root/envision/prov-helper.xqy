xquery version "1.0-ml";

module namespace ns = "http://marklogic.com/prov-helper";

import module namespace config = "http://marklogic.com/data-hub/config" at '/com.marklogic.hub/config.xqy';

declare namespace prov = "http://www.w3.org/ns/prov#";

declare option xdmp:mapping "false";

(:
	Given a $uri, returns an array of provenance entries from the jobs db
:)
declare function ns:get-prov($uri)
{
	xdmp:invoke-function(function() {
		let $a := json:array()
		let $_ :=
			for $prov in cts:search(fn:collection('http://marklogic.com/provenance-services/record')/prov:document, cts:element-value-query(xs:QName('location'), $uri))
			let $o := json:object()
			let $_ := map:put($o, "id", sem:uuid-string())
			let $_ :=
				for $key in $prov/*
				let $oo := json:object()
				return
					for $child in $key/*
					return (
						map:put($oo, fn:local-name($child), ($child/@prov:ref/fn:string(), $child/fn:string())[1]),
						map:put($o, fn:local-name($key), $oo)
					)
			return
				json:array-push($a, $o)
		return
			document { xdmp:to-json($a) }
	}, map:entry("database", xdmp:database($config:JOB-DATABASE)))
};

