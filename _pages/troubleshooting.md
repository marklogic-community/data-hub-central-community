---
layout: inner
title: Troubleshooting
permalink: /troubleshooting/
---

# Troubleshooting

Tips for solving problems

### Unable to start Community Edition

So you go to start CE and it won't connect to your data hub.  Remember to have your  application.properties file defined.

### I changed my entities outside of Community Edition and Connect is not resynching. 

The first time you start CE, it will look in the Final DB of your hub and if there are any entities present, will create a default "My Hub Model" for you.  CE assumes it's in charge of all modeling edits so this read of Final and creation of "My Hub Model" only occurs the first time you start CE.  CE is intended to be an app, and not a developer tool.  However...

If you wish to update your model with changes you've made out of CE. Then...
1.  Delete your model.json from /conceptConnectorModels
2.  Restart CE with a force install of modules to provide the first time initialization of CE.  This will recreate "My Hub Model" for you.

`java -DforceInstall=true -jar hub-central-community.jar`

