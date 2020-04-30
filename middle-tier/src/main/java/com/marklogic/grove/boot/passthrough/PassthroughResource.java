package com.marklogic.grove.boot.passthrough;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.extensions.ResourceServices;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;

public class PassthroughResource extends ResourceManager {

    public PassthroughResource(String name, DatabaseClient client) {
        super();
        client.init(name, this);
    }

    public String doGet(RequestParameters params) {
        ResourceServices.ServiceResultIterator resultItr;
        try {
            resultItr = this.getServices().get(params);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (resultItr == null || ! resultItr.hasNext()) {
            return null;
        }
        ResourceServices.ServiceResult res = resultItr.next();
        return res.getContent(new StringHandle()).get();

    }

    public String doPost(RequestParameters params, String body) {
        ResourceServices.ServiceResultIterator resultItr;
        try {
            resultItr =  this.getServices().post(params, new StringHandle(body).withFormat(Format.JSON));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (resultItr == null || ! resultItr.hasNext()) {
            return null;
        }
        ResourceServices.ServiceResult res = resultItr.next();
        return res.getContent(new StringHandle()).get();
    }

    public String doPut(RequestParameters params, String body) {
        try {
            return this.getServices().put(params, new StringHandle(body).withFormat(Format.JSON), new StringHandle()).get();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
