package com.marklogic.grove.boot.crud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.*;
import com.marklogic.client.io.*;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.error.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/crud")
public class CrudController extends AbstractController {

    @RequestMapping(method = RequestMethod.GET)
    void getDoc(@RequestParam String uri, @RequestParam(defaultValue = "final") String database, HttpServletResponse response) throws IOException {
		DatabaseClient client;
		if (database.equals("staging")) {
			client = getHubClient().getStagingClient();
		}
		else {
			client = getHubClient().getFinalClient();
		}
		DocumentPage page = client.newBinaryDocumentManager().read(new ServerTransform("prettifyXML"), uri);

        if (!page.hasNext()) {
            throw new NotFoundException();
        }
        DocumentRecord documentRecord = page.next();
		String mime = documentRecord.getMimetype();
        response.setContentType(mime);
		byte[] bytes = documentRecord.getContent(new BytesHandle()).get();
		response.getOutputStream().write(bytes);
    }

    @RequestMapping(value = "/metadata", method = RequestMethod.GET)
    void getDocMetadata(@RequestParam String uri, @RequestParam(defaultValue = "final") String database, HttpServletResponse response) throws IOException {
        DatabaseClient client;
        if (database.equals("staging")) {
        	client = getHubClient().getStagingClient();
		}
        else {
        	client = getHubClient().getFinalClient();
		}
        GenericDocumentManager documentManager = client.newDocumentManager();
        documentManager.setNonDocumentFormat(Format.JSON);
        DocumentPage metaPage = documentManager.readMetadata(uri);
        DocumentPage contentPage = documentManager.read(uri);

        if (!metaPage.hasNext() || !contentPage.hasNext()) {
            throw new NotFoundException();
        }

        DocumentRecord metaRecord = metaPage.next();
        DocumentRecord contentRecord = contentPage.next();

		String mimeType = contentRecord.getMimetype();

        ObjectNode node = (ObjectNode)metaRecord.getMetadata(new JacksonHandle()).get();
        node.put("contentType", mimeType);


        metaPage = documentManager.read(uri);
        if (!metaPage.hasNext()) {
            throw new NotFoundException();
        }
        metaRecord = metaPage.next();

        String[] splits = uri.split("/");
        String fileName = splits[splits.length - 1];
        node.put("fileName", fileName);
        node.put("format", metaRecord.getFormat().name());
        node.put("size",  metaRecord.getContent(new StringHandle()).get().length());
        node.put("uri", uri);
        ObjectMapper om = new ObjectMapper();
        response.setContentType(mimeType);
        response.getWriter().write(om.writeValueAsString(node));
    }

    @RequestMapping(value = "/{type}/{uri}", method = RequestMethod.PUT)
    void updateDoc(@PathVariable String type, @PathVariable String uri, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getHubClient().getFinalClient();

        DocumentMetadataHandle meta = new DocumentMetadataHandle();
        InputStreamHandle content = new InputStreamHandle(request.getInputStream());

        client.newDocumentManager().write(uri, meta, content);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    void createDoc(@PathVariable String type, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getHubClient().getFinalClient();
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
