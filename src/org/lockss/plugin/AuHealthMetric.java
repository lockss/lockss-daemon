/*
 * $Id: AuHealthMetric.java,v 1.2 2011-08-11 16:52:38 easyonthemayo Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.lockss.app.LockssDaemon;
import org.lockss.state.AuState;
import org.lockss.state.SubstanceChecker;

/**
 * Enums and static methods for creating and manipulating a set of metrics 
 * which represent the health of an ArchivalUnit. The metrics can be 
 * interpreted to give a PreservationStatus, an enum which currently maps 
 * health metrics to a status with a dummy health value.
 * <p>
 * This class is experimental, providing mostly dummy values but supporting 
 * early implementations which make use of the metrics to decide upon the 
 * preservation status of an ArchivalUnit.
 * <p>
 * The format is very likely to change, and eventually there will be a
 * configurable polynomial/expression which informs the interpretation of the 
 * metrics. 
 *  
 *
 */
public class AuHealthMetric {
 
  /**
   * Representation of the preservation status of an AU based on its state with 
   * respect to crawls and substance. Each status currently has an arbitrary 
   * health value associated with it for testing.
   */
  public static enum PreservationStatus {
    HasContent(1), HasNoContent(0.2f), CrawledUnknownContent(0.4f), UncrawledUnknownContent(0.2f), Unknown(0);
    
    /**
     * The health associated with the status.
     */
    public final float health;
    
    /**
     * Create a PreservationStatus with a specific health value.
     * @param health the health associated with the status
     */
    private PreservationStatus(float health) { this.health = health; }
    
    /**
     * Interpret an AuState object directly to produce a PreservationStatus.
     * @param state an AuState representing the state of an AU
     * @return the PreservationStatus of the AU 
     */
    public static PreservationStatus interpret(AuState state) {
      return interpret(state.hasCrawled(), state.getSubstanceState());
    }   

    /**
     * Interpret a pair of values to produce a PreservationStatus.
     * @param hasCrawled whether the AU has been crawled
     * @param substanceState the substance state of the AU
     * @return a PreservationStatus based on analysis of the values
     */
    public static PreservationStatus interpret(boolean hasCrawled, SubstanceChecker.State substanceState) {
      switch(substanceState) {
      case Yes:     return HasContent;
      case No:      return HasNoContent;
      case Unknown: return hasCrawled ? CrawledUnknownContent : UncrawledUnknownContent;
      default:      return Unknown;
      }
    }

    /**
     * Interpret a map of HealthMetrics to produce a PreservationStatus.
     * @param metric a map of metrics to values
     * @return a PreservationStatus based on analysis of the map
     */
    public static PreservationStatus interpret(Map<HealthMetric, ?> metric) {
      try {
	boolean hasCrawled = (Boolean)metric.get(HealthMetric.SuccessfulCrawl);
	SubstanceChecker.State substanceState = (SubstanceChecker.State)metric.get(HealthMetric.SubstanceState);
	return interpret(hasCrawled, substanceState);
      } catch (ClassCastException e) {
	return PreservationStatus.Unknown;
      }
    }

  }
  
  
  /**
   * An enum of all the keys to the HealthMetric map.
   */
  public static enum HealthMetric {
    SubstanceState, PercentAgreement, TimeSinceLastPoll, SuccessfulCrawl, 
    AvailableFromPublisher, NumberOfArticles, ExpectedSizeAgreement; 
  }
  
  /**
   * A preliminary method to get a map of HealthMetrics from an ArchivalUnit.
   * This function will add more metrics as they becoms available;
   * this is just an initial implementation with mostly dummy values.
   *   
   * @param au an ArchivalUnit
   * @return a map of HealthMetrics to values
   */
  public static Map<HealthMetric, ?> getHealthMetrics(ArchivalUnit au) {
    Map<HealthMetric, Object> metrics = new HashMap<HealthMetric, Object>();
    AuState state = LockssDaemon.getLockssDaemon().getNodeManager(au).getAuState();
    metrics.put(HealthMetric.SuccessfulCrawl, state.hasCrawled());
    metrics.put(HealthMetric.SubstanceState, state.getSubstanceState());
    metrics.put(HealthMetric.TimeSinceLastPoll, Calendar.getInstance().getTimeInMillis() - state.getLastPollStart());    
    return metrics;
  }
  
}
