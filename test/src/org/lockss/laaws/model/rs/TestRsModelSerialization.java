package org.lockss.laaws.model.rs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.lockss.laaws.client.JSON;
import org.lockss.test.LockssTestCase;

import java.util.*;

/**
 * Tests JSON serialization/deserialization consistency for all model objects
 * in org.lockss.laaws.model.rs
 */
public class TestRsModelSerialization extends LockssTestCase {

  private final ObjectMapper jackson = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  // ============================================================================
  // ApiStatus Tests
  // ============================================================================

  public void testApiStatus_roundTrip() throws Exception {
    ApiStatus original = createTestApiStatus();

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    ApiStatus deserialized = JSON.deserialize(json, ApiStatus.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Ready status should match", original.getReady(), deserialized.getReady());
    assertEquals("Version should match", original.getApiVersion(), deserialized.getApiVersion());
    assertEquals("Service name should match", original.getServiceName(), deserialized.getServiceName());
    assertEquals("Startup message should match", original.getReason(), deserialized.getReason());
  }

  public void testApiStatus_nullFields() throws Exception {
    ApiStatus original = new ApiStatus();
    // All fields null
    // serialization succeeds and only deserialization fails this seems wrong.
    // XXX fix the ApiStatus class to handle null fields when deserializing
    String json = JSON.serialize(original);
    try {
      ApiStatus deserialized = JSON.deserialize(json, ApiStatus.class);
      fail("Serialization should have failed");
    } catch (Exception e) {
      // expected
    }
  }

  public void testApiStatus_gsonToJackson() throws Exception {
    ApiStatus original = new ApiStatus();
    original.setReady(false);
    original.setApiVersion("1.5.2");

    String gsonJson = JSON.serialize(original);
    ApiStatus jacksonParsed = jackson.readValue(gsonJson, ApiStatus.class);

    assertEquals("Ready should match", original.getReady(), jacksonParsed.getReady());
    assertEquals("Version should match", original.getApiVersion(), jacksonParsed.getApiVersion());
  }

  public void testApiStatus_jacksonToGson() throws Exception {
    ApiStatus original = createTestApiStatus();

    String jacksonJson = jackson.writeValueAsString(original);
    ApiStatus gsonParsed = JSON.deserialize(jacksonJson, ApiStatus.class);

    assertEquals("Ready should match", original.getReady(), gsonParsed.getReady());
    assertEquals("Version should match", original.getApiVersion(), gsonParsed.getApiVersion());
  }

  // ============================================================================
  // Artifact Tests
  // ============================================================================

  public void testArtifact_roundTrip() throws Exception {
    Artifact original = new Artifact();
    original.uuid("artifact-123");
    original.setNamespace("lockss");
    original.auid("test-auid");
    original.setUri("http://example.com/test");
    original.setVersion(2);
    original.setCommitted(true);
    original.setStorageUrl("file:///repo/test");
    original.setContentLength(1024L);
    original.setContentDigest("sha256:abcdef123456");
    original.setCollectionDate(System.currentTimeMillis());

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    Artifact deserialized = JSON.deserialize(json, Artifact.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("ID should match", original.getUuid(), deserialized.getUuid());
    assertEquals("Namespace should match", original.getNamespace(), deserialized.getNamespace());
    assertEquals("AUID should match", original.getAuid(), deserialized.getAuid());
    assertEquals("URI should match", original.getUri(), deserialized.getUri());
    assertEquals("Version should match", original.getVersion(), deserialized.getVersion());
    assertEquals("Committed should match", original.getCommitted(), deserialized.getCommitted());
    assertEquals("Storage URL should match", original.getStorageUrl(), deserialized.getStorageUrl());
    assertEquals("Content length should match", original.getContentLength(), deserialized.getContentLength());
    assertEquals("Content digest should match", original.getContentDigest(), deserialized.getContentDigest());
    assertEquals("Collection date should match", original.getCollectionDate(), deserialized.getCollectionDate());
  }

  public void testArtifact_minimalFields() throws Exception {
    Artifact original = new Artifact();
    original.auid("minimal-artifact");
    original.setUri("http://example.com/minimal");

    String json = JSON.serialize(original);
    Artifact deserialized = JSON.deserialize(json, Artifact.class);

    assertEquals("ID should match", original.getAuid(), deserialized.getAuid());
    assertEquals("URI should match", original.getUri(), deserialized.getUri());
  }

  public void testArtifact_gsonToJackson() throws Exception {
    Artifact original = new Artifact();
    original.auid("test-123");
    original.setVersion(1);
    original.setCommitted(false);

    String gsonJson = JSON.serialize(original);
    Artifact jacksonParsed = jackson.readValue(gsonJson, Artifact.class);

    assertEquals("ID should match", original.getAuid(), jacksonParsed.getAuid());
    assertEquals("Version should match", original.getVersion(), jacksonParsed.getVersion());
    assertEquals("Committed should match", original.getCommitted(), jacksonParsed.getCommitted());
  }

  // ============================================================================
  // ArtifactPageInfo Tests
  // ============================================================================

  public void testArtifactPageInfo_roundTrip() throws Exception {
    ArtifactPageInfo original = new ArtifactPageInfo();

    PageInfo pageInfo = new PageInfo();
    pageInfo.setTotalCount(100);
    pageInfo.setResultsPerPage(10);
    pageInfo.setContinuationToken("next-page-token");
    pageInfo.setCurLink("/artifacts?page=1");
    pageInfo.setNextLink("/artifacts?page=2");
    original.setPageInfo(pageInfo);

    List<Artifact> artifacts = new ArrayList<Artifact>();
    Artifact artifact1 = new Artifact();
    artifact1.auid("art-1");
    artifact1.setUri("http://example.com/1");
    artifacts.add(artifact1);

    Artifact artifact2 = new Artifact();
    artifact2.auid("art-2");
    artifact2.setUri("http://example.com/2");
    artifacts.add(artifact2);

    original.setArtifacts(artifacts);

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    ArtifactPageInfo deserialized = JSON.deserialize(json, ArtifactPageInfo.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertNotNull("Page info should not be null", deserialized.getPageInfo());
    assertEquals("Total count should match",
      original.getPageInfo().getTotalCount(),
      deserialized.getPageInfo().getTotalCount());
    assertEquals("Results per page should match",
      original.getPageInfo().getResultsPerPage(),
      deserialized.getPageInfo().getResultsPerPage());
    assertEquals("Continuation token should match",
      original.getPageInfo().getContinuationToken(),
      deserialized.getPageInfo().getContinuationToken());
    assertEquals("Current link should match",
      original.getPageInfo().getCurLink(),
      deserialized.getPageInfo().getCurLink());
    assertEquals("Next link should match",
      original.getPageInfo().getNextLink(),
      deserialized.getPageInfo().getNextLink());

    assertNotNull("Artifacts list should not be null", deserialized.getArtifacts());
    assertEquals("Artifacts list size should match", 2, deserialized.getArtifacts().size());
    assertEquals("First artifact ID should match", "art-1", deserialized.getArtifacts().get(0).getAuid());
    assertEquals("Second artifact ID should match", "art-2", deserialized.getArtifacts().get(1).getAuid());
  }

  public void testArtifactPageInfo_emptyList() throws Exception {
    ArtifactPageInfo original = new ArtifactPageInfo();
    original.setArtifacts(new ArrayList<Artifact>());

    PageInfo pageInfo = new PageInfo();
    pageInfo.setTotalCount(0);
    pageInfo.setResultsPerPage(10);
    pageInfo.setContinuationToken("");
    pageInfo.setCurLink("/artifacts?page=1");
    pageInfo.setNextLink("");
    original.setPageInfo(pageInfo);

    String json = JSON.serialize(original);
    ArtifactPageInfo deserialized = JSON.deserialize(json, ArtifactPageInfo.class);

    assertNotNull("Deserialized object should not be null", deserialized);
    assertNotNull("Artifacts list should not be null", deserialized.getArtifacts());
    assertEquals("Artifacts list should be empty", 0, deserialized.getArtifacts().size());
    assertNotNull("Page info should not be null", deserialized.getPageInfo());
    assertEquals("Total count should be 0", Integer.valueOf(0), deserialized.getPageInfo().getTotalCount());
    assertEquals("Results per page should match", pageInfo.getResultsPerPage(), deserialized.getPageInfo().getResultsPerPage());
    assertEquals("Continuation token should match", pageInfo.getContinuationToken(), deserialized.getPageInfo().getContinuationToken());
    assertEquals("Current link should match", pageInfo.getCurLink(), deserialized.getPageInfo().getCurLink());
    assertEquals("Next link should match", pageInfo.getNextLink(), deserialized.getPageInfo().getNextLink());
  }

  // ============================================================================
  // AuSize Tests
  // ============================================================================

  public void testAuSize_roundTrip() throws Exception {
    AuSize original = new AuSize();
    original.setTotalLatestVersions(1500L);
    original.setTotalAllVersions(3000L);
    original.setTotalWarcSize(50000000L);

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    AuSize deserialized = JSON.deserialize(json, AuSize.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Total latest versions should match",
      original.getTotalLatestVersions(),
      deserialized.getTotalLatestVersions());
    assertEquals("Total all versions should match",
      original.getTotalAllVersions(),
      deserialized.getTotalAllVersions());
    assertEquals("Total WARC size should match",
      original.getTotalWarcSize(),
      deserialized.getTotalWarcSize());
  }

  public void testAuSize_zeroValues() throws Exception {
    AuSize original = new AuSize();
    original.setTotalLatestVersions(0L);
    original.setTotalAllVersions(0L);
    original.setTotalWarcSize(0L);

    String json = JSON.serialize(original);
    AuSize deserialized = JSON.deserialize(json, AuSize.class);

    assertEquals("Total latest versions should be 0", Long.valueOf(0L), deserialized.getTotalLatestVersions());
    assertEquals("Total all versions should be 0", Long.valueOf(0L), deserialized.getTotalAllVersions());
    assertEquals("Total WARC size should be 0", Long.valueOf(0L), deserialized.getTotalWarcSize());
  }

  // ============================================================================
  // AuidPageInfo Tests
  // ============================================================================

  public void testAuidPageInfo_roundTrip() throws Exception {
    AuidPageInfo original = new AuidPageInfo();

    PageInfo pageInfo = createTestPageInfo();
    original.setPageInfo(pageInfo);

    List<String> auids = new ArrayList<String>();
    auids.add("auid-1");
    auids.add("auid-2");
    auids.add("auid-3");
    original.setAuids(auids);

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    AuidPageInfo deserialized = JSON.deserialize(json, AuidPageInfo.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertNotNull("Page info should not be null", deserialized.getPageInfo());
    assertEquals("Total count should match",
      original.getPageInfo().getTotalCount(),
      deserialized.getPageInfo().getTotalCount());

    assertNotNull("AUIDs list should not be null", deserialized.getAuids());
    assertEquals("AUIDs list size should match", 3, deserialized.getAuids().size());
    assertEquals("First AUID should match", "auid-1", deserialized.getAuids().get(0));
    assertEquals("Second AUID should match", "auid-2", deserialized.getAuids().get(1));
    assertEquals("Third AUID should match", "auid-3", deserialized.getAuids().get(2));
  }

  // ============================================================================
  // ImportStatus Tests
  // ============================================================================

  public void testImportStatus_roundTrip() throws Exception {
    ImportStatus original = new ImportStatus();
    original.artifactUuid("test-import");
    original.setStatus(ImportStatus.StatusEnum.OK);
    original.setStatusMessage("Import completed successfully");

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    ImportStatus deserialized = JSON.deserialize(json, ImportStatus.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Name should match", original.getArtifactUuid(), deserialized.getArtifactUuid());
    assertEquals("Status should match", original.getStatus(), deserialized.getStatus());
    assertEquals("Message should match", original.getStatusMessage(), deserialized.getStatusMessage());
  }

  public void testImportStatus_allStatuses() throws Exception {
    // Test each status enum value
    ImportStatus.StatusEnum[] statuses = ImportStatus.StatusEnum.values();

    for (ImportStatus.StatusEnum status : statuses) {
      ImportStatus original = new ImportStatus();
      original.setStatus(status);

      String json = JSON.serialize(original);
      ImportStatus deserialized = JSON.deserialize(json, ImportStatus.class);

      assertEquals("Status should match for " + status, status, deserialized.getStatus());
    }
  }

  // ============================================================================
  // PageInfo Tests
  // ============================================================================

  public void testPageInfo_roundTrip() throws Exception {
    PageInfo original = new PageInfo();
    original.setTotalCount(250);
    original.setResultsPerPage(25);
    original.setContinuationToken("page-token-abc");
    original.setCurLink("/api/resource?page=current");
    original.setNextLink("/api/resource?page=next");

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    PageInfo deserialized = JSON.deserialize(json, PageInfo.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Total count should match", original.getTotalCount(), deserialized.getTotalCount());
    assertEquals("Results per page should match", original.getResultsPerPage(), deserialized.getResultsPerPage());
    assertEquals("Continuation token should match", original.getContinuationToken(), deserialized.getContinuationToken());
    assertEquals("Current link should match", original.getCurLink(), deserialized.getCurLink());
    assertEquals("Next link should match", original.getNextLink(), deserialized.getNextLink());
  }

  public void testPageInfo_nullLinks() throws Exception {
    PageInfo original = createTestPageInfo();
    original.setCurLink("/api/resource?page=current");
    original.setNextLink(null);
    // (last page)

    String json = JSON.serialize(original);
    PageInfo deserialized = JSON.deserialize(json, PageInfo.class);

    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Total count should match", original.getTotalCount(), deserialized.getTotalCount());
    assertNull("Next link should be null", deserialized.getNextLink());
  }

  // ============================================================================
  // RepositoryInfo Tests
  // ============================================================================

  public void testRepositoryInfo_roundTrip() throws Exception {
    RepositoryInfo original = createTestRepositoryInfo();

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    RepositoryInfo deserialized = JSON.deserialize(json, RepositoryInfo.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Store info should match", original.getStoreInfo(), deserialized.getStoreInfo());
    assertEquals("Index info should match", original.getIndexInfo(), deserialized.getIndexInfo());
  }


  // ============================================================================
  // StorageInfo Tests
  // ============================================================================

  public void testStorageInfo_roundTrip() throws Exception {
    StorageInfo original = createTestStorageInfo(new Random());

    String json = JSON.serialize(original);
    assertNotNull("Serialized JSON should not be null", json);

    StorageInfo deserialized = JSON.deserialize(json, StorageInfo.class);
    assertNotNull("Deserialized object should not be null", deserialized);
    assertEquals("Name should match", original.getName(), deserialized.getName());
    assertEquals("Used space should match", original.getUsed(), deserialized.getUsed());
    assertEquals("Available space should match", original.getAvail(), deserialized.getAvail());
    assertEquals("Percent used should match", original.getPercentUsed(), deserialized.getPercentUsed());
    assertEquals("Percent used string should match", original.getPercentUsedString(), deserialized.getPercentUsedString());
    assertEquals("Type should match", original.getType(), deserialized.getType());
  }

  public void testStorageInfo_fullDisk() throws Exception {
    StorageInfo original = createTestStorageInfo(new Random());
    original.setAvail(0L);
    original.setPercentUsed(100.0);
    original.setPercentUsedString("100%");
    String json = JSON.serialize(original);
    StorageInfo deserialized = JSON.deserialize(json, StorageInfo.class);

    assertEquals("Used space should match", original.getUsed(), deserialized.getUsed());
    assertEquals("Available space should be 0", Long.valueOf(0L), deserialized.getAvail());
    assertEquals("Percent used should be 100%", Double.valueOf(100.0), deserialized.getPercentUsed());
  }

  public void testStorageInfo_gsonToJackson() throws Exception {
    StorageInfo original = new StorageInfo();
    original.setUsed(1000000L);
    original.setAvail(9000000L);
    original.setPercentUsed(10.0);

    String gsonJson = JSON.serialize(original);
    StorageInfo jacksonParsed = jackson.readValue(gsonJson, StorageInfo.class);

    assertEquals("Used space should match", original.getUsed(), jacksonParsed.getUsed());
    assertEquals("Available space should match", original.getAvail(), jacksonParsed.getAvail());
    assertEquals("Percent used should match", original.getPercentUsed(), jacksonParsed.getPercentUsed());
  }

  // ============================================================================
  // Complex Nested Structure Test
  // ============================================================================

  public void testComplexNestedStructure_roundTrip() throws Exception {
    // Create a complex nested structure to test deep serialization
    ArtifactPageInfo pageInfo = new ArtifactPageInfo();

    PageInfo pagination = createTestPageInfo();
    List<Artifact> artifacts = new ArrayList<Artifact>();
    for (int i = 0; i < 3; i++) {
      Artifact artifact = createTestArtifact("lockss", "au_id"+i);
      artifacts.add(artifact);
    }
    pageInfo.setArtifacts(artifacts);
    pageInfo.setPageInfo(pagination);

    String json = JSON.serialize(pageInfo);
    assertNotNull("Serialized JSON should not be null", json);
    assertTrue("JSON should be non-empty", json.length() > 100);

    ArtifactPageInfo deserialized = JSON.deserialize(json, ArtifactPageInfo.class);
    assertEquals("Page info should match", pageInfo.getPageInfo(), deserialized.getPageInfo());
    assertEquals("Artifacts should match", pageInfo.getArtifacts(), deserialized.getArtifacts());
  }




  private Artifact createTestArtifact(String namespace, String auidPrefix) {
    Random random = new Random();

    Artifact artifact = new Artifact();
    artifact.setUuid(UUID.randomUUID().toString());
    artifact.setNamespace(namespace);
    artifact.setAuid(auidPrefix + "-" + random.nextInt(1000));
    artifact.setUri("http://example.com/" + UUID.randomUUID());
    artifact.setVersion(random.nextInt(5) + 1);
    artifact.setCommitted(true);
    artifact.setStorageUrl("file:///repo/" + UUID.randomUUID());
    artifact.setContentLength((long) (random.nextInt(1000000) + 1024));
    artifact.setContentDigest("sha256:" + generateRandomHex(64, random));
    artifact.setCollectionDate(System.currentTimeMillis());

    return artifact;
  }

  private String generateRandomHex(int length, Random random) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(Integer.toHexString(random.nextInt(16)));
    }
    return sb.toString();
  }

  private ApiStatus createTestApiStatus() {
    Random random = new Random();

    ApiStatus apiStatus = new ApiStatus();
    apiStatus.setApiVersion("2.0." + random.nextInt(10));
    apiStatus.setComponentName("lockss-repository-service");
    apiStatus.setComponentVersion("2." + random.nextInt(5) + "." + random.nextInt(20));
    apiStatus.setLockssVersion("1." + random.nextInt(10) + "." + random.nextInt(100));
    apiStatus.setReady(true);
    apiStatus.setServiceName("repository");
    apiStatus.setReadyTime(System.currentTimeMillis() - random.nextInt(3600000)); // Ready within last hour
    apiStatus.setReason("Service started successfully");
    apiStatus.setPluginsReady(true);

    return apiStatus;
  }

  private PageInfo createTestPageInfo() {
    Random random = new Random();

    PageInfo pageInfo = new PageInfo();
    int totalCount = random.nextInt(1000) + 100;
    int resultsPerPage = 10 + random.nextInt(40); // 10-50 results per page
    int currentPage = random.nextInt(10) + 1;

    pageInfo.setTotalCount(totalCount);
    pageInfo.setResultsPerPage(resultsPerPage);
    pageInfo.setContinuationToken("token-" + UUID.randomUUID().toString());
    pageInfo.setCurLink("/api/artifacts?page=" + currentPage);
    pageInfo.setNextLink("/api/artifacts?page=" + (currentPage + 1));

    return pageInfo;
  }

  private RepositoryInfo createTestRepositoryInfo() {
    Random random = new Random();

    RepositoryInfo repositoryInfo = new RepositoryInfo();

    StorageInfo storeInfo = createTestStorageInfo(random);
    StorageInfo indexInfo = createTestIndexInfo(random);

    repositoryInfo.setStoreInfo(storeInfo);
    repositoryInfo.setIndexInfo(indexInfo);

    return repositoryInfo;
  }

  private StorageInfo createTestIndexInfo(Random random) {
    StorageInfo indexInfo = new StorageInfo();
    indexInfo.setType("solr");
    indexInfo.setName("primary-index");
    long indexSize = 10000000000L; // 10 GB
    long indexUsed = random.nextInt(8) * 1000000000L; // Random used space (0-8 GB)
    long indexAvail = indexSize - indexUsed;
    double indexPercentUsed = (indexUsed * 100.0) / indexSize;
    indexInfo.setSize(indexSize);
    indexInfo.setUsed(indexUsed);
    indexInfo.setAvail(indexAvail);
    indexInfo.setPercentUsed(indexPercentUsed);
    indexInfo.setPercentUsedString(String.format("%.1f%%", indexPercentUsed));
    return indexInfo;
  }

  private StorageInfo createTestStorageInfo(Random random) {
    StorageInfo storeInfo = new StorageInfo();
    storeInfo.setType("filesystem");
    storeInfo.setName("primary-store");
    long storeSize = 100000000000L; // 100 GB
    long storeUsed = random.nextInt(80) * 1000000000L; // Random used space (0-80 GB)
    long storeAvail = storeSize - storeUsed;
    double storePercentUsed = (storeUsed * 100.0) / storeSize;
    storeInfo.setSize(storeSize);
    storeInfo.setUsed(storeUsed);
    storeInfo.setAvail(storeAvail);
    storeInfo.setPercentUsed(storePercentUsed);
    storeInfo.setPercentUsedString(String.format("%.1f%%", storePercentUsed));
    return storeInfo;
  }

  // Legacy JUnit 3-style suite for project harness
  public static Test suite() {
    return new TestSuite(TestRsModelSerialization.class);
  }
}
