/*
 * $Id: TdbUtil.java,v 1.11 2012-05-14 16:36:03 easyonthemayo Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.daemon.AuHealthMetric;
import org.lockss.plugin.AuOrderComparator;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.Logger;

/**
 * Static utility methods for getting lists of Tdb entities. Sometimes the 
 * process of retrieving a list of entities that meet particular criteria 
 * (for example only those TdbAus which are configured or preserved) can be 
 * convoluted and require going through other formats such as ArchivalUnit.
 * The static methods in this class attempt to mitigate the complexity.
 * <p>
 * Could be more efficient. All retrievals are made from the current Tdb 
 * record which is retrieved from the CurrentConfig. It might be desirable to 
 * provide a parallel method for each one which takes a Tdb argument, or to 
 * instantiate the class with a tdb reference.
 * <p>
 * Currently TdbAu objects are returned as Collections, but it might be more 
 * useful to return (ordered) Lists. 
 * <p>
 * Caching should be implemented for the lists of TdbTitles where possible. 
 * 
 * @author Neil Mayo
 *
 */
public class TdbUtil {
  
  private static final Logger logger = Logger.getLogger("TdbUtil");

  /**
   * Get the Tdb record from the current configuration.
   * 
   * @return the current Tdb object
   */
  public static Tdb getTdb() {
    return CurrentConfig.getCurrentConfig().getTdb();
  }

  /**
   * 
   * @param scope
   * @return
   */
  /*public static int getTdbTitleCount(ContentScope scope) {
    // If we just want the total available count, use the tdb method
    if (scope==ContentScope.ALL) return getTdb().getTdbTitleCount();
    // Otherwise we have to actually create a list of titles and count them, 
    // which is not very efficient  
    return getTdbTitles(scope).size();
  }*/
  
  /**
   * Get the TdbTitle with which an AU is associated.
   * 
   * @param au an ArchivalUnit
   * @return the TdbTitle corresponding to the ArchivalUnit, or null if no corresponding record could be found
   */
  public static TdbTitle getTdbTitle(ArchivalUnit au) {
    TdbAu tdbAu = getTdbAu(au);
    return (tdbAu == null) ? null : tdbAu.getTdbTitle();
  }
  
  
  /**
   * Get the TdbAu that corresponds to an ArchivalUnit. If the ArchivalUnit has
   * a TitleConfig, it is used to get the corresponding TdbAu; otherwise null
   * is returned. Note that the TitleConfig may also return a null TdbAu. 
   * 
   * @param au an ArchivalUnit
   * @return a TdbAu, or null if no corresponding record could be found
   */
  public static TdbAu getTdbAu(ArchivalUnit au) {
    TitleConfig tcfg = au.getTitleConfig();
    if (tcfg==null) return null;
    return tcfg.getTdbAu(); 
  }
  
  /**
   * Get a list of all the TdbTitles which are available for archiving. That
   * is, titles which are marked as available in the LOCKSS box's TDB records.
   * Iterates through the publishers retrieving their title lists.
   * 
   * @return a list of TdbTitle objects
   */
  public static List<TdbTitle> getAllTdbTitles() {
    List<TdbTitle> allTitles = new Vector<TdbTitle>();
    for (TdbPublisher pub : getTdb().getAllTdbPublishers().values()) {
      allTitles.addAll(pub.getTdbTitles());
    }
    return allTitles;
  }
  
  /**
   * Retrieve a collection of all the TdbTitles available within a specified 
   * scope. If the scope is null then all titles available are returned.  
   * In the case of CONFIGURED or PRESERVED scopes, any title from which an AU 
   * is configured or preserved respectively, gets included. This does not 
   * suggest that the whole title is configured or preserved.
   * <p>
   * The list does not account for coverage gaps either; there may be more 
   * than one range for each title.
   * 
   * @param scope the scope of content over which to get titles
   * @return a collection of TdbTitles representing titles in the requested scope
   */
  public static Collection<TdbTitle> getTdbTitles(ContentScope scope) {
    if (scope==null) return getAllTdbTitles();
    switch(scope) {
    case CONFIGURED: 
      return getConfiguredTdbTitles();
    case COLLECTED:
      return getPreservedTdbTitles();
    case ALL: 
    default: 
      return getAllTdbTitles();
    }
  }

