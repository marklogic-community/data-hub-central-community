package com.marklogic.grove.boot.crud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.*;
import com.marklogic.client.io.*;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.error.NotFoundException;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLDecoder;

@RestController
@RequestMapping("/api/crud")
public class CrudController extends AbstractController {

    @Autowired
	CrudController(HubConfigImpl hubConfig) {
		super(hubConfig);
	}

    @RequestMapping(method = RequestMethod.GET)
    void getDoc(@RequestParam String uri, @RequestParam String database, HttpServletResponse response) throws IOException {
		DatabaseClient client;
		if (database.equals("staging")) {
			client = getStagingClient();
		}
		else {
			client = getFinalClient();
		}
        DocumentPage page = client.newDocumentManager().read(new ServerTransform("prettifyXML"), URLDecoder.decode(uri, "UTF-8"));

        if (!page.hasNext()) {
            throw new NotFoundException();
        }
        DocumentRecord documentRecord = page.next();
        String mime = documentRecord.getFormat().getDefaultMimetype();
        response.setContentType(mime);
        response.getWriter().write(documentRecord.getContent(new StringHandle()).get());
    }

    @RequestMapping(value = "/metadata", method = RequestMethod.GET)
    void getDocMetadata(@RequestParam String uri, @RequestParam String database, HttpServletResponse response) throws IOException {
        DatabaseClient client;
        if (database.equals("staging")) {
        	client = getStagingClient();
		}
        else {
        	client = getFinalClient();
		}
        GenericDocumentManager documentManager = client.newDocumentManager();
        documentManager.setNonDocumentFormat(Format.JSON);
        String docUri = URLDecoder.decode(uri, "UTF-8");
        DocumentPage metaPage = documentManager.readMetadata(docUri);
        DocumentPage contentPage = documentManager.read(docUri);

        if (!metaPage.hasNext() || !contentPage.hasNext()) {
            throw new NotFoundException();
        }

        DocumentRecord metaRecord = metaPage.next();
        DocumentRecord contentRecord = contentPage.next();

		String mimeType = contentRecord.getFormat().getDefaultMimetype();

        ObjectNode node = (ObjectNode)metaRecord.getMetadata(new JacksonHandle()).get();
        node.put("contentType", mimeType);


        metaPage = documentManager.read(docUri);
        if (!metaPage.hasNext()) {
            throw new NotFoundException();
        }
        metaRecord = metaPage.next();

        String[] splits = docUri.split("/");
        String fileName = splits[splits.length - 1];
        node.put("fileName", fileName);
        node.put("format", metaRecord.getFormat().name());
        node.put("size",  metaRecord.getContent(new StringHandle()).get().length());
        node.put("uri", docUri);
        ObjectMapper om = new ObjectMapper();
        response.setContentType(mimeType);
        response.getWriter().write(om.writeValueAsString(node));
    }

    @RequestMapping(value = "/{type}/{uri}", method = RequestMethod.PUT)
    void updateDoc(@PathVariable String type, @PathVariable String uri, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getFinalClient();

        DocumentMetadataHandle meta = new DocumentMetadataHandle();
        InputStreamHandle content = new InputStreamHandle(request.getInputStream());

        client.newDocumentManager().write(uri, meta, content);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    void createDoc(@PathVariable String type, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getFinalClient();
        GenericDocumentManager documentManager = client.newDocumentManager();
        DocumentUriTemplate uriTemplate;
        Format format = Format.getFromMimetype(request.getContentType());
        if (format.equals(Format.JSON)) {
            uriTemplate = documentManager.newDocumentUriTemplate("json");
        }
        else if (format.equals(Format.XML)) {
            uriTemplate = documentManager.newDocumentUriTemplate("xml");
        }
        else {
            uriTemplate = documentManager.newDocumentUriTemplate("");
        }
        DocumentMetadataHandle meta = new DocumentMetadataHandle();
        InputStreamHandle content = new InputStreamHandle(request.getInputStream());

        DocumentDescriptor documentDescriptor = documentManager.create(uriTemplate, meta, content);

        response.setStatus(HttpStatus.CREATED.value());
        response.addHeader("location", documentDescriptor.getUri());
    }
}
