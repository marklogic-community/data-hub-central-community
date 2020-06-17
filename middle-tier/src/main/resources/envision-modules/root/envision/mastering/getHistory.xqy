xquery version "1.0-ml";

declare namespace prov = "http://www.w3.org/ns/prov#";
declare namespace auditing = "http://marklogic.com/smart-mastering/auditing";

declare variable $uri as xs:string external;

declare option xdmp:mapping "false";

declare function local:auditing-receipts-for-doc-history($doc-uris as xs:string*, $returned-docs)
{
  let $cols := cts:collection-match("sm-*-auditing")
  return
  if (fn:exists($doc-uris)) then
    cts:search(fn:collection($cols)/prov:document,
      cts:and-query((
        cts:element-value-query(
          (
            xs:QName("auditing:previous-uri"),
            xs:QName("auditing:new-uri"),
            xs:QName("previous-uri"),
            xs:QName("new-uri")
          ),
          $doc-uris,
          "exact"
        ),
        cts:not-query(cts:document-query($returned-docs ! xdmp:node-uri(.)))
      ))
    )
  else
    $returned-docs
};

declare function local:document-history($doc-uri as xs:string)
  as document-node()
{
  xdmp:to-json(
    object-node {
      'activities':
        array-node {
          for $audit in local:auditing-receipts-for-doc-history($doc-uri, ())
          let $time := xs:dateTime($audit/prov:wasGeneratedBy/prov:time)
          (: order most recent to oldest :)
          order by $time descending
          return
            object-node {
              "auditUri": xdmp:node-uri($audit),
              "type": fn:string($audit/prov:activity/prov:type),
              "label": fn:string($audit/prov:activity/prov:label),
              "resultUri": fn:string($audit/auditing:new-uri),
              "wasDerivedFromUris": array-node { $audit/auditing:previous-uri ! fn:string(.) },
              "time": fn:string($time)
            }
        }
    }
  )
};

local:document-history($uri)
