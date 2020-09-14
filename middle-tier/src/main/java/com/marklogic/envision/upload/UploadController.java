package com.marklogic.envision.upload;

import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class UploadController extends AbstractController {

	final private UploadService uploadService;

	@Autowired
	UploadController(UploadService uploadService) {
		this.uploadService = uploadService;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> uploadFile(@RequestParam("collection") String collection, @RequestParam("file") MultipartFile file) throws IOException {
		uploadService.asyncUploadFile(getHubClient(), file.getInputStream(), collection);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
