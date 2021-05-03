package com.example.searchinsightsdemo.rest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.example.searchinsightsdemo.service.FileService;
import com.example.searchinsightsdemo.service.StorageFileNotFoundException;

@RestController
@RequestMapping("/csv")
public class FileController {

	private final FileService fileService;

	public FileController(FileService fileService) {
		this.fileService = fileService;
	}

	@PostMapping("/upload")
	public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
		Path path = fileService.store(file);
		return ResponseEntity.ok(MvcUriComponentsBuilder.fromMethodName(FileController.class, "serveFile", path.getFileName().toString()).build().toString());
	}

	@GetMapping("/uploads")
	public ResponseEntity<List<String>> listUploadedFiles() throws IOException {
		return ResponseEntity
				.ok(fileService.loadAll()
						.map(path -> MvcUriComponentsBuilder.fromMethodName(FileController.class, "serveFile", path.getFileName().toString()).build().toString())
						.collect(Collectors.toList()));
	}

	@GetMapping("/uploads/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
		Resource file = fileService.loadAsResource(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}
}
