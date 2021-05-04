package com.example.searchinsightsdemo.parquet;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;

public class CsvParquetWriter extends ParquetWriter<List<String>> {

	public CsvParquetWriter(Path file, MessageType schema) throws IOException {
		this(file, schema, DEFAULT_IS_DICTIONARY_ENABLED);
	}

	public CsvParquetWriter(Path file, MessageType schema, boolean enableDictionary) throws IOException {
		this(file, schema, CompressionCodecName.SNAPPY, enableDictionary);
	}

	public CsvParquetWriter(Path file, MessageType schema, CompressionCodecName codecName, boolean enableDictionary) throws IOException {
		super(file, new CsvWriteSupport(schema), codecName, DEFAULT_BLOCK_SIZE, DEFAULT_PAGE_SIZE, enableDictionary, DEFAULT_IS_VALIDATING_ENABLED);
	}
}
