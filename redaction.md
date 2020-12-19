# Redaction 

## What is Redaction?
Redaction is the process of removing, replacing or obscuring selected parts of a document when it is read from the database.

A typical use case for redaction is to export data from a production instance so it can be loaded into a test instance. During this process the data can be redacted. This results in a testing database that represents the real application, but with important data changed. For example, a customer name of "John Smith" could become "Mary Jones", a product code of ABC123 could become 12345, an email address could change from john@demo.com to mary@demo.com, a credit card number could change from 1234 1234 1234 1234  to ###1234 etc. Redaction rules define which parts of a document to redact, and what function is used to redact them. MarkLogic supports several standard redaction functions, and additional ones can be implemented. Please refer to the MarkLogic documentation  https://docs.marklogic.com/guide/app-dev/redaction

## What is Element Level Security?
Element level security is used to hide parts of a document from users, based on the roles assigned to the user. For example, the 'salary' property of a json document could be hidden from all users that do not have the 'HRsalary' role assigned to them.

## How do Redaction and Element Level Security differ?
Element Level Security uses roles to determine who can see parts of a document. Redaction does not use roles.
Element Level Security hides parts of a document. Redaction can conceal, mask, partially mask and change the parts.

## Data hubs and PII
While defining a model the user can identify properties that contain PII. A security configuration can then be deployed that restricts access to these properties to users that have the 'pii-reader' role. This approach uses Element Level Security - please refer to https://docs.marklogic.com/datahub/5.2/security/pii/pii-data-mgmt.html

## Redaction in Envision
In Envision you can set redaction rules for a property by selecting "Redacted" in the advance tab for a property setting at the parent entity level. Envision supports ways of defining rules that determine how to redact parts of a document based on selecting this setting.

You can also set the PII for a property on an entity to implement Element Level Security for that property.

For redaction to work the "envision" role must also have the "redaction-user" role. To achive this, run 
```
./gradlew mlDeployRoles 
```

When you define a model, using the Connect tab, the user can select the 'Advanced' property options and mark a property as containing Redacted. In Envision this creates a redaction rule in the schema database associated with the final database. The collection for the rule is 'redactionRule' if the system is not configured for multi tenant. If multi tenant is being used the collection is called redactionRule4<currentUserName>. 

After doing this, all users will see the message "### PII Redacted ###" when they view a document that uses the property.

Note that redaction applies to the final database only, data in the staging database is not currently shown in redacted form. 

To control which users should see the real value, we use a config document in the FINAL database. This document has the uri /redactionRules2Roles.json ( or redactionRules2Roles4<currentUserName>.json for multi tenant.) For example, the code below, which you can run using Query Console, says that the any rules in the redactionRule collection should not be applied if the current user has the 'pii-reader' role :
```
declareUpdate()

xdmp.documentInsert("/redactionRules2Roles.json",
	{rules:
		[{ redactionRuleCollection: "redactionRule",
				rolesThatDoNotUseRedaction: ["pii-reader"]
			}
		]
	},
	{
	permissions : [xdmp.defaultPermissions(), xdmp.permission("envision", "read")]
	}
)
```

In addition to Redaction rules defined via the properties on the connect tab, you can implement additional rules. For example, the Query Console code below, which should be run in the final database's schema database (e.g. 'data-hub-final-SCHEMAS'), redacts the Employee's departmentId, surname and email. These rules must be put into a collection that starts with the string 'redactionRole' for single tenant, or 'redactionRole4<currentUserName>' when in multi tenant mode. In multi tenant mode the document names should also include the users name, to ensure they are unique. For these rules to be implemented the config document at /redactionRules2Roles.json should be changed as per the second set of code below. This code will result in the department and surname being redacted if the user does not belong to either the 'admin' or 'anotherRole' roles, and the email being redacted for all users (because redactionRule-Set2 is not mentioned in /redactionRules2Roles.json) :
```
'use strict';

declareUpdate()

xdmp.documentInsert("/rules/envision/conceal-departmentId.json",
          { "rule": {
              "description": "Remove departmentId",
              "path": "/envelope/instance/Employee/departmentId",

              "method": { "function": "redact-regex" },
              "options": {
                "pattern" : "^[\u0001-\uE007F].*",
                "replacement" :  "### redacted ###"
              }
           }
         },
         {
           permissions : [xdmp.defaultPermissions(), xdmp.permission("envision", "read")],
            collections : ["redactionRule"]
         }
)

xdmp.documentInsert("/rules/envision/mask-surname.json",
          { "rule": {
              "description": "Hide emails",
              "path": "/envelope/instance/Employee/lastName",

              "method": { "function": "mask-random" },
              "options": {
                "length" : 10
              }
           }
         },
         {
           permissions : [xdmp.defaultPermissions(), xdmp.permission("envision", "read")],
            collections : ["redactionRule"]
         }
)

xdmp.documentInsert("/rules/envision/conceal-email.json",
          { "rule": {
              "description": "Hide emails",
              "path": "/envelope/instance/Employee/email",

              "method": { "function": "redact-email" },
              "options": {
                "level" : "full"
              }
           }
         },
         {
           permissions : [xdmp.defaultPermissions(), xdmp.permission("envision", "read")],
            collections : ["redactionRule-Set2"]
         }
)
```

/redactionRules2Roles.json:
```
'use strict';
declareUpdate()

xdmp.documentInsert("/redactionRules2Roles.json",
  {rules:
     [{ redactionRuleCollection: "redactionRule",
        rolesThatDoNotUseRedaction: ["pii-reader"]
      },

      { redactionRuleCollection: "redactionRule",
        rolesThatDoNotUseRedaction: ["anotherRole", "admin"]
      }
    ]
  },
  {
  permissions : [xdmp.defaultPermissions(), xdmp.permission("envision", "read")]
  }
)
```
Note that after changing redaction rules or config, you should perform a new search on the Explore tab before you will see the result of your change.

Note that the redaction rules are applied on search results. For example, if a document has a name field containing "David", and this field has redaction, the user can search for "David" and find the document, but instead of "David" they will see the result of the redaction which could be "### redacted ###" or "fdgdgsg".

Redaction is applied on egress of data.  PII is applied on the data itself.

If you switch from single to multi tenant mode you should re-apply any redaction rules. 
