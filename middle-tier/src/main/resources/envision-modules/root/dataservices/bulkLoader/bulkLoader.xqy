xquery version "1.0-ml";

declare option xdmp:mapping "false";

declare variable $session as document-node()? external;
declare variable $endpointState as document-node()? external;
declare variable $workUnit as document-node()? external;
declare variable $input as document-node()* external;

let $keys := $workUnit/keys/fn:string()
let $key-delimiter := $workUnit/keyDelimiter/fn:string()
let $uri-prefix := $workUnit/uriPrefix/fn:string()
let $uri-suffix := $workUnit/uriSuffix/fn:string()
let $collections := $workUnit/collections/fn:string()
let $doc-type := $workUnit/docType/fn:string()

let $_ := if("json" eq fn:lower-case($doc-type)) then
  for $doc in $input
  let $jsonDoc := xdmp:unquote($doc, "format-json")
  let $primary-id :=
    fn:string-join(
      for $key in $keys
        let $s := $jsonDoc/*/*[fn:name() eq $key]/fn:string()
        return $s
      ,
      $key-delimiter
    )
  let $uri := fn:concat($uri-prefix, $primary-id, $uri-suffix)
  return
    xdmp:document-insert($uri, $jsonDoc,
      map:map() => map:with("collections", ($collections))
    )
  else
  for $doc in $input
  let $xmlDoc := xdmp:unquote($doc)
  let $primary-id :=
    fn:string-join(
      for $key in $keys
        return $xmlDoc/*/*[fn:name() eq $key]/fn:string()
      ,
      $key-delimiter
    )
  let $uri := fn:concat($uri-prefix, $primary-id, $uri-suffix)
  return
    xdmp:document-insert($uri, $xmlDoc,
      map:map() => map:with("collections", ($collections))
    )
return ()