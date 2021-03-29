---
layout: inner
title: Data Hub Central Community Edition
lead_text: ''
permalink: /
---

# Data Hub Central Community Edition (DHCCE)
<p>Data Hub Central Community Edition provides a visual way to interact with and experience your true multi-model data hub.  You can go from loading data, to integrating, exploring, validating, and exporting curated results using the following components:  </p>

<p><i>This project was formerly known as Envision. This project has been renamed and enhanced to be compatible with Data Hub 5.4.</i></p>

* **Upload** to load source csv files as-is into the data hub via drag and drop
* **Connect** to visually model the specification for how you want to express your integrated business concepts as entities and relationships
* **Integrate** to map your source data to your model as well as deduplicate and master it
* **Explore** to view your harmonized data graph along with provenance
* **Export** to export your newly haromnized entities as .csv files for consumption by BI tools
*  **Know** to search and navigate your semantics ontology.

## Integration with MarkLogic Data Hub
DHCCE v5.4.1 requires **[MarkLogic Data Hub Framework](https://github.com/marklogic/marklogic-data-hub/releases)** 5.4.x or greater and **[MarkLogic 10](https://developer.marklogic.com/products/marklogic-server/10.0)** or greater. It will work with on-prem data hubs as well as with cloud including MarkLogic Data Hub Service (DHS)

<i>Note: If you are using MarkLogic Data Hub 5.2.x then continue to use DHCCE v2.0.5.</i>

You can use Connect to create models that you can visualize in Explore with new as well as existing data hubs. Integrate also works with new as well as existing data hubs.  Know provides a visualization for Semantics triples only.
<br>
### Run the jar

The jar is configured so that you can drop it into a DHF project directory and run it there.

`java -jar hub-central-community.jar`

#### Non-standard ports or different User
On first run DHCCE will install its modules into your DHF modules database. This happens before DHCCE tries to read your hub configuration. If you are using non-standard ports you'll need to make a few changes to an application.properties file. Simply create application.properties next to hub-central-community.jar.

```properties
# Change these as needed
marklogic.username=admin
marklogic.password=admin
marklogic.port=8011
marklogic.managePort=8002
marklogic.adminPort=8001
```

_Note: Models will be saved in a directory called ./conceptConnectorModels, which can be found sibling to the hub-central-community.jar file by default. You can move your existing models here or see below for how to specify a different models directory._

If you need to point it at another folder where the DHF is installed, run like so:

`java -DdhfDir=/full/path/to/your/datahub -jar hub-central-community.jar`

If you have existing Connect models you'd like to use you can also specify a different models directory:

`java -DdhfDir=/full/path/to/your/datahub -DmodelsDir=/full/path/to/your/models/dir -jar hub-central-community.jar`

And if you need to specify the DHF environment (it defaults to local):

`java -DdhfEnv=prod -DdhfDir=/full/path/to/your/datahub -DmodelsDir=/full/path/to/your/models/dir -jar hub-central-community.jar`
