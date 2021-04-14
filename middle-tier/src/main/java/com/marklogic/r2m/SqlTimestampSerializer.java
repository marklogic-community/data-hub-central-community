package com.marklogic.r2m;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class SqlTimestampSerializer extends StdSerializer<Timestamp> {

	private DateTimeFormatter formatter;

	public SqlTimestampSerializer() {
		super(Timestamp.class);
		this.formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	}

	@Override
	public void serialize(Timestamp value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(formatter.format(value.toLocalDateTime()));
	}
}
