(:
  Copyright 2012-2019 MarkLogic Corporation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
:)
xquery version "1.0-ml";

module namespace transform = "http://marklogic.com/rest-api/transform/prettifyXML";

declare function transform(
  $context as map:map,
  $params as map:map,
  $content as document-node()
  ) as document-node()
{
	map:put($context, "output-types", "text/plain"),
	let $redacted := xdmp:javascript-eval("
		const rdt = require('/envision/redaction-lib.sjs');
		rdt.redact(content);
	", ("content", $content))
	return
		if (fn:exists($redacted/element())) then
			document {
				xdmp:quote(
					$redacted,
					<options xmlns="xdmp:quote">
						<indent>yes</indent>
						<indent-untyped>yes</indent-untyped>
						<omit-xml-declaration>yes</omit-xml-declaration>
					</options>
				)
			}
		else
			$redacted
};

