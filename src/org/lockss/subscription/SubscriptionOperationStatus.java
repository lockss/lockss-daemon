/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.subscription;

import java.util.ArrayList;
import java.util.List;
import org.lockss.remote.RemoteApi.BatchAuStatus;

/**
 * Status representation of a subscription operation.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SubscriptionOperationStatus {
  /*
   * The status entry for the total subscription option.
   */
  private PublisherStatusEntry totalSubscriptionStatusEntry = null;

  /*
   * The list of publisher subscription status entries.
   */
  private List<PublisherStatusEntry> publisherSubscriptionStatusEntries =
      new ArrayList<PublisherStatusEntry>();

  /*
   * The list of status entries resulting from the operation.
   */
  private List<StatusEntry> statusEntries = new ArrayList<StatusEntry>();

  /**
   * Provides the status entry for the total subscription option.
   * 
   * @return a PublisherStatusEntry with the status entry.
   */
  public PublisherStatusEntry getTotalSubscriptionStatusEntry() {
    return totalSubscriptionStatusEntry;
  }

  /**
   * Provides the list of publisher subscription status entries.
   * 
   * @return a List<PublisherStatusEntry> with the status entries.
   */
  public List<PublisherStatusEntry> getPublisherSubscriptionStatusEntries() {
    return publisherSubscriptionStatusEntries;
  }

  /**
   * Provides the list of status entries resulting from the operation.
   * 
   * @return a List<StatusEntry> with the status entries.
   */
  public List<StatusEntry> getStatusEntries() {
    return statusEntries;
  }

  /**
   * Creates the status entry for the total subscription option.
   * 
   * @param subscriptionSuccess
   *          A boolean with the indication of whether the processing of the
   *          subscription succeeded.
   * @param subscriptionErrorMessage
   *          A String with an error message if the processing of the
   *          subscription failed.
   * @param subscriptionStatus
   *          A Boolean with the publisher subscription status.
   * @return a PublisherStatusEntry with the status entry just created.
   */
  public PublisherStatusEntry createTotalSubscriptionStatusEntry(
      boolean subscriptionSuccess, String subscriptionErrorMessage,
      Boolean subscriptionStatus) {
    totalSubscriptionStatusEntry =
	new PublisherStatusEntry(SubscriptionManager.ALL_PUBLISHERS_NAME,
	subscriptionSuccess, subscriptionErrorMessage, subscriptionStatus);

    return totalSubscriptionStatusEntry;
  }

  /**
   * Adds a new empty publisher status entry.
   * 
   * @return a PublisherStatusEntry with the status entry just created.
   */
  public PublisherStatusEntry addPublisherStatusEntry() {
    PublisherStatusEntry entry = new PublisherStatusEntry();
    publisherSubscriptionStatusEntries.add(entry);

    return entry;
  }

  /**
   * Adds a new fully-populated publisher status entry.
   * 
   * @param publisherName
   *          A String with the name of the publisher involved in the
   *          subscription.
   * @param subscriptionSuccess
   *          A boolean with the indication of whether the processing of the
   *          subscription succeeded.
   * @param subscriptionErrorMessage
   *          A String with an error message if the processing of the
   *          subscription failed.
   * @param subscriptionStatus
   *          A Boolean with the publisher subscription status.
   * @return a PublisherStatusEntry with the status entry just created.
   */
  public PublisherStatusEntry addPublisherStatusEntry(String publisherName,
      boolean subscriptionSuccess, String subscriptionErrorMessage,
      Boolean subscriptionStatus) {
    PublisherStatusEntry entry = new PublisherStatusEntry(publisherName,
	subscriptionSuccess, subscriptionErrorMessage, subscriptionStatus);
    publisherSubscriptionStatusEntries.add(entry);

    return entry;
  }

  /**
   * Adds a new empty status entry.
   * 
   * @return a StatusEntry with the status entry just created.
   */
  public StatusEntry addStatusEntry() {
    StatusEntry entry = new StatusEntry();
    statusEntries.add(entry);

    return entry;
  }

  /**
   * Adds a new successful status entry.
   * 
   * @param publicationName
   *          A String with the name of the publication involved in the
   *          subscription.
   * @param auStatus
   *          A BatchAuStatus with the status resulting from configuring
   *          archival units.
   * @return a StatusEntry with the status entry just created.
   */
  public StatusEntry addStatusEntry(String publicationName,
      BatchAuStatus auStatus) {
    StatusEntry entry = new StatusEntry(publicationName, auStatus);
    statusEntries.add(entry);

    return entry;
  }

  /**
   * Adds a new fully-populated status entry.
   * 
   * @param publicationName
   *          A String with the name of the publication involved in the
   *          subscription.
   * @param subscriptionSuccess
   *          A boolean with the indication of whether the processing of the
   *          subscription succeeded.
   * @param subscriptionErrorMessage
   *          A String with an error message if the processing of the
   *          subscription failed.
   * @param auStatus
   *          A BatchAuStatus with the status resulting from configuring
   *          archival units.
   * @return a StatusEntry with the status entry just created.
   */
  public StatusEntry addStatusEntry(String publicationName,
      boolean subscriptionSuccess, String subscriptionErrorMessage,
      BatchAuStatus auStatus) {
    StatusEntry entry = new StatusEntry(publicationName, subscriptionSuccess,
	subscriptionErrorMessage, auStatus);
    statusEntries.add(entry);

    return entry;
  }

  @Override
  public String toString() {
    return "SubscriptionOperationStatus [totalSubscriptionStatusEntry="
	+ totalSubscriptionStatusEntry + ", publisherSubscriptionStatusEntries="
	+ publisherSubscriptionStatusEntries + ", statusEntries="
	+ statusEntries + "]";
  }

  /**
   * Status representation of the operation on one publisher subscription.
   * 
   * @author Fernando Garcia-Loygorri
   */
  public static class PublisherStatusEntry {
    /*
     * The name of the publisher involved in the subscription.
     */
    private String publisherName;

    /*
     * The indication of whether the processing of the subscription succeeded.
     */
    private boolean subscriptionSuccess = true;

    /*
     * The error message if the processing of the subscription failed.
     */
    private String subscriptionErrorMessage = null;

    /*
     * The publisher subscription status.
     */
    private Boolean subscriptionStatus;

    /**
     * Default constructor.
     */
    PublisherStatusEntry() {
    }

    /**
     * Constructor of a new fully-populated status entry.
     * 
     * @param publisherName
     *          A String with the name of the publisher involved in the
     *          subscription.
     * @param subscriptionSuccess
     *          A boolean with the indication of whether the processing of the
     *          subscription succeeded.
     * @param subscriptionErrorMessage
     *          A String with an error message if the processing of the
     *          subscription failed.
     * @param subscriptionStatus
     *          A Boolean with the publisher subscription status.
     */
    PublisherStatusEntry(String publisherName, boolean subscriptionSuccess,
	String subscriptionErrorMessage, Boolean subscriptionStatus) {
      this.publisherName = publisherName;
      this.subscriptionSuccess = subscriptionSuccess;
      this.subscriptionErrorMessage = subscriptionErrorMessage;
      this.subscriptionStatus = subscriptionStatus;
    }

    public boolean isSubscriptionSuccess() {
      return subscriptionSuccess;
    }

    public void setSubscriptionSuccess(boolean subscriptionSuccess) {
      this.subscriptionSuccess = subscriptionSuccess;
    }

    public String getSubscriptionErrorMessage() {
      return subscriptionErrorMessage;
    }

    public void setSubscriptionErrorMessage(String subscriptionErrorMessage) {
      this.subscriptionErrorMessage = subscriptionErrorMessage;
    }

    public String getPublisherName() {
      return publisherName;
    }

    public void setPublisherName(String publisherName) {
      this.publisherName = publisherName;
    }

    public Boolean getSubscriptionStatus() {
      return subscriptionStatus;
    }

    public void setSubscriptionStatus(Boolean subscriptionStatus) {
      this.subscriptionStatus = subscriptionStatus;
    }

    @Override
    public String toString() {
      return "PublisherStatusEntry [publisherName=" + publisherName
	  + ", subscriptionSuccess=" + subscriptionSuccess
	  + ", subscriptionFailureMessage=" + subscriptionErrorMessage
	  + ", subscriptionStatus=" + subscriptionStatus + "]";
    }
  }

  /**
   * Status representation of the operation on one subscription.
   * 
   * @author Fernando Garcia-Loygorri
   */
  public static class StatusEntry {
    /*
     * The name of the publication involved in the subscription.
     */
    private String publicationName;

    /*
     * The indication of whether the processing of the subscription succeeded.
     */
    private boolean subscriptionSuccess = true;

    /*
     * The error message if the processing of the subscription failed.
     */
    private String subscriptionErrorMessage = null;

    /*
     * The status resulting from configuring archival units.
     */
    private BatchAuStatus auStatus;

    /**
     * Default constructor.
     */
    StatusEntry() {
    }

    /**
     * Constructor of a new successful status entry.
     * 
     * @param publicationName
     *          A String with the name of the publication involved in the
     *          subscription.
     * @param auStatus
     *          A BatchAuStatus with the status resulting from configuring
     *          archival units.
     */
    StatusEntry(String publicationName, BatchAuStatus auStatus) {
      this.publicationName = publicationName;
      this.auStatus = auStatus;
    }

    /**
     * Constructor of a new fully-populated status entry.
     * 
     * @param publicationName
     *          A String with the name of the publication involved in the
     *          subscription.
     * @param subscriptionSuccess
     *          A boolean with the indication of whether the processing of the
     *          subscription succeeded.
     * @param subscriptionErrorMessage
     *          A String with an error message if the processing of the
     *          subscription failed.
     * @param auStatus
     *          A BatchAuStatus with the status resulting from configuring
     *          archival units.
     */
    StatusEntry(String publicationName, boolean subscriptionSuccess,
	String subscriptionErrorMessage, BatchAuStatus auStatus) {
      this.publicationName = publicationName;
      this.subscriptionSuccess = subscriptionSuccess;
      this.subscriptionErrorMessage = subscriptionErrorMessage;
      this.auStatus = auStatus;
    }

    public boolean isSubscriptionSuccess() {
      return subscriptionSuccess;
    }

    public void setSubscriptionSuccess(boolean subscriptionSuccess) {
      this.subscriptionSuccess = subscriptionSuccess;
    }

    public String getSubscriptionErrorMessage() {
      return subscriptionErrorMessage;
    }

    public void setSubscriptionErrorMessage(String subscriptionErrorMessage) {
      this.subscriptionErrorMessage = subscriptionErrorMessage;
    }

    public String getPublicationName() {
      return publicationName;
    }

    public void setPublicationName(String publicationName) {
      this.publicationName = publicationName;
    }

    public BatchAuStatus getAuStatus() {
      return auStatus;
    }

    public void setAuStatus(BatchAuStatus auStatus) {
      this.auStatus = auStatus;
    }

    @Override
    public String toString() {
      return "StatusEntry [publicationName=" + publicationName
	  + ", subscriptionSuccess=" + subscriptionSuccess
	  + ", subscriptionFailureMessage=" + subscriptionErrorMessage
	  + ", auStatus=" + auStatus + "]";
    }
  }
}
