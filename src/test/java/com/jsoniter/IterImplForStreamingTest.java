package com.jsoniter;

import com.jsoniter.spi.JsonException;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;

public class IterImplForStreamingTest extends TestCase {

	public void testReadMaxDouble() throws Exception {
		String maxDouble = "1.7976931348623157e+308";
		JsonIterator iter = JsonIterator.parse("1.7976931348623157e+308");
		String number = IterImplForStreaming.readNumber(iter);
		assertEquals(maxDouble, number);
	}

	public void testReadBackspace() throws Exception {




		byte[] bytes = {(byte) '\\', (byte) 'u', (byte) 'D', (byte) '8', (byte) '0', (byte) '0',
				(byte) '\\', (byte) 'u', (byte) 'D', (byte) '8', (byte) '0', (byte) '0',};
		JsonIterator iter = JsonIterator.parse(new ByteArrayInputStream(bytes), 12);
		try {
			IterImplForStreaming.readStringSlowPath(iter,0);
			fail();
		} catch (JsonException e) {
			assertEquals("invalid surrogate", e.getMessage());
		}
	}
}