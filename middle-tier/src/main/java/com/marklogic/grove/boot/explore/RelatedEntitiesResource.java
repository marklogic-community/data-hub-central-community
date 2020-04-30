package com.marklogic.grove.boot.explore;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.extensions.ResourceServices;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;

import java.io.IOException;

public class RelatedEntitiesResource extends ResourceManager {

    private static final String NAME = "related-entities";

    public RelatedEntitiesResource(DatabaseClient client) {
        super();
        client.init(NAME, this);
    }

    public String getRelatedEntities(InputStreamHandle body) {
        RequestParameters params = new RequestParameters();
        try {
            ResourceServices.ServiceResultIterator resultItr = this.getServices().post(params, body);
            if (resultItr == null || !resultItr.hasNext()) {
                throw new IOException("Unable to get related entities");
            }
            ResourceServices.ServiceResult res = resultItr.next();
            return res.getContent(new StringHandle()).get();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return "";

    }
}

