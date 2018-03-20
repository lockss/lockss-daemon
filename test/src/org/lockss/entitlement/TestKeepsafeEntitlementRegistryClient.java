package org.lockss.entitlement;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.mockito.Mockito;

import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.test.StringInputStream;
import org.lockss.util.urlconn.LockssUrlConnection;

public class TestKeepsafeEntitlementRegistryClient extends LockssTestCase {
  private static final String ER_URI = "http://keepsafe.org";
  private KeepsafeEntitlementRegistryClient client;

  private Map<String,String> validEntitlementParams;
  private Map<String,String> validResponseParams;
  private Map<String,String> validPublisherParams;

  public void setUp() throws Exception {
    super.setUp();
    client = Mockito.spy(new KeepsafeEntitlementRegistryClient());
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setEntitlementRegistryClient(client);
    daemon.setDaemonInited(true);
    Properties p = new Properties();
    p.setProperty(KeepsafeEntitlementRegistryClient.PARAM_ER_URI, ER_URI);
    p.setProperty(KeepsafeEntitlementRegistryClient.PARAM_ER_APIKEY, "00000000-0000-0000-0000-000000000000");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    client.initService(daemon);
    client.startService();

    validEntitlementParams = new HashMap<String, String>();
    validEntitlementParams.put("identifier_value", "0123-456X");
    validEntitlementParams.put("institution", "11111111-1111-1111-1111-111111111111");
    validEntitlementParams.put("start", "20120101");
    validEntitlementParams.put("end", "20151231");
    validEntitlementParams.put("validate", "1");

    validResponseParams = new HashMap<String,String>(validEntitlementParams);
    validResponseParams.put("institution", ER_URI + "/institutions/11111111-1111-1111-1111-111111111111/");
    validResponseParams.put("publisher", ER_URI + "/publishers/33333333-0000-0000-0000-000000000000/");

    validPublisherParams = new HashMap<String, String>();
    validPublisherParams.put("guid", "33333333-0000-0000-0000-000000000000");
    validPublisherParams.put("name", "Wiley");
  }

