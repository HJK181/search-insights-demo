package com.example.searchinsightsdemo.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.searchinsightsdemo.config.ApplicationProperties;
import com.example.searchinsightsdemo.parquet.CsvParquetWriter;

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

	public Path csvToParquet(String filename) {
		Resource csvResource = loadAsResource(filename);
		String outputName = getFilenameWithDiffExt(csvResource, ".parquet");
		String rawSchema = getSchema(csvResource);
		Path outputParquetFile = uploadLocation.resolve(outputName);
		if (Files.exists(outputParquetFile)) {
			throw new StorageException("Output file " + outputName + " already exists");
		}

		org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(outputParquetFile.toUri());
		MessageType schema = MessageTypeParser.parseMessageType(rawSchema);
		try (
				CSVParser csvParser = CSVFormat.DEFAULT
						.withFirstRecordAsHeader()
						.parse(new InputStreamReader(csvResource.getInputStream()));
				CsvParquetWriter writer = new CsvParquetWriter(path, schema, false);
		) {
			for (CSVRecord record : csvParser) {
				List<String> values = new ArrayList<String>();
				Iterator<String> iterator = record.iterator();
				while (iterator.hasNext()) {
					values.add(iterator.next());
				}
				writer.write(values);
			}
		}
		catch (IOException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename);
		}

		return outputParquetFile;
	}

	private String getFilenameWithDiffExt(Resource csvResource, String ext) {
		String outputName = csvResource.getFilename()
				.substring(0, csvResource.getFilename().length() - ".csv".length()) + ext;
		return outputName;
	}

	private String getSchema(Resource csvResource) {
		try {
			String fileName = getFilenameWithDiffExt(csvResource, ".schema");
			File csvFile = csvResource.getFile();
			File schemaFile = new File(csvFile.getParentFile(), fileName);
			return Files.readString(schemaFile.toPath());
		}
		catch (IOException e) {
			throw new StorageFileNotFoundException("Schema file does not exist for the given csv file, did you forget to upload it?", e);
		}
	}
}
