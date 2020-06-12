xquery version "1.0-ml";

module namespace idx = "https://marklogic.com/envision/indexes";

import module namespace admin = "http://marklogic.com/xdmp/admin"
		  at "/MarkLogic/admin.xqy";

import module namespace config = "http://marklogic.com/data-hub/config"
	at "/com.marklogic.hub/config.xqy";

declare variable $finalDB := $config:FINAL-DATABASE;
declare variable $model := fn:doc('model.json');

declare function idx:get-search-config() {
	let $config := admin:get-configuration()
	return
		<options xmlns="http://marklogic.com/appservices/search">
			<search-option>unfiltered</search-option>
			<page-length>10</page-length>

			<additional-query>
				{
					cts:and-query((
						let $collections := cts:collection-match('sm-*-archived')
						return
							if ($collections) then
								cts:not-query(
									cts:collection-query($collections)
								)
							else (),
						cts:or-query((
							cts:json-property-scope-query("instance", cts:true-query()),
							cts:element-query(fn:QName('http://marklogic.com/entity-services', 'instance'), cts:true-query())
						))
					))

				}
			</additional-query>
			<term apply="term">
				<empty apply="all-results"/>
			</term>

			<!-- Default grammer -->
			<grammar>
				<quotation>"</quotation>
				<implicit>
					<cts:and-query strength="20" xmlns:cts="http://marklogic.com/cts"/>
				</implicit>
				<starter strength="30" apply="grouping" delimiter=")">(</starter>
				<starter strength="40" apply="prefix" element="cts:not-query">-</starter>
				<joiner strength="10" apply="infix" element="cts:or-query" tokenize="word">OR</joiner>
				<joiner strength="20" apply="infix" element="cts:and-query" tokenize="word">AND</joiner>
				<joiner strength="30" apply="infix" element="cts:near-query" tokenize="word">NEAR</joiner>
				<joiner strength="30" apply="near2" consume="2" element="cts:near-query">NEAR/</joiner>
				<joiner strength="50" apply="constraint">:</joiner>
				<joiner strength="50" apply="constraint" compare="LT" tokenize="word">LT</joiner>
				<joiner strength="50" apply="constraint" compare="LE" tokenize="word">LE</joiner>
				<joiner strength="50" apply="constraint" compare="GT" tokenize="word">GT</joiner>
				<joiner strength="50" apply="constraint" compare="GE" tokenize="word">GE</joiner>
				<joiner strength="50" apply="constraint" compare="NE" tokenize="word">NE</joiner>
			</grammar>

			<!-- Default sort order. You can also associate states with sort orders to allow switching sort orders -->
			<sort-order direction="descending">
				<score/>
			</sort-order>

			<return-query>1</return-query>

			<operator name="sort">
				<state name="score">
					<sort-order>
						<score/>
					</sort-order>
				</state>

			{
				for $idx in admin:database-get-range-element-indexes($config, xdmp:database($finalDB))
				for $name in fn:tokenize($idx/*:localname/fn:string(), " ")
				return (
					<state name="{$name}Asc" xmlns="http://marklogic.com/appservices/search">
						<sort-order direction="ascending" type="xs:{$idx/*:scalar-type}" collation="http://marklogic.com/collation/codepoint">
							<element ns="{$idx/*:namespace-uri/fn:string()}" name="{$name}"/>
						</sort-order>
					</state>,

					<state name="{$name}Desc" xmlns="http://marklogic.com/appservices/search">
						<sort-order direction="descending" type="xs:{$idx/*:scalar-type}" collation="http://marklogic.com/collation/codepoint">
							<element ns="{$idx/*:namespace-uri/fn:string()}" name="{$name}"/>
						</sort-order>
					</state>
				)
			}
				<!-- <state name="eyeColorAsc">
					<sort-order
				direction="ascending" type="xs:string" collation="http://marklogic.com/collation/codepoint">
						<path-index>eyeColor</path-index>
					</sort-order>
				</state> -->
			</operator>

			<constraint name="Collections">
				<collection facet="true">
					<facet-option>frequency-order</facet-option>
					<facet-option>descending</facet-option>
					<facet-option>limit=5</facet-option>
				</collection>
			</constraint>

			<values name="Collections">
				<collection>
					<values-option>frequency-order</values-option>
					<values-option>descending</values-option>
				</collection>
			</values>


			{
				for $idx in admin:database-get-range-element-indexes($config, xdmp:database($finalDB))
				for $name in fn:tokenize($idx/*:localname/fn:string(), " ")
				return (
					<constraint name="{$name}" xmlns="http://marklogic.com/appservices/search">
						<range type="xs:{$idx/*:scalar-type}" facet="true" collation="{$idx/*:collation/fn:string()}">
							<facet-option>frequency-order</facet-option>
							<facet-option>descending</facet-option>
							<facet-option>limit=5</facet-option>
							<element ns="{$idx/*:namespace-uri/fn:string()}" name="{$name}"/>
						</range>
					</constraint>,
					<values name="{$name}" xmlns="http://marklogic.com/appservices/search">
						<range type="xs:{$idx/*:scalar-type}" collation="{$idx/*:collation/fn:string()}">
							<element name="{$name}" ns="{$idx/*:namespace-uri/fn:string()}" />
						</range>
						<values-option>frequency-order</values-option>
						<values-option>descending</values-option>
					</values>
				)
			}
	</options>
};
