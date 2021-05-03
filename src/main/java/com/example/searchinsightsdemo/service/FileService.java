package com.example.searchinsightsdemo.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.searchinsightsdemo.config.ApplicationProperties;

@Service
public class FileService {

	private final Path uploadLocation;

	public FileService(ApplicationProperties properties) {
		this.uploadLocation = Paths.get(properties.getStorageConfiguration().getUploadDir());
	}

	public Path store(MultipartFile file) {
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			if (filename.contains("..")) {
				// This is a security check, should practically not happen as
				// cleanPath is handling that ...
				throw new StorageException("Cannot store file with relative path outside current directory " + filename);
			}
			try (InputStream inputStream = file.getInputStream()) {
				Path filePath = this.uploadLocation.resolve(filename);
				Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
				return filePath;
			}
		}
		catch (IOException e) {
			throw new StorageException("Failed to store file " + filename, e);
		}
	}

	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageFileNotFoundException("Could not read file: " + filename);

			}
		}
		catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	public Path load(String filename) {
		return uploadLocation.resolve(filename);
	}

	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.uploadLocation, 1)
					.filter(path -> !path.equals(this.uploadLocation))
					.map(this.uploadLocation::relativize);
		}
		catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}

	}

	public void init() {
		try {
			Files.createDirectories(uploadLocation);
		}
		catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
}
