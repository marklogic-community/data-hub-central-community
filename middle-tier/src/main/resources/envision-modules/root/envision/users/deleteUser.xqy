xquery version "1.0-ml";

import module namespace sec="http://marklogic.com/xdmp/security" at
  "/MarkLogic/security.xqy";

import module namespace config = "http://marklogic.com/data-hub/config"
	at "/com.marklogic.hub/config.xqy";

declare variable $user as xs:string external;
declare variable $role-name := xdmp:md5($user);
declare variable $finalDB := $config:FINAL-DATABASE;
declare variable $stagingDB := $config:STAGING-DATABASE;

declare option xdmp:mapping "false";

declare function local:delete-envision-assets($user, $role-name) {
	for $db in (xdmp:database($finalDB), xdmp:database($stagingDB), xdmp:schema-database(xdmp:database($finalDB)), xdmp:schema-database(xdmp:database($finalDB)))
	return
	  xdmp:invoke-function(function() {
			cts:uri-match("/entities/" || $user || "*.entity.json") ! xdmp:document-delete(.),
			cts:uri-match("/envision/" || $user || "*") ! xdmp:document-delete(.),
			xdmp:collection-delete("http://marklogic.com/envision/user/" || $user),

			let $uri := "/envision/users/" || $role-name || ".json"
			where fn:doc-available($uri)
			return
				xdmp:document-delete($uri),

			let $uri := "/flows/" || $role-name || ".flow.json"
			where fn:doc-available($uri)
			return
				xdmp:document-delete($uri),

			cts:uri-match("/ingest/" || $user || "*") ! xdmp:document-delete(.),
			cts:uri-match("/data/" || $user || "*") ! xdmp:document-delete(.),
			xdmp:collection-delete($user),
			xdmp:collection-delete("http://marklogic.com/envision/" || $user || "_mappings")
		}, map:entry("database", $db))
};

declare function local:delete-user($user, $role-name) {
	if (xdmp:invoke-function(function() {
			sec:user-exists($user)
		},
		map:entry("database", xdmp:security-database())
	)) then
	(
		xdmp:invoke-function(function() {
			sec:remove-user($user)
		},
		map:entry("database", xdmp:security-database())),

		xdmp:invoke-function(function() {
			sec:remove-role($role-name)
		},
		map:entry("database", xdmp:security-database()))
	)
	else ()
};

let $_ := (
	local:delete-envision-assets($user, $role-name),
	local:delete-user($user, $role-name)
)
return
	fn:true()
