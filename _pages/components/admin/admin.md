---
layout: inner
title: Administration
lead_text: ''
permalink: /components/admin/
---

# Deploy to DHS

Envision is platform neutral and can be used with on-prem and cloud deployed Data Hubs as well as with the MarkLogic Data Hub Service (DHS).

Envision (1.0.5 and greater) requires **[Data Hub Framework 5.2.3 (DHF)](https://developer.marklogic.com/products/data-hub/)**  to be used with the current **[MarkLogic Data Hub Service on Azure (DHS)](https://docs.marklogic.com/cloudservices/azure/getting-started-azure.html)**.

To use Envision with DHS, start by downloading the gradle config file for your DHS instance. After downloading, remember to update the username and password with your DHS credentials. <br><br>

![DHS](/envision/images/DHS-1.png)


Save this file in your data hub root directory as         "gradle-dhs.properties".<br>

Use gradle deploy your data hub to DHS.<br>
```gradlew hubDeploy -PenvironmentName=dhs -i  ```   

Stop Envision, and restart it using the new properties file so that it points to your DHS instance.<br>
```java -DdhfEnv=dhs -jar envision-1.0.5.jar ```

Login to Envision and revisit the Admin pane to confirm your using Envision with your Data Hub Instance.<br>

You can load your data and run your flows using gradle of course, and then use Envision Explore to explore your data and to curate entities that require mastering.  You can also load data and run your flows from Envision. <br>

To run your flows in Envision, navigate to the Admin pane and click the "Run Flows" button to run your flows.  Data from any ingest steps will be loaded from the location the ingest step points to where Envision is currently running and transfer that data to the DHS Data Hub for processing.  Any flows defined will all be run then as well.<br>

Using either method above you can then explore and master your data in DHS using Envision.
