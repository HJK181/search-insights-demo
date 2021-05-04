package com.example.searchinsightsdemo.parquet;

import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.ParquetEncodingException;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

public class CsvWriteSupport extends WriteSupport<List<String>> {

	MessageType				schema;
	RecordConsumer			recordConsumer;
	List<ColumnDescriptor>	cols;

	// TODO: support specifying encodings and compression
	public CsvWriteSupport(MessageType schema) {
		this.schema = schema;
		this.cols = schema.getColumns();
	}

	@Override
	public WriteContext init(Configuration config) {
		return new WriteContext(schema, new HashMap<String, String>());
	}

	@Override
	public void prepareForWrite(RecordConsumer r) {
		recordConsumer = r;
	}

	@Override
	public void write(List<String> values) {
		if (values.size() != cols.size()) {
			throw new ParquetEncodingException("Invalid input data. Expecting " +
					cols.size() + " columns. Input had " + values.size() + " columns (" + cols + ") : " + values);
		}

		recordConsumer.startMessage();
		for (int i = 0; i < cols.size(); ++i) {
			String val = values.get(i);
			if (val.length() > 0) {
				recordConsumer.startField(cols.get(i).getPath()[0], i);
				switch (cols.get(i).getType()) {
					case INT64:
						recordConsumer.addLong(Long.parseLong(val));
						break;
					case BINARY:
						recordConsumer.addBinary(stringToBinary(val));
						break;
					default:
						throw new ParquetEncodingException(
								"Unsupported column type: " + cols.get(i).getType());
				}
				recordConsumer.endField(cols.get(i).getPath()[0], i);
			}
		}
		recordConsumer.endMessage();
	}

	private Binary stringToBinary(Object value) {
		return Binary.fromString(value.toString());
	}
}
