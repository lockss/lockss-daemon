/*
 * $Id: AuHealthMetric.java,v 1.4 2011-08-30 04:41:21 tlipkis Exp $
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
import javax.script.*;

import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

/**
 * Metric that represents the preservation status, or "health" of an AU or
 * a collection of AUs.
 *
 * Health is a float (expected to be in the range 0.0 - 1.0), calculated
 * from a number of individual characteristics (poll agreement & recency,
 * substance state, etc) by a configurable expression or script.  The
 * individual characteristics are also available separately.
 */
public class AuHealthMetric {

  private static final Logger log = Logger.getLogger("AuHealthMetric");

  static final String PREFIX = Configuration.PREFIX + "auHealth.";

  /** Script language. */
  public static final String PARAM_SCRIPT_LANGUAGE = PREFIX + "scriptLanguage";
  public static final String DEFAULT_SCRIPT_LANGUAGE = "JavaScript";

  /** Threshold for inclusion of AUs as "healthy". */
  public static final String PARAM_INCLUSION_THRESHOLD =
    PREFIX + "inclusionThreshold";
  public static final double DEFAULT_INCLUSION_THRESHOLD = 0.7;

  /** A script (in the language specified by
   * org.lockss.auHealth.scriptLanguage) that references input values from
   * the set defined by #HealthMetric and returns a float.  */
  public static final String PARAM_HEALTH_EXPR = PREFIX + "healthExpr";
  public static final String DEFAULT_HEALTH_EXPR =
    "  function h1() {\n" +
    "    switch (SubstanceState) {\n" +
    "    case \"Yes\":\n" +
    "      return 1.0;\n" +
    "    case \"No\":\n" +
    "      return 0.3;\n" +
    "    case \"Unknown\":\n" +
    "    default:\n" +
    "      if (SuccessfulCrawl) {\n" +
    "        return 0.7;\n" +
    "      } else {\n" +
    "        return 0.4;\n" +
    "      }\n" +
    "    }\n" +
    "  }\n" +
    "  function h2() {\n" +
    "    var val = h1();\n" +
    "    if (DaysSinceLastPoll < 0) {\n" +
    "      val = val * 0.8;\n" +
    "    } else {\n" +
    "      if (DaysSinceLastPoll > 90) {\n" +
    "        val = val * 0.9;\n" +
    "      }\n" +
    "      val = val * PollAgreement;\n" +
    "    }\n" +
    "    return val;\n" +
    "  }\n" +
    "  h2();\n";

  /** Set of variables available to the script. */
  public static enum HealthMetric {
    /** SubstanceState is "Yes", "No" or "Unknown". */
    SubstanceState,
    /** PollAgreement is a float between 0.0 and 1.0. */
    PollAgreement,
    /** HighestPollAgreement is a float between 0.0 and 1.0. */
    HighestPollAgreement,
    /** DaysSinceLastPoll is an int (-1 if never polled).   */
    DaysSinceLastPoll,
    /** SuccessfulCrawl is a boolean. */
    SuccessfulCrawl,
    /** AvailableFromPublisher is a boolean. */
    AvailableFromPublisher;
//     NumberOfArticles,
//     ExpectedSizeAgreement;
  }

  private static String scriptLanguage = DEFAULT_SCRIPT_LANGUAGE;
  private static String healthExpr = DEFAULT_HEALTH_EXPR;
  private static double inclusionThreshold = DEFAULT_INCLUSION_THRESHOLD;
  private static ScriptEngineManager sem;

  private Bindings bindings;
  private ArchivalUnit au;
  private AuState aus;

  
  /** Create and return an AuHealthMetric for the given AU */
  public static AuHealthMetric getAuHealthMetric(ArchivalUnit au) {
    AuHealthMetric res = new AuHealthMetric(au);
    return res;
  }

  /**
   * Get the health of an ArchivalUnit.
   * @param au an ArchivalUnit
   * @return the calculated health of the AU
   * @throws HealthUnavailableException if there was an error
   */
  public static double getHealth(ArchivalUnit au)
      throws HealthUnavailableException {
    return getAuHealthMetric(au).getHealth();
  }
  
  /**
   * Get the health of an ArchivalUnit.
   * @return the calculated health of the AU
   * @throws HealthUnavailableException if there was an error
   */
  public double getHealth() throws HealthUnavailableException {
    return eval();
  }
  
  /**
   * For a list of AUs, work out their individual health and produce an
   * aggregate value. The aggregation algorithm is currently just to take
   * an average. If any AU's health is unavailable, the aggregate health is
   * unavailable.
   * 
   * @param aus the aus over which to aggregate health
   * @return a value between 0.0 and 1.0.
   * @throws HealthUnavailableException if there was an error
   */
  public static double getAggregateHealth(List<ArchivalUnit> aus)
      throws HealthUnavailableException {
    double total = 0.0;
    int auCount = 0;
    for (ArchivalUnit au : aus) {
      double h = getHealth(au);
      auCount++;
      total += h;
    }
    return total / auCount;
  }
  
  /**
   * Return the threshold for including an AU in reports.
   */
  public static double getInclusionThreshold() {
    return inclusionThreshold;
  }
  
