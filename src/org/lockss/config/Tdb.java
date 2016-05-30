/*
 * $Id$
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
 * @version $Id$
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
  private final Map<String, Map<TdbAu.Id,TdbAu>> pluginIdTdbAuIdsMap = 
    new HashMap<String,Map<TdbAu.Id,TdbAu>>(4, 1F);
  
  /**
   * Map of publisher names to TdBPublishers for this configuration
   */
  private final Map<String, TdbPublisher> tdbPublisherMap = 
    new HashMap<String,TdbPublisher>(4, 1F);

  /**
   * Map of provider names to TdBProviders for this configuration
   */
  private final Map<String, TdbProvider> tdbProviderMap = 
    new HashMap<String,TdbProvider>(4, 1F);

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
  static final String UNKNOWN_PUBLISHER_PREFIX = "Publisher of ";
  
  /**
   * Prefix appended to generated unknown publisher
   */
  static final String UNKNOWN_PROVIDER_PREFIX = "Provider of ";
  
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
   * @version $Id$
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
   * @return <code>false</code> if already registered, 
   *   otherwise <code>true</code>
   * @throws TdbException if adding new TdbAu with duplicate id
   */
  private boolean addTdbAuForPlugin(TdbAu au) throws TdbException {
    // add AU to list for plugins 
    String pluginId = au.getPluginId();
    Map<TdbAu.Id,TdbAu> auids = pluginIdTdbAuIdsMap.get(pluginId);
    if (auids == null) {
      auids = new HashMap<TdbAu.Id, TdbAu>(4, 1F);
      pluginIdTdbAuIdsMap.put(pluginId, auids);
    } 
    TdbAu otherAu = auids.put(au.getId(),au);
    if (otherAu != null) {
      // check existing entry
      if (au == otherAu) {
        // OK if same AU
        return false;
      }
      // restore otherAu and throw exception
      auids.put(otherAu.getId(),otherAu);
      
      // throw exception if adding another AU with duplicate au id.
      throw new TdbException("New au with duplicate id: " + au.getName());
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
    Map<TdbAu.Id, TdbAu> c = pluginIdTdbAuIdsMap.get(pluginId);
    if (c.remove(au.getId()) != null) {
      if (c.isEmpty()) {
        pluginIdTdbAuIdsMap.remove(c);
      }
      tdbAuCount--;
      return true;
    }
    return false;
  }

  /**
   * Add TdbProvider to Tdb
   * @param provider the TdbProvider
   * @return <code>true</code> if a new provider was added, <code>false<code>
   *   if this providerr is already added
   * @throws TdbException if trying to add new provider with duplicate name
   */
  public boolean addTdbProvider(TdbProvider provider)  throws TdbException {
    String providerName = provider.getName();
    TdbProvider oldProvider = tdbProviderMap.put(providerName, provider);
    if ((oldProvider != null) && (oldProvider != provider)) {
      // restore old publisher and report error
      tdbProviderMap.put(providerName, oldProvider);
      throw new TdbException("New au provider with duplicate name: " 
                            + providerName);
    }
    return (oldProvider == null);
  }
  
  /**
   * Add TdbPublisher to Tdb
   * @param publisher the TdbPublisher
   * @return <code>true</code> if a new publisher was added, <code>false<code>
   *   if this publisher is already added
   * @throws TdbException if trying to add new publisher with duplicate name
   */
  public boolean addTdbPublisher(TdbPublisher publisher)  throws TdbException {
    String pubName = publisher.getName();
    TdbPublisher oldPublisher = tdbPublisherMap.put(pubName, publisher);
    if ((oldPublisher != null) && (oldPublisher != publisher)) {
      // restore old publisher and report error
      tdbPublisherMap.put(pubName, oldPublisher);
      throw new TdbException("New au publisher with duplicate name: " 
                            + pubName);
    }
    return (oldPublisher == null);
  }
  
  /**
   * Add a new TdbAu to this title database. The TdbAu must have
   * its pluginID, provider, and title set.  The TdbAu's title must also 
   * have its titleId and publisher set. The publisher name must be unique
   * to all publishers in this Tdb.
   * 
   * @param au the TdbAu to add.
   * @return <code>true</code> if new AU was added, <code>false</code> if
   *   this AU was previously added
   * @throws TdbException if Tdb is sealed, this is a duplicate au, or
   *   the au's publisher or provider is a duplicate 
   */
  public boolean addTdbAu(TdbAu au) throws TdbException {
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

    // validate provider
    TdbProvider provider = au.getTdbProvider();
    if (provider == null) {
      String pubName = publisher.getName();
      // assume the provider is the same as the publisher
      provider = tdbProviderMap.get(pubName);
      if (provider == null) {
        provider = new TdbProvider(pubName);
      }
      // add publisher provider to the au
      provider.addTdbAu(au);
      if (logger.isDebug3()) {
        logger.debug3("Creating default provider for publisher " + pubName);
      }
    }

    boolean newPublisher = false;
    boolean newProvider = false;
    boolean newAu = false;
    
    try {
      // add au publisher if not already added
      newPublisher = addTdbPublisher(publisher);
    
      // add au provider if not already added
      newProvider = addTdbProvider(provider);
  
      // add au if not already added
      newAu = addTdbAuForPlugin(au);

    } catch (TdbException ex) {
      // remove new publisher and new provider, and report error 
      if (newPublisher) {
        tdbPublisherMap.remove(publisher.getName());
      }
      if (newProvider) {
        tdbProviderMap.remove(provider.getName());
      }
      TdbException ex2 = new TdbException("Cannot register au " + au.getName());
      ex2.initCause(ex);
      throw ex2;
    }
    
    return newAu;
  }
  
  /**
   * Seals a Tdb against further additions.
   */
  public void seal() {
      isSealed = true;
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
    return newTdb.computeDifferences(oldTdb);
  }

  Differences computeDifferences(Tdb oldTdb) {
    if (oldTdb == null) {
      return new AllDifferences(this);
    } else {
      return new Differences(this, oldTdb);
    }
  }

  /**
   * Adds to the {@link Tdb.Differences} the differences found between
   * oldTdb and this Tdb
   * 
   * @param the {@link Tdb.Differences} to which to add items.
   * @param otherTdb a Tdb
   */
  private void addDifferences(Differences diffs, Tdb oldTdb) {
    // process publishers
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

    // process providers
    Map<String, TdbProvider> oldProviders = oldTdb.getAllTdbProviders();

    for (TdbProvider oldProvider : oldProviders.values()) {
      if (!this.tdbProviderMap.containsKey(oldProvider.getName())) {
        // add pluginIds for providers in tdb that are not in this Tdb
        diffs.addProvider(oldProvider, Differences.Type.Old);
      }
    }

    for (TdbProvider thisProvider : tdbProviderMap.values()) {
      if (!oldTdb.tdbProviderMap.containsKey(thisProvider.getName())) {
        // add pluginIds for provider in this Tdb that is not in tdb
        diffs.addProvider(thisProvider, Differences.Type.New);
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
        String titleName = otherTitle.getName();
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
            logger.error("Ignorning duplicate title entry: \"" + titleName 
                         + "\" with the same ID as \"" 
                         + thisTitle.getName() + "\"");
          }
        }
        
        // merge non-duplicate TdbAus of otherTitle into thisTitle
        for (TdbAu otherAu : otherTitle.getTdbAus()) {
          String auName = otherAu.getName();
          String providerName = otherAu.getProviderName();
          TdbAu thisAu = getTdbAuById(otherAu);
          if (thisAu == null) {
            // copy new TdbAu
            thisAu = otherAu.copyForTdbTitle(thisTitle);
            // add copy to provider of same name
            TdbProvider thisProvider = getTdbProvider(providerName);
            if (thisProvider == null) {
              // add new provider to this Tdb
              thisProvider = new TdbProvider(providerName);
              tdbProviderMap.put(providerName, thisProvider);
            }
            thisProvider.addTdbAu(thisAu);
            // finish adding this AU
            addTdbAuForPlugin(thisAu);
          } else {
            // ignore and log error if existing AU is not identical
            if (   !thisAu.getName().equals(auName)
                || !thisAu.getTdbTitle().getName().equals(titleName)
                || !thisAu.getTdbPublisher().getName().equals(pubName)
                || !thisAu.getTdbProvider().getName().equals(providerName)) {
              logger.error("Ignoring duplicate au entry id \"" + thisAu.getId()
                         + " for provider \"" + providerName
                         + "\" (" + thisAu.getProviderName() + "), publisher \""
                         + pubName + "\" (" + thisAu.getPublisherName() 
                         + "), title \"" + titleName + "\" (" 
                         + thisAu.getPublicationTitle() + "), name \"" 
                         + auName + "\" (" + thisAu.getName() + ")");
            }
          }
        }
      }
    }
  }
  
  /**
   * Get existing TdbAu with same Id as another one.
   * @param otherAu another TdbAu
   * @return an existing TdbAu already in thisTdb
   */
  public TdbAu getTdbAuById(TdbAu otherAu) {
    Map<TdbAu.Id,TdbAu> map = pluginIdTdbAuIdsMap.get(otherAu.getPluginId());
    return (map == null) ? null : map.get(otherAu.getId());
  }
  
  /**
   * Get existing TdbAu with the the specified id.
   * @param auId the TdbAu.Id
   * @return the existing TdbAu or <code>null</code> if not in this Tdb
   */
  public TdbAu getTdbAuById(TdbAu.Id auId) {
    return getTdbAuById(auId.getTdbAu());
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
    Map<TdbAu.Id,TdbAu> auIdMap = pluginIdTdbAuIdsMap.get(pluginId);
    return (auIdMap != null) ? 
        auIdMap.keySet() : Collections.<TdbAu.Id>emptyList();
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
    for (Map<TdbAu.Id,TdbAu> auIdMap : pluginIdTdbAuIdsMap.values()) {
      allAuIds.addAll(auIdMap.keySet());
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
   * Return the number of TdbProviders in this Tdb.
   * 
   * @return the total TdbProvider count
   */
  public int getTdbProviderCount() {
    return tdbProviderMap.size();
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
    
    // generate new TdbAu from properties
    TdbAu au = newTdbAu(props);

    // add au for plugin assuming it is not a duplicate
    try {
      addTdbAuForPlugin(au);
    } catch (TdbException ex) {
      // au already registered -- report existing au
      TdbAu existingAu = getTdbAuById(au);
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
    
    // get or create the TdbProvider for this 
    TdbProvider provider = getTdbProvider(props, au);
    try {
      // add AU to title 
      provider.addTdbAu(au);
    } catch (TdbException ex) {
      // if we can't add au to provider, remove for plugin and title,
      // and re-throw exception
      removeTdbAuForPlugin(au);
      // TODO: what to do about unregistering with the tdbTitle?
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
   * Get or create TdbProvider for the specified properties and TdbAu.
   * 
   * @param props the properties
   * @param au the TdbAu
   * @return the corresponding TdbProvider
   */
  private TdbProvider getTdbProvider(Properties props, TdbAu au) {
    // get publisher name
    String providerNameFromProps = getTdbProviderName(props, au);
    
    // get publisher specified by property name
    TdbProvider provider = tdbProviderMap.get(providerNameFromProps);
    if (provider == null) {
      // warn of missing provider name
      if (providerNameFromProps.startsWith(UNKNOWN_PROVIDER_PREFIX)) {
        logger.warning("Provider missing for au \"" + au.getName() 
                     + "\" -- using \"" + providerNameFromProps + "\"");
      }

      // create new publisher for specified publisher name
      provider = new TdbProvider(providerNameFromProps);
      tdbProviderMap.put(providerNameFromProps, provider);
    }
    return provider;
  }
  
  /**
   * Get provider name from properties and TdbAu. Uses the
   * pubisher name as the provider name if not specified.
   * 
   * @param props the properties
   * @param au the TdbAu
   * @return the provider name
   */
  private String getTdbProviderName(Properties props, TdbAu au) {
    // Use "provider" attribute if specified. 
    // Otherwise use the publisher name as the provider name.
    String providerName = props.getProperty("attributes.provider");
    if (providerName == null) {
      providerName = au.getPublisherName();
      if (providerName == null) {
        providerName = getTdbPublisherName(props, au);
        if (providerName == null) {
          // create provider name from au name as last resort
          providerName = UNKNOWN_PROVIDER_PREFIX + "[" + au.getName() + "]";
        }
      }
    }

    return providerName;
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
   * Get the title for the specified titleId. Only the first matchine 
   * title is returned.
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
   * Get the TdbTitles with the specified titleid.
   * @param titleId the title ID
   * @return a collection of matching titleids
   */
  public Collection<TdbTitle> getTdbTitlesById(String titleId) {
    Collection<TdbTitle> tdbTitles = new ArrayList<TdbTitle>();
    getTdbTitlesById(titleId, tdbTitles);
    return tdbTitles;
  }
  
  
  /**
   * Add the TdbTitles with the specified titleid to the collection.
   * @param titleId the title ID
   * @param matchingTdbTitles the collection of matching TdbTitles
   * @return <code>true</code> if items were added, else <code>false></code>
   */
  public boolean getTdbTitlesById(String titleId, 
                                  Collection<TdbTitle> matchingTdbTitles) {
    boolean added = false;
    if (titleId != null) {
      for (TdbPublisher publisher : tdbPublisherMap.values()) {
        // titleId constrained to be unique for a given publisher
        TdbTitle title = publisher.getTdbTitleById(titleId);
        if (title != null) {
          added |= matchingTdbTitles.add(title);
        }
      }
    }
    return added;
  }
  
  /**
   * Get a title for the specified issn. Only the first matching title 
   * is returned.
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
   * Return a collection of TdbTitles for this TDB that match the ISSN.
   * @param issn the ISSN
   * @return a colleciton of TdbTitles that match the ISSN
   */
  public Collection<TdbTitle> getTdbTitlesByIssn(String issn) {
    Collection<TdbTitle> tdbTitles = new ArrayList<TdbTitle>();
    getTdbTitlesByIssn(issn, tdbTitles);
    return tdbTitles;
  }
  
  /**
   * Add a collection of TdbTitles for this TDB that match the ISBN.
   * @param issn the ISSN
   * @param matchingTdbTitles the collection of TdbTitles
   * @return <code>true</code> if titles were added, else <code>false</code>
   */
  public boolean getTdbTitlesByIssn(String issn, 
                                    Collection<TdbTitle> matchingTdbTitles) {
    boolean added = false;
    if (issn != null) {
      for (TdbPublisher publisher : tdbPublisherMap.values()) {
        added |= publisher.getTdbTitlesByIssn(issn, matchingTdbTitles);
      }
    }    
    return added;
  }
  
  /**
   * Return a collection of TdbAus for this TDB that match the ISBN.
   * 
   * @return a colleciton of TdbAus that match the ISBN
   */
  public Collection<TdbAu> getTdbAusByIsbn(String isbn) {
    Collection<TdbAu> tdbAus = new ArrayList<TdbAu>();
    getTdbAusByIsbn(isbn, tdbAus);
    return tdbAus;
  }
  
  /**
   * Add to a collection of TdbAus for this TDB that match the ISBN.
   * @param isbn the ISSN
   * @param matchingTdbAus
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
  public Collection<TdbAu> getTdbAusByName(String tdbAuName) {
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

  /**
   * Get the publisher for the specified name.
   * 
   * @param name the publisher name
   * @return the publisher, or <code>null</code> if not found
   */
  public TdbProvider getTdbProvider(String name) {
    return (tdbProviderMap != null) ? tdbProviderMap.get(name) : null;
  }

  /**
   * Returns all TdbPubishers in this configuration. 
   * <p>
   * Note: The returned map should not be modified.
   * 
   * @return a map of publisher names to publishers
   */
  public Map<String, TdbProvider> getAllTdbProviders() {
    return (tdbProviderMap != null) 
      ? tdbProviderMap : Collections.<String,TdbProvider>emptyMap();
  }

  /** ObjectGraphIterator Transformer that descends into collections of
   * TdbPublishers and TdbTitles, returning TdbAus */
  static Transformer AU_ITER_XFORM = new Transformer() {
      public Object transform(Object input) {
	if (input instanceof TdbPublisher) {
	  return ((TdbPublisher)input).tdbTitleIterator();
	}
	if (input instanceof TdbTitle) {
	  return ((TdbTitle)input).tdbAuIterator();
	}
	if (input instanceof TdbAu) {
	  return input;
	}
	if (input instanceof TdbProvider) {
	  return ((TdbProvider)input).tdbAuIterator();
	}
	throw new
	  ClassCastException(input+" is not a TdbPublisher, TdbTitle or TdbAu");
      }};

  /** ObjectGraphIterator Transformer that descends into collections of
   * TdbPublishers, returning TdbTitles */
  static Transformer TITLE_ITER_XFORM = new Transformer() {
      public Object transform(Object input) {
	if (input instanceof TdbPublisher) {
	  return ((TdbPublisher)input).tdbTitleIterator();
	}
	if (input instanceof TdbTitle) {
	  return input;
	}
	throw new
	  ClassCastException(input+" is not a TdbPublisher or TdbTitle");
      }};


  /** @return an Iterator over all the TdbProviders in this Tdb. */
  public Iterator<TdbProvider> tdbProviderIterator() {
    return tdbProviderMap.values().iterator();
  }

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

    // newly added providers
    private final Set<TdbProvider> newProviders
      = new HashSet<TdbProvider>();;
    // newly added publishers
    private final Set<TdbPublisher> newPublishers
      = new HashSet<TdbPublisher>();;
    // titles that have been added to existing publishers
    private final Set<TdbTitle> newTitles = new HashSet<TdbTitle>();;
    // AUs that have been added to existing titles
    private final Set<TdbAu> newAus = new HashSet<TdbAu>();
    // Retained for compatibility with legacy Plugin/TitleConfig mechanism
    private final Set<String> diffPluginIds = new HashSet<String>();
    private int tdbAuCountDiff;
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[Tdb.Diffa");
      if (!newPublishers.isEmpty()) {
	sb.append(" newPubs: ");
	sb.append(newPublishers);
      }
      if (!newTitles.isEmpty()) {
	sb.append(" newTitles: ");
	sb.append(newTitles);
      }
      if (!newAus.isEmpty()) {
	sb.append(" newAus: ");
	sb.append(newAus);
      }
      sb.append("]");
      return sb.toString();
    }

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

    /** @return an Iterator over all the newly added or changed TdbAus, */
    public Iterator<TdbAu> newTdbAuIterator() {
      return new IteratorChain(new Iterator[] {
	  new ObjectGraphIterator(newPublishers.iterator(),
				  AU_ITER_XFORM),
	  new ObjectGraphIterator(newTitles.iterator(),
				  AU_ITER_XFORM),
	  newAus.iterator()
	});
    }

    /** Return the {@link TdbProvider}s that appear in the new Tdb and not
     * the old. */
    public Set<TdbProvider> rawNewTdbProviders() {
      return newProviders;
    }

    /** Return the {@link TdbPublisher}s that appear in the new Tdb and not
     * the old. */
    public Set<TdbPublisher> rawNewTdbPublishers() {
      return newPublishers;
    }

    /** Return the {@link TdbTitle}s that have been added to existing
     * {@link TdbPublisher}s.  To get the entire set of new titles, use
     * {@link #newTdbTitleIterator()}. */
    public Set<TdbTitle> rawNewTdbTitles() {
      return newTitles;
    }

    /** Return the {@link TdbAu}s that have been added to existing
     * {@link TdbTitle}s.  To get the entire set of new AUs, use
     * {@link #newTdbAuIterator()}. */
    public Set<TdbAu> rawNewTdbAus() {
      return newAus;
    }

    void addPluginId(String id) {
      diffPluginIds.add(id);
    }

    void addProvider(TdbProvider provider, Type type) {
      switch (type) {
      case New:
        newProviders.add(provider);
        break;
      }
      provider.addAllPluginIds(this);
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
      void addProvider(TdbProvider provider, Type type) {
        throw new UnsupportedOperationException(
            "Can't modify unmodifiable Differences");
      }
      void addPublisher(TdbPublisher pub, Type type) {
	throw new UnsupportedOperationException(
	    "Can't modify unmodifiable Differences");
      }
      void addTitle(TdbTitle title, Type type) {
	throw new UnsupportedOperationException(
	    "Can't modify unmodifiable Differences");
      }
      void addAu(TdbAu au, Type type) {
	throw new UnsupportedOperationException(
	    "Can't modify unmodifiable Differences");
      }
      void addPluginId(String id) {
	throw new UnsupportedOperationException(
	    "Can't modify unmodifiable Differences");
      }
    }
  }

  /** Implements Differences(tdb, null) efficiently */
  public static class AllDifferences extends Differences  {
    private Tdb tdb;
    
    public String toString() {
      return "[Tdb.Diffa: all]";
    }

    AllDifferences(Tdb newTdb) {
      tdb = newTdb;
    }

    /** Return the ID of every plugin that has at least one changed or
     * added or removed AU */
    Set<String> getPluginIdsForDifferences() {
      return tdb.pluginIdTdbAuIdsMap.keySet();
    }

    /** Return the difference in number of AUs from old to new Tdb. */
    public int getTdbAuDifferenceCount() {
      return tdb.getTdbAuCount();
    }

    /** @return an Iterator over all the newly added TdbPublishers. */
    public Iterator<TdbProvider> newTdbProviderIterator() {
      return tdb.tdbProviderIterator();
    }

    /** @return an Iterator over all the newly added TdbPublishers. */
    public Iterator<TdbPublisher> newTdbPublisherIterator() {
      return tdb.tdbPublisherIterator();
    }

    /** @return an Iterator over all the newly added TdbTitles, including
     * those belonging to newly added TdbPublishers. */
    public Iterator<TdbTitle> newTdbTitleIterator() {
      return tdb.tdbTitleIterator();
    }

    /** @return an Iterator over all the newly added or changed TdbAus, */
    public Iterator<TdbAu> newTdbAuIterator() {
      return tdb.tdbAuIterator();
    }

    /** Return the {@link TdbProvider}s that appear in the new Tdb and not
     * the old. */
    public Set<TdbProvider> rawNewTdbProviders() {
      return SetUtil.fromIterator(newTdbProviderIterator());
    }

    /** Return the {@link TdbPublisher}s that appear in the new Tdb and not
     * the old. */
    public Set<TdbPublisher> rawNewTdbPublishers() {
      return SetUtil.fromIterator(newTdbPublisherIterator());
    }

    /** Return the {@link TdbTitle}s that have been added to existing
     * {@link TdbPublisher}s.  To get the entire set of new titles, use
     * {@link #newTdbTitleIterator()}. */
    public Set<TdbTitle> rawNewTdbTitles() {
      return Collections.emptySet();
    }

    /** Return the {@link TdbAu}s that have been added to existing
     * {@link TdbTitle}s.  To get the entire set of new AUs, use
     * {@link #newTdbAuIterator()}. */
    public Set<TdbAu> rawNewTdbAus() {
      return Collections.emptySet();
    }

  }
}
