package org.lockss.safenet;

import java.io.IOException;

import org.lockss.app.LockssManager;

public interface EntitlementRegistryClient extends LockssManager {
  boolean isUserEntitled(String issn, String institution, String start, String end) throws IOException;
  String getInstitution(String scope) throws IOException;
  String getPublisher(String issn, String start, String end) throws IOException;
  PublisherWorkflow getPublisherWorkflow(String publisherGuid) throws IOException;
}
