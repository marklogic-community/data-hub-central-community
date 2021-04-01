package com.marklogic.r2m;

import java.util.concurrent.BlockingQueue;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import java.io.*;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.io.StringReader;


public class DocumentLoader implements Runnable {
	private final BlockingQueue<String> queue;
	private bulkLoaderDS loader;
	private DatabaseClient dbclient;
	private int batchSize;
	private int retryAttempts = 3000;
	private volatile boolean doneRunning = false;
	private int numRetrieved;
	private String exitFlag = "Done";
	private String basicAuthFlag = "basic";
	private String digestAuthFlag = "digest";
	private String mlInsertConfigJson;
	ArrayList<String> docList = new ArrayList<String>();
	
    @Override
    public void run() {
		int totalLoaded = 0;
        try {
            do {
            	numRetrieved = queue.drainTo(docList, batchSize);
            	if(numRetrieved > 0) {
            		checkDocListForExitFlag(docList);
            		if(docList.size() > 0) {
	            		try {
	            			load(dbclient, docList);
							totalLoaded += numRetrieved;
	            			docList.clear();
	            		} catch(IOException e) {
							// This is an issue with some streams not being viewed and "closed" properly.  
							// These errors are harmless and can be ignored
						} catch (Exception e) {
	            			e.printStackTrace();
	            		}
            		}
            	} else {
            		Thread.sleep(10);
            	}
            } while (!doneRunning);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
		dbclient.release();
    }
	
	public void stopRunning() {
		doneRunning = true;
	}
    
    private void checkDocListForExitFlag(ArrayList<String> docList) throws InterruptedException {
    	for (int i = 0; i < docList.size(); i++) {
    		if (exitFlag.equals(docList.get(i))) {
    			doneRunning = true;
    			queue.put(docList.get(i));
    			docList.remove(i);
    			return;
    		}
    	}
    }
    
    public void load(DatabaseClient dbclient, ArrayList<String> docList) throws Exception {
    	Reader workUnit = new StringReader(mlInsertConfigJson);
		Stream<Reader> input = docList.stream().map(doc -> new StringReader(doc));
		Reader output = loader.bulkLoadDocs(null, null, workUnit, input);
	}

    public DocumentLoader(BlockingQueue<String> queue, String mlInsertConfigJson, String host, MarkLogicConfiguration conf) {
    	this.queue = queue;
    	this.mlInsertConfigJson = mlInsertConfigJson;
    	this.batchSize = conf.getBatchSize();

        if(basicAuthFlag.equals(conf.getAuthContext())){
			dbclient = DatabaseClientFactory.newClient(
				host,
				conf.getPort(),
				new DatabaseClientFactory.BasicAuthContext(conf.getUsername(), conf.getPassword())
			);
        } else {
        	dbclient = DatabaseClientFactory.newClient(
    				host,
    				conf.getPort(),
    				new DatabaseClientFactory.DigestAuthContext(conf.getUsername(), conf.getPassword())
    			);
        }
        loader = bulkLoaderDS.on(dbclient);
    }
}