  /**
   * Retrieve a collection of all the ArchivalUnits available within a specified 
   * scope. If the scope is null or ALL, an empty collection is returned as we 
   * cannot guarantee that any AUs are available unless they are configured.
   * Perhaps we should throw something like an OperationNotSupported exception.
   *  
   * @param scope the scope of content over which to get titles
   * @return a collection of ArchivalUnits, empty if the scope requested was not box-specific
   */
  public static Collection<ArchivalUnit> getAus(ContentScope scope) {
    if (scope==null) return Collections.emptyList();
    switch(scope) {
    case CONFIGURED: 
      return getConfiguredAus();
    case COLLECTED:
      return getPreservedAus();
    case ALL: 
    default: 
      return Collections.emptyList();
    }
  }
  
  /**
   * Count the number of TdbTitles available in the given scope.  
   * Note that it is necessary to get a list of AUs and construct from it a set 
   * of all the titles covered by those AUs. This is quite an expensive 
   * operation and is performed by {@link getTdbTitles}, on whose result
   * size() is called. The same caveats apply as for getTdbTitles(), regarding the
   * interpretation of the number for CONFIGURED or PRESERVED scopes.
   * 
   * @param scope the ContentScope in which to count titles
   * @return the number of TdbTitles which have at least one AU in the given scope
   */
  public static int getNumberTdbTitles(ContentScope scope) {
    return getTdbTitles(scope).size();
  }
 
  /**
   * Get ArchivalUnit records for all the AUs which are available for preservation.
   * At the moment the only way I am aware of to do this is to get all the TdbAus
   * and then get their associated ArchivalUnits, which is expensive.
   * 
   * @return a collection of ArchivalUnit objects
   */
  /*public static Collection<ArchivalUnit> getAllAus() {
    Collection<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
    for (TdbAu tdbau : getAllTdbAus()) {
      ArchivalUnit au;
      aus.add(au);
    }
    return aus;
  }*/

  /**
   * Get ArchivalUnit records for all the AUs which are configured in this 
   * LOCKSS box.
   * 
   * @return a collection of ArchivalUnit objects
   */
  public static Collection<ArchivalUnit> getConfiguredAus() {
    return LockssDaemon.getLockssDaemon().getPluginManager().getAllAus();
  }

  /**
   * Get ArchivalUnit records for all the AUs which are preserved in this 
   * LOCKSS box. This relies on the AuHealthMetric class to provide
   * an interpretation of the ArchivalUnit's status.  
   * 
   * @return a collection of ArchivalUnit objects
   */
  public static Collection<ArchivalUnit> getPreservedAus() {
    return getPreservedAus(getConfiguredAus());
  }

  /**
   * Get ArchivalUnit records for each of the candidate AUs which are preserved
   * in this LOCKSS box. This relies on the AuHealthMetric class to provide
   * an interpretation of the ArchivalUnit's status. This method allows control
   * over the list of AUs which are considered, providing better performance.
   *
   * @return a collection of ArchivalUnit objects
   */
  public static Collection<ArchivalUnit> getPreservedAus(
      Collection<ArchivalUnit> candidateAus) {
    List<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
    double inclusionThreshold = AuHealthMetric.getInclusionThreshold();
    for (ArchivalUnit au : candidateAus) {
      // XXX Preservation measure based on substance for now
      if (isAuPreserved(au)) aus.add(au);
      // TODO re-enable use of getHealth
      /*
      long s = System.currentTimeMillis();
      try {
        if (AuHealthMetric.isHealthy(au))
          aus.add(au);
        //logger.debug(String.format("Preserved AU %s checked in %sms",
        //    au.getName(), (System.currentTimeMillis()-s)
        //));
      } catch (HealthUnavailableException e) {
        // Do not add AUs whose health is unknown
        logger.warning("ArchivalUnit omitted from list of preserved AUs.", e);
      }*/
    }
    return aus;
  }


