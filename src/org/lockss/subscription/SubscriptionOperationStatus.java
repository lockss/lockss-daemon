/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
   * The list of status entries resulting from the operation.
   */
  private List<StatusEntry> statusEntries = new ArrayList<StatusEntry>();

  /**
   * Provides the list of status entries resulting from the operation.
   * 
   * @return a List<StatusEntry> with the status entries.
   */
  public List<StatusEntry> getStatusEntries() {
    return statusEntries;
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
   * @param susbcriptionSuccess
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
      boolean susbcriptionSuccess, String subscriptionErrorMessage,
      BatchAuStatus auStatus) {
    StatusEntry entry = new StatusEntry(publicationName, susbcriptionSuccess,
	subscriptionErrorMessage, auStatus);
    statusEntries.add(entry);

    return entry;
  }

  @Override
  public String toString() {
    return "SubscriptionOperationStatus [statusEntries=" + statusEntries + "]";
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
    private boolean susbcriptionSuccess = true;

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
     * @param susbcriptionSuccess
     *          A boolean with the indication of whether the processing of the
     *          subscription succeeded.
     * @param subscriptionErrorMessage
     *          A String with an error message if the processing of the
     *          subscription failed.
     * @param auStatus
     *          A BatchAuStatus with the status resulting from configuring
     *          archival units.
     */
    StatusEntry(String publicationName, boolean susbcriptionSuccess,
	String subscriptionErrorMessage, BatchAuStatus auStatus) {
      this.publicationName = publicationName;
      this.susbcriptionSuccess = susbcriptionSuccess;
      this.subscriptionErrorMessage = subscriptionErrorMessage;
      this.auStatus = auStatus;
    }

    public boolean isSusbcriptionSuccess() {
      return susbcriptionSuccess;
    }

    public void setSusbcriptionSuccess(boolean susbcriptionSuccess) {
      this.susbcriptionSuccess = susbcriptionSuccess;
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
	  + ", susbcriptionSuccess=" + susbcriptionSuccess
	  + ", subscriptionFailureMessage=" + subscriptionErrorMessage
	  + ", auStatus=" + auStatus + "]";
    }
  }
}
