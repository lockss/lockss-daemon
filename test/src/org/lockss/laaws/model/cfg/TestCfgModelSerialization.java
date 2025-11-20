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


  public void testApiStatus_gsonToJackson() throws Exception {
    ApiStatus original = createApiStatus();
    String gsonJson = JSON.serialize(original);
    ApiStatus jacksonParsed = jackson.readValue(gsonJson, ApiStatus.class);
    assertEquals(original.getApiVersion(), jacksonParsed.getApiVersion());
    assertEquals(original.getReady(), jacksonParsed.getReady());
    assertEquals(original.getServiceName(), jacksonParsed.getServiceName());
  }

  public void testApiStatus_jacksonToGson() throws Exception {
    ApiStatus original = createApiStatus();
    String jacksonJson = jackson.writeValueAsString(original);
    ApiStatus gsonParsed = JSON.deserialize(jacksonJson, ApiStatus.class);
    assertEquals(original.getApiVersion(), gsonParsed.getApiVersion());
    assertEquals(original.getReady(), gsonParsed.getReady());
    assertEquals(original.getServiceName(), gsonParsed.getServiceName());
  }


  public void testAuConfiguration_gsonToJackson() throws Exception {
    AuConfiguration original = createAuConfiguration();
    String gsonJson = JSON.serialize(original);
    AuConfiguration jacksonParsed = jackson.readValue(gsonJson, AuConfiguration.class);
    assertEquals(original.getAuId(), jacksonParsed.getAuId());
    assertEquals(original.getAuConfig(), jacksonParsed.getAuConfig());
  }

  public void testAuConfiguration_jacksonToGson() throws Exception {
    AuConfiguration original = createAuConfiguration();
    String jacksonJson = jackson.writeValueAsString(original);
    AuConfiguration gsonParsed = JSON.deserialize(jacksonJson, AuConfiguration.class);
    assertEquals(original.getAuId(), gsonParsed.getAuId());
    assertEquals(original.getAuConfig(), gsonParsed.getAuConfig());
  }


  public void testAuStatus_gsonToJackson() throws Exception {
    AuStatus original = createAuStatus();
    String gsonJson = JSON.serialize(original);
    AuStatus jacksonParsed = jackson.readValue(gsonJson, AuStatus.class);
    assertEquals(original.getVolume(), jacksonParsed.getVolume());
  }

  public void testAuStatus_jacksonToGson() throws Exception {
    AuStatus original = createAuStatus();
    String jacksonJson = jackson.writeValueAsString(original);
    AuStatus gsonParsed = JSON.deserialize(jacksonJson, AuStatus.class);
    assertEquals(original.getVolume(), gsonParsed.getVolume());
  }


  public void testAuWsResult_gsonToJackson() throws Exception {
    AuWsResult original = createAuWsResult();
    String gsonJson = JSON.serialize(original);
    AuWsResult jacksonParsed = jackson.readValue(gsonJson, AuWsResult.class);
    assertEquals(original.getAuId(), jacksonParsed.getAuId());
  }

  public void testAuWsResult_jacksonToGson() throws Exception {
    AuWsResult original = createAuWsResult();
    String jacksonJson = jackson.writeValueAsString(original);
    AuWsResult gsonParsed = JSON.deserialize(jacksonJson, AuWsResult.class);
    assertEquals(original.getAuId(), gsonParsed.getAuId());
  }


  public void testCheckSubstanceResult_gsonToJackson() throws Exception {
    CheckSubstanceResult original = createCheckSubstanceResult();
    String gsonJson = JSON.serialize(original);
    CheckSubstanceResult jacksonParsed = jackson.readValue(gsonJson, CheckSubstanceResult.class);
    assertEquals(original.getId(), jacksonParsed.getId());
    assertEquals(original.getOldState(), jacksonParsed.getOldState());
    assertEquals(original.getNewState(), jacksonParsed.getNewState());
    assertEquals(original.getErrorMessage(), jacksonParsed.getErrorMessage());
  }

  public void testCheckSubstanceResult_jacksonToGson() throws Exception {
    CheckSubstanceResult original = createCheckSubstanceResult();
    String jacksonJson = jackson.writeValueAsString(original);
    CheckSubstanceResult gsonParsed = JSON.deserialize(jacksonJson, CheckSubstanceResult.class);
    assertEquals(original.getId(), gsonParsed.getId());
    assertEquals(original.getOldState(), gsonParsed.getOldState());
    assertEquals(original.getNewState(), gsonParsed.getNewState());
    assertEquals(original.getErrorMessage(), gsonParsed.getErrorMessage());
  }


  public void testContentConfigurationResult_gsonToJackson() throws Exception {
    ContentConfigurationResult original = createContentConfigurationResult();
    String gsonJson = JSON.serialize(original);
    ContentConfigurationResult jacksonParsed = jackson.readValue(gsonJson, ContentConfigurationResult.class);
    assertEquals(original.getAuId(), jacksonParsed.getAuId());
    assertEquals(original.getName(), jacksonParsed.getName());
    assertEquals(original.getIsSuccess(), jacksonParsed.getIsSuccess());
    assertEquals(original.getMessage(), jacksonParsed.getMessage());
  }

  public void testContentConfigurationResult_jacksonToGson() throws Exception {
    ContentConfigurationResult original = createContentConfigurationResult();
    String jacksonJson = jackson.writeValueAsString(original);
    ContentConfigurationResult gsonParsed = JSON.deserialize(jacksonJson, ContentConfigurationResult.class);
    assertEquals(original.getAuId(), gsonParsed.getAuId());
    assertEquals(original.getName(), gsonParsed.getName());
    assertEquals(original.getIsSuccess(), gsonParsed.getIsSuccess());
    assertEquals(original.getMessage(), gsonParsed.getMessage());
  }


  public void testPlatformConfigurationWsResult_gsonToJackson() throws Exception {
    PlatformConfigurationWsResult original = createPlatformConfigurationWsResult();
    String gsonJson = JSON.serialize(original);
    PlatformConfigurationWsResult jacksonParsed = jackson.readValue(gsonJson, PlatformConfigurationWsResult.class);
    assertEquals(original.getHostName(), jacksonParsed.getHostName());
  }

  public void testPlatformConfigurationWsResult_jacksonToGson() throws Exception {
    PlatformConfigurationWsResult original = createPlatformConfigurationWsResult();
    String jacksonJson = jackson.writeValueAsString(original);
    PlatformConfigurationWsResult gsonParsed = JSON.deserialize(jacksonJson, PlatformConfigurationWsResult.class);
    assertEquals(original.getHostName(), gsonParsed.getHostName());
  }


  public void testPluginWsResult_gsonToJackson() throws Exception {
    PluginWsResult original = createPluginWsResult();
    String gsonJson = JSON.serialize(original);
    PluginWsResult jacksonParsed = jackson.readValue(gsonJson, PluginWsResult.class);
    assertEquals(original.getPluginId(), jacksonParsed.getPluginId());
    assertEquals(original.getName(), jacksonParsed.getName());
    assertEquals(original.getVersion(), jacksonParsed.getVersion());
    assertEquals(original.getType(), jacksonParsed.getType());
    assertEquals(original.getDefinition(), jacksonParsed.getDefinition());
    assertEquals(original.getRegistry(), jacksonParsed.getRegistry());
    assertEquals(original.getUrl(), jacksonParsed.getUrl());
    assertEquals(original.getAuCount(), jacksonParsed.getAuCount());
    assertEquals(original.getPublishingPlatform(), jacksonParsed.getPublishingPlatform());
  }

  public void testPluginWsResult_jacksonToGson() throws Exception {
    PluginWsResult original = createPluginWsResult();
    String jacksonJson = jackson.writeValueAsString(original);
    PluginWsResult gsonParsed = JSON.deserialize(jacksonJson, PluginWsResult.class);
    assertEquals(original.getPluginId(), gsonParsed.getPluginId());
    assertEquals(original.getName(), gsonParsed.getName());
    assertEquals(original.getVersion(), gsonParsed.getVersion());
    assertEquals(original.getType(), gsonParsed.getType());
    assertEquals(original.getDefinition(), gsonParsed.getDefinition());
    assertEquals(original.getRegistry(), gsonParsed.getRegistry());
    assertEquals(original.getUrl(), gsonParsed.getUrl());
    assertEquals(original.getAuCount(), gsonParsed.getAuCount());
    assertEquals(original.getPublishingPlatform(), gsonParsed.getPublishingPlatform());
  }


  public void testRequestAuControlResult_gsonToJackson() throws Exception {
    RequestAuControlResult original = createRequestAuControlResult();
    String gsonJson = JSON.serialize(original);
    RequestAuControlResult jacksonParsed = jackson.readValue(gsonJson, RequestAuControlResult.class);
    assertEquals(original.getId(), jacksonParsed.getId());
    assertEquals(original.getSuccess(), jacksonParsed.getSuccess());
    assertEquals(original.getErrorMessage(), jacksonParsed.getErrorMessage());
  }

  public void testRequestAuControlResult_jacksonToGson() throws Exception {
    RequestAuControlResult original = createRequestAuControlResult();
    String jacksonJson = jackson.writeValueAsString(original);
    RequestAuControlResult gsonParsed = JSON.deserialize(jacksonJson, RequestAuControlResult.class);
    assertEquals(original.getId(), gsonParsed.getId());
    assertEquals(original.getSuccess(), gsonParsed.getSuccess());
    assertEquals(original.getErrorMessage(), gsonParsed.getErrorMessage());
  }


  public void testTdbAuWsResult_gsonToJackson() throws Exception {
    TdbAuWsResult original = createTdbAuWsResult();
    String gsonJson = JSON.serialize(original);
    TdbAuWsResult jacksonParsed = jackson.readValue(gsonJson, TdbAuWsResult.class);
    assertEquals(original.getAuId(), jacksonParsed.getAuId());
    assertEquals(original.getPluginName(), jacksonParsed.getPluginName());
  }

  public void testTdbAuWsResult_jacksonToGson() throws Exception {
    TdbAuWsResult original = createTdbAuWsResult();
    String jacksonJson = jackson.writeValueAsString(original);
    TdbAuWsResult gsonParsed = JSON.deserialize(jacksonJson, TdbAuWsResult.class);
    assertEquals(original.getAuId(), gsonParsed.getAuId());
    assertEquals(original.getPluginName(), gsonParsed.getPluginName());
  }


  public void testTdbPublisherWsResult_gsonToJackson() throws Exception {
    TdbPublisherWsResult original = createTdbPublisherWsResult();
    String gsonJson = JSON.serialize(original);
    TdbPublisherWsResult jacksonParsed = jackson.readValue(gsonJson, TdbPublisherWsResult.class);
    assertEquals(original.getName(), jacksonParsed.getName());
  }

  public void testTdbPublisherWsResult_jacksonToGson() throws Exception {
    TdbPublisherWsResult original = createTdbPublisherWsResult();
    String jacksonJson = jackson.writeValueAsString(original);
    TdbPublisherWsResult gsonParsed = JSON.deserialize(jacksonJson, TdbPublisherWsResult.class);
    assertEquals(original.getName(), gsonParsed.getName());
  }


  public void testTdbTitleWsResult_gsonToJackson() throws Exception {
    TdbTitleWsResult original = createTdbTitleWsResult();
    String gsonJson = JSON.serialize(original);
    TdbTitleWsResult jacksonParsed = jackson.readValue(gsonJson, TdbTitleWsResult.class);
    assertEquals(original.getName(), jacksonParsed.getName());
    assertEquals(original.getId(), jacksonParsed.getId());
    assertEquals(original.getIssn(), jacksonParsed.getIssn());
  }

  public void testTdbTitleWsResult_jacksonToGson() throws Exception {
    TdbTitleWsResult original = createTdbTitleWsResult();
    String jacksonJson = jackson.writeValueAsString(original);
    TdbTitleWsResult gsonParsed = JSON.deserialize(jacksonJson, TdbTitleWsResult.class);
    assertEquals(original.getName(), gsonParsed.getName());
    assertEquals(original.getId(), gsonParsed.getId());
    assertEquals(original.getIssn(), gsonParsed.getIssn());
  }


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
    Map<String,String> map = new HashMap<>();
    map.put("key1", "val1");
    map.put("key2", "val2");
    c.setAuConfig(map);
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
    Map<String,String> def = new HashMap<>();
    def.put("base_url", "http://example.com/");
    r.setDefinition(def);
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
    List<String> ids = new ArrayList<>();
    ids.add("prop1");
    ids.add("prop2");
    t.setProprietaryids(ids);
    return t;
  }

  private TdbAuWsResult createTdbAuWsResult() {
    TdbAuWsResult a = new TdbAuWsResult();
    a.setAuId("au:test");
    a.setName("TDB AU");
    a.setPluginName("org.lockss.plugin.Example");
    Map<String,String> params = new HashMap<>();
    params.put("base_url", "http://example.com/");
    a.setParams(params);
    return a;
  }

}
