/*
 * $Id: Tdb.java,v 1.23 2013-06-06 06:33:02 tlipkis Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.apache.commons.collections .*;
import org.apache.commons.collections.iterators .*;

import org.lockss.util.*;

/**
 * This class represents a title database (TDB).  The TDB consists of
 * hierarchy  of <code>TdbPublisher</code>s, and <code>TdbAu</code>s. 
 * Special indexing provides fast access to all <code>TdbAu</code>s for 
 * a specified plugin ID. 
 *
 * @author  Philip Gust
 * @version $Id: Tdb.java,v 1.23 2013-06-06 06:33:02 tlipkis Exp $
 */
public class Tdb {
  /**
   * Set up logger
   */
  protected final static Logger logger = Logger.getLogger("Tdb");
  
  /**
   * A map of AUs per plugin, for this configuration
   * (provides faster access for Plugins)
   */
  private final Map<String, Collection<TdbAu.Id>> pluginIdTdbAuIdsMap = 
    new HashMap<String,Collection<TdbAu.Id>>();
  
  /**
   * Map of publisher names to TdBPublishers for this configuration
   */
  private final Map<String, TdbPublisher> tdbPublisherMap = new HashMap<String,TdbPublisher>();

  /**
   * Determines whether more AUs can be added.
   */
  private boolean isSealed = false;
  
  /**
   * The total number of TdbAus in this TDB (sum of collections in pluginIdTdbAus map
   */
  private int tdbAuCount = 0;
  
  /**
   * Prefix appended to generated unknown title
   */
  private static final String UNKNOWN_TITLE_PREFIX = "Title of ";

  /**
   * Prefix appended to generated unknown publisher
   */
  private static final String UNKNOWN_PUBLISHER_PREFIX = "Publisher of ";
  
  /**
   * This exception is thrown by Tdb related classes in place of an
   * unchecked IllegalStateException when an operation cannot be
   * performed because it is incompatible with state of the Tdb.
   * <p>
   * This class inherits from IOException to avoid having higher
   * level routines that already have to handle IOException when
   * creating and copying Configuration objects from having to
   * also handle this exception.
   * 
   * @author  Philip Gust
   * @version $Id: Tdb.java,v 1.23 2013-06-06 06:33:02 tlipkis Exp $
   */
  @SuppressWarnings("serial")
  static public class TdbException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param   message   the detail message. The detail message is saved for 
     *          later retrieval by the {@link #getMessage()} method.
     */
    public TdbException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     */
    public TdbException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     */
    public TdbException(Throwable cause) {
        super(cause);
    }
  }
  
  /**
   * Register the au with this Tdb for its plugin.
   * 
   * @param au the TdbAu
   * @return <code>false</code> if already registered, otherwise <code>true</code>
   */
  private boolean addTdbAuForPlugin(TdbAu au) {
    // add AU to list for plugins 
    String pluginId = au.getPluginId();
    Collection<TdbAu.Id> auids = pluginIdTdbAuIdsMap.get(pluginId);
    if (auids == null) {
      auids = new HashSet<TdbAu.Id>();
      pluginIdTdbAuIdsMap.put(pluginId, auids);
    } 
    
    if (!auids.add(au.getId())) {
      return false;
    }
    
    // increment the total AU count;
    tdbAuCount++;
    return true;
  }
  
  /**
   * Unregister the au with this Tdb for its plugin.
   * 
   * @param au the TdbAu
   * @return <code>false</code> if au was not registered, otherwise <code>true</code>
   */
  private boolean removeTdbAuForPlugin(TdbAu au) {
    // if can't add au to title, we need to undo the au
    // registration and re-throw the exception we just caught
    String pluginId = au.getPluginId();
    Collection<TdbAu.Id> c = pluginIdTdbAuIdsMap.get(pluginId);
    if (c.remove(au.getId())) {
      if (c.isEmpty()) {
        pluginIdTdbAuIdsMap.remove(c);
      }
      tdbAuCount--;
      return true;
    }
    return false;
  }

  /**
   * Add a new TdbAu to this title database. The TdbAu must have
   * its pluginID, and title set.  The TdbAu''s title must also have 
   * its titleId and publisher set. The publisher name must be unique
   * to all publishers in this Tdb.
   * 
   * @param au the TdbAu to add.
   * @throws TdbException if Tdb is sealed, this is a duplicate au, or
   *   the au's publisher is a duplicate 
   */
  public void addTdbAu(TdbAu au) throws TdbException {
    if (au == null) {
      throw new IllegalArgumentException("TdbAu cannot be null");
    }
    
    // verify not sealed
    if (isSealed()) {
      throw new TdbException("Cannot add TdbAu to sealed Tdb");
    }
    
    // validate title
    TdbTitle title = au.getTdbTitle();
    if (title == null) {
      throw new IllegalArgumentException("TdbAu's title not set");
    }
    
    // validate publisher
    TdbPublisher publisher = title.getTdbPublisher();
    if (publisher == null) {
      throw new IllegalArgumentException("TdbAu's publisher not set");
    }

    // make sure publisher is not a duplicate
    String pubName = publisher.getName();
    TdbPublisher oldPublisher = tdbPublisherMap.put(pubName, publisher);
    if ((oldPublisher != null) && (oldPublisher != publisher)) {
      // restore old publisher and report error
      tdbPublisherMap.put(pubName, oldPublisher);
      throw new TdbException("New au publisher with duplicate name: " + pubName);
    }
   
    // register the au with this instance
    if (!addTdbAuForPlugin(au)) {
      // remove new publisher and report error 
      if (oldPublisher == null) {
        tdbPublisherMap.remove(pubName);
      }
      throw new TdbException("Cannot register au " + au.getName());
    }
  }
  
