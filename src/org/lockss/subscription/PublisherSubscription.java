/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Representation of a publisher subscription.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class PublisherSubscription implements Comparable<PublisherSubscription>
{
  private Publisher publisher;
  private Long publisherSubscriptionSeq;
  private Boolean subscribed;

  public Publisher getPublisher() {
    return publisher;
  }
  public void setPublisher(Publisher publisher) {
    this.publisher = publisher;
  }
  public Long getPublisherSubscriptionSeq() {
    return publisherSubscriptionSeq;
  }
  public void setPublisherSubscriptionSeq(Long publisherSubscriptionSeq) {
    this.publisherSubscriptionSeq = publisherSubscriptionSeq;
  }
  public Boolean getSubscribed() {
    return subscribed;
  }
  public void setSubscribed(Boolean subscribed) {
    this.subscribed = subscribed;
  }

  /**
   * Comparator.
   */
  public int compareTo(PublisherSubscription other) {
    // Sort by publisher name.
    return getPublisher().getPublisherName()
	.compareTo(other.getPublisher().getPublisherName());
  }

  @Override
  public String toString() {
    return "PublisherSubscription [publisher=" + publisher
	+ ", publisherSubscriptionSeq=" + publisherSubscriptionSeq
	+ ", subscribed=" + subscribed + "]";
  }
}
