xquery version "1.0-ml";

declare option xdmp:mapping "false";

declare variable $uri as xs:string external;
declare variable $namespaces external;

declare function local:speed-walk($o, $nodes as node()*, $root-xpath) {
	for $n in $nodes
	let $xpath := xdmp:path($n, fn:false())
	let $xpath := fn:substring-after($xpath, $root-xpath)
	let $xpath := fn:replace($xpath, "\[[^\]]\]", "")
	let $oo := (map:get($o, $xpath), json:object())[1]
	let $_ := map:put($o, $xpath, $oo)
	let $_ :=
		if ($n/@*) then
			for $attr in $n/@*
			let $xpath := xdmp:path($attr, fn:false())
			let $xpath := fn:substring-after($xpath, $root-xpath)
			let $xpath := fn:replace($xpath, "\[[^\]]\]", "")
			return
				map:put($oo, $xpath, json:object())
		else ()
	let $_ := local:speed-walk($oo, $n/*, $root-xpath)
	return
		$o
};

declare function local:speed-walk-json($o, $nodes as node(), $parent) {
	for $n in $nodes
	let $xpath := $parent || fn:node-name($n)
	let $oo := (map:get($o, $xpath), json:object())[1]
	let $_ := map:put($o, $xpath, $oo)
	let $_ :=
    for $nn in $n/*
    return
      local:speed-walk-json($oo, $nn, $xpath || "/")
	return
		$o
};

declare function local:walk($o, $n as node()) {
	for $xpath in map:keys($o)
	let $obj := map:get($o, $xpath)
	let $node := (xdmp:value("$n/*[fn:local-name(.) = '" || $xpath || "']"))[1]
	let $oo := json:object()
	let $ns := fn:namespace-uri-from-QName(fn:node-name($node))
	let $ns-prefix := fn:prefix-from-QName(fn:node-name($node))
	let $_ := map:put($oo, "ns", if (fn:string-length($ns) > 0) then $ns else json:null())
	let $_ := map:put($oo, "nsPrefix", if (fn:string-length($ns-prefix) > 0) then $ns-prefix else json:null())
	let $_ := map:put($oo, "name",
		if ($node instance of attribute()) then
			"@" || fn:node-name($node)
		else if ($node instance of element()) then
			fn:local-name($node)
		else
			fn:node-name($node))
	let $_ := map:put($oo, "xpath", $xpath)
	let $_ :=
		if (map:count($obj) > 0) then
			map:put($oo, "children", json:to-array(local:walk($obj, $n)))
		else
			map:put($oo, "value", $node/string())
	return
		$oo
};

declare function local:to-json($n as node()*) {
	if ($n/.. instance of element()) then
		let $root-xpath := xdmp:path($n/.., fn:false()) || "/"
		let $o := local:speed-walk(json:object(), $n, $root-xpath)
		return
			json:to-array(local:walk($o, $n/..))
	else
		let $o := json:object()
		let $_ :=
			for $nn in $n
			return
				local:speed-walk-json($o, $nn, "")
		return
			json:to-array(local:walk($o, $n/..))
};

let $_ :=
	let $ns := xdmp:from-json($namespaces)
	return
	xdmp:set($namespaces,
		for $key in map:keys($ns)
		return
			($key, fn:string(map:get($ns, $key)))
	)

let $i := fn:doc($uri)/*:envelope/*:instance
return
	xdmp:with-namespaces($namespaces, local:to-json($i/*))
