---
layout: inner
title: MarkLogic Data Hub on Docker
permalink: /docker/
---
# MarkLogic Data Hub on Docker

Docker Hub provides the image **[MarkLogic Data Hub HR360 Developer Edition](https://hub.docker.com/_/marklogic-datahub-hr360)** for free. 

This single image includes the following:
* Marklogic Server: 10.0-6.1
* Data Hub Central Community Edition: 5.4.2
* Data Hub Hub Central 5.4.3
* Pipes: 2.0.5
* A pre-installed HR360 Data Hub with Sample Data and Flows

These have all been preconfigured so you can just start the image and be up an running with an out of the box data hub example for a Human Resources (HR) 360 use case.

To access applications:

* MarkLogic Admin Ui: http://localhost:8001
* Data Hub Central Community Edition: http://localhost:9003
* Hub Central: http://localhost:9000
* Pipes: http://localhost:9005
* MarkLogic Query Console: http://localhost:8000

User: maverick/goose (update your credentials accordingly)

The HR360 example is the same one provided in **[this youtube demonstration](https://www.youtube.com/watch?v=uwOryM5fO6A&t=4s)**. 

You can use the ootb example to get familiar with the applications. You can also use the applications to upload your own data, create your own model, and create your own flows and steps to harmonize and integrate your data as well.

### Getting Started with Docker

1. Install Docker Desktop for Mac or Windows – https://www.docker.com/products/docker-desktop
2. The Docker service is accessible through the Windows Task Bar or the Mac Menu Bar.
3. Navigate to Docker > Preferences
4. In the Preferences pane, navigate to the Advanced section. Allocate at least 8GB of RAM to the project.
5. **[Download MarkLogic Data Hub HR360](https://hub.docker.com/_/marklogic-datahub-hr360)** Docker image from Docker Hub
6. Follow the run instructions provided on the Docker Hub page to start the image: 

docker run -d  --name hc -p 8000-8002:8000-8002 -p 8010-8011:8010-8011 -p 8013:8013 -p 9000-9005:9000-9005 store/marklogicdb/marklogic-hr360-datahub:5.4.2
7. Login to Community Edition using the URL and user above and Enjoy!

