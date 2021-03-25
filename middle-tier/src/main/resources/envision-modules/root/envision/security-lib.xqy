xquery version "1.0-ml";

module namespace ns = "http://marklogic.com/envision/security-lib";

import module namespace sec="http://marklogic.com/xdmp/security" at  "/MarkLogic/security.xqy";

declare function ns:get-users() {
	xdmp:invoke-function(function() {
		let $array := json:array()
		let $roles := (xdmp:role("envision"), xdmp:role("hub-central-user"))
		let $role-ids := /sec:role[sec:role-ids/sec:role-id=$roles]/sec:role-id
		let $_ :=
			for $user in /sec:user[sec:role-ids/sec:role-id=$role-ids]/sec:user-name/fn:string()
			return
				json:array-push($array, $user)
		return
			$array
	}, map:entry("database", xdmp:security-database()))
};
