xquery version "1.0-ml";

declare option xdmp:mapping "false";

declare variable $uri as xs:string external;
declare variable $namespaces external;

declare function local:speed-walk($o, $nodes as node()*, $root-xpath) {
	let $_ :=
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
		()
	return
		()
};

declare function local:speed-walk-json($o, $nodes as node(), $parent) {
	for $n in $nodes
	let $xpath := $parent || fn:string(fn:node-name($n))
	let $oo := (map:get($o, $xpath), json:object())[1]
	let $_ := map:put($o, $xpath, $oo)
	let $_ :=
    for $nn in $n/*
    return
      local:speed-walk-json($oo, $nn, $xpath || "/")
	return
		$o
};

declare function local:walk($o, $n as node()+) {
	for $xpath in map:keys($o)
	let $obj := map:get($o, $xpath)
	let $local-name := fn:string((fn:tokenize($xpath, "/"))[fn:last()])
	let $value-string :=
		if (fn:starts-with($local-name, '@')) then
			"$n/" || $local-name
		else
			let $local-name := fn:replace($local-name, "\*:", "")
			return
				"$n/*[fn:local-name(.) = '" || $local-name || "']"
	let $nodes := xdmp:value($value-string)
	let $nodes :=
		if (fn:empty($nodes)) then
			object-node {}
		else $nodes
	let $oo := json:object()
	let $ns := fn:namespace-uri-from-QName(fn:node-name($nodes[1]))
	let $ns-prefix := (
		fn:prefix-from-QName(fn:node-name($nodes[1])),
		if (fn:exists($ns)) then
			map:get($namespaces, $ns)
		else ()
	)[1]
	let $_ := map:put($oo, "ns", if (fn:string-length($ns) > 0) then $ns else json:null())
	let $_ := map:put($oo, "nsPrefix", if (fn:string-length($ns-prefix) > 0) then $ns-prefix else json:null())
	let $_ := map:put($oo, "name",
		if ($nodes[1] instance of attribute()) then
			"@" || fn:node-name($nodes[1])
		else if ($nodes[1] instance of element()) then
			fn:local-name($nodes[1])
		else
			fn:node-name($nodes[1]))
	let $_ := map:put($oo, "xpath", $xpath)
	let $_ :=
		if (map:count($obj) > 0) then (
			map:put($oo, "children", json:to-array(local:walk($obj, $nodes))),
			if ($nodes[1] instance of element() and fn:count($nodes/text()) le fn:count($nodes)) then
				map:put($oo, "value", $nodes/string())
			else ()
		)
		else if ($nodes[1] instance of attribute()) then
			map:put($oo, "value", $nodes)
		else
			map:put($oo, "value", $nodes/string())
	return
		$oo
};

declare function local:to-json($n as node()*) {
	if ($n[1] instance of element()) then
		let $root-xpath := xdmp:path($n/.., fn:false())
		let $root-xpath :=
			if (fn:ends-with($root-xpath, "/")) then
				$root-xpath
			else
				$root-xpath || "/"
		let $o := json:object()
		let $_ := local:speed-walk($o, $n, $root-xpath)
		return
			json:to-array(local:walk($o, $n/..))
	else
		let $o := json:object()
		let $_ :=
			for $nn in $n
			return
				local:speed-walk-json($o, $nn, "")
		return
			json:to-array(local:walk($o, ($n/..)[1]))
};

let $ns-sequence :=
	let $ns := xdmp:from-json($namespaces)
	for $key in map:keys($ns)
	return
		($key, fn:string(map:get($ns, $key)))

let $_ := xdmp:set($namespaces, -$namespaces)

let $doc := fn:doc($uri)
let $i := ($doc/*:envelope/*:instance, $doc)[1]
let $i :=
	if ($i instance of element()) then
		$i/*
	else $i/*
return
	xdmp:with-namespaces($ns-sequence, local:to-json($i))
