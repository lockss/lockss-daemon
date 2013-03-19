/*
 * $Id: AuHealthMetric.java,v 1.7 2013-03-19 04:26:53 tlipkis Exp $
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lockss.app.LockssDaemon;
import org.lockss.state.AuState;
import org.lockss.state.SubstanceChecker;
import org.lockss.state.SubstanceChecker.State;
import org.lockss.util.*;

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
 * @deprecated Replaced by a non-dummy version in org.lockss.daemon
 */
public class AuHealthMetric {

  private static final Logger logger = Logger.getLogger("AuHealthMetric");

  /**
   * The max number of milliseconds that can pass before an AU cannot be safely
   * considered to have max health. If the last poll was longer ago than this 
   * figure, the health gets discounted.
   */
  static final long MAX_ACCEPTABLE_POLL_HIATUS = 2592000000l; // 30 days
  /** 
   * How much to discount the maximum possible health by if the last poll is 
   * too far in the past. 
   */
  static final float DISTANT_POLL_DISCOUNT = 0.1f;
  /** 
   * How much to discount the maximum possible health by if no poll has 
   * occurred.
   */
  static final float NO_POLL_DISCOUNT = 0.2f;
  /** 
   * A value returned to indicate an unknown health value. This should be used
   * to indicate an inability to calculate health, usually due to an error.
   * If the health is unknown due to, for example, lack of polls, a maximum is 
   * imposed by {@link PreservationStatus} Unknown.
   */
  public static final int UNKNOWN_HEALTH = -1;
  /** A value returned to indicate that a poll has not occurred. */
  public static final long NEVER_POLLED = -1;

  /**
   * Representation of the preservation status of an AU based on its state with 
   * respect to crawls and substance. Each status currently has an arbitrary 
   * health value associated with it for testing.
   */
  public static enum PreservationStatus {
    HasContent(1), 
    HasNoContent(0.3f),
    CrawledUnknownContent(0.7f), 
    UncrawledUnknownContent(0.4f),
    Unknown(0.2f)
    ;
    
    /** The maximum possible health associated with the status. */
    public final float maxHealth;
    
    /**
     * Create a PreservationStatus with a specific health value.
     * @param health the health associated with the status
     */
    private PreservationStatus(float health) { this.maxHealth = health; }
    
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
    SubstanceState(AuState.class),
    PollAgreement(Double.class),
    TimeSinceLastPoll(Long.class),
    SuccessfulCrawl(Boolean.class),
    AvailableFromPublisher(Boolean.class),
    NumberOfArticles(Integer.class),
    ExpectedSizeAgreement(Double.class)
    ;
    public final Class expectedClass;
    private HealthMetric(Class c) { this.expectedClass = c; }
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
    long lps = state.getLastPollStart();
    if (lps != -1) {
      metrics.put(HealthMetric.TimeSinceLastPoll, TimeBase.msSince(lps));
    } else {
      metrics.put(HealthMetric.TimeSinceLastPoll, NEVER_POLLED);
    }
    // Use creation time to inform the metrics also? This would be useful to
    // inform the interpretation of TimeSinceLastPoll
    //long ct = state.getAuCreationTime();
    // Check for repository exception -1
    //if (ct != -1) 

    // Poll agreement is -1 if not calculated:
    metrics.put(HealthMetric.PollAgreement, state.getV3Agreement()); 
    return metrics;
  }

  /**
   * For a list of AUs, work out their individual health and produce an
   * aggregate value. The aggregation algorithm is currently just to take an 
   * average. If any AU's health is unknown, the aggregate health is unknown.
   * 
   * @param aus the aus over which to aggregate health
   * @return a value between 0 and 1, or -1 if health is unknown
   */
  public static double getAggregateHealth(List<ArchivalUnit> aus) {
    double total = 0;
    int auCount = 0;
    for (ArchivalUnit au : aus) {
      double h = getHealth(au);
      if (h==UNKNOWN_HEALTH) return UNKNOWN_HEALTH;
      auCount++;
      total += h;
    }
//     DecimalFormat df = new DecimalFormat("#.##");
//     return Double.valueOf(df.format(total/auCount));
    return total/auCount;
  }
  
  /**
   * Get the health of an ArchivalUnit. Provides a dummy value based on the
   * AU's HealthMetrics. 
   * @param au an ArchivalUnit
   * @return the calculated health of the AU, or -1 if there was an error 
   */
  public static double getHealth(ArchivalUnit au) {
    //return PreservationStatus.interpret(getHealthMetrics(au)).health;
    return interpret(getHealthMetrics(au));
  }

  /**
   * Interpret a set of HealthMetrics to produce a health value.
   * This is a pretty arbitrary mix of calculations which will just
   * give us a range of dummy values for testing.
   * <p>
   * There are some situations where the health metrics should perhaps suggest
   * returning a value representing "unknown", rather than attempting to
   * provide some kind of midpoint as currently happens. 
   * 
   * @param metrics a map of metrics representing the state of an AU
   * @return the calculated health of the AU, or -1 if there was an error 
   */
  public static double interpret(Map<HealthMetric, ?> metrics) {
    try {
      // The preservation status interprets the metrics SuccessfulCrawl 
      // and SubstanceState to provide a maximum possible health
      PreservationStatus ps = PreservationStatus.interpret(metrics);
      float max = ps.maxHealth;
      
      // If time since last poll is too great, discount the possible max  
      long tslp = (Long)metrics.get(HealthMetric.TimeSinceLastPoll);
      if (tslp == NEVER_POLLED) {
	max = max * (1-NO_POLL_DISCOUNT);
      }
      if (tslp > MAX_ACCEPTABLE_POLL_HIATUS) {
	max = max * (1-DISTANT_POLL_DISCOUNT);
      }
      // We may want to check how long it has been since the AU was created,
      // and be more lenient if the max poll hiatus is greater than the 
      // lifetime of the AU.
      
      // The remaining metrics provide values representing a proportional health 
      double pa = (Double)metrics.get(HealthMetric.PollAgreement);
      if (pa == -1) {
	// Percent agreement should only be -1 when no polls have completed?
	// Multiply the max by a not very clever default value:
	//return max * PreservationStatus.Unknown.maxHealth;
	//return max * PreservationStatus.CrawledUnknownContent.maxHealth;
	return max * ps.maxHealth;
	// Note that this discount approach means that an AU with unknown 
	// health can return a higher value than one with known bad health.
      } else {
	return pa * max;
      }
    } catch (ClassCastException e) {
      logger.warning("Exception retrieving HealthMetric", e);      
      return UNKNOWN_HEALTH;
    }
  }

  /**
   * Calculate the inclusion threshold for AUs based on max health and 
   * discounts. This depends closely on how the metrics are interpreted
   * to yield the health value.
   * <p>
   * An alternative method would be to set up a set of metrics representing 
   * minimum acceptable values, and run it through interpret().
   * 
   * @return
   */
  public static float calculateInclusionThreshold() {
    // Calculate the minimum level which is acceptable for a "preserved" AU
    //return 0.7f;

    // Allow inclusion when an AU has content but has not been polled (recently)
    float threshold = Math.min(
	PreservationStatus.HasContent.maxHealth * (1-NO_POLL_DISCOUNT),
	PreservationStatus.HasContent.maxHealth * (1-DISTANT_POLL_DISCOUNT)
    );
    // Allow inclusion when an AU has crawled but its content is unknown (?)
    threshold = Math.min(
	threshold, PreservationStatus.CrawledUnknownContent.maxHealth
    );
    return threshold;
  }   
  
}
