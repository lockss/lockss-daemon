package org.lockss.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.lockss.app.LockssDaemon;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;

/**
 * Static utility methods for getting lists of Tdb entities. Sometimes the process of retrieving a list 
 * of entities that meet particular criteria (for example only those TdbAus which are configured 
 * or preserved) can be convoluted and require going through other formats such as ArchivalUnit.
 * The static methods in this class attempt to mitigate the complexity.
 * <p>
 * Could be more efficient. All retrievals are made from the current Tdb record which is retrieved 
 * from the CurrentConfig. It might be desirable to provide a parallel method for each one which 
 * takes a Tdb argument, or to instantiate the class with a tdb reference.
 * <p>
 * Currently TdbAu objects are returned as Collections, but it might be more useful to return 
 * (ordered) Lists. 
 * 
 * 
 * @author Neil Mayo
 *
 */
public class TdbUtil {
  
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
  public static int getTdbTitleCount(ContentScope scope) {
    // If we just want the total available count, use the tdb method
    if (scope==ContentScope.ALL) return getTdb().getTdbTitleCount();
    // Otherwise we have to actually create a list of titles and count them, 
    // which is not very efficient  
    return getTdbTitles(scope).size();
  }
  
  /**
   * Get the TdbTitle with which an AU is associated.
   * 
   * @param au an ArchivalUnit
   * @return the TdbTitle corresponding to the ArchivalUnit, or null if no corresponding record could be found
   */
  public static TdbTitle getTdbTitle(ArchivalUnit au) {
    return getTdbAu(au).getTdbTitle();
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
   * Get a list of all the TdbAus which are available for archiving.
   * 
   * @return a collection of TdbAu objects
   */
  public static Collection<TdbAu> getAllTdbAus() {
    List<TdbAu> allAus = new Vector<TdbAu>();
    // For each plugin's AU set, add them all to the set.
    for (Collection<TdbAu> aus : getTdb().getAllTdbAus().values()) {
      allAus.addAll(aus);
    }
    return allAus;
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
   * Retrieve a collection of all the TdbTitles available within a specified scope.
   * If the scope is null then all titles available are returned.
   * 
   * @param scope the scope of content over which to get titles
   * @return a collection of TdbTitles representing titles in the requested scope
   */
  public static Collection<TdbTitle> getTdbTitles(ContentScope scope) {
    if (scope==null) return getAllTdbTitles();
    switch(scope) {
    case CONFIGURED: 
      return getConfiguredTdbTitles();
    case PRESERVED: 
      return getPreservedTdbTitles();
    case ALL: 
    default: 
      return getAllTdbTitles();
    }
  }
 
  /**
   * Get ArchivalUnit records for all the AUs which are configured in this LOCKSS box.
   * 
   * @return a collection of ArchivalUnit objects
   */
  public static Collection<ArchivalUnit> getConfiguredAus() {
    return LockssDaemon.getLockssDaemon().getPluginManager().getAllAus();
  }

  /**
   * Get ArchivalUnit records for all the AUs which are preserved in this LOCKSS box.
   * 
   * NOT IMPLEMENTED; RETURNS EMPTY LIST
   * 
   * @return a collection of ArchivalUnit objects
   */
  public static Collection<ArchivalUnit> getPreservedAus() {
    return Collections.emptyList();
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
   * Get records for all the TdbAus actually preserved in this box. An AU should 
   * appear in this list iff it is configured for collection and has been collected.
   * 
   * @return a list of TdbAu objects
   */
  public static List<TdbAu> getPreservedTdbAus() {
    return getTdbAusFromAus(getPreservedAus());
  }

  /**
   * Get a list of all titles from which at least one AU is configured to be archived locally 
   * on this LOCKSS box. Note that this currently has to iterate through the entire list of AUs
   * in order to establish the titles containing those AUs.
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
   * Get a collection of TdbTitles which represent the titles containing the supplied ArchivalUnits.
   * If there is no TitleConfig for a set of ArchivalUnits, there will be no corresponding TdbTitle 
   * in the returned collection.
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
   * Get a collection of TdbAus which correspond to the the supplied ArchivalUnits. Note
   * that if an ArchivalUnit has no TitleConfig, there will be no corresponding TdbAu 
   * in the returned collection, and so it may differ in size to the argument.
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
   * An enum represnting the various scopes which can be requested and returned for the 
   * contents of a LOCKSS box. 
   */
  public static enum ContentScope {
    /** Everything available according to the TDB files. */
    ALL ("Available"),
    /** Everything configured for collection. */
    CONFIGURED ("Configured"),
    /** Everything preserved/collected and available in the LOCKSS box. */
    PRESERVED ("Collected")
    ;
    
    /** The default fallback scope. */
    public static final ContentScope DEFAULT_SCOPE = ALL;

    /** A label for describing the scope in the UI. */
    public String label;
    
    /**
     * Create a scope option 
     * @param label the public label for the scope option
     */
    ContentScope(String label) {
      this.label = label;
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
