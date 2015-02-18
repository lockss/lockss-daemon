/*
 * $Id$
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.test.*;
import org.lockss.daemon.AuHealthMetric;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.protocol.*;

import static org.lockss.util.Constants.DAY;


public class TestAuHealthMetric extends LockssTestCase {

  MockLockssDaemon daemon;
  MockArchivalUnit au1, au2, au3, au4;
  MockIdentityManager idm;
  
  protected void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon(); 

    // These values are held in statics in AuHealthMetric.  Ensure each
    // test starts with the default values.
    ConfigurationUtil.removeKeys(ListUtil.list(AuHealthMetric.PARAM_SCRIPT_LANGUAGE,
					       AuHealthMetric.PARAM_HEALTH_EXPR, 
					       AuHealthMetric.PARAM_INCLUSION_THRESHOLD));

    idm = new MockIdentityManager();
    daemon.setIdentityManager(idm);
    au1 = MockArchivalUnit.newInited(daemon);
    au2 = MockArchivalUnit.newInited(daemon);
    au3 = MockArchivalUnit.newInited(daemon);
    au4 = MockArchivalUnit.newInited(daemon);
    List<ArchivalUnit> aulist = ListUtil.list(au1, au2, au3, au4);
    for (ArchivalUnit au : aulist) {
      MockNodeManager nodeMgr = new MockNodeManager();
      daemon.setNodeManager(nodeMgr, au); 
      MockAuState maus = new MockAuState(au);
      nodeMgr.setAuState(new MockAuState(au));
    }
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  MockAuState getMaus(ArchivalUnit au) {
    return (MockAuState)AuUtil.getAuState(au);
  }

  void setupAu(MockArchivalUnit au, SubstanceChecker.State substanceState,
	       double agreement, double highestAgreement,
	       long lastPoll, long lastCrawl, boolean availableFromPublisher) {
    MockAuState aus = getMaus(au);
    aus.setSubstanceState(substanceState);
    aus.setV3Agreement(agreement);
    aus.setHighestV3Agreement(highestAgreement);
    aus.setLastTopLevelPollTime(lastPoll);
    aus.setLastCrawlTime(lastCrawl);
    try {
      au.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_DOWN.getKey(),
						     availableFromPublisher ? "true" : "false"));
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Unexpected", e);
    }
  }

  void setHealthExpr(String expr) {
    ConfigurationUtil.addFromArgs(AuHealthMetric.PARAM_HEALTH_EXPR, expr);
  }
  
  boolean noScripting(String test) {
    if (!PlatformUtil.getInstance().hasScriptingSupport()) {
      log.debug("Skipping " + test +
		" because platform has no scripting support.");
      return true;
    }
    return false;
  }

  public void testIsSupported() {
    assertEquals(PlatformUtil.getInstance().hasScriptingSupport(),
		 AuHealthMetric.isSupported());
  }

  void setNumRepairers(ArchivalUnit au, int n) {
    Map map = new HashMap();
    for (int ix = 0; ix < 3; ix++) {
      MockPeerIdentity id = new MockPeerIdentity("127.0.0."+ix);
      map.put(id, new Long(10+ix));
    }
    idm.setAgeedForAu(au, map);
  }

  public void testAccessors() {
    setupAu(au1, SubstanceChecker.State.Unknown, 0.8, 0.9, 2 * DAY, 0, true);
    TimeBase.setSimulated(5 * DAY);
    AuHealthMetric hm = AuHealthMetric.getAuHealthMetric(au1);
    assertEquals(SubstanceChecker.State.Unknown, hm.getSubstanceState());
    assertEquals(.8, hm.getPollAgreement());
    assertEquals(.9, hm.getHighestPollAgreement());
    assertEquals(3, hm.getDaysSinceLastPoll());
    assertEquals(0, hm.getNumberOfRepairers());

    setNumRepairers(au1, 3);
    assertEquals(3, hm.getNumberOfRepairers());
  }

  public void testIll()
      throws AuHealthMetric.HealthUnavailableException {
    if (noScripting("testIll")) return;
    setupAu(au1, SubstanceChecker.State.Unknown, 0.8, 0.9, 2 * DAY, 0, true);

    setHealthExpr("\"String value\"");
    try {
      AuHealthMetric.getHealth(au1);
      fail("Should throw HealthValueException");
    } catch (AuHealthMetric.HealthValueException e) {
      assertMatchesRE("wrong type", e.getMessage());
    }

    setHealthExpr("\"String value\"");
    try {
      AuHealthMetric.getHealth(au1);
      fail("Should throw HealthValueException");
    } catch (AuHealthMetric.HealthValueException e) {
      assertMatchesRE("wrong type", e.getMessage());
    }

    setHealthExpr("//comment");
    try {
      AuHealthMetric.getHealth(au1);
      fail("Should throw HealthValueException");
    } catch (AuHealthMetric.HealthValueException e) {
      assertMatchesRE("Script returned null", e.getMessage());
    }

    setHealthExpr("no_such_fn()");
    try {
      AuHealthMetric.getHealth(au1);
      fail("Should throw HealthScriptException");
    } catch (AuHealthMetric.HealthScriptException e) {
    }

    // disabled because there's a non-null default expr
//     setHealthExpr("");
//     try {
//       AuHealthMetric.getHealth(au1);
//       fail("Should throw HealthScriptException");
//     } catch (AuHealthMetric.HealthScriptException e) {
//       assertMatchesRE("No health metric script is defined", e.getMessage());
//     }

  }

  public void testNoEngine() throws AuHealthMetric.HealthUnavailableException {
    setupAu(au1, SubstanceChecker.State.Unknown, 0.8, 0.9, 2 * DAY, 0, true);
    ConfigurationUtil.addFromArgs(AuHealthMetric.PARAM_SCRIPT_LANGUAGE,
				  "NonexistentLanguage");

    setHealthExpr("1.0");
    try {
      AuHealthMetric.getHealth(au1);
      fail("Should throw HealthScriptException");
    } catch (AuHealthMetric.HealthScriptException e) {
      assertMatchesRE("No script engine available", e.getMessage());
    }
  }

  public void testExprVars()
      throws AuHealthMetric.HealthUnavailableException {
    if (noScripting("testExprVars")) return;
    setupAu(au1, SubstanceChecker.State.No, 0.8, 0.9, 3 * DAY, -1, true);
    setupAu(au2, SubstanceChecker.State.Unknown, 0.9, 1.0, 4 * DAY, 2000, false);
    TimeBase.setSimulated(5 * DAY);
    setHealthExpr("(SubstanceState == \"No\") ? 0.25 : 0.5;");
    assertEquals(0.25, AuHealthMetric.getHealth(au1));
    assertEquals(0.5, AuHealthMetric.getHealth(au2));

    setHealthExpr("PollAgreement;");
    assertEquals(0.8, AuHealthMetric.getHealth(au1));
    assertEquals(0.9, AuHealthMetric.getHealth(au2));
    setHealthExpr("HighestPollAgreement;");
    assertEquals(0.9, AuHealthMetric.getHealth(au1));
    assertEquals(1.0, AuHealthMetric.getHealth(au2));
    setHealthExpr("DaysSinceLastPoll / 3;");
    assertEquals(0.666, AuHealthMetric.getHealth(au1), .001);
    assertEquals(0.333, AuHealthMetric.getHealth(au2), .001);
    setHealthExpr("SuccessfulCrawl ? 0.6 : 0.4;");
    assertEquals(0.4, AuHealthMetric.getHealth(au1), .01);
    assertEquals(0.6, AuHealthMetric.getHealth(au2), .01);
    setHealthExpr("AvailableFromPublisher ? 0.2 : 0.4;");
    assertEquals(0.4, AuHealthMetric.getHealth(au1), .01);
    assertEquals(0.2, AuHealthMetric.getHealth(au2), .01);

    setNumRepairers(au2, 3);
    setHealthExpr("NumberOfRepairers / 4;");
    assertEquals(0.0, AuHealthMetric.getHealth(au1), .01);
    assertEquals(0.75, AuHealthMetric.getHealth(au2), .01);
  }

  public void testOutOfRangeValue()
      throws AuHealthMetric.HealthUnavailableException {
    if (noScripting("testOutOfRangeValue")) return;
    setupAu(au1, SubstanceChecker.State.No, 0.8, 0.9, 3 * DAY, -1, true);
    setupAu(au2, SubstanceChecker.State.Unknown, 0.9, 1.0, 4 * DAY, 2000, false);
    TimeBase.setSimulated(5 * DAY);
    setHealthExpr("(SubstanceState == \"No\") ? 0.5 : 1.5;");
    assertEquals(0.5, AuHealthMetric.getHealth(au1));
    try {
      AuHealthMetric.getHealth(au2);
      fail("Should throw HealthValueException");
    } catch (AuHealthMetric.HealthValueException e) {
      assertMatchesRE("returned out-of-range value", e.getMessage());
    }
    try {
      AuHealthMetric.getHealth(au1);
      fail("Should throw HealthValueException");
    } catch (AuHealthMetric.HealthValueException e) {
      assertMatchesRE("disabled because it previously returned out-of-range value", e.getMessage());
    }

    setHealthExpr("(SubstanceState == \"No\") ? 0.5 : 0.75;");
    assertEquals(0.5, AuHealthMetric.getHealth(au1));
    assertEquals(0.75, AuHealthMetric.getHealth(au2));
  }


  String fn1 =
    "  function health1() {\n" +
    "    var val;\n" +
    "    switch (SubstanceState) {\n" +
    "    case \"No\":\n" +
    "      return 0.0;\n" +
    "    case \"Yes\":\n" +
    "      val = 1.0;\n" +
    "      break;\n" +
    "    case \"Unknown\":\n" +
    "    default:\n" +
    "      if (SuccessfulCrawl) {\n" +
    "	val = 0.6;\n" +
    "      } else {\n" +
    "	val = 0.3;\n" +
    "      }\n" +
    "      break;\n" +
    "    }\n" +
    "    if (DaysSinceLastPoll > 10) {\n" +
    "      val = val * .7;\n" +
    "    }\n" +
    "    if (PollAgreement > 0.0) {\n" +
    "      // consider poll results only if at least one has finished\n" +
    "      val = val * PollAgreement;\n" +
    "    }\n" +
    "    Logger.info(\"Script returning \" + val + \" for AU \" + AuName);\n" +
    "    return val;\n" +
    "  }\n" +
    "  health1();\n";


  public void testFunction()
      throws AuHealthMetric.HealthUnavailableException {
    if (noScripting("testFunction")) return;
    setupAu(au1, SubstanceChecker.State.No, 0.8, 0.9, 5*DAY, -1, true);
    setupAu(au2, SubstanceChecker.State.Unknown, 0.9, 1.0, 10*DAY, 2000, false);
    setupAu(au3, SubstanceChecker.State.Yes, 0.9, 1.0, 4*DAY, 2000, false);
    TimeBase.setSimulated(15*DAY);
    setHealthExpr(fn1);
    assertEquals(0.0, AuHealthMetric.getHealth(au1), .001);
    assertEquals(0.54, AuHealthMetric.getHealth(au2), .001);
    assertEquals(0.63, AuHealthMetric.getHealth(au3), .001);
  }

  public void testDefaultFunction()
      throws AuHealthMetric.HealthUnavailableException {
    if (noScripting("testDefaultFunction")) return;
    setupAu(au1, SubstanceChecker.State.No, 0.8, 0.9, 5*DAY, -1, true);
    setupAu(au2, SubstanceChecker.State.Unknown, 0.9, 1.0, 10*DAY, 2000, false);
    setupAu(au3, SubstanceChecker.State.Yes, 0.9, 1.0, 4*DAY, 2000, false);
    TimeBase.setSimulated(15*DAY);
    setHealthExpr("");
    assertEquals(0.16, AuHealthMetric.getHealth(au1), .001);
    assertEquals(0.72, AuHealthMetric.getHealth(au2), .001);
    assertEquals(0.9, AuHealthMetric.getHealth(au3), .001);
  }

  public void testGetAggregateHealth()
      throws AuHealthMetric.HealthUnavailableException {
    if (noScripting("testGetAggregateHealth")) return;
    setupAu(au1, SubstanceChecker.State.No, 0.8, 0.9, 5*DAY, -1, true);
    setupAu(au2, SubstanceChecker.State.Unknown, 0.9, 1.0, 10*DAY, 2000, false);
    setupAu(au3, SubstanceChecker.State.Yes, 0.9, 1.0, 4*DAY, 2000, false);

    List<ArchivalUnit> lst = ListUtil.list(au1, au2, au3);

    double h = AuHealthMetric.getAggregateHealth(lst);
    double totalIndividualHealth = 0;
    for (ArchivalUnit au : lst) {
      totalIndividualHealth += AuHealthMetric.getHealth(au); 
    }
    assertEquals(totalIndividualHealth / lst.size(), h, 0.00001);
  }

  public void testGetInclusionThreshold() {
    assertEquals(0.7, AuHealthMetric.getInclusionThreshold());
    ConfigurationUtil.addFromArgs(AuHealthMetric.PARAM_INCLUSION_THRESHOLD,
				  "0.5");
    assertEquals(0.5, AuHealthMetric.getInclusionThreshold());
  }

}
