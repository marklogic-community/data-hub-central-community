---
layout: inner
title: How to Use Envision
lead_text: ''
permalink: /how-to-use/
---

# How Do I Use It?

**Envision** **[Connect](/envision/components/connect/)** and **[Explore](/envision/components/explore/)**,  with the **[DataHub Framework's QuickStart](http://docs.marklogic.com/datahub/)** and **[Pipes](https://github.com/marklogic-community/pipes/wiki)** provide a low-code solution for integrating data in the MarkLogic Data Hub.
<br><br> 
![Workflow.](/envision/images/Workflow.png)
<br><br>

### QuickStart
With MarkLogic Data Hub Framework Quickstart you can have a data hub set up and ready for use in minutes. QuickStart is used for ingest, harmonizing and mastering of data. Create your model in Connect and then use Quickstart to ingest data into your hub and map the data sources to the model you've defined. Once data is harmonized, you're ready to validate and query your data using Explore. 

### Connect
Use Connect to define your Entity model.  Connect also is used to configure the Explore visualization to tailor it appropriately for your consumers. 

The modeler creates entity services descriptors for use in the MarkLogic Data Hub. You can create and save a named model with Connect. Create entities, add properties to entities, define and name relationships, and define indexes.  The model will all be persisted in the data hub as entity services. 

In addition, you can choose the properties to use for the node labels that will be displayed in the Explore visualization. 

You can also project entity property values as concepts. These concepts will be expressed as Semantic triples in the data hub using MarkLogic's template driven extraction and materialize in the Explore visualization as well.

### Explore
Use Explore for validating your data and to visualize the results your data harmonization that conforms to the Connect model. You can search across entities and expand their relationships. For a selected entity, you can view its property values and even view its provenance and lineage information from the info tab.

### Notification Inbox and Mastering Validation
Match steps in Quickstart data hub flows with an action of "notify" will result in notifications delivered to the Notifications Inbox.  From the inbox, users can view similar records side by side and whether to merge a match, or block subsequent matches.  The merge history is available for discovered mastered records in Explore so users always know what records have been used to create the document they are viewing.

### Pipes
In the event a custom step is needed for data processing in QuickStart, Pipes can be used.

Pipes is a visual programming environment for MarkLogic. It integrates with the MarkLogic Data Hub and produces the code for a Custom Step using a no-code UI environment.