  /**
   * Make a decision on whether an AU should be considered to be preserved
   * (collected) based on a hard coded algorithm using data collected in
   * the AuHealthMetric. If there is a substance checker, return whether the AU
   * has substance; otherwise return true if the AU has completed at least one
   * successful crawl. Eventually all plugins will have substance checkers.
   * Don't penalize AUs whose plugins don't yet have a substance checker.
   *
   * @param au an ArchivalUnit
   * @return whether it should be considered to be preserved
   */
  private static boolean isAuPreserved(ArchivalUnit au) {
    AuHealthMetric metric = AuHealthMetric.getAuHealthMetric(au);
    SubstanceChecker.State state = metric.getSubstanceState();
    switch (state) {
      // If there is a substance checker, return whether the AU has substance
      case Yes:
        return true;
      case No:
        return false;
      // If there is no substance checker, return true if the AU has
      // completed at least one successful crawl
      case Unknown:
      default:
        return metric.hasCrawled();
    }
  }


  /**
   * Get records for all the TdbAus configured for collection in this box.
   * Not all the AUs will necessarily have been collected.
   * 
   * @return a list of TdbAu objects
   */
  public static List<TdbAu> getConfiguredTdbAus() {
    return getTdbAusFromAus(getConfiguredAus());
  }

  /**
   * Get records for all the TdbAus actually preserved in this box. An AU
   * should appear in this list iff it is configured for collection, has
   * been collected, and has a health above the threshold.
   *
   * @return a list of TdbAu objects
   */
  public static List<TdbAu> getPreservedTdbAus() {
    return getTdbAusFromAus(getPreservedAus());
  }

  /**
   * Get a list of all titles from which at least one AU is configured to be 
   * archived locally on this LOCKSS box.
   * 
   * @return a collection of TdbTitle objects
   */
  public static Collection<TdbTitle> getConfiguredTdbTitles() {
    return getTdbTitlesFromAus(getConfiguredAus());
  }

  /**
   * Get a list of all titles which have at least one AU preserved locally 
   * on this LOCKSS box.
   * 
   * @return a collection of TdbTitle objects
   */
  public static Collection<TdbTitle> getPreservedTdbTitles() {
    return getTdbTitlesFromAus(getPreservedAus());
  }
  
  /**
   * Get a collection of TdbTitles which represent the titles containing the 
   * supplied ArchivalUnits. If there is no TitleConfig for a set of 
   * ArchivalUnits, there will be no corresponding TdbTitle in the returned 
   * collection.
   *  
   * @param units a collection of ArchivalUnits
   * @return a collection of TdbTitles containing the supplied ArchivalUnits
   */
  public static Collection<TdbTitle> getTdbTitlesFromAus(Collection<ArchivalUnit> units) {
    Set<TdbTitle> tdbTitles = new HashSet<TdbTitle>();
    for (ArchivalUnit unit : units) {
      TdbAu au = getTdbAu(unit);
      if (au!=null) tdbTitles.add(au.getTdbTitle()); 
    }
    return tdbTitles; 
  }
  
