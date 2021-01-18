---
layout: inner
title: Upload
lead_text: ''
permalink: /components/upload/
---

# Upload

Start by loading data into the MarkLogic Data Hub.  Upload currently supports csv files.

### Getting Started
Loading data is pretty self explanatory. Drag and drop a csv file onto the page or click "Choose a File" to choose the file from your file system.

Once loaded, the name of the file and how many rows have loaded will be displayed at the bottom of the page.  You can click the trashcan icon to delete these records and file from the Data Hub Staging database.

If you're using existing flows with existing integrate steps to ingest data, you don't need to upload the data here.  Once the ingest steps have run, filenames will be displayed here as well.

As of 2.0.4 you can now upload Semantic triples directly into the data hub using Upload.  You'll notice an advanced option when setting the collection name for your data.  By default, all data is loaded into the Staging database. However, using the advanced dropdown, you can choose to load your data and triples to the Final database directly.

<br><br> 
![Upload](/envision/images/upload-1.png)
<br><br>

