package org.lockss.plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lockss.app.LockssDaemon;
import org.lockss.plugin.AuHealthMetric.HealthMetric;
import org.lockss.plugin.AuHealthMetric.PreservationStatus;
import org.lockss.state.AuState;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockNodeManager;

import static org.lockss.plugin.AuHealthMetric.HealthMetric.*;


public class TestAuHealthMetric extends LockssTestCase {

  /** 
   * List of the metrics which are actually used, and can be expected to be 
   * non-null.
   */
  final List<HealthMetric> implementedMetrics = Arrays.asList(
      SuccessfulCrawl, SubstanceState, TimeSinceLastPoll, PollAgreement
  );

  /**
   * Map expected classes to instances of inappropriate types for testing.
   * There must be a mapping for each of the types expected in HealthMetrics. 
   */
  final Map<Class, Object> badClassMap = new HashMap<Class, Object>() {{
    put(Integer.class, new Double(0.2));
    put(AuState.class, new Object());
    put(Double.class,  new Integer(2)); 
    put(Boolean.class, new Integer(1));
    put(Long.class, new Float(0.1));
  }};

  MockLockssDaemon daemon;
  ArchivalUnit au1, au2, au3, au4;
  List<ArchivalUnit> aus;
  MockNodeManager nodeManager;
  Map<HealthMetric, ?> brokenMetrics;
  
  protected void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon(); 
    au1 = MockArchivalUnit.newInited(daemon);
    au2 = MockArchivalUnit.newInited(daemon);
    au3 = MockArchivalUnit.newInited(daemon);
    au4 = MockArchivalUnit.newInited(daemon);
    aus = Arrays.asList(au1, au2, au3, au4);
    nodeManager = new MockNodeManager();
    for (ArchivalUnit au : aus) {
      daemon.setNodeManager(nodeManager, au); 
    }
    
    brokenMetrics = new HashMap<HealthMetric, Object>() {{
      for (HealthMetric hm : implementedMetrics) {
	put(hm, badClassMap.get(hm.expectedClass));
      }
    }};
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  
  public final void testGetHealthMetrics() {
    // There should be a non-null value for each metric
    Map<HealthMetric, ?> metrics = AuHealthMetric.getHealthMetrics(au1);
    for (HealthMetric hm : implementedMetrics) {
      assertNotNull(metrics.get(hm));
    }
  }

  public final void testGetAggregateHealth() {
    double h = AuHealthMetric.getAggregateHealth(aus);
    double totalIndividualHealth = 0;
    for (ArchivalUnit au : aus) {
      totalIndividualHealth += AuHealthMetric.getHealth(au); 
    }
    System.out.println("Aggregate health "+h);
    assertEquals(totalIndividualHealth/aus.size(), h, 0.00001);
  }

  // Note that getHealth() is a simple wrapper for interpret() 
  public final void testGetHealth() {
    for (ArchivalUnit au : aus) {
      double h = AuHealthMetric.getHealth(au);
      // Ensure the health does not exceed max health of the preservation status
      PreservationStatus ps = PreservationStatus.interpret(AuHealthMetric.getHealthMetrics(au));
      assertTrue(h < ps.maxHealth);
      assertTrue(h >= 0);
      // No poll - expect < PreservationStatus.Unknown.maxHealth;
      if (ps == PreservationStatus.Unknown) {} 
	//
      
    }    
  }

  // Note that getHealth() is a simple wrapper for interpret(), so this
  // essentially duplicates the tests there.
  public final void testInterpret() {
    for (ArchivalUnit au : aus) {
      Map<HealthMetric, ?> metrics = AuHealthMetric.getHealthMetrics(au);
      double h = AuHealthMetric.interpret(metrics);
      System.out.println("Health "+h);
    }
    
    // Try interpret/getHealth on a broken set of metrics
    double h = AuHealthMetric.interpret(brokenMetrics);
    assertTrue(h == AuHealthMetric.UNKNOWN_HEALTH);
  }

}
