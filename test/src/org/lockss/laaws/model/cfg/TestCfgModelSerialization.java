package org.lockss.laaws.model.cfg;

import org.lockss.test.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.lockss.laaws.client.JSON;
import java.util.*;
import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * Tests JSON serialization/deserialization consistency for all model objects
 * in org.lockss.laaws.model.cfg
 */
public class TestCfgModelSerialization extends LockssTestCase {

  private final ObjectMapper jackson = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  // Test serialization consistency between Gson and Jackson for all model types

  public void testApiStatus_SerializationConsistency() throws Exception {
    testSerializationConsistency(createApiStatus(), ApiStatus.class);
  }

  public void testAuConfiguration_SerializationConsistency() throws Exception {
    testSerializationConsistency(createAuConfiguration(), AuConfiguration.class);
  }

  public void testAuStatus_SerializationConsistency() throws Exception {
    testSerializationConsistency(createAuStatus(), AuStatus.class);
  }

  public void testAuWsResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createAuWsResult(), AuWsResult.class);
  }

  public void testCheckSubstanceResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createCheckSubstanceResult(), CheckSubstanceResult.class);
  }

  public void testContentConfigurationResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createContentConfigurationResult(), ContentConfigurationResult.class);
  }

  public void testPlatformConfigurationWsResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createPlatformConfigurationWsResult(), PlatformConfigurationWsResult.class);
  }

  public void testPluginWsResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createPluginWsResult(), PluginWsResult.class);
  }

  public void testRequestAuControlResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createRequestAuControlResult(), RequestAuControlResult.class);
  }

  public void testTdbAuWsResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createTdbAuWsResult(), TdbAuWsResult.class);
  }

  public void testTdbPublisherWsResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createTdbPublisherWsResult(), TdbPublisherWsResult.class);
  }

  public void testTdbTitleWsResult_SerializationConsistency() throws Exception {
    testSerializationConsistency(createTdbTitleWsResult(), TdbTitleWsResult.class);
  }

  // Generic test method for serialization consistency
  private <T> void testSerializationConsistency(T original, Class<T> clazz) throws Exception {
    // Test Gson -> Jackson
    String gsonJson = JSON.serialize(original);
    T jacksonParsed = jackson.readValue(gsonJson, clazz);
    assertObjectsEqual(original, jacksonParsed, clazz.getSimpleName() + " (Gson->Jackson)");

    // Test Jackson -> Gson
    String jacksonJson = jackson.writeValueAsString(original);
    T gsonParsed = JSON.deserialize(jacksonJson, clazz);
    assertObjectsEqual(original, gsonParsed, clazz.getSimpleName() + " (Jackson->Gson)");
  }

  // Generic equality assertion method
  private <T> void assertObjectsEqual(T expected, T actual, String context) {
    if (!expected.equals(actual)) {
      fail("Objects not equal in " + context +
           "\nExpected: " + expected +
           "\nActual: " + actual);
    }
  }


  // Factory methods for test objects

  private ApiStatus createApiStatus() {
    ApiStatus s = new ApiStatus();
    s.setApiVersion("2.0.0");
    s.setReady(true);
    s.setServiceName("config");
    s.setReason("OK");
    s.setStartupStatus(ApiStatus.StartupStatusEnum.PLUGINS_LOADED);
    return s;
  }

  private AuConfiguration createAuConfiguration() {
    AuConfiguration c = new AuConfiguration();
    c.setAuId("au:test");
    c.setAuConfig(createStringMap("key1", "val1", "key2", "val2"));
    return c;
  }

  private AuStatus createAuStatus() {
    AuStatus s = new AuStatus();
    s.setVolume("42");
    return s;
  }

  private AuWsResult createAuWsResult() {
    AuWsResult r = new AuWsResult();
    r.setAuId("au:test");
    return r;
    }

  private CheckSubstanceResult createCheckSubstanceResult() {
    CheckSubstanceResult r = new CheckSubstanceResult();
    r.setId("au:test");
    r.setOldState(CheckSubstanceResult.OldStateEnum.YES);
    r.setNewState(CheckSubstanceResult.NewStateEnum.NO);
    r.setErrorMessage("none");
    return r;
  }

  private ContentConfigurationResult createContentConfigurationResult() {
    ContentConfigurationResult r = new ContentConfigurationResult();
    r.setAuId("au:test");
    r.setName("Example AU");
    r.setIsSuccess(true);
    r.setMessage("created");
    return r;
  }

  private PlatformConfigurationWsResult createPlatformConfigurationWsResult() {
    PlatformConfigurationWsResult r = new PlatformConfigurationWsResult();
    r.setHostName("lockss.example.org");
    return r;
  }

  private PluginWsResult createPluginWsResult() {
    PluginWsResult r = new PluginWsResult();
    r.setPluginId("org.lockss.plugin.Example");
    r.setName("Example Plugin");
    r.setVersion("1.0.0");
    r.setType("content");
    r.setDefinition(createStringMap("base_url", "http://example.com/"));
    r.setRegistry("local");
    r.setUrl("http://example.com/plugin.jar");
    r.setAuCount(3);
    r.setPublishingPlatform("platformX");
    return r;
  }

  private RequestAuControlResult createRequestAuControlResult() {
    RequestAuControlResult r = new RequestAuControlResult();
    r.setId("au:test");
    r.setSuccess(true);
    r.setErrorMessage("none");
    return r;
  }

  private TdbPublisherWsResult createTdbPublisherWsResult() {
    TdbPublisherWsResult p = new TdbPublisherWsResult();
    p.setName("Publisher Name");
    return p;
  }

  private TdbTitleWsResult createTdbTitleWsResult() {
    TdbTitleWsResult t = new TdbTitleWsResult();
    t.setName("Title Name");
    t.setId("title-1");
    t.setIssn("1234-5678");
    t.setTdbPublisher(createTdbPublisherWsResult());
    t.setProprietaryids(Arrays.asList("prop1", "prop2"));
    return t;
  }

  private TdbAuWsResult createTdbAuWsResult() {
    TdbAuWsResult a = new TdbAuWsResult();
    a.setAuId("au:test");
    a.setName("TDB AU");
    a.setPluginName("org.lockss.plugin.Example");
    a.setParams(createStringMap("base_url", "http://example.com/"));
    return a;
  }

  // Helper methods

  private Map<String, String> createStringMap(String... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Key-value pairs must be even number of arguments");
    }

    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put(keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }

}
