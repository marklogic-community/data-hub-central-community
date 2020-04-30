# Envision

## Just want to run it?
Go grab it as a docker container!

- _vpn required_
- _instructions are on the page_

https://mlregistry.marklogic.com/tag/marklogic/topgun/latest/  


## Want to write some code or tests?
If you are here to write code then keep reading. First thing to do is clone the repo. Then create a new branch to do your work.

### Before You Start
You will need access to a Data Hub project somewhere on your disk. You'll also need a running MarkLogic with the data hub deployed to it.

### UI Prereqs
For the UI you need:
 - node 12.x or later
 - npm 6.9.0 or later

### Envision Installation Instructions 

Clone this repo:
- git clone ssh://git@project.marklogic.com:7999/int/topgun.git

### Running the project

#### Development Mode

Open 2 terminal tabs/windows
##### Middle Tier
Open a terminal in the project root

The basic command to run the middle tier is:
`./gradlew bootrun`

But... you'll need to point the middle tier at your DHF instance

`./gradlew -DdhfDir=/full/path/to/your/datahub bootrun`

Also you might want to override where the Concept Connector models live

`./gradlew -DdhfDir=/full/path/to/your/datahub -DmodelsDir=/full/path/to/your/models/dir bootrun`


##### User Interface
Open a terminal in the project root

launch the ui in develop mode
`gradle runui`

#### Access the Envision UI
http://localhost:9999

#### Running End to End Tests
Open a terminal in the top level envision directory.

`cd ui`  
`npm run test:e2e`

This will launch the Cypress UI. http://cypress.io is a testing framework we are using to test the UI e2e. When the Cypress UI launches you can click the run specs button to run the tests. Cypress will autowatch your files. If you make any changes the tests will re-run.

#### Production Mode
Open a terminal window:  

##### Build the jar
`./gradlew clean build`  

##### Run the jar
`java -jar middle-tier/build/lib/envision.jar`

The jar is configured so that you can drop it into a DHF project dir and run it there.

`java -jar envision.jar`

However, if you need to point it at another folder where the DHF is installed, run like so:

`java -DdhfDir=/full/path/to/your/datahub -jar envision.jar`

If you also need to specify a different models directory:

`java -DdhfDir=/full/path/to/your/datahub -DmodelsDir=/full/path/to/your/models/dir -jar envision.jar`

And if you need to specify the DHF environment (it defaults to local):

`java -DdhfEnv=prod -DdhfDir=/full/path/to/your/datahub -DmodelsDir=/full/path/to/your/models/dir -jar envision.jar`
