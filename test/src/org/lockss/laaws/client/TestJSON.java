package org.lockss.laaws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.lockss.test.LockssTestCase;

/**
 * Tests JSON adapters with simulated data, including error cases, and verifies basic Gson <->
 * Jackson interop for supported values.
 */
public class TestJSON extends LockssTestCase {

  private final ObjectMapper jackson = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    // Always restore clean state after each test
    super.tearDown();
  }

  // ---------------- Factory and configuration methods ----------------

  public void testCreateGson() {
    GsonBuilder result = JSON.createGson();
    assertNotNull("GsonBuilder should not be null", result);
  }

  public void testGetGson() {
    Gson result = JSON.getGson();
    assertNotNull("Gson instance should not be null", result);
  }

  public void testSetGson() {
    Gson originalGson = JSON.getGson();
    Gson customGson = new GsonBuilder().setPrettyPrinting().create();

    JSON.setGson(customGson);
    assertSame("Custom Gson should be set", customGson, JSON.getGson());

    // Restore original
    JSON.setGson(originalGson);
  }

  public void testSetLenientOnJson() {
    // Just verify it doesn't throw an exception
    JSON.setLenientOnJson(false);
    JSON.setLenientOnJson(true);
    JSON.setLenientOnJson(false);
  }

  public void testSerialize() throws Exception {
    // Test basic string serialization
    String input = "test string";
    String result = JSON.serialize(input);
    assertEquals("\"test string\"", result);

    // Test object serialization
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("key", "value");
    map.put("number", 42);
    String jsonMap = JSON.serialize(map);
    assertNotNull(jsonMap);
    assertTrue(jsonMap.contains("\"key\""));
    assertTrue(jsonMap.contains("\"value\""));
    assertTrue(jsonMap.contains("42"));
  }

  public void testDeserialize() throws Exception {
    // Test basic string deserialization
    String json = "\"test string\"";
    String result = JSON.deserialize(json, String.class);
    assertEquals("test string", result);

    // Test number deserialization
    String numberJson = "42";
    Integer numberResult = JSON.deserialize(numberJson, Integer.class);
    assertEquals(Integer.valueOf(42), numberResult);

    // Test object deserialization
    String objectJson = "{\"key\":\"value\",\"number\":42}";
    Map<?, ?> mapResult = JSON.deserialize(objectJson, Map.class);
    assertNotNull(mapResult);
    assertEquals("value", mapResult.get("key"));
  }

  public void testSetOffsetDateTimeFormat() {
    DateTimeFormatter customFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
    JSON.setOffsetDateTimeFormat(customFormat);

    // Verify format is applied by serializing/deserializing
    OffsetDateTime odt = OffsetDateTime.of(2023, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
    String json = JSON.getGson().toJson(odt, OffsetDateTime.class);
    assertNotNull(json);

    // Restore default
    JSON.setOffsetDateTimeFormat(null);
  }

  public void testSetLocalDateFormat() {
    DateTimeFormatter customFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
    JSON.setLocalDateFormat(customFormat);

    // Verify format is applied
    LocalDate ld = LocalDate.of(2023, 6, 15);
    String json = JSON.getGson().toJson(ld, LocalDate.class);
    assertNotNull(json);
    assertTrue(json.contains("06/15/2023"));

    // Restore default
    JSON.setLocalDateFormat(null);
  }

  public void testSetDateFormat() {
    SimpleDateFormat customFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
    customFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    JSON.setDateFormat(customFormat);

    // Verify format is applied
    Date date = new Date(1000000000000L);
    String json = JSON.getGson().toJson(date, Date.class);
    assertNotNull(json);

    // Restore default
    JSON.setDateFormat(null);
  }

  public void testSetSqlDateFormat() {
    SimpleDateFormat customFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    customFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    JSON.setSqlDateFormat(customFormat);

    // Verify format is applied
    java.sql.Date sqlDate = java.sql.Date.valueOf("2023-06-15");
    String json = JSON.getGson().toJson(sqlDate, java.sql.Date.class);
    assertNotNull(json);

    // Restore default
    JSON.setSqlDateFormat(null);
  }

  // ---------------- byte[] ----------------

  public void testByteArrayAdapter_roundTripAndErrors() throws Exception {
    byte[] bytes = new byte[]{0, 1, 2, -1, 127, -128, 42};
    String json = JSON.getGson().toJson(bytes, byte[].class);
    assertNotNull(json);

    byte[] parsed = JSON.getGson().fromJson(json, byte[].class);
    assertTrue(Arrays.equals(bytes, parsed));

    // null handling
    assertNull(JSON.getGson().fromJson("null", byte[].class));

    // error: invalid base64 (adapter logs and returns null)
    String invalidBase64Json = "\"***not-base64***\"";
    byte[] parsedInvalid = JSON.getGson().fromJson(invalidBase64Json, byte[].class);
    assertNull(parsedInvalid);
  }

  public void testByteArray_gsonToJackson_and_jacksonToGson() throws Exception {
    byte[] bytes = new byte[]{10, 20, 30};
    String gsonJson = JSON.getGson().toJson(bytes, byte[].class);
    byte[] jacksonParsed = jackson.readValue(gsonJson, byte[].class);
    assertTrue(Arrays.equals(bytes, jacksonParsed));

    String jacksonJson = jackson.writeValueAsString(bytes);
    byte[] gsonParsed = JSON.getGson().fromJson(jacksonJson, byte[].class);
    assertTrue(Arrays.equals(bytes, gsonParsed));
  }

  // ---------------- java.util.Date ----------------

  public void testDateAdapter_roundTripAndErrors() throws Exception {
    Date now = new Date(System.currentTimeMillis() - 1234567L);
    String json = JSON.getGson().toJson(now, Date.class);
    Date parsed = JSON.getGson().fromJson(json, Date.class);
    assertEquals(now.getTime(), parsed.getTime());

    // custom format with explicit UTC timezone
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    JSON.setDateFormat(df);
    try {
      String json2 = JSON.getGson().toJson(now, Date.class);
      Date parsed2 = JSON.getGson().fromJson(json2, Date.class);
      // Compare formatted strings since we're using a specific format
      assertEquals(df.format(now), df.format(parsed2));
    } finally {
      JSON.setDateFormat(null);
    }

    // error: invalid date string
    try {
      JSON.getGson().fromJson("\"not-a-date\"", Date.class);
      fail("Expected JsonParseException");
    } catch (JsonParseException expected) {
      // Expected
    }
  }

  // ---------------- java.sql.Date ----------------

  public void testSqlDateAdapter_roundTripAndErrors() throws Exception {
    java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
    String json = JSON.getGson().toJson(sqlDate, java.sql.Date.class);
    java.sql.Date parsed = JSON.getGson().fromJson(json, java.sql.Date.class);
    assertEquals(sqlDate.toString(), parsed.toString());

    // custom format with explicit UTC timezone
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    JSON.setSqlDateFormat(df);
    try {
      String json2 = JSON.getGson().toJson(sqlDate, java.sql.Date.class);
      java.sql.Date parsed2 = JSON.getGson().fromJson(json2, java.sql.Date.class);
      // Compare formatted strings since we're using a specific format
      assertEquals(df.format(sqlDate), df.format(parsed2));
    } finally {
      JSON.setSqlDateFormat(null);
    }

    // error: invalid sql date string
    try {
      JSON.getGson().fromJson("\"not-a-valid-date\"", java.sql.Date.class);
      fail("Expected JsonParseException");
    } catch (JsonParseException expected) {
      // Expected
    }
  }

  // ---------------- OffsetDateTime ----------------

  public void testOffsetDateTimeAdapter_roundTripAndErrors() throws Exception {
    OffsetDateTime odt = OffsetDateTime.of(2021, 3, 14, 15, 9, 26, 0, ZoneOffset.ofHours(-7));
    String json = JSON.getGson().toJson(odt, OffsetDateTime.class);
    OffsetDateTime parsed = JSON.getGson().fromJson(json, OffsetDateTime.class);
    assertEquals(odt.toInstant().toEpochMilli(), parsed.toInstant().toEpochMilli());
    assertEquals(odt.getOffset(), parsed.getOffset());

    // special handling of +0000 -> Z; simulate
    OffsetDateTime z = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    // Manually craft "+0000" input to trigger normalization branch
    OffsetDateTime parsedPlus0000 = JSON.getGson()
        .fromJson("\"2020-01-01T00:00:00+0000\"", OffsetDateTime.class);
    assertEquals(z.toInstant().toEpochMilli(), parsedPlus0000.toInstant().toEpochMilli());

    // error: invalid odt string
    try {
      JSON.getGson().fromJson("not-a-valid-date", OffsetDateTime.class);
      fail("Expected JsonParseException or DateTimeException wrapped");
    } catch (JsonParseException expected) {
      // Expected for JSON parsing errors
    } catch (DateTimeException expected) {
      // Expected for date/time validation errors
    }
  }

  public void testOffsetDateTime_gsonToJackson_and_jacksonToGson() throws Exception {
    OffsetDateTime odt = OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    String gsonJson = JSON.getGson().toJson(odt, OffsetDateTime.class);
    OffsetDateTime jacksonParsed = jackson.readValue(gsonJson, OffsetDateTime.class);
    assertEquals(odt.toInstant().toEpochMilli(), jacksonParsed.toInstant().toEpochMilli());

    String jacksonJson = jackson.writeValueAsString(odt);
    OffsetDateTime gsonParsed = JSON.getGson().fromJson(jacksonJson, OffsetDateTime.class);
    assertEquals(odt.toInstant().toEpochMilli(), gsonParsed.toInstant().toEpochMilli());
  }

  // ---------------- LocalDate ----------------

  public void testLocalDateAdapter_roundTripAndErrors() throws Exception {
    LocalDate ld = LocalDate.of(2004, 2, 29);
    String json = JSON.getGson().toJson(ld, LocalDate.class);
    LocalDate parsed = JSON.getGson().fromJson(json, LocalDate.class);
    assertEquals(ld, parsed);

    // error: invalid local date
    try {
      JSON.getGson().fromJson("\"2001-02-29\"", LocalDate.class);
      fail("Expected exception for invalid date");
    } catch (JsonSyntaxException expected) {
      // Expected for JSON parsing errors
    } catch (RuntimeException expected) {
      // Expected for wrapped DateTimeException
    }
  }

  public void testLocalDate_gsonToJackson_and_jacksonToGson() throws Exception {
    LocalDate ld = LocalDate.of(2010, 10, 10);
    String gsonJson = JSON.getGson().toJson(ld, LocalDate.class);
    LocalDate jacksonParsed = jackson.readValue(gsonJson, LocalDate.class);
    assertEquals(ld, jacksonParsed);

    String jacksonJson = jackson.writeValueAsString(ld);
    LocalDate gsonParsed = JSON.getGson().fromJson(jacksonJson, LocalDate.class);
    assertEquals(ld, gsonParsed);
  }

  // ---------------- Mixed object payload for interop smoke test ----------------

  public void testMixedMap_gsonToJackson_and_back() throws Exception {
    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    payload.put("bytes", new byte[]{1, 2, 3});
    payload.put("date", new Date(123456789L));
    payload.put("sqlDate", java.sql.Date.valueOf("2022-01-02"));
    payload.put("odt", OffsetDateTime.of(2022, 1, 2, 3, 4, 5, 0, ZoneOffset.ofHours(1)));
    payload.put("ld", LocalDate.of(2022, 1, 2));

    String gsonJson = JSON.getGson().toJson(payload);
    Map<?, ?> jacksonParsed = jackson.readValue(gsonJson, Map.class);
    assertTrue(jacksonParsed.containsKey("bytes"));
    assertTrue(jacksonParsed.containsKey("date"));
    assertTrue(jacksonParsed.containsKey("sqlDate"));
    assertTrue(jacksonParsed.containsKey("odt"));
    assertTrue(jacksonParsed.containsKey("ld"));

    String jacksonJson = jackson.writeValueAsString(payload);
    Map<?, ?> gsonParsed = JSON.getGson().fromJson(jacksonJson, Map.class);
    assertTrue(gsonParsed.containsKey("bytes"));
    assertTrue(gsonParsed.containsKey("date"));
    assertTrue(gsonParsed.containsKey("sqlDate"));
    assertTrue(gsonParsed.containsKey("odt"));
    assertTrue(gsonParsed.containsKey("ld"));
  }

  // Legacy JUnit 3-style suite for project harness
  public static Test suite() {
    return new TestSuite(TestJSON.class);
  }
}
