package org.lockss.safenet;

import java.io.IOException;

import org.mockito.Mockito;

import org.lockss.safenet.PublisherWorkflow;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;

public class TestCachingEntitlementRegistryClient extends LockssTestCase {
  private EntitlementRegistryClient client;
  private BaseEntitlementRegistryClient mock;

  public void setUp() throws Exception {
    super.setUp();
    mock = Mockito.mock(BaseEntitlementRegistryClient.class);
    client = new CachingEntitlementRegistryClient(mock, 5);
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setEntitlementRegistryClient(client);
    daemon.setDaemonInited(true);
    client.initService(daemon);
    client.startService();
  }

  public void testCaching() throws Exception {
    Mockito.when(mock.isUserEntitled("0000-0000", "11111111-1111-1111-1111-111111111111", "20120101", "20151231")).thenReturn(true);
    Mockito.when(mock.isUserEntitled("0000-0001", "11111111-1111-1111-1111-111111111111", "20120101", "20151231")).thenReturn(false);
    Mockito.when(mock.isUserEntitled("0000-0002", "11111111-1111-1111-1111-111111111111", "20120101", "20151231")).thenThrow(new IOException("Error communicating with entitlement registry. Response was 500 null"));
    Mockito.when(mock.getPublisher("0000-0000", "20120101", "20151231")).thenReturn("33333333-0000-0000-0000-000000000000");
    Mockito.when(mock.getPublisher("0000-0001", "20120101", "20151231")).thenReturn("33333333-0000-0000-0000-000000000001");
    Mockito.when(mock.getPublisherWorkflow("33333333-0000-0000-0000-000000000000")).thenReturn(PublisherWorkflow.PRIMARY_SAFENET);
    Mockito.when(mock.getPublisherWorkflow("33333333-1111-1111-1111-111111111111")).thenReturn(PublisherWorkflow.PRIMARY_PUBLISHER);

    assertTrue(client.isUserEntitled("0000-0000", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    assertFalse(client.isUserEntitled("0000-0001", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    try {
      client.isUserEntitled("0000-0002", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertEquals("Error communicating with entitlement registry. Response was 500 null", e.getMessage());
    }
    assertEquals("33333333-0000-0000-0000-000000000000", client.getPublisher("0000-0000", "20120101", "20151231"));
    assertEquals("33333333-0000-0000-0000-000000000001", client.getPublisher("0000-0001", "20120101", "20151231"));
    assertEquals(PublisherWorkflow.PRIMARY_SAFENET, client.getPublisherWorkflow("33333333-0000-0000-0000-000000000000"));
    assertEquals(PublisherWorkflow.PRIMARY_PUBLISHER, client.getPublisherWorkflow("33333333-1111-1111-1111-111111111111"));

    assertEquals(PublisherWorkflow.PRIMARY_PUBLISHER, client.getPublisherWorkflow("33333333-1111-1111-1111-111111111111"));
    assertEquals(PublisherWorkflow.PRIMARY_SAFENET, client.getPublisherWorkflow("33333333-0000-0000-0000-000000000000"));
    assertEquals("33333333-0000-0000-0000-000000000001", client.getPublisher("0000-0001", "20120101", "20151231"));
    assertEquals("33333333-0000-0000-0000-000000000000", client.getPublisher("0000-0000", "20120101", "20151231"));
    try {
      client.isUserEntitled("0000-0002", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertEquals("Error communicating with entitlement registry. Response was 500 null", e.getMessage());
    }
    assertFalse(client.isUserEntitled("0000-0001", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    assertTrue(client.isUserEntitled("0000-0000", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));

    Mockito.verify(mock, Mockito.times(2)).isUserEntitled("0000-0000", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
    Mockito.verify(mock).isUserEntitled("0000-0001", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
    Mockito.verify(mock, Mockito.times(2)).isUserEntitled("0000-0002", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
    Mockito.verify(mock).getPublisher("0000-0000", "20120101", "20151231");
    Mockito.verify(mock).getPublisher("0000-0001", "20120101", "20151231");
    Mockito.verify(mock).getPublisherWorkflow("33333333-0000-0000-0000-000000000000");
    Mockito.verify(mock).getPublisherWorkflow("33333333-1111-1111-1111-111111111111");
  }

}


