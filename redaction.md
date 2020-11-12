# Redaction 

## What is Redaction?
Redaction is the process of removing, replacing or obscuring selected parts of a document when it is read from the database.

A typical use case for redaction is to export data from a production instance so it can be loaded into a test instance. During this process the data can be redacted. This results in a testing database that represents the real application, but with important data changed. For example, a customer name of "John Smith" could become "Mary Jones", a product code of ABC123 could become 12345, an email address could change from john@demo.com to mary@demo.com, a credit card number could change from 1234 1234 1234 1234  to ###1234 etc. Redaction rules define which parts of a document to redact, and what function is used to redact them. MarkLogic supports several standard redaction functions, and additional ones can be implemented. Please refer to the MarkLogic documentation  https://docs.marklogic.com/guide/app-dev/redaction

## What is Element Level Security?
Element level security is used to hide parts of a document from users, based on the roles assigned to the user. For example, the 'salary' property of a json document could be hidden form all users that do not have the 'HRsalary' role assigned to them.

## How do Redaction and Element Level Security differ?
Element Level Security uses roles to determine who can see parts of a document. Redaction does not use roles.
Element Level Security hides parts of a document, redaction can conceal, mask, partially mask and change the parts.

## Data hubs and PII
While defining a model the user can identify properties that contain PII. A security configuration can then be deployed that restricts access to these properties to users that have the 'pii-reader' role. This approach uses Element Level Security - please refer to https://docs.marklogic.com/datahub/5.2/security/pii/pii-data-mgmt.html

## Redaction in Envision
For Envision we decided to implement PII using redaction rather than Element Level Security. We also support other ways of defining rules that determine when to redact parts of a document.

When you define a model, using the contect tab, the user can select the 'Advanced' property options and mark a property as containing PII. In Envision this creates a redaction rule in the 'piiRules' collection.

After doing this, all users will see the message "### PII Redacted ###" when they view a document that uses the property.

To control which users should see the real value, we use a config document in the FINAL database. This document has the uri /redactionRules2Roles.json. For example, the code below, which you can run using Query Console, says that the piiRules should not be applied if the current user has the 'pii-reader' role :
```
declareUpdate()

xdmp.documentInsert("/redactionRules2Roles.json",
	{rules:
		[{ redactionRuleCollection: "piiRules",
				rolesThatDoNotUseRedaction: ["pii-reader"]
			}
		]
	},
	{
	permissions : [xdmp.defaultPermissions(), xdmp.permission("rest-reader", "read")],
	collections : ["redactionRules"]
	}
)
```

In addition to PII rules defined via the properties on the connect tab, you can implement additional rules via Query Console. For example, the Query Console code below, which should be run in the 'data-hub-final-SCHEMAS' database, redacts the Employee's departmentId, surname and email. These rules are put into a collection called 'envisionRules'. For these rules to be implemented the config document at /redactionRules2Roles.json should be changed as per the second set of code. This code will result in these new redactions being applied if the user does not belong to either the 'admin' or 'anotherRole' roles:
```
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
           permissions : xdmp.defaultPermissions(),
            collections : ["envisionRules"]
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
           permissions : xdmp.defaultPermissions(),
            collections : ["envisionRules"]
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
           permissions : xdmp.defaultPermissions(),
            collections : ["envisionRules"]
         }
)
```

/redactionRules2Roles.json:
```
'use strict';
declareUpdate()

xdmp.documentInsert("/redactionRules2Roles.json",
  {rules:
     [{ redactionRuleCollection: "piiRules",
        rolesThatDoNotUseRedaction: ["pii-reader"]
      },

      { redactionRuleCollection: "envisionRules",
        rolesThatDoNotUseRedaction: ["anotherRole", "admin"]
      }
    ]
  },
  {
  permissions : [xdmp.defaultPermissions(), xdmp.permission("rest-reader", "read")],
  collections : ["redactionRules"]
  }
)
```
Note that after changing redaction rules or config, you should perform a new search on the Explore tab before you will see the result of your change.