  /**
   * Seals a Tdb against further additions.
   */
  public void seal() {
    if (!isSealed) {
      isSealed = true;

      // convert map values to array lists to save space because
      // they will not be modified now that the Tdb is sealed.
      synchronized(pluginIdTdbAuIdsMap) {
        for (Map.Entry<String, Collection<TdbAu.Id>> entry : pluginIdTdbAuIdsMap.entrySet()) {
          ArrayList<TdbAu.Id> list = new ArrayList<TdbAu.Id>(entry.getValue());
          list.trimToSize();
          entry.setValue(list);
        }
      }
    }
  }

  /**
   * Determines whether this Tdb is sealed.
   * 
   * @return <code>true</code> if sealed
   */
  public boolean isSealed() {
    return isSealed;
  }

  /**
   * Determines whether the title database is empty.
   * 
   * @return <code> true</code> if the title database has no entries
   */
  public boolean isEmpty() {
    return pluginIdTdbAuIdsMap.isEmpty();
  }
  
  /** 
   * Return an object describing the differences between the Tdbs.
   * Logically a symmetric operation but currently records only changes and
   * addition, not deletions.
   * @param newTdb the new Tdb
   * @param oldTdb the previous Tdb
   * @return a {@link Tdb.Differences}
   */
  public static Differences computeDifferences(Tdb newTdb, Tdb oldTdb) {
    if (newTdb == null) {
      newTdb = new Tdb();
    }
    if (oldTdb == null) {
      oldTdb = new Tdb();
    }
    return newTdb.computeDifferences(oldTdb);
  }

  Differences computeDifferences(Tdb oldTdb) {
    Differences res = new Differences(this, oldTdb);
    return res;
  }

  /**
   * Adds to the {@link Tdb.Differences} the differences found between
   * oldTdb and this Tdb
   * 
   * @param the {@link Tdb.Differences} to which to add items.
   * @param otherTdb a Tdb
   */
  private void addDifferences(Differences diffs, Tdb oldTdb) {
    Map<String, TdbPublisher> oldPublishers = oldTdb.getAllTdbPublishers();

    for (TdbPublisher oldPublisher : oldPublishers.values()) {
      if (!this.tdbPublisherMap.containsKey(oldPublisher.getName())) {
        // add pluginIds for publishers in tdb that are not in this Tdb
        diffs.addPublisher(oldPublisher, Differences.Type.Old);
      }
    }

    for (TdbPublisher thisPublisher : tdbPublisherMap.values()) {
      TdbPublisher oldPublisher = oldPublishers.get(thisPublisher.getName());
      if (oldPublisher == null) {
        // add pluginIds for publisher in this Tdb that is not in tdb
        diffs.addPublisher(thisPublisher, Differences.Type.New);
      } else {
        // add pluginIds for publishers in both Tdbs that are different 
        thisPublisher.addDifferences(diffs, oldPublisher);
      }
    }
  }

  /**
   * Determines two Tdbs are equal.  Equality is based on having
   * equal TdbPublishers, and their child TdbTitles and TdbAus.
   * 
   * @param o the other object
   * @return <code>true</code> iff they are equal Tdbs
   */
  public boolean equals(Object o) {
    // check for identity
    if (this == o) {
      return true;
    }
    
    if (o instanceof Tdb) {
      try {
        // if no exception thrown, there are no differences
        // because the method did not try to modify the set
	Differences diffs = new Differences.Unmodifiable();
	addDifferences(diffs, (Tdb)o);
        return true;
      } catch (UnsupportedOperationException ex) {
        // differences because method tried to add to unmodifiable set
      } catch (IllegalArgumentException ex) {
        // if something was wrong with the other Tdb
      } catch (IllegalStateException ex) {
        // if something is wrong with this Tdb
      }
    }
    return false;
  }

