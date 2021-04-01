package com.marklogic.r2m;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.sql.Date;
import java.time.format.DateTimeFormatter;

public class SqlDateSerializer extends StdSerializer<Date> {

	private DateTimeFormatter formatter;

	public SqlDateSerializer(String pattern) {
		super(Date.class);
		this.formatter = DateTimeFormatter.ofPattern(pattern);
	}

	@Override
	public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(formatter.format(value.toLocalDate()));
	}
}