  /**
   * Go through the ArchivalUnits and group them under titles.
   * @param units
   * @return
   */
  public static Map<TdbTitle, List<ArchivalUnit>> mapTitlesToAus(
      Collection<ArchivalUnit> units) {
    if (units==null) return Collections.emptyMap();
    Map<TdbTitle, List<ArchivalUnit>> tdbTitles = 
      new HashMap<TdbTitle, List<ArchivalUnit>>();
    // Add each AU to a list for its title
    for (final ArchivalUnit unit : units) {
      TdbAu au = getTdbAu(unit);
      if (au!=null) {
	TdbTitle title = au.getTdbTitle();
	if (tdbTitles.containsKey(title)) tdbTitles.get(title).add(unit);
	else tdbTitles.put(title, new ArrayList<ArchivalUnit>(){{add(unit);}}); 
      }
    }
    // Order each list
    for (List<ArchivalUnit> list : tdbTitles.values()) 
      Collections.sort(list, new AuOrderComparator());
    return tdbTitles; 
  }

  /**
   * Get a collection of TdbAus which correspond to the supplied ArchivalUnits.
   * Note that if an ArchivalUnit has no TitleConfig, there will be no 
   * corresponding TdbAu in the returned collection, and so it may differ in 
   * size to the argument.
   *   
   * @param units a collection of ArchivalUnits
   * @return a collection of TdbAus corresponding to the supplied ArchivalUnits
   */
  public static List<TdbAu> getTdbAusFromAus(Collection<ArchivalUnit> units) {
    List<TdbAu> tdbAus = new Vector<TdbAu>();
    for (ArchivalUnit unit : units) {
      TdbAu au = getTdbAu(unit);
      if (au!=null) tdbAus.add(au); 
    }
    return tdbAus; 
  }

  /**
   * For each of the the supplied ArchivalUnits, get the corresponding TdbAu 
   * and map it to the AU. Note that if an ArchivalUnit has no TitleConfig, 
   * there will be no corresponding TdbAu in the returned map, and so it may 
   * differ in size to the argument.
   *   
   * @param units a collection of ArchivalUnits
   * @return a map of TdbAus to ArchivalUnits
   */
  public static Map<TdbAu, ArchivalUnit> mapTdbAusToAus(Collection<ArchivalUnit> units) {
    Map<TdbAu, ArchivalUnit> map = new HashMap<TdbAu, ArchivalUnit>();
    for (ArchivalUnit unit : units) {
      TdbAu tdbAu = getTdbAu(unit);
      if (tdbAu!=null) map.put(tdbAu, unit); 
    }
    return map; 
  }


  /**
   * An enum representing the various scopes which can be requested and 
   * returned for the contents of a LOCKSS box. ContentScope is only really
   * relevant to TDB-based reports in a running daemon, not external data.
   */
  public static enum ContentScope {
    /** Everything available according to the TDB files. */
    ALL ("Available", false),
    /** Everything configured for collection. */
    CONFIGURED ("Configured", true),
    /** Everything collected and available in the LOCKSS box. */
    COLLECTED("Collected", true)
    ;
    
    /** The default fallback scope. */
    public static final ContentScope DEFAULT_SCOPE = ALL;

    /** A label for describing the scope in the UI. */
    public String label;
    
    /** 
     * A flag indicating whether this scope has ArchivalUnits. Only scopes
     * which involve items being configured on the LOCKSS box can provide AUs. 
     * For example, the list of all content is independent of whether anything 
     * is preserved on the box, and ArchivalUnits will not be available for 
     * unconfigured items.
     * <p> 
     * It is not possible to get from a TdbAu to an ArchivalUnit.
     */
    public boolean areAusAvailable;
    
    /**
     * Create a scope option 
     * @param label the public label for the scope option
     */
    ContentScope(String label, boolean hasAus) {
      this.label = label;
      this.areAusAvailable = hasAus;
    }
    
    /**
     * Get a ContentScope by name.
     * 
     * @param name a string representing the name of the enum
     * @return a ContentScope with the specified name, or the default if none was found
     */
    public static ContentScope byName(String name) {
      try {
	return valueOf(name);
      } catch (Exception e) {
	return DEFAULT_SCOPE;
      }
    } 

    public String toString() {
      return label; 
    }
    
  }

}
