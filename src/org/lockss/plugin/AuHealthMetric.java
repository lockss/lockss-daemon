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
