---
layout: inner
title: Integrate
lead_text: ''
permalink: /components/integrate/
---

# Integrate

Create flows, define mapping and mastering steps and run them to integrate data in the MarkLogic Data Hub.

### Getting Started
A flow is just a collection of steps. Steps are what you configure to integrate data in the data hub.  Steps can be of type ingest, mapping, matching, merging, and custom.

Data Hub Central Community Edition (DHCCE) allows you to create mapping, matching, and merging steps. However, you can run DHCCE against an existing hub and you'll have your existing flows and steps available for use. You will find that all step types are respected and usable by DHCCE. See "Additional Step Info" below for more details on ingest and custom step types.

<br> 
![Integrate](/data-hub-central-community/images/IntegrateGH.png)
<br><br>

* Flows are detailed on the left pane.  
	* Click the '+' icon to add a new flow.
	* Click a flow name to display the flow in the center pane.
* The selected flow and its steps are illlustrated in the center.
	* Click "Add Step" to add a step to the flow.
	* Click "Run Steps" to run the steps in your flow.
	* Click the trashcan icon next to the flow name to delete the flow and all its steps from the hub.
	* Click a step to see its detail displayed below the flow.
* The details of which entities are available for use (based on the model selectin in Connect) and how many have already been harmonized is detailed on the right. 
	* Click the trashcan icon to the right of an entity count in the "Manage My Data" pane to delete those entities from the final database in the data hub.

#### Mapping Steps
When using Upload to load data, the name of the file will be the name of the collection the data is saved to in Staging.  When using ingest steps, typically the name of the ingest step is used. This collection name can be used for the Data Source field when creating mapping steps.

#### Mastering Steps
Mapped data will be saved in a collection named for the entity it is mapped to. This collection name should be used for the Data Source when creating Matching or Merging steps.

#### Additional Icons
Collapsing the "Manage My Data" pane to increase the size of the workspace of mapping step configurations can be very helpful. The following icons help to maximize the workspace in the center and to provide additional useful information.

* Click the 2-way arrow icon in the upper left of the center pane to minimize the "Flows" pane.
* Click the database icon in the upper right of the center pane to minimize the "Manage My Data" pane.
* Click the counter-clockwise arrow next to the database icon in the upper right to display the Jobs information for the selected flow.
	* If you run a flow and get an error. You can inspect the Jobs information for error messages.

#### Additional Step Info
Ingest steps aren't really needed if you think about it. Data can be loaded outside of DHCCE into the hub as well and still processed in mapping and mastering steps by DHCCE.  All a mapping step requires to map a source file to the target is a collection name or a query, not a previous ingestion step. Using Upload, we already know what the file is and where it came from, so no additional ingest step required.

The path of data for existing ingest steps is editable. For other ingest step configurations, attend to the corresponding .json config file in the data hub project.

For custom steps, the path is displayed.  To edit the custom module, attend to the .json file on the file system associated with the displayed path.

