---
layout: inner
title: How to Use DHCCE
lead_text: ''
permalink: /how-to-use/
---

# How Do I Use It?

**Data Hub Central Community Edition (DHCCE)** provides a no-code solution for integrating data into the MarkLogic Data Hub.  For more advanced solutions, **DHCCE** components can be used with the **[DataHub Framework](http://docs.marklogic.com/datahub/)** and **[Pipes](https://github.com/marklogic-community/pipes/wiki)** to provide a low-code solution for integrating data in the MarkLogic Data Hub as well.

### Data Hub QuickStart
With MarkLogic **[Data Hub](https://docs.marklogic.com/datahub/5.4/projects/create-project-hubcentral.html)** you can have a data hub set up and ready for use in minutes. After you've created your data hub, use DHCCE to begin integrating data.

### Upload
Use Upload to load csv files into your data hub via drag and drop.  

### Connect
Use Connect to define your Entity model.  Connect also is used to configure the Explore visualization to tailor it appropriately for your consumers. 

The modeler creates entity services descriptors for use in the MarkLogic Data Hub. You can create and save a named model with Connect. Create entities, add properties to entities, define and name relationships, and define indexes.  The model will all be persisted in the data hub as entity services. 

In addition, you can choose the properties to use for the node labels that will be displayed in the Explore visualization. 

You can also project entity property values as concepts. These concepts will be expressed as Semantic triples in the data hub using MarkLogic's template driven extraction and materialize in the Explore visualization as well.

### Integrate
Use Integrate to define and manage mapping and mastering steps for your uploaded data. Map the data you loaded in Upload to the model you've created in Connect. You can also use mastering for deduplication of data and entity resolution.

### Explore
Use Explore for validating your data and to visualize the results your data harmonization that conforms to the Connect model. You can search across entities and expand their relationships. For a selected entity, you can view its property values and even view its provenance and lineage information from the info tab. Explore allows you to view the Staging and Final databases and for each provides a graph and grid view of search results.

### Notification Inbox and Mastering Validation
Match steps in Quickstart data hub flows with an action of "notify" will result in notifications delivered to the Notifications Inbox.  From the inbox, users can view similar records side by side and whether to merge a match, or block subsequent matches.  The merge history is available for discovered mastered records in Explore so users always know what records have been used to create the document they are viewing.

### Export
Use Export to export the entities you've validated in Explore as .csv files.

### Know
Know shows you all semantics triples in the data hub. You can load external sets of triples into your hub and connect them to your model by expressing Concepts in Connect. Know then allows you to search and traverse these nodes and relationships.  

Example: A "Claims" entity has an ICD9Code property (a numeric value) projected as a Concept in the Connect model.  ICD-9 Code triples are downloaded from the CDC and loaded into the MarkLogic Data Hub.  These provided text descriptions for those numeric codes.  In Know, you can then search on the text values in the downloaded triples to see the Claims they are connected to by ICD-9 Code.

### Pipes
In the event a custom step is needed for data processing in QuickStart, **[Pipes](https://github.com/marklogic-community/pipes/wiki)** can be used.

Pipes is a visual programming environment for MarkLogic. It integrates with the MarkLogic Data Hub and produces the code for a Custom Step using a no-code UI environment.


