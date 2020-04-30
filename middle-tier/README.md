This is a Spring Boot-based implementation of the Grove API. The intent of this project is to support the following use 
cases:

1. For development, you want to run the UI as you normally would (e.g. npm start), but you want it to talk to a Spring 
Boot middle tier instead of a Node middle tier
1. For production, you want to deploy an executable Spring Boot jar that includes all of the static UI resources

The instructions below assume that you've already [created a Grove project](https://marklogic-community.github.io/grove/guides/getting-started/).

## Using this during development

During development, you'll run two separate processes - the Spring Boot middle tier (which defaults to Tomcat), and 
the UI. Node is still used for building and running the UI. 

You can run the Spring Boot middle tier via the following Gradle task (run this from the same directory that this README
file is in):

    gradle bootRun

Alternatively, you can also run the com.marklogic.grove.boot.Application class in your IDE. 

You'll see in the logging that Tomcat is listening on port 9003. This is configured via the server.port file in 
src/main/resources/application.properties. The Grove UI defaults to connecting to this port, which is defined in the 
ui/package.json file via the "proxy" property. You're free to change this to any port that you wish.

Next, instead of running "npm start" from the root Grove project directory (which starts both the UI and the Node 
middle tier), you should run "npm start" from the "ui" project directory:

    cd ../ui
    npm start
 
This will only run the UI. If you were to run the Node middle tier as well, you'd get an error because Tomcat is 
already listening on port 9003.

## Using this for production 

For a production environment, it's typical to package up a Spring Boot app as an executable jar. Thus, we won't be 
using Node to run the UI - we'll need the UI to be hosted from within the Spring Boot app. 

To build the Spring Boot executable jar, with the UI files included in it, just run:

    gradle clean build

This will produce an executable jar in the "build/libs" directory. Note that you can change the name and version in the 
jar filename by modifying the "bootJar" block in build.gradle.

The jar can then be run from the location where it was created:

    java -jar build/libs/grove-spring-boot.jar
    
Or copy the jar to any other location to run it. More information on this topic can be found in the 
[Spring Boot docs for installing applications](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html).

## Configuring the application

The Spring Boot application - including connection information to MarkLogic - can be configured in 
src/main/resources/application.properties. The [Spring Boot docs](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html) 
contain a full list of all the properties that can be configured.

## More information

- Spring Boot docs are at https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/ 
- Grove docs are at https://marklogic-community.github.io/grove/ 
