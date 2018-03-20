package org.lockss.entitlement;

/**
 * When running a network where content is not available to all users, agreements with the publishers may limit how that content should be used
 * This enum encapsulates the different options, allowing the EntitlementRegistry to determine, based on the publisher and other information where any content should be served from
 */
public enum PublisherWorkflow {
  /* Serve the content from LOCKSS first, only attempting to pass the request to the publisher if it cannot be found */
  PRIMARY_LOCKSS,
  /* Pass the request to the publisher first, only attempting to use the archived copy if the publisher does not serve it */
  PRIMARY_PUBLISHER,
  /* Pass the request to the publisher first, if the publisher does not serve it then display some error */
  LIBRARY_NOTIFICATION
};