  /**
   * Not supported for this class.
   * 
   * @throws UnsupportedOperationException
   */
  public int hashCode() {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Merge other Tdb into this one. Makes copies of otherTdb's non-duplicate 
   * TdbPublisher, TdbTitle, and TdbAu objects and their non-duplicate children.
   * The object themselves are not merged.
   * 
   * @param otherTdb the other Tdb
   * @throws TdbException if Tdb is sealed
   */
  public void copyFrom(Tdb otherTdb) throws TdbException {
    // ignore inappropriate Tdb values
    if ((otherTdb == null) || (otherTdb == this)) {
      return;
    }
    
    if (isSealed()) {
      throw new TdbException("Cannot add otherTdb AUs to sealed Tdb");
    }
    
    // merge non-duplicate publishers of otherTdb
    boolean tdbIsNew = tdbPublisherMap.isEmpty();
    for (TdbPublisher otherPublisher : otherTdb.getAllTdbPublishers().values()) {
      String pubName = otherPublisher.getName();
      TdbPublisher thisPublisher;
      boolean publisherIsNew = true;
      if (tdbIsNew) {
        // no need to check for existing publisher if TDB is new
        thisPublisher = new TdbPublisher(pubName);
        tdbPublisherMap.put(pubName, thisPublisher);
      } else {
        thisPublisher = tdbPublisherMap.get(pubName);
        publisherIsNew = (thisPublisher == null);
        if (publisherIsNew) {
          // copy publisher if not present in this Tdb
          thisPublisher = new TdbPublisher(pubName);
          tdbPublisherMap.put(pubName, thisPublisher);
        }
      }
      
      // merge non-duplicate titles of otherPublisher into thisPublisher
      for (TdbTitle otherTitle : otherPublisher.getTdbTitles()) {
        String otherId = otherTitle.getId();
        TdbTitle thisTitle;
        boolean titleIsNew = true;
        if (publisherIsNew) {
          // no need to check for existing title if publisher is new
          thisTitle = otherTitle.copyForTdbPublisher(thisPublisher);
          thisPublisher.addTdbTitle(thisTitle);
        } else {
          thisTitle = thisPublisher.getTdbTitleById(otherId);
          titleIsNew = (thisTitle == null);
          if (titleIsNew) {
            // copy title if not present in this publisher
            thisTitle = otherTitle.copyForTdbPublisher(thisPublisher);
            thisPublisher.addTdbTitle(thisTitle);
          } else if (! thisTitle.getName().equals(otherTitle.getName())) {
            // error because it could lead to a missing title -- one probably has a typo
            // (what about checking other title elements too?)
            logger.error("Ignorning duplicate title entry: \"" + otherTitle.getName() 
                         + "\" with the same ID as \"" + thisTitle.getName() + "\"");
          }
        }
        
        // merge non-duplicate TdbAus of otherTitle into thisTitle
        for (TdbAu otherAu : otherTitle.getTdbAus()) {
          // no need to check for existing au if title is new
          String pluginId = otherAu.getPluginId();
          if (titleIsNew || !getTdbAuIds(pluginId).contains(otherAu.getId())) {
            // always succeeds we've already checked for duplicate
            TdbAu thisAu = otherAu.copyForTdbTitle(thisTitle);            
            addTdbAuForPlugin(thisAu);
          } else {
            TdbAu thisAu = findExistingTdbAu(otherAu);
            if (!thisAu.getTdbTitle().getName().equals(otherAu.getTdbTitle().getName())) {
              if (!thisAu.getName().equals(otherAu.getName())) {
                logger.error("Ignorning duplicate au entry: \"" + otherAu.getName() 
                             + "\" for title \"" + otherAu.getTdbTitle().getName()
                             + "\" with same definion as existing au entry: \"" 
                             + thisAu.getName() + "\" for title \"" 
                             + thisAu.getTdbTitle().getName() + "\"");
              } else {
                logger.error("Ignorning duplicate au entry: \"" + otherAu.getName() 
                             + "\" for title \"" + otherAu.getTdbTitle().getName() 
                             + "\" with same definion as existing one for title \""
                             + thisAu.getTdbTitle().getName() + "\"");
              }
            } else if (!thisAu.getName().equals(otherAu.getName())) {
              // error because it could lead to a missing AU -- one probably has a typo
              logger.error("Ignorning duplicate au entry: \"" + otherAu.getName() 
                           + "\" with the same definition as \"" + thisAu.getName() 
                           + "\" for title \"" + otherAu.getTdbTitle().getName());
            } else {
              logger.warning("Ignoring duplicate au entry: \"" + otherAu.getName() 
                             + "\" for title \"" + otherAu.getTdbTitle().getName());
            }
          }
        }
      }
    }
  }
  
  /**
   * Find existing TdbAu with same Id as another one.
   * @param otherAu another TdbAu
   * @return an existing TdbAu already in thisTdb
   */
  protected TdbAu findExistingTdbAu(TdbAu otherAu) {
    // check for duplicate AU with same plugin for this Tdb
    Collection<TdbAu.Id> ids = getTdbAuIds(otherAu.getPluginId());
    for (TdbAu.Id id : ids) {
      if (id.equals(otherAu.getId())) {
        return id.getTdbAu();
      }
    }
    return null;
  }
  
  /**
   * Returns a collection of TdbAus for the specified plugin ID.
   * <p>
   * Note: the returned collection should not be modified.
   * 
   * @param pluginId the plugin ID
   * @return a collection of TdbAus for the plugin; <code>null</code> 
   *    if no TdbAus for the specified plugin in this configuration.
   */
  public Collection<TdbAu.Id> getTdbAuIds(String pluginId) {
    Collection<TdbAu.Id> auIds = pluginIdTdbAuIdsMap.get(pluginId);
    return (auIds != null) ? auIds : Collections.<TdbAu.Id>emptyList();
  }
  
  /**
   * Returns the set of plugin ids for this Tdb.
   * @return the set of all plugin ids for this Tdb.
   */
  public Set<String> getAllPluginsIds() {
    return (pluginIdTdbAuIdsMap != null)
      ? Collections.unmodifiableSet(pluginIdTdbAuIdsMap.keySet())
      : Collections.<String>emptySet();
  }
  
  /**
   * Get a list of all the TdbAu.Ids for this Tdb
   * 
   * @return a collection of TdbAu objects
   */
  public Set<TdbAu.Id> getAllTdbAuIds() {
    if (pluginIdTdbAuIdsMap == null) {
      return Collections.<TdbAu.Id> emptySet();
    }
    Set<TdbAu.Id> allAuIds = new HashSet<TdbAu.Id>();
    // For each plugin's AU set, add them all to the set.
    for (Collection<TdbAu.Id> auIds : pluginIdTdbAuIdsMap.values()) {
      allAuIds.addAll(auIds);
    }
    return allAuIds;
  }
  
  /**
   * Return the number of TdbAus in this Tdb.
   * 
   * @return the total TdbAu count
   */
  public int getTdbAuCount() {
    return tdbAuCount;
  }
  
  /**
   * Return the number of TdbTitles in this Tdb.
   * 
   * @return the total TdbTitle count
   */
  public int getTdbTitleCount() {
    int titleCount = 0;
    for (TdbPublisher publisher : tdbPublisherMap.values()) {
      titleCount += publisher.getTdbTitleCount();
    }
    return titleCount;
  }

  /**
   * Return the number of TdbPublishers in this Tdb.
   * 
   * @return the total TdbPublisher count
   */
  public int getTdbPublisherCount() {
    return tdbPublisherMap.size();
  }

  /**
   * Add a new TdbAu from properties.  This method recognizes
   * properties of the following form:
   * <pre>
   * Properties p = new Properties(); 
   * p.setProperty("title", "Air & Space Volume 1)");
   * p.setProperty("journalTitle", "Air and Space");
   * p.setProperty("plugin", "org.lockss.plugin.smithsonian.SmithsonianPlugin");
   * p.setProperty("pluginVersion", "4");
   * p.setProperty("issn", "0886-2257");
   * p.setProperty("param.1.key", "volume");
   * p.setProperty("param.1.value", "1");
   * p.setProperty("param.2.key", "year");
   * p.setProperty("param.2.value", "2001");
   * p.setProperty("param.2.editable", "true");
   * p.setProperty("param.3.key", "journal_id");
   * p.setProperty("param.3.value", "0886-2257");
   * p.setProperty("attributes.publisher", "Smithsonian Institution");
   * </pre>
   * <p>
   * The "attributes.publisher" property is used to identify the publisher.
   * If a unique journalID is specified it is used to select among titles
   * for a publisher. A journalID can be specified indirectly through a 
   * "journal_id" param or an "issn" property. If a journalId is not 
   * specified, the "journalTitle" property is used to select the the title.  
   * <p>
   * Properties other than "param", "attributes", "title", "journalTitle",
   * "journalId", and "plugin" are converted to attributes of the AU. Only 
   * "title" and "plugin" are required properties.  If "attributes.publisher" 
   * or "journalTitle" are missing, their values are synthesized from the 
   * "title" property.
   * 
   * @param props a map of title properties
   * @return the TdbAu that was added
   * @throws TdbException if this Tdb is sealed, or the
   *    AU already exists in this Tdb
   */
  public TdbAu addTdbAuFromProperties(Properties props) throws TdbException {
    if (props == null) {
      throw new IllegalArgumentException("properties cannot be null");
    }

    // verify not sealed
    if (isSealed()) {
      throw new TdbException("cannot add au to sealed TDB");
    }
    TdbAu au = newTdbAu(props);
    addTdbAu(props, au);
    
    return au;
  }

  /**
   * Create a new TdbAu instance from the properties.
   * 
   * @param props the properties
   * @return a TdbAu instance set built from the properties
   */
  private TdbAu newTdbAu(Properties props) {
    String pluginId = (String)props.get("plugin");
    if (pluginId == null) {
      throw new IllegalArgumentException("TdbAu plugin ID not specified");
    }
    
    String auName = props.getProperty("title");
    if (auName == null) {
      throw new IllegalArgumentException("TdbAu title not specified");
    }
    
    // create a new TdbAu and set its elements
    TdbAu au = new TdbAu(auName, pluginId);

    // process attrs, and params
    Map<String, Map<String,String>> paramMap = new HashMap<String, Map<String,String>>();
    for (Map.Entry<Object,Object> entry : props.entrySet()) {
      String key = String.valueOf(entry.getKey());
      String value = String.valueOf(entry.getValue());
      if (key.startsWith("attributes.")) {
        // set attributes directly
        String name = key.substring("attributes.".length());
        try {
          au.setAttr(name, value);
        } catch (TdbException ex) {
          logger.warning("Cannot set attribute \"" + name + "\" with value \"" + value + "\" -- ignoring");
        }
        
      } else if (key.startsWith("param.")) {
        // skip to param name
        String param = key.substring("param.".length());
        int i;
        if (   ((i = param.indexOf(".key")) < 0)
            && ((i = param.indexOf(".value")) < 0)) {
          logger.warning("Ignoring unexpected param key for au \"" + auName + "\" key: \"" + key + "\" -- ignoring");
        } else {
          // get param map for pname
          String pname = param.substring(0,i);
          Map<String,String> pmap = paramMap.get(pname);
          if (pmap == null) {
            pmap = new HashMap<String,String>();
            paramMap.put(pname, pmap);
          }
          // add name and value to param map for pname
          String name = param.substring(i+1);
          pmap.put(name, value);
        }
        
      } else if (   !key.equals("title")              // TdbAu has "name" property
                 && !key.equals("plugin")             // TdbAu has "pluginId" property
                 && !key.equals("journalTitle")       // TdbAu has "title" TdbTitle property
                 && !key.startsWith("journal.")) {    // TdbAu has "title" TdbTitle property
        // translate all other properties into AU properties
        try {
          au.setPropertyByName(key, value);
        } catch (TdbException ex) {
          logger.warning("Cannot set property \"" + key + "\" with value \"" + value + "\" -- ignoring");
        }
      }
    }
    
    // set param from accumulated "key", and "value" entries
    for (Map<String, String> pmap : paramMap.values()) {
      String name = pmap.get("key");
      String value = pmap.get("value");
      if (name == null) {
        logger.warning("Ignoring property with null name");
      } else if (value == null) {
        logger.warning("Ignoring property \"" + name + "\" with null value");
      } else {
        try {
          au.setParam(name, value);
        } catch (TdbException ex) {
          logger.warning("Cannot set param \"" + name + "\" with value \"" + value + "\" -- ignoring");
        }
      }
    }

    return au;
  }
  
  /**
   * Add a TdbAu to a TdbTitle and TdbPubisher, and add links to 
   * the TdbTitle specified by the properties.
   * 
   * @param props the properties
   * @param au the TdbAu to add
   * @throws TdbException if the AU already exists in this Tdb
   */
  private void addTdbAu(Properties props, TdbAu au) throws TdbException {
    // add au for plugin assuming it is not a duplicate
    if (!addTdbAuForPlugin(au)) {
      // au already registered -- report existing au
      TdbAu existingAu = findExistingTdbAu(au);
      String titleName = getTdbTitleName(props, au);
      if (!titleName.equals(existingAu.getTdbTitle().getName())) {
        throw new TdbException(
                       "Cannot add duplicate au entry: \"" + au.getName() 
                     + "\" for title \"" + titleName 
                     + "\" with same definition as existing au entry: \"" 
                     + existingAu.getName() + "\" for title \"" 
                     + existingAu.getTdbTitle().getName() + "\" to title database");
      } else if (!existingAu.getName().equals(au.getName())) {
        // error because it could lead to a missing AU -- one probably has a typo
        throw new TdbException(
                       "Cannot add duplicate au entry: \"" + au.getName() 
                     + "\" with the same definition as \"" + existingAu.getName() 
                     + "\" for title \"" + titleName + "\" to title database");
      } else {
        throw new TdbException(
                       "Cannot add duplicate au entry: \"" + au.getName() 
                     + "\" for title \"" + titleName + "\" to title database");
      }
    }

    // get or create the TdbTitle for this 
    TdbTitle title = getTdbTitle(props, au);
    try {
      // add AU to title 
      title.addTdbAu(au);
    } catch (TdbException ex) {
      // if we can't add au to title, remove for plugin and re-throw exception
      removeTdbAuForPlugin(au);
      throw ex;
    }
    
    // process title links
    Map<String, Map<String,String>> linkMap = new HashMap<String, Map<String,String>>();
    for (Map.Entry<Object,Object> entry : props.entrySet()) {
      String key = ""+entry.getKey();
      String value = ""+entry.getValue();
      if (key.startsWith("journal.link.")) {
        // skip to link name
        String param = key.substring("link.".length());
        int i;
        if (   ((i = param.indexOf(".type")) < 0)
            && ((i = param.indexOf(".journalId")) < 0)) {
          logger.warning("Ignoring nexpected link key for au \"" + au.getName() + "\" key: \"" + key + "\"");
        } else {
          // get link map for linkName
          String lname = param.substring(0,i);
          Map<String,String> lmap = linkMap.get(lname);
          if (lmap == null) {
            lmap = new HashMap<String,String>();
            linkMap.put(lname, lmap);
          }
          // add name and value to link map for link
          String name = param.substring(i+1);
          lmap.put(name, value);
        }
      }
    }
    
    // add links to title from accumulated "type", "journalId" entries
    for (Map<String, String> lmap : linkMap.values()) {
      String name = lmap.get("type");
      String value = lmap.get("journalId");
      if ((name != null) && (value != null)) {
        try {
          TdbTitle.LinkType linkType = TdbTitle.LinkType.valueOf(name);
          title.addLinkToTdbTitleId(linkType, value);
        } catch (IllegalArgumentException ex) {
          logger.warning("Ignoring unknown link type for au \"" + au.getName() + "\" name: \"" + name + "\"");
        }
      }
    }
  }

  /**
   * Get title ID from properties and TdbAu.
   * 
   * @param props the properties
   * @param au the TdbAu
   * @return the title ID or <code>null</code> if not found
   */
  private String getTdbTitleId(Properties props, TdbAu au) {
    // get the title ID from one of several props
    String titleId = props.getProperty("journalId");
    return titleId;
  }
  
  /** 
   * Get or create TdbTitle for the specified properties and TdbAu.
   * 
   * @param props the properties
   * @param au the TdbAu
   * @return the corresponding TdbTitle
   */
  private TdbTitle getTdbTitle(Properties props, TdbAu au) {
    TdbTitle title = null;

    // get publisher name
    String publisherNameFromProps = getTdbPublisherName(props, au);

    // get the title name 
    String titleNameFromProps = getTdbTitleName(props, au);

    // get the title ID 
    String titleIdFromProps = getTdbTitleId(props, au);
    
    String titleId = titleIdFromProps;
    if (titleId == null) {
      // generate a titleId if one not specified, using the
      // hash code of the combined title name and publisher names
      int hash = (titleNameFromProps + publisherNameFromProps).hashCode();
      titleId = (hash < 0) ? ("id:1" +(-hash)) : ("id:0" + hash);
    }
    
    // get publisher specified by property name
    TdbPublisher publisher = tdbPublisherMap.get(publisherNameFromProps);
    if (publisher != null) {
      // find title from publisher
      title = publisher.getTdbTitleById(titleId);
      if (title != null) {
        // warn that title name is different
        if (!title.getName().equals(titleNameFromProps)) {
          logger.warning("Title for au \"" + au.getName() + "\": \"" + titleNameFromProps 
                         + "\" is different than existing title \"" + title.getName() 
                         + "\" for id " + titleId + " -- using existing title.");
        }
        return title;
      }
    }
    
    if (publisher == null) {
      // warn of missing publisher name
      if (publisherNameFromProps.startsWith(UNKNOWN_PUBLISHER_PREFIX)) {
        logger.warning("Publisher missing for au \"" + au.getName() + "\" -- using \"" + publisherNameFromProps + "\"");
      }

      // create new publisher for specified publisher name
      publisher = new TdbPublisher(publisherNameFromProps);
      tdbPublisherMap.put(publisherNameFromProps, publisher);
    }

    // warn of missing title name and/or id
    if (titleNameFromProps.startsWith(UNKNOWN_TITLE_PREFIX)) {
      logger.warning("Title missing for au \"" + au.getName() + "\" -- using \"" + titleNameFromProps + "\"");
    }
    if (titleIdFromProps == null) {
      logger.debug2("Title ID missing for au \"" + au.getName() + "\" -- using " + titleId);
    }

    
    // create title and add to publisher
    title = new TdbTitle(titleNameFromProps, titleId);
    try {
      publisher.addTdbTitle(title);
    } catch (TdbException ex) {
      // shouldn't happen: title already exists in publisher
      logger.error(ex.getMessage(), ex);
    }
    
    return title;
  }
  
  /**
   * Get publisher name from properties and TdbAu. Creates a name 
   * based on title name if not specified.
   * 
   * @param props the properties
   * @param au the TdbAu
   * @return the publisher name
   */
  private String getTdbPublisherName(Properties props, TdbAu au) {
    // use "publisher" attribute if specified, or synthesize from titleName.
    // proposed to replace with publisher.name property 
    String publisherName = props.getProperty("attributes.publisher");
    if (publisherName == null) {
      publisherName = props.getProperty("publisher.name");  // proposed new property
    }

    if (publisherName == null) {
      // create publisher name from title name if not specified 
      String titleName = getTdbTitleName(props, au);
      publisherName = UNKNOWN_PUBLISHER_PREFIX + "[" + titleName + "]";
    }
    
    return publisherName;
  }

  
  /**
   * Get TdbTitle name from properties alone.
   *
   * @param props a group of properties
   * @return
   */
  private String getTdbTitleName(Properties props) {
    // from auName and one of several properties 
    String titleName = props.getProperty("journalTitle");
    if (titleName == null) {
      titleName = props.getProperty("journal.title");  // proposed to replace journalTitle
    }
    return titleName;
  }
  
  /**
   * Get the TdbTitle name from the properties.  Fall back to a name
   * derived from the TdbAU name if not specified.
   * 
   * @param props a group of properties
   * @param au the TdbAu
   * @return a TdbTitle name
   */
  private String getTdbTitleName(Properties props, TdbAu au) {
    // use "journalTitle" prop if specified, or synthesize it 
    // from auName and one of several properties 
    String titleName = getTdbTitleName(props);
    if (titleName == null) {
      String year = au.getYear();
      String volume = au.getVolume();
      String issue = au.getIssue();

      String auName = au.getName();
      String auNameLC = auName.toLowerCase();
      if ((volume != null) && auNameLC.endsWith(" vol " + volume)) {
        titleName = auName.substring(0, auName.length()-" vol ".length() - volume.length());
      } else if ((volume != null) && auNameLC.endsWith(" volume " + volume)) {
        titleName = auName.substring(0, auName.length()-" volume ".length() - volume.length());
      } else if ((issue != null) && auNameLC.endsWith(" issue " + issue)) {
        titleName = auName.substring(0, auName.length()-" issue ".length() - issue.length());
      } else if ((year != null) && auNameLC.endsWith(" " + year)) {
        titleName = auName.substring(0, auName.length()-" ".length() - year.length());
      } else {   
        titleName = UNKNOWN_TITLE_PREFIX + "[" + auName + "]";
      }
    }
    return titleName;
  }
  
  /**
   * Get the linked titles for the specified link type.
   * 
   * @param linkType the link type (see {@link TdbTitle} for description of
   * link types)
   * @param title the TdbTitle with links
   * @return a collection of linked titles for the specified type 
   */
  public Collection<TdbTitle> getLinkedTdbTitlesForType(
      TdbTitle.LinkType linkType, TdbTitle title) {
    if (linkType == null) {
      throw new IllegalArgumentException("linkType cannot be null");
    }
    if (title == null) {
      throw new IllegalArgumentException("title cannot be null");
    }
    Collection<String> titleIds = title.getLinkedTdbTitleIdsForType(linkType);
    if (titleIds.isEmpty()) {
      return Collections.emptyList();
    }
    ArrayList<TdbTitle> titles = new ArrayList<TdbTitle>();
    for (String titleId : titleIds) {
      TdbTitle aTitle = getTdbTitleById(titleId);
      if (aTitle != null) {
        titles.add(aTitle);
      }
    }
    titles.trimToSize();
    return titles;
  }
  
  /**
   * Get the title for the specified titleId.
   *  
   * @param titleId the titleID
   * @return the title for the titleId or <code>null</code. if not found
   */
  public TdbTitle getTdbTitleById(String titleId)
  {
    if (titleId != null) {
      for (TdbPublisher publisher : tdbPublisherMap.values()) {
        TdbTitle title = publisher.getTdbTitleById(titleId);
        if (title != null) {
          return title;
        }
      }
    }
    return null;
  }
  
  /**
   * Get a title for the specified issn.
   *  
   * @param issn the issn
   * @return the title for the titleId or <code>null</code. if not found
   */
  public TdbTitle getTdbTitleByIssn(String issn)
  {
    if (issn != null) {
      for (TdbPublisher publisher : tdbPublisherMap.values()) {
        TdbTitle title = publisher.getTdbTitleByIssn(issn);
        if (title != null) {
          return title;
        }
      }
    }
    return null;
  }
  
  /**
   * Return a collection of TdbAus for this TDB that match the ISBN.
   * 
   * @return a colleciton of TdbAus for this publisher that match the ISBN
   */
  public Collection<TdbAu> getTdbAusByIsbn(String isbn) {
    Collection<TdbAu> tdbAus = new ArrayList<TdbAu>();
    getTdbAusByIsbn(isbn, tdbAus);
    return tdbAus;
  }
  
  /**
   * Add to a collection of TdbAus for this TDB that match the ISBN.
   */
  public boolean getTdbAusByIsbn(String isbn, 
                                 Collection<TdbAu> matchingTdbAus) {
    boolean added = false;
    if (isbn != null) {
      for (TdbPublisher tdbPublisher : tdbPublisherMap.values()) {
        added |= tdbPublisher.getTdbAusByIsbn(matchingTdbAus, isbn);
      }
    }
    return added;
  }
  
  /**
   * Returns a collection of TdbTitles for the specified title name
   * across all publishers.
   *  
   * @param titleName the title name
   * @return a collection of TdbTitles that match the title name
   */
  public Collection<TdbTitle> getTdbTitlesByName(String titleName)
  {
    ArrayList<TdbTitle> titles = new ArrayList<TdbTitle>();
    getTdbTitlesByName(titleName, titles);
    titles.trimToSize();
    return titles;
  }
  
  /**
   * Adds to a collection of TdbTitles for the specified title name
   * across all publishers.
   *  
   * @param titleName the title name
   * @param titles the collection of TdbTitles to add to
   * @return <code>true</code> if TdbTitles were adddthe titles collection
   */
  public boolean getTdbTitlesByName(String titleName, 
                                    Collection<TdbTitle> titles) {
    boolean added = false;
    if (titleName != null) {
      for (TdbPublisher publisher : tdbPublisherMap.values()) {
        added |= publisher.getTdbTitlesByName(titleName, titles);
      }
    }
    return added;
  }
  
  /**
   * Returns a collection of TdbTitles like (starts with) the 
   * specified title name across all publishers.
   *  
   * @param titleName the title name
   * @return a collection of TdbTitles that match the title name
   */
  public Collection<TdbTitle> getTdbTitlesLikeName(String titleName)
  {
    ArrayList<TdbTitle> titles = new ArrayList<TdbTitle>();
    getTdbTitlesLikeName(titleName, titles);
    titles.trimToSize();
    return titles;
  }
  
  /**
   * Adds to a collection of TdbTitles like (starts with) the 
   * specified title name across all publishers.
   *  
   * @param titleName the title name
   * @param titles a collection of matching titles
   * @return a collection of TdbTitles that match the title name
   */
  public boolean getTdbTitlesLikeName(String titleName,
                                      Collection<TdbTitle> titles) {
    boolean added = false;
    if (titleName != null) {
      for (TdbPublisher publisher : tdbPublisherMap.values()) {
        added |= publisher.getTdbTitlesLikeName(titleName, titles); 
      }
    }
    return added;
  }
  
  /**
   * Return the TdbAus with the specified TdbAu name (ignoring case).
   * 
   * @param tdbAuName the name of the AU to select
   * @return all TdbAus with the specified name
   */
  public List<TdbAu> getTdbAusByName(String tdbAuName) {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    getTdbAusByName(tdbAuName, aus);
    return aus;
  }
  
  /**
   * Add TdbAus with the specified TdbAu name.
   * 
   * @param tdbAuName the name of the AU to select
   * @param aus the collection to add to
   * @return <code>true</code> if TdbAus were added to the collection
   */
  public boolean getTdbAusByName(String tdbAuName, Collection<TdbAu> aus) {
    boolean added = false;
    for (TdbPublisher publisher : tdbPublisherMap.values()) {
      added |= publisher.getTdbAusByName(tdbAuName, aus);
    }
    return added;
  }
  
  /**
   * Return the TdbAus like (starts with, ignoring case) the specified
   * TdbAu name.
   * 
   * @param tdbAuName initial substring of the name of AUs to return
   * @return all TdbAus like the specified name
   */
  public List<TdbAu> getTdbAusLikeName(String tdbAuName) {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    getTdbAusLikeName(tdbAuName, aus);
    return aus;
  }
  
  /**
   * Add TdbAus for like (starts with) the specified TdbAu name.
   * 
   * @param tdbAuName the name of the AU to select
   * @param aus the collection to add to
   * @return <code>true</code> if TdbAus were added to the collection
   */
  public boolean getTdbAusLikeName(String tdbAuName, Collection<TdbAu> aus) {
    boolean added = false;
    for (TdbPublisher publisher : tdbPublisherMap.values()) {
      added |= publisher.getTdbAusLikeName(tdbAuName, aus);
    }
    return added;
  }
  
  /**
   * Get the publisher for the specified name.
   * 
   * @param name the publisher name
   * @return the publisher, or <code>null</code> if not found
   */
  public TdbPublisher getTdbPublisher(String name) {
    return (tdbPublisherMap != null) ? tdbPublisherMap.get(name) : null;
  }

  /**
   * Returns all TdbPubishers in this configuration. 
   * <p>
   * Note: The returned map should not be modified.
   * 
   * @return a map of publisher names to publishers
   */
  public Map<String, TdbPublisher> getAllTdbPublishers() {
    return (tdbPublisherMap != null) 
      ? tdbPublisherMap : Collections.<String,TdbPublisher>emptyMap();
  }

  /** ObjectGraphIterator Transformer that descends into collections of
   * TdbPublishers and TdbTitles, returning TdbAus */
  static Transformer AU_ITER_XFORM = new Transformer() {
      public Object transform(Object input) {
	if (input instanceof TdbPublisher) {
	  return ((TdbPublisher)input).getTdbTitles().iterator();
	}
	if (input instanceof TdbTitle) {
	  return ((TdbTitle)input).getTdbAus().iterator();
	}
	if (input instanceof TdbAu) {
	  return input;
	}
	throw new
	  ClassCastException(input+" is not a TdbPublisher, TdbTitle or TdbAu");
      }};

  /** ObjectGraphIterator Transformer that descends into collections of
   * TdbPublishers, returning TdbTitles */
  static Transformer TITLE_ITER_XFORM = new Transformer() {
      public Object transform(Object input) {
	if (input instanceof TdbPublisher) {
	  return ((TdbPublisher)input).getTdbTitles().iterator();
	}
	if (input instanceof TdbTitle) {
	  return input;
	}
	throw new
	  ClassCastException(input+" is not a TdbPublisher or TdbTitle");
      }};


  /** @return an Iterator over all the TdbPublishers in this Tdb. */
  public Iterator<TdbPublisher> tdbPublisherIterator() {
    return tdbPublisherMap.values().iterator();
  }

  /** @return an Iterator over all the TdbTitles (in all the TdbPublishers)
   * in this Tdb. */
  public Iterator<TdbTitle> tdbTitleIterator() {
    return new ObjectGraphIterator(tdbPublisherMap.values().iterator(),
				   TITLE_ITER_XFORM);
  }

  /** @return an Iterator over all the TdbAus (in all the TdbTitles in all
   * the TdbPublishers) in this Tdb. */
  public Iterator<TdbAu> tdbAuIterator() {
    return new ObjectGraphIterator(tdbPublisherMap.values().iterator(),
				   AU_ITER_XFORM);
  }

  /** Print a full description of all elements in the Tdb */
  public void prettyPrint(PrintStream ps) {
    ps.println("Tdb");
    TreeMap<String, TdbPublisher> sorted =
      new TreeMap<String, TdbPublisher>(CatalogueOrderComparator.SINGLETON);
    sorted.putAll(getAllTdbPublishers());
    for (TdbPublisher tdbPublisher : sorted.values()) {
      tdbPublisher.prettyPrint(ps, 2);
    }
  }

  /** Differences represents the changes in a Tdb from the previous Tdb,
   * primarily oriented toward enumerating the changes (rather than testing
   * containment as with {@link Configuration.Differences}).  It currently
   * retains only additions and changes, as deletions aren't needed (and
   * would require holding on to objects that could otherwise be GCed).
   */
  public static class Differences  {
    enum Type {Old, New}

    // newly added publishers
    private final List<TdbPublisher> newPublishers
      = new ArrayList<TdbPublisher>();;
    // titles that have been added to existing publishers
    private final List<TdbTitle> newTitles = new ArrayList<TdbTitle>();;
    // AUs that have been added to existing titles
    private final List<TdbAu> newAus = new ArrayList<TdbAu>();
    // Retained for compatibility with legacy Plugin/TitleConfig mechanism
    private final Set<String> diffPluginIds = new HashSet<String>();
    private int tdbAuCountDiff;
    
    Differences() {
    }

    Differences(Tdb newTdb, Tdb oldTdb) {
      tdbAuCountDiff =
	newTdb.getTdbAuCount()-((oldTdb == null) ? 0 : oldTdb.getTdbAuCount());
      newTdb.addDifferences(this, oldTdb);
    }

    /** Return the ID of every plugin that has at least one changed or
     * added or removed AU */
    Set<String> getPluginIdsForDifferences() {
      return diffPluginIds;
    }

    /** Return the difference in number of AUs from old to new Tdb. */
    public int getTdbAuDifferenceCount() {
      return tdbAuCountDiff;
    }

    /** @return an Iterator over all the newly added TdbPublishers. */
    public Iterator<TdbPublisher> newTdbPublisherIterator() {
      return newPublishers.iterator();
    }

    /** @return an Iterator over all the newly added TdbTitles, including
     * those belonging to newly added TdbPublishers. */
    public Iterator<TdbTitle> newTdbTitleIterator() {
      return new IteratorChain(new ObjectGraphIterator(newPublishers.iterator(),
						       TITLE_ITER_XFORM),
			       newTitles.iterator());
    }

    /** @return an Iterator over all the newly added TdbAus, including
     * those belonging to newly added TdbTitles and TdbPublishers. */
    public Iterator<TdbAu> newTdbAuIterator() {
      return new IteratorChain(new Iterator[] {
	  new ObjectGraphIterator(newPublishers.iterator(),
				  AU_ITER_XFORM),
	  new ObjectGraphIterator(newTitles.iterator(),
				  AU_ITER_XFORM),
	  newAus.iterator()
	});
    }

    /** Return the {@link TdbPublisher}s that appear in the new Tdb and not
     * the old. */
    public List<TdbPublisher> rawNewTdbPublishers() {
      return newPublishers;
    }

    /** Return the {@link TdbTitle}s that have been added to existing
     * {@link TdbPublisher}s.  To get the entire set of new titles, use
     * {@link #newTdbTitleIterator()}. */
    public List<TdbTitle> rawNewTdbTitles() {
      return newTitles;
    }

    /** Return the {@link TdbAu}s that have been added to existing
     * {@link TdbTitle}s.  To get the entire set of new AUs, use
     * {@link #newTdbAuIterator()}. */
    public List<TdbAu> rawNewTdbAus() {
      return newAus;
    }

    void addPluginId(String id) {
      diffPluginIds.add(id);
    }

    void addPublisher(TdbPublisher pub, Type type) {
      switch (type) {
      case New:
	newPublishers.add(pub);
	break;
      }
      pub.addAllPluginIds(this);
    }

    void addTitle(TdbTitle title, Type type) {
      switch (type) {
      case New:
	newTitles.add(title);
	break;
      }
      title.addAllPluginIds(this);
    }

    void addAu(TdbAu au, Type type) {
      switch (type) {
      case New:
	newAus.add(au);
	break;
      }
      addPluginId(au.getPluginId());
    }

    static class Unmodifiable extends Differences {
      void addPublisher(TdbPublisher pub, Type type) {
	throw new UnsupportedOperationException("Can't modify unmodifiable Differences");
      }

      void addTitle(TdbTitle title, Type type) {
	throw new UnsupportedOperationException("Can't modify unmodifiable Differences");
      }

      void addAu(TdbAu au, Type type) {
	throw new UnsupportedOperationException("Can't modify unmodifiable Differences");
      }
      void addPluginId(String id) {
	throw new UnsupportedOperationException("Can't modify unmodifiable Differences");
      }
    }
  }
}