  public void testEntitlementRegistryError() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 500, "Internal server error")).when(client).openConnection(url);

    try {
      client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertEquals("Error communicating with entitlement registry. Response was 500 null", e.getMessage());
    }
    Mockito.verify(client).openConnection(url);
  }

  public void testEntitlementRegistryInvalidResponse() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 200, "[]")).when(client).openConnection(url);

    assertFalse(client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    Mockito.verify(client).openConnection(url);
  }

  public void testEntitlementRegistryInvalidJson() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 200, "[{\"this\": isn't, JSON}]")).when(client).openConnection(url);

    try {
      client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertTrue(e.getMessage().startsWith("Unrecognized token 'isn': was expecting ('true', 'false' or 'null')"));
    }
    Mockito.verify(client).openConnection(url);
  }

  public void testEntitlementRegistryUnexpectedJson() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 200, "{\"surprise\": \"object\"}")).when(client).openConnection(url);

    assertFalse(client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    Mockito.verify(client).openConnection(url);
  }

  public void testUserEntitled() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 200, "[" + mapToJson(validResponseParams) + "]")).when(client).openConnection(url);

    assertTrue(client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    Mockito.verify(client).openConnection(url);
  }

  public void testUserNotEntitled() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 204, "")).when(client).openConnection(url);

    assertFalse(client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    Mockito.verify(client).openConnection(url);
  }

  public void testGetInstitution() throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put("scope", "ed.ac.uk");
    Map<String, String> institution = new HashMap<String, String>();
    institution.put("guid", "11111111-0000-0000-0000-000000000000");
    institution.put("name", "University of Edinburgh");
    institution.put("scope", "ed.ac.uk");

    String url = url("/institutions", KeepsafeEntitlementRegistryClient.mapToPairs(queryParams));
    Mockito.doReturn(connection(url, 200, "[" + mapToJson(institution) + "]")).when(client).openConnection(url);
    assertEquals("11111111-0000-0000-0000-000000000000", client.getInstitution("ed.ac.uk"));

    Mockito.verify(client).openConnection(url);
  }

  public void testGetInstitutionNoResponse() throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put("scope", "ed.ac.uk");

    String url = url("/institutions", KeepsafeEntitlementRegistryClient.mapToPairs(queryParams));
    Mockito.doReturn(connection(url, 200, "[]")).when(client).openConnection(url);
    try {
      client.getInstitution("ed.ac.uk");
      fail("Expected exception not thrown");
    }
    catch (IOException e) {
      assertEquals("No matching institutions returned from entitlement registry", e.getMessage());
    }

    Mockito.verify(client).openConnection(url);
  }

  public void testGetInstitutionMultipleResults() throws Exception {
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put("scope", "ed.ac.uk");
    Map<String, String> institution1 = new HashMap<String, String>();
    institution1.put("id", "11111111-0000-0000-0000-000000000000");
    institution1.put("name", "University of Edinburgh");
    institution1.put("scope", "ed.ac.uk");
    Map<String, String> institution2 = new HashMap<String, String>();
    institution2.put("id", "11111111-1111-1111-1111-111111111111");
    institution2.put("name", "University of Edinburgh 2");
    institution2.put("scope", "ed.ac.uk");

    String url = url("/institutions", KeepsafeEntitlementRegistryClient.mapToPairs(queryParams));
    Mockito.doReturn(connection(url, 200, "[" + mapToJson(institution1) + "," + mapToJson(institution2) + "]")).when(client).openConnection(url);
    try {
      client.getInstitution("ed.ac.uk");
      fail("Expected exception not thrown");
    }
    catch (IOException e) {
      assertEquals("Multiple matching institutions returned from entitlement registry", e.getMessage());
    }

    Mockito.verify(client).openConnection(url);
  }

  public void testGetPublisher() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 200, "[" + mapToJson(validResponseParams) + "]")).when(client).openConnection(url);

    assertEquals("33333333-0000-0000-0000-000000000000", client.getPublisher("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    Mockito.verify(client).openConnection(url);
  }

  public void testGetPublisherNotEntitled() throws Exception {
    String url = url("/entitlements", KeepsafeEntitlementRegistryClient.mapToPairs(validEntitlementParams));
    Mockito.doReturn(connection(url, 204, "")).when(client).openConnection(url);

    assertEquals(null, client.getPublisher("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    Mockito.verify(client).openConnection(url);
  }

  public void testGetPublisherWorkflow() throws Exception {
    Map<String, String> responseParams = new HashMap<String,String>(validPublisherParams);
    responseParams.put("workflow", "primary_lockss");
    String url = url("/publishers/33333333-0000-0000-0000-000000000000");
    Mockito.doReturn(connection(url, 200, mapToJson(responseParams))).when(client).openConnection(url);

    assertEquals(PublisherWorkflow.PRIMARY_LOCKSS, client.getPublisherWorkflow("33333333-0000-0000-0000-000000000000"));
    Mockito.verify(client).openConnection(url);
  }

  public void testGetPublisherWorkflowMissingWorkflow() throws Exception {
    Map<String, String> responseParams = new HashMap<String,String>(validPublisherParams);
    String url = url("/publishers/33333333-0000-0000-0000-000000000000");
    Mockito.doReturn(connection(url, 200, mapToJson(responseParams))).when(client).openConnection(url);

    assertEquals(PublisherWorkflow.PRIMARY_LOCKSS, client.getPublisherWorkflow("33333333-0000-0000-0000-000000000000"));
    Mockito.verify(client).openConnection(url);
  }

  public void testGetPublisherWorkflowInvalidWorkflow() throws Exception {
    Map<String, String> responseParams = new HashMap<String,String>(validPublisherParams);
    responseParams.put("workflow", "gibberish");
    String url = url("/publishers/33333333-0000-0000-0000-000000000000");
    Mockito.doReturn(connection(url, 200, mapToJson(responseParams))).when(client).openConnection(url);

    try {
      client.getPublisherWorkflow("33333333-0000-0000-0000-000000000000");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertTrue(e.getMessage().startsWith("No valid workflow returned from entitlement registry"));
    }
    Mockito.verify(client).openConnection(url);
  }

  private String url(String endpoint) {
    return url(endpoint, null);
  }

  private String url(String endpoint, List<NameValuePair> params) {
    URIBuilder builder = new URIBuilder();
    builder.setScheme("http");
    builder.setHost("keepsafe.org");
    builder.setPath(endpoint);
    if(params != null) {
      builder.setParameters(params);
    }
    return builder.toString();
  }

  private MockLockssUrlConnection connection(String url, int responseCode, String response) throws IOException {
    MockLockssUrlConnection connection = new MockLockssUrlConnection(url);
    connection.setResponseCode(responseCode);
    connection.setResponseInputStream(new StringInputStream(response));
    return connection;
  }

  private String mapToJson(Map<String, ? extends Object> params) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(params);
  }
}

