package com.example.searchinsightsdemo.service;

import static com.example.searchinsightsdemo.db.Tables.ANALYTICS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.example.searchinsightsdemo.config.ApplicationProperties;
import com.example.searchinsightsdemo.parquet.CsvParquetWriter;

@Service
public class FileService {

	private static final String QUERY_REPAIR_TABLE = "MSCK REPAIR TABLE " + ANALYTICS.getName();

	private final Path			uploadLocation;
	private final DSLContext	context;

	public FileService(ApplicationProperties properties, DSLContext context) {
		this.uploadLocation = Paths.get(properties.getStorageConfiguration().getUploadDir());
		this.context = context;
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

	public URL uploadToS3(String filename) {
		Resource parquetFile = loadAsResource(filename);
		if (!parquetFile.getFilename().endsWith(".parquet")) {
			throw new StorageException("You must upload parquet files to S3!");
		}
		try {
			AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
			File file = parquetFile.getFile();
			long lastModified = file.lastModified();
			LocalDate partitionDate = Instant.ofEpochMilli(lastModified)
					.atZone(ZoneId.systemDefault())
					.toLocalDate();
			String bucket = String.format("search-insights-demo/dt=%s", partitionDate.toString());
			s3.putObject(bucket, "analytics.parquet", file);

			// Athena does not scan for newly updated files, instead we need to
			// execute a repair table statement
			context.execute(QUERY_REPAIR_TABLE);
			return s3.getUrl(bucket, "analytics.parquet");
		}
		catch (SdkClientException | IOException | DataAccessException e) {
			throw new StorageException("Failed to upload file to s3", e);
		}
	}

	public List<URL> createRandomData(int numberOfDays) {
		List<String> queries = new ArrayList<>(List.of("dress", "shoes", "jeans", "dress red", "jacket", "shoes women", "t-shirt black", "tshirt", "shirt", "hoodie"));
		String rawSchema = getSchemaFromRootDir();
		MessageType schema = MessageTypeParser.parseMessageType(rawSchema);

		LocalDate now = LocalDate.now();
		Random random = new Random();
		AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
		List<URL> uploadUrls = new ArrayList<>(numberOfDays);

		for (int i = 0; i < numberOfDays; i++) {
			Collections.shuffle(queries);
			Path tempFile = createTempDir().resolve("analytics" + String.valueOf(i) + ".parquet");
			org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(tempFile.toUri());
			try (
					CsvParquetWriter writer = new CsvParquetWriter(path, schema, false);
			) {
				for (String query : queries) {
					Integer searches = random.nextInt(100);
					Double ctrBound = 0.3 * searches;
					Integer clicks = ctrBound.intValue() == 0 ? 0 : random.nextInt(ctrBound.intValue());
					Double transactionsBound = 0.1 * searches;
					Integer transactions = transactionsBound.intValue() == 0 ? 0 : random.nextInt(transactionsBound.intValue());
					List<String> values = List.of(query, searches.toString(), clicks.toString(), transactions.toString());
					writer.write(values);
				}
			}
			catch (IOException e) {
				throw new StorageFileNotFoundException("Could not create random data", e);
			}
			String bucket = String.format("search-insights-demo/dt=%s", now.minusDays(i).toString());
			s3.putObject(bucket, "analytics.parquet", tempFile.toFile());
			uploadUrls.add(s3.getUrl(bucket, "analytics.parquet"));
		}
		context.execute(QUERY_REPAIR_TABLE);
		return uploadUrls;
	}

	private String getSchemaFromRootDir() {
		try {
			return Files.readString(Paths.get(".").resolve("sample_data.schema"));
		}
		catch (IOException e) {
			throw new StorageFileNotFoundException("Schema file on root directory does not exists, did you delete it?", e);
		}
	}

	private Path createTempDir() {
		try {
			return Files.createTempDirectory("random_data");
		}
		catch (IOException e) {
			throw new StorageException("Failed to create temp file", e);
		}
	}
}