  /**
   * Return true if health metrics are supported.
   */
  public static boolean isSupported() {
    return PlatformUtil.getInstance().hasScriptingSupport();
  }
  
  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      healthExpr = config.get(PARAM_HEALTH_EXPR, DEFAULT_HEALTH_EXPR);
      inclusionThreshold = config.getDouble(PARAM_INCLUSION_THRESHOLD,
					    DEFAULT_INCLUSION_THRESHOLD);
      if (StringUtil.isNullString(healthExpr)) {
	healthExpr = DEFAULT_HEALTH_EXPR;
      }
      scriptLanguage = config.get(PARAM_SCRIPT_LANGUAGE,
				  DEFAULT_SCRIPT_LANGUAGE);
    }
  }

  private AuHealthMetric(ArchivalUnit au) {
    this.au = au;
    this.aus = AuUtil.getAuState(au);
  }

  public SubstanceChecker.State getSubstanceState() {
    return aus.getSubstanceState();
  }

  public double getPollAgreement() {
    return aus.getV3Agreement();
  }
  
  public double getHighestPollAgreement() {
    return aus.getHighestV3Agreement();
  }
  
  public long getDaysSinceLastPoll() {
    long lastPoll = aus.getLastTopLevelPollTime();
    return lastPoll <= 0 ? -1 :
      (TimeBase.msSince(lastPoll) / Constants.DAY);
  }
    
  public boolean hasCrawled() {
    return aus.hasCrawled();
  }

  public boolean isAvailableFromPublisher() {
    return !AuUtil.isPubDown(au);
  }

  private Bindings getBindings() {
    if (bindings == null) {
      bindings = new SimpleBindings();
      bindings.put(HealthMetric.SubstanceState.name(),
		   getSubstanceState().name());
      bindings.put(HealthMetric.PollAgreement.name(), getPollAgreement());
      bindings.put(HealthMetric.HighestPollAgreement.name(),
	       getHighestPollAgreement());
      bindings.put(HealthMetric.DaysSinceLastPoll.name(),
		   getDaysSinceLastPoll());
      bindings.put(HealthMetric.SuccessfulCrawl.name(), hasCrawled());
      bindings.put(HealthMetric.AvailableFromPublisher.name(),
		   isAvailableFromPublisher());
    }
    return bindings;
  }

  ScriptEngineManager getManager() {
    if (sem == null) {
      sem = new ScriptEngineManager();
    }
    return sem;
  }

  // tk - is it expensive to create a new engine?  Could cache statically
  // if thread-safe.
  ScriptEngine getEngine() {
    return getManager().getEngineByName(scriptLanguage);
  }

  double eval() throws HealthUnavailableException {
    if (StringUtil.isNullString(healthExpr)) {
      throw new HealthScriptException("No health metric script is defined");
    }
    ScriptEngine engine = getEngine();
    if (engine == null) {
      throw new HealthScriptException("No script engine available for " +
				      scriptLanguage);
    }
    Object val = null;
    try {
      val = engine.eval(healthExpr, getBindings());
      log.debug3("val: " + val);
      if (val == null) {
	log.warning("Script returned null");
	throw new HealthValueException("Script returned null");
      }
      return ((Number)val).doubleValue();
    } catch (ScriptException e) {
      log.warning("Script error", e);
      throw new HealthScriptException(e);
    } catch (ClassCastException e) {
      String msg ="Script returned wrong type ("
	+ val.getClass().getName() + "): " + val;
      log.warning(msg, e);
      throw new HealthValueException(msg, e);
    }
  }

  /** Parent Exception thrown if health metric is unavailable for any
   * reason */
  public static class HealthUnavailableException extends Exception {
    public HealthUnavailableException() {
      super();
    }
    public HealthUnavailableException(String message) {
      super(message);
    }
    public HealthUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
    public HealthUnavailableException(Throwable cause) {
      super(cause);
    }
  }

  /** The health metric evaliation script got an error. */
  public static class HealthScriptException extends HealthUnavailableException {
    public HealthScriptException() {
      super();
    }
    public HealthScriptException(String message) {
      super(message);
    }
    public HealthScriptException(String message, Throwable cause) {
      super(message, cause);
    }
    public HealthScriptException(Throwable cause) {
      super(cause);
    }
  }

  /** The health metric evaliation script returned an illegal value. */
  public static class HealthValueException extends HealthUnavailableException {
    public HealthValueException() {
      super();
    }
    public HealthValueException(String message) {
      super(message);
    }
    public HealthValueException(String message, Throwable cause) {
      super(message, cause);
    }
    public HealthValueException(Throwable cause) {
      super(cause);
    }
  }

  // Enumerate and print details of supported script engines
  public static void main(String[] args) {
    ScriptEngineManager sem = new ScriptEngineManager();
    for (ScriptEngineFactory fact : sem.getEngineFactories()) {
      log.info("fact: " + fact.getEngineName());
      log.info(" ver: " + fact.getEngineVersion());
      log.info(" nms: " + fact.getNames());
      log.info(" lng: " + fact.getLanguageName());
      log.info(" ext: " + fact.getExtensions());
//       log.info(" mth: " + fact.getMethodCallSyntax("obj", "mth", "arg1", "arg2"));
//       log.info(" out: " + fact.getOutputStatement("to display"));
    }
  }
}
