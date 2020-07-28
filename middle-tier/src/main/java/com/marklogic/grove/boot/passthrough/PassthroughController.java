package com.marklogic.grove.boot.passthrough;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.io.Format;
import com.marklogic.client.util.RequestParameters;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/resources")
public class PassthroughController extends AbstractController {

	@Autowired
	PassthroughController(HubConfigImpl hubConfig) {
		super(hubConfig);
	}

    @RequestMapping(value = "/{resource}", method = {RequestMethod.GET, RequestMethod.POST})
    void getDoc(@PathVariable String resource, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getFinalClient();

        RequestParameters params = new RequestParameters();
        request.getParameterMap().forEach((name, values) -> {
            params.add(name.replace("rs:", ""), values);
        });
        PassthroughResource ptr = new PassthroughResource(resource, client);
        String method = request.getMethod();
        String result = null;
        if (method.equals("GET")) {
            result = ptr.doGet(params);
        }
        else if (method.equals("POST")) {
            result = ptr.doPost(params, request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
        }

        response.setContentType(Format.JSON.getDefaultMimetype());
        response.getWriter().write(result);
    }
}
