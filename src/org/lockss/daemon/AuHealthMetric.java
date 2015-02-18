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
import javax.script.*;

import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;

/**
 * Metric that represents the preservation status, or "health" of an AU or
 * a collection of AUs.
 *
 * Health is a floating point number in the range [0.0 - 1.0], calculated
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
    "      return 0.2;\n" +
    "    case \"Unknown\":\n" +
    "    default:\n" +
    "      if (SuccessfulCrawl) {\n" +
    "        return 0.8;\n" +
    "      } else if (AvailableFromPublisher) {\n" +
    "        return 0.3;\n" +
    "      } else {\n" +
    "        return 0.7;\n" +
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
    /** The AuHealthMetric logger. */
    Logger,
    /** The AU name. */
    AuName,
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
    AvailableFromPublisher,
    /** Estimated number of peers willing to repair the AU. */
    NumberOfRepairers;
//     NumberOfArticles,
//     ExpectedSizeAgreement;
  }

  private static String scriptLanguage = DEFAULT_SCRIPT_LANGUAGE;
  private static String healthExpr = DEFAULT_HEALTH_EXPR;
  private static double inclusionThreshold = DEFAULT_INCLUSION_THRESHOLD;
  private static ScriptEngineManager sem;
  // This flag is made volatile so changes to its value can be seen instantly
  // in other threads.
  private static volatile boolean outOfRangeValueSeen = false;

  private Bindings bindings;
  private ArchivalUnit au;
  private AuState aus;

  /** Whether the health metric's engine can compile the script for efficiency. */
  static boolean isCompilable;
  /** Whether the health metric's engine can be used thread-safely. */
  static boolean isThreadSafe;
  /**
   * A statically cached ScriptEngine. Will hold an engine if the engine
   * implementation as reported by the factory is thread-safe; otherwise the
   * variable will be null.
   */
  private static ScriptEngine theOneTrueEngine = null;
  /**
   * A statically cached, compiled version of the script. If the engine is not
   * thread-safe, or is incapable of compiling the script, this will be null.
   */
  private static CompiledScript compiledScript = null;
  /**
   * Set flags describing the capabilities of the script engine, and compile
   * the script if possible.
   */
  static {
    if (!isSupported() || !setEngineProperties()) {
      // Reset everything if scripting is not supported
      isCompilable = false;
      isThreadSafe = false;
      theOneTrueEngine = null;
      compiledScript = null;
    }
  }

  /**
   * Set the static engine, and if it is thread safe and supports compilation,
   * use it to compile the script. If the engine is null or is not thread safe,
   * the method returns <tt>false</tt> as the engine cannot be used safely.
   * 
   * @return <tt>true</tt> if a thread safe engine was set up successfully;
   */
  private static synchronized boolean setEngineProperties() {
    // Set a single static engine and establish whether it is thread-safe,
    // and whether scripts can be compiled.
    ScriptEngineManager sem = new ScriptEngineManager();
    theOneTrueEngine = sem.getEngineByName(scriptLanguage);
    // Return false if an engine could not be retrieved
    if (theOneTrueEngine==null) return false;;
    Object threading = theOneTrueEngine.getFactory().getParameter("THREADING");
    // The engine is thread-safe if the threading parameter is non-null.
    // Note however that if there is any state in the script, then we require
    // at least "THREAD-ISOLATED". See
    // http://download.oracle.com/javase/6/docs/api/javax/script/ScriptEngineFactory.html#getParameter(java.lang.String)
    isThreadSafe = threading != null;
    // The script can be compiled if scripting is supported and the engine
    // implements Compilable.
    isCompilable = isThreadSafe && theOneTrueEngine instanceof Compilable;

    // If the engine is not thread safe, we can't use a statically cached
    // instance, nor can we cache a compile script which uses the engine.
    if (!isThreadSafe) return false;
    // Compile the script if this engine supports it
    if (isCompilable) compileScript();
    return true;
  }

  /**
   * Compile the script using the engine, if possible. If there is an exception,
   * the compiledScript variable is set to null.
   */
  private static synchronized void compileScript() {
    try {
      compiledScript = ((Compilable) theOneTrueEngine).compile(healthExpr);
    } catch (Exception e) {
      compiledScript = null;
    }
  }

  
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
   * Whether an ArchivalUnit's health is above the inclusion threshold.
   * @param au an ArchivalUnit
   * @return <tt>true</tt> if the AU's health is above or equal to the threshold
   * @throws HealthUnavailableException if there was an error
   */
  public static boolean isHealthy(ArchivalUnit au)
      throws HealthUnavailableException {
    return getHealth(au) >= inclusionThreshold;
  }

  /**
   * Get the health of an ArchivalUnit.
   * @return the calculated health of the AU
   * @throws HealthUnavailableException if there was an error
   */
  public double getHealth() throws HealthUnavailableException {
    if (outOfRangeValueSeen) {
      throw new HealthValueException("Health evaluation script disabled " +
				     "because it previously returned " +
				     "out-of-range value");
    }
    long s = System.currentTimeMillis();
    double res = eval();
    log.debug(String.format("Preserved AU %s health metric eval took %sms",
        au.getName(), (System.currentTimeMillis()-s)
    ));
    if (res >= 0.0 && res <= 1.0) {
      return res;
    } else {
      outOfRangeValueSeen = true;
      throw new HealthValueException("Script returned out-of-range value: "
				     + res);
    }
  }
  
  /**
   * For a list of AUs, work out their individual health and produce an
   * aggregate value. The aggregation algorithm is currently just to take
   * an average. If any AU's health is unavailable, the aggregate health is
   * unavailable.
   * 
   * @param aus the aus over which to aggregate health
   * @return a value between 0.0 and 1.0.
   * @throws HealthUnavailableException (actually, one of its subclasses)
   * if there was an error
   */
  public static double getAggregateHealth(Collection<ArchivalUnit> aus)
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
      inclusionThreshold = config.getDouble(PARAM_INCLUSION_THRESHOLD,
					    DEFAULT_INCLUSION_THRESHOLD);
      scriptLanguage = config.get(PARAM_SCRIPT_LANGUAGE,
				  DEFAULT_SCRIPT_LANGUAGE);
      String newExpr = config.get(PARAM_HEALTH_EXPR);
      if (StringUtil.isNullString(newExpr)) {
	newExpr = DEFAULT_HEALTH_EXPR;
      }
      if (!StringUtil.equalStrings(newExpr, healthExpr)) {
	healthExpr = newExpr;
	outOfRangeValueSeen = false;
      }
      // TODO: I'm not clear whether this is the right place to run this? (NM)
      // Note that it is run in a static initialiser too
      setEngineProperties();
    }
  }

  private AuHealthMetric(ArchivalUnit au) {
    this.au = au;
    this.aus = AuUtil.getAuState(au);
  }

  /** Return the AU's SubstanceState */
  public SubstanceChecker.State getSubstanceState() {
    return aus.getSubstanceState();
  }

  /** Return the AU's most recent poll agreement */
  public double getPollAgreement() {
    return aus.getV3Agreement();
  }
  
  /** Return the AU's highest poll agreement */
  public double getHighestPollAgreement() {
    return aus.getHighestV3Agreement();
  }
  
  /** Return the number of days since the AU's last completed poll */
  public long getDaysSinceLastPoll() {
    long lastPoll = aus.getLastTopLevelPollTime();
    return lastPoll <= 0 ? -1 :
      (TimeBase.msSince(lastPoll) / Constants.DAY);
  }
    
  /** Return the estimated number of peers willing to repair this AU */
  public int getNumberOfRepairers() {
    IdentityManager idMgr = AuUtil.getDaemon(au).getIdentityManager();
    return idMgr.countCachesToRepairFrom(au);
  }

  /** Return true if the AU has at least one successful crawl */
  public boolean hasCrawled() {
    return aus.hasCrawled();
  }

  /** Return true if the AU is still available from its original site */
  public boolean isAvailableFromPublisher() {
    return !AuUtil.isPubDown(au);
  }

  private Bindings getBindings() {
    if (bindings == null) {
      bindings = new SimpleBindings();
      bindings.put(HealthMetric.Logger.name(), log);
      bindings.put(HealthMetric.AuName.name(), au.getName());

      bindings.put(HealthMetric.SubstanceState.name(),
		   getSubstanceState().name());
      bindings.put(HealthMetric.PollAgreement.name(), getPollAgreement());
      bindings.put(HealthMetric.HighestPollAgreement.name(),
	       getHighestPollAgreement());
      bindings.put(HealthMetric.DaysSinceLastPoll.name(),
		   getDaysSinceLastPoll());
      bindings.put(HealthMetric.NumberOfRepairers.name(),
		   getNumberOfRepairers());
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
    Object val = null;
    try {
      val = evalExpression();
      log.debug3("val: " + val);
      if (val == null) {
	log.warning("Script returned null");
	throw new HealthValueException("Script returned null");
      }
      return ((Number)val).doubleValue();
    } catch (ClassCastException e) {
      String msg ="Script returned wrong type ("
	+ val.getClass().getName() + "): " + val;
      log.warning(msg, e);
      throw new HealthValueException(msg, e);
    }
  }

  /**
   * Evaluate the script expression using the available method, that is, by
   * retrieving a new engine instance or by using the compiled script.
   * @return the result of the expression evaluation
   * @throws HealthScriptException
   */
  Object evalExpression() throws HealthScriptException {
    try {
      // Retrieve a new engine, or use the statically cached instance
      ScriptEngine engine = theOneTrueEngine==null ? getEngine() : theOneTrueEngine;
      if (engine == null) {
        throw new HealthScriptException("No script engine available for " +
            scriptLanguage);
      }

      // eval using the compiled script if possible
      if (isCompilable) {
        return compiledScript.eval(getBindings());
      } else {
        return engine.eval(healthExpr, getBindings());
      }
    } catch (ScriptException e) {
      log.warning("Script error", e);
      throw new HealthScriptException(e);
    }
  }

  /** Parent Exception thrown if health metric is unavailable for any
   * reason */
  public static class HealthUnavailableException extends Exception {
    HealthUnavailableException() {
      super();
    }
    HealthUnavailableException(String message) {
      super(message);
    }
    HealthUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
    HealthUnavailableException(Throwable cause) {
      super(cause);
    }
  }

  /** The health metric evaluation script got an error. */
  public static class HealthScriptException extends HealthUnavailableException {
    HealthScriptException() {
      super();
    }
    HealthScriptException(String message) {
      super(message);
    }
    HealthScriptException(String message, Throwable cause) {
      super(message, cause);
    }
    HealthScriptException(Throwable cause) {
      super(cause);
    }
  }

  /** The health metric evaluation script returned an illegal value. */
  public static class HealthValueException extends HealthUnavailableException {
    HealthValueException() {
      super();
    }
    HealthValueException(String message) {
      super(message);
    }
    HealthValueException(String message, Throwable cause) {
      super(message, cause);
    }
    HealthValueException(Throwable cause) {
      super(cause);
    }
  }

  // Enumerate and print details of supported script engines
  public static void main(String[] args) {
    ScriptEngineManager sem = new ScriptEngineManager();
    Collection<ScriptEngineFactory> facts = sem.getEngineFactories();
    if (facts.isEmpty()) {
      log.info("No script engines found");
    } else {
      for (ScriptEngineFactory fact : facts) {
	log.info("fact: " + fact.getEngineName());
	log.info(" ver: " + fact.getEngineVersion());
	log.info(" nms: " + fact.getNames());
	log.info(" lng: " + fact.getLanguageName());
	log.info(" ext: " + fact.getExtensions());
        log.info(" thr: " + fact.getParameter("THREADING"));
        log.info(" its: " + (isThreadSafe ? "" : "not ") + "thread safe");
// 	log.info(" mth: " + fact.getMethodCallSyntax("obj", "mth", "arg1", "arg2"));
// 	log.info(" out: " + fact.getOutputStatement("to display"));
        ScriptEngine eng = fact.getScriptEngine();
        //log.info("eng:  " + eng.getClass());
        log.info(" cmp: " + (isCompilable ? "" : "not ") + "compilable");
        log.info(" ivk: " +
            (eng instanceof Invocable ? "" : "not ") + "invocable");
      }
    }
  }
}
