package org.lockss.laaws.model.rs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

  public void testArtifact_SerializationConsistency() throws Exception {
    testSerializationConsistency(createArtifact(), Artifact.class);
  }

  public void testArtifactPageInfo_SerializationConsistency() throws Exception {
    testSerializationConsistency(createArtifactPageInfo(), ArtifactPageInfo.class);
  }

  public void testAuSize_SerializationConsistency() throws Exception {
    testSerializationConsistency(createAuSize(), AuSize.class);
  }

  public void testAuidPageInfo_SerializationConsistency() throws Exception {
    testSerializationConsistency(createAuidPageInfo(), AuidPageInfo.class);
  }

  public void testImportStatus_SerializationConsistency() throws Exception {
    testSerializationConsistency(createImportStatus(), ImportStatus.class);
  }

  public void testPageInfo_SerializationConsistency() throws Exception {
    testSerializationConsistency(createPageInfo(), PageInfo.class);
  }

  public void testRepositoryInfo_SerializationConsistency() throws Exception {
    testSerializationConsistency(createRepositoryInfo(), RepositoryInfo.class);
  }

  public void testRepositoryStatistics_SerializationConsistency() throws Exception {
    testSerializationConsistency(createRepositoryStatistics(), RepositoryStatistics.class);
  }

  public void testStorageInfo_SerializationConsistency() throws Exception {
    testSerializationConsistency(createStorageInfo(), StorageInfo.class);
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
    s.setServiceName("repository");
    s.setReason("Service started successfully");
    return s;
  }

  private Artifact createArtifact() {
    Artifact a = new Artifact();
    a.setUuid("artifact-123");
    a.setNamespace("lockss");
    a.setAuid("test-auid");
    a.setUri("http://example.com/test");
    a.setVersion(2);
    a.setCommitted(true);
    a.setStorageUrl("file:///repo/test");
    a.setContentLength(1024L);
    a.setContentDigest("sha256:abcdef123456");
    a.setCollectionDate(System.currentTimeMillis());
    return a;
  }

  private ArtifactPageInfo createArtifactPageInfo() {
    ArtifactPageInfo api = new ArtifactPageInfo();
    api.setPageInfo(createPageInfo());
    api.setArtifacts(Arrays.asList(createArtifact(), createArtifact()));
    return api;
  }

  private AuSize createAuSize() {
    AuSize s = new AuSize();
    s.setTotalLatestVersions(1500L);
    s.setTotalAllVersions(3000L);
    s.setTotalWarcSize(50000000L);
    return s;
  }

  private AuidPageInfo createAuidPageInfo() {
    AuidPageInfo api = new AuidPageInfo();
    api.setPageInfo(createPageInfo());
    api.setAuids(Arrays.asList("auid-1", "auid-2", "auid-3"));
    return api;
  }

  private ImportStatus createImportStatus() {
    ImportStatus s = new ImportStatus();
    s.setArtifactUuid("test-import");
    s.setStatus(ImportStatus.StatusEnum.OK);
    s.setStatusMessage("Import completed successfully");
    return s;
  }

  private PageInfo createPageInfo() {
    PageInfo p = new PageInfo();
    p.setTotalCount(100);
    p.setContinuationToken("next-page-token");
    p.setCurLink("/artifacts?page=1");
    p.setNextLink("/artifacts?page=2");
    return p;
  }

  private RepositoryInfo createRepositoryInfo() {
    RepositoryInfo ri = new RepositoryInfo();
    ri.setStoreInfo(createStorageInfo());
    ri.setIndexInfo(createStorageInfo());
    return ri;
  }

  private RepositoryStatistics createRepositoryStatistics() {
    RepositoryStatistics rs = new RepositoryStatistics();
    rs.setTimeSpentReiteratingIterators(12345L);
    return rs;
  }

  private StorageInfo createStorageInfo() {
    StorageInfo si = new StorageInfo();
    si.setType("filesystem");
    si.setName("primary-store");
//    si.setPath("/var/repo");
//    si.setSizeKB(100000000000L);
//    si.setUsedKB(50000000000L);
//    si.setAvailKB(50000000000L);
    si.setPercentUsed(50.0);
    si.setPercentUsedString("50.0%");
    return si;
  }

  // Legacy JUnit 3-style suite for project harness
  public static Test suite() {
    return new TestSuite(TestRsModelSerialization.class);
  }
}
