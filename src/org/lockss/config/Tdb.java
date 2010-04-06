/*
 * $Id: Tdb.java,v 1.4 2010-04-06 18:14:51 pgust Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
n
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.lockss.util.Logger;

/**
 * This class represents a title database (TDB).  The TDB consists of
 * hierarchy  of <code>TdbPublisher</code>s, and <code>TdbAu</code>s. 
 * Special indexing provides fast access to all <code>TdbAu</code>s for 
 * a specified plugin ID. 
 *
 * @author  Philip Gust
 * @version $Id: Tdb.java,v 1.4 2010-04-06 18:14:51 pgust Exp $
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
  private final Map<String, Collection<TdbAu>> pluginTdbAusMap = new HashMap<String,Collection<TdbAu>>();
  
  /**
   * Map of publisher names to TdBPublishers for this configuration
   */
  private final Map<String, TdbPublisher> tdbPublisherMap = new HashMap<String,TdbPublisher>();

  /**
   * Determines whether more AUs can be added.
   */
  private boolean isSealed = false;
  
  /**
   * The total number of TdbAus in this TDB (sum of collections in pluginTdbAus map
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
    return pluginTdbAusMap.isEmpty();
  }
  
  /**
   * Returns a collection of pluginIds for TdbAus that are
   * different from those in this Tdb.
   * 
   * @param otherTdb a Tdb
   * @return a collection of pluginIds that are different,
   * , or all plugin Ids in this Tdb if otherTdb is <code>null</code>
   */
  public Set<String> getPluginIdsForDifferences(Tdb otherTdb) {
    if (otherTdb == null) {
      return pluginTdbAusMap.keySet();
    }
    if (otherTdb == this) {
      return Collections.EMPTY_SET;
    }
    
    Set<String> pluginIds = new HashSet<String>();
    addPluginIdsForDifferences (pluginIds, otherTdb);
    return pluginIds;
  }
  
  /**
   * Adds a collection of pluginIds for TdbAus that are
   * different from those in this Tdb.
   * 
   * @param pluginIds the set of pluginIds
   * @param otherTdb a Tdb
   */
  private void addPluginIdsForDifferences(Set<String> pluginIds, Tdb otherTdb) {
    Map<String, TdbPublisher> tdbPublishers = otherTdb.getAllTdbPublishers();

    for (TdbPublisher tdbPublisher : tdbPublishers.values()) {
      if (!this.tdbPublisherMap.containsKey(tdbPublisher.getName())) {
        // add pluginIds for publishers in tdb that are not in this Tdb
        tdbPublisher.addAllPluginIds(pluginIds);
      }
    }

    for (TdbPublisher thisPublisher : tdbPublisherMap.values()) {
      TdbPublisher tdbPublisher = tdbPublishers.get(thisPublisher.getName());
      if (tdbPublisher == null) {
        // add pluginIds for publisher in this Tdb that is not in tdb
        thisPublisher.addAllPluginIds(pluginIds);
      } else {
        // add pluginIds for publishers in both Tdbs that are different 
        thisPublisher.addPluginIdsForDifferences(pluginIds, tdbPublisher);
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
        addPluginIdsForDifferences(Collections.EMPTY_SET, (Tdb)o);
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
   * Merge another Tdb into this one. Makes copies of otherTdb's non-duplicate 
   * TdbPublisher, TdbTitle, and TdbAu objects and their non-duplicate children.
   * The object themselves are not merged.
   * 
   * @param otherTdb the other Tdb
   */
  public void copyFrom(Tdb otherTdb) {
    // ignore inappropriate Tdb values
    if ((otherTdb == null) || (otherTdb == this)) {
      return;
    }
    
    // merge non-duplicate publishers of otherTdb
    for (TdbPublisher otherPublisher : otherTdb.getAllTdbPublishers().values()) {
      String pubName = otherPublisher.getName();
      TdbPublisher thisPublisher = tdbPublisherMap.get(pubName);
      if (thisPublisher == null) {
        // copy publisher if not present in this Tdb
        thisPublisher = new TdbPublisher(pubName);
        tdbPublisherMap.put(pubName, thisPublisher);
      }
      
      // merge non-duplicate titles of otherPublisher into thisPublisher
      for (TdbTitle otherTitle : otherPublisher.getTdbTitles()) {
        String otherId = otherTitle.getId();
        TdbTitle thisTitle = thisPublisher.getTdbTitleById(otherId);
        if (thisTitle == null) {
          // copy title if not present in this publisher
          thisTitle = otherTitle.copyForTdbPublisher(thisPublisher);
        } else if (! thisTitle.getName().equals(otherTitle.getName())) {
          // error because it could lead to a missing title -- one probably has a typo
          // (what about checking other title elements too?)
          logger.error("Ignorning duplicate title entry: \"" + otherTitle.getName() 
                       + "\" with the same ID as \"" + thisTitle.getName() + "\"");
        }
        
        // merge non-duplicate TdbAus of otherTitle into thisTitle
        for (TdbAu otherAu : otherTitle.getTdbAus()) {
          TdbAu thisAu = thisTitle.getTdbAuById(otherAu.getId());
          if (thisAu == null) {
            thisAu = otherAu.copyForTdbTitle(thisTitle);
            registerTdbAu(thisAu);
          } else if (thisAu.getName().equals(otherAu.getName())) {
            logger.warning("Ignoring duplicate au entry: \"" + otherAu.getName() + "\"");
          } else {
            // error because it could lead to a missing AU -- one probably has a typo
            logger.error("Ignorning duplicate au entry: \"" + otherAu.getName() 
                         + "\" with the same definition as \"" + thisAu.getName() + "\"");
          }
        }
      }
    }
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
  public Collection<TdbAu> getTdbAus(String pluginId) {
    if (pluginTdbAusMap == null) {
      return Collections.EMPTY_LIST;
    }
    Collection<TdbAu> aus = pluginTdbAusMap.get(pluginId);
    return (aus != null) ? aus : Collections.EMPTY_LIST;
  }
  
  /**
   * Returns the TdbAus for all plugin IDs. 
   * <p>
   * Note: the returned map should not be modified.
   * 
   * @return the TdbAus for all plugin IDs
   */
  public Map<String, Collection<TdbAu>> getAllTdbAus() {
    return (pluginTdbAusMap != null) ? pluginTdbAusMap : Collections.EMPTY_MAP; 
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
   * Add a new TdbAu to this title database. The TdbAu must have
   * its pluginID, and title set.  The TdbAu''s title must also have 
   * its titleId and publisher set. The publisher name must be unique
   * to all publishers in this Tdb.
   * 
   * @param au the TdbAu to add.
   * @throws IllegalStateException if Tdb is sealed
   */
  public void addTdbAu(TdbAu au) {
    if (au == null) {
      throw new IllegalArgumentException("TdbAu cannot be null");
    }
    
    // verify not sealed
    if (isSealed()) {
      throw new IllegalStateException("cannot add TdbAu to sealed Tdb");
    }
    
    // ensure plugin ID is set
    String pluginId = au.getPluginId();
    if (pluginId == null) {
      throw new IllegalArgumentException("TdbAu plugin ID cannot be null");
    }
    
    // validate title
    TdbTitle title = au.getTdbTitle();
    if (title == null) {
      throw new IllegalArgumentException("TdbAu's title not set");
    }
    
    // validate titleID
    String titleId = title.getId();
    if (titleId == null) {
      throw new IllegalArgumentException("TdbAu's title ID not set"); 
    }

    // validate publisher
    TdbPublisher publisher = title.getTdbPublisher();
    if (publisher == null) {
      throw new IllegalArgumentException("TdbAu's publisher not set");
    }

    // make sure publisher is not a duplicate
    String publisherName = publisher.getName();
    TdbPublisher oldPublisher = tdbPublisherMap.get(publisherName);
    if ((oldPublisher != null) && (oldPublisher != publisher)) {
      throw new IllegalArgumentException("new au publisher with duplicate name: " + publisherName);
    }
   
    // register the au with this instance
    registerTdbAu(au);
    
    // if that was successful, OK to add new publisher to publisher map
    if (oldPublisher == null) {
      tdbPublisherMap.put(publisherName, publisher);
    }
  }
  
  /**
   * Register the TdbAu with this Tdb and perform necessary bookeeping.
   * 
   * @param au the TdbAu
   */
  private void registerTdbAu(TdbAu au) {
    // add AU to list for plugins 
    String pluginId = au.getPluginId();
    Collection<TdbAu> aus = pluginTdbAusMap.get(pluginId);
    if (aus == null) {
      aus = new ArrayList<TdbAu>();
      pluginTdbAusMap.put(pluginId, aus);
    } else if (aus.contains(au)) {
      // cannot register TdbAU that is already registered
      throw new IllegalStateException("cannot add au \"" + au.getName() 
                                      + "\": another au with id \"" + au.getId()
                                      + "\" already exists");
    }
    aus.add(au);
    
    // increment the total AU count;
    tdbAuCount++;
  }

  /**
   * Add a new TdbAu from properties.  This method recognizes
   * properties of the following form:
   * <pre>
   * Properties p = new Properties(); 
   * p.setProperty("title", "Air & Space Volume 1)");
   * p.setProperty("journalTitle", "Air and Space");
   * p.setProperty("plugin", org.lockss.plugin.smithsonian);
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
   * @throws IllegalStateException if this Tdb is sealed
   */
  public TdbAu addTdbAuFromProperties(Properties props) {
    if (props == null) {
      throw new IllegalArgumentException("properties cannot be null");
    }

    // verify not sealed
    if (isSealed()) {
      throw new IllegalStateException("cannot add au to sealed TDB");
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
      throw new IllegalArgumentException("au plugin ID not specified");
    }
    
    String auName = props.getProperty("title");
    if (auName == null) {
      throw new IllegalArgumentException("TdbAu title not specified");
    }
    
    // create a new TdbAu and set its elements
    TdbAu au = new TdbAu(auName);
    au.setPluginId(pluginId);

    // process attrs, and params
    Map<String, Map<String,String>> paramMap = new HashMap<String, Map<String,String>>();
    for (Map.Entry<Object,Object> entry : props.entrySet()) {
      String key = ""+entry.getKey();
      String value = ""+entry.getValue();
      if (key.startsWith("attributes.")) {
        // set attributes directly
        String name = key.substring("attributes.".length());
        au.setAttr(name, value);
        
      } else if (key.startsWith("param.")) {
        // skip to param name
        String param = key.substring("param.".length());
        int i;
        if (   ((i = param.indexOf(".key")) < 0)
            && ((i = param.indexOf(".value")) < 0)) {
          logger.warning("Ignoring unexpected param key for au \"" + auName + "\" key: \"" + key + "\"");
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
        au.setPropertyByName(key, value);
      }
    }
    
    // set param from accumulated "key", and "value" entries
    for (Map<String, String> pmap : paramMap.values()) {
      String name = pmap.get("key");
      String value = pmap.get("value");
      if ((name == null) || (value == null)) {
        logger.warning("Ignoring property with null name");
      } else if (value == null) {
        logger.warning("Ignoring property \"" + name + "\" with null value");
      } else {
        au.setParam(name, value);
      }
    }

    return au;
  }
  
  /**
   * Add an TdbAu to a TdbTitle and TdbPubisher, and adds links to 
   * the TdbTitle specified by the properties.  The TdbAu plugin ID
   * must be set before adding it to the Tdb.
   * 
   * @param props the properties
   * @param au the TdbAu to add
   */
  private void addTdbAu(Properties props, TdbAu au) {
    if (au.getPluginId() == null) {
      throw new IllegalArgumentException("TdbAu plugin ID cannot be null");
    }

    // get or create the TdbTitle for this 
    TdbTitle title = getTdbTitle(props, au);

    // add AU to title and to list of AUs for its plugin 
    title.addTdbAu(au);
    
    // register the au with this instance
    registerTdbAu(au);
    
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
    String titleId = props.getProperty("journal.id");  // proposed new property
    if (titleId == null) {
      // use "journal_id" param as title Id if not already set
      // proposed to replace with "journal.id" property
      titleId = au.getParam("journal_id");
    }

    // use ISSN property as title id if not already set
    // proposed to eliminate in favor of journal.id or
    // perhaps leave this are rename to journal.issn
    String issn = props.getProperty("issn");
    if (titleId == null) {
      titleId = issn;
    }
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

    // get the title ID 
    String titleId = getTdbTitleId(props, au);
    if (titleId != null) {
      title = getTdbTitleForId(titleId);
    }
    if (title != null) {
      // sanity check -- stated publisher name should match one for matching title
      String pubName = title.getTdbPublisher().getName();
      String pubNameFromProps = getTdbPublisherName(props, au); 
      if (pubNameFromProps.startsWith(UNKNOWN_PUBLISHER_PREFIX)) {
        logger.warning(  "Publisher not specified for au \"" + au.getName() 
                       + "\". Using known publisher \"" + pubName + "\"");
      } else if (!pubName.equals(pubNameFromProps)) {
        logger.warning("Ignoring publisher for au \"" + au.getName()
                       + "\": \"" + pubNameFromProps 
                       + "\". Does not match known publisher \"" + pubName 
                       + "\" for title \"" + title.getName() + "\"");
      }
    } else {
      // get or create the publisher
      TdbPublisher publisher = getTdbPublisher(props, au);
      
      // get the title name 
      String titleName = getTdbTitleName(props, au);

      // find a matching title for this publisher
      if (titleId != null) {
        title = publisher.getTdbTitleById(titleId); 
      } else {
        // locate title with no title ID by name
        Collection<TdbTitle> titles = publisher.getTdbTitlesByName(titleName);
        for (TdbTitle aTitle : titles) {
          if (aTitle.getId() == null) {
            title = aTitle;
            break;
          }
        }
      }

      // create and add title to publisher if not found
      if (title == null) {
        // create new title and add to publisher
        if (titleName.startsWith(UNKNOWN_TITLE_PREFIX)) {
          logger.warning("Journal title name missing. Using: \"" + titleName + "\"");
        }
        title = new TdbTitle(titleName);
        if (titleId != null) {
          title.setId(titleId);
        }
        publisher.addTdbTitle(title);
      }
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
   * Find or create a TdbPubisher from the properties and the TdbAu.
   * 
   * @param props the properties
   * @param au the TdbAu instance
   * @return a TdbPublisher
   */
  private TdbPublisher getTdbPublisher(Properties props, TdbAu au) {
    // get the publisher name form the properties
    String publisherName = getTdbPublisherName(props, au);

    // find or add publisher
    TdbPublisher publisher = tdbPublisherMap.get(publisherName);
    if (publisher == null) {
      if (publisherName.startsWith(UNKNOWN_PUBLISHER_PREFIX)) {
        logger.warning("Publisher missing for au \"" + au.getName() + "\". Using: \"" + publisherName + "\"");
      }
      // create new publisher and add to publisher map
      publisher = new TdbPublisher(publisherName);
      tdbPublisherMap.put(publisherName, publisher);
    }
    
    return publisher;
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
    String titleName = props.getProperty("journalTitle");
    if (titleName == null) {
      titleName = props.getProperty("journal.title");  // proposed to replace journalTitle
    }
    if (titleName == null) {
      String issue = au.getParam("issue");
      String year = au.getParam("year");
      String volume = au.getParam("volume");
      if (volume == null) {
        volume = au.getParam("volume_str");
      }
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
   * @param linkType the link type {@see TdbTitle} for description of link types
   * @param title the TdbTitle with links
   * @return a collection of linked titles for the specified type 
   */
  public Collection<TdbTitle> getLinkedTdbTitlesForType(TdbTitle.LinkType linkType, TdbTitle title) {
    if (linkType == null) {
      throw new IllegalArgumentException("linkType cannot be null");
    }
    if (title == null) {
      throw new IllegalArgumentException("title cannot be null");
    }
    Collection<String> titleIds = title.getLinkedTdbTitleIdsForType(linkType);
    if (titleIds.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    ArrayList<TdbTitle> titles = new ArrayList<TdbTitle>();
    for (String titleId : titleIds) {
      TdbTitle aTitle = getTdbTitleForId(titleId);
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
  public TdbTitle getTdbTitleForId(String titleId)
  {
    if (titleId == null) {
      throw new IllegalArgumentException("titleId cannot be null");
    }
    
    TdbTitle title = null;
    for (TdbPublisher publisher : tdbPublisherMap.values()) {
      title = publisher.getTdbTitleById(titleId);
      if (title != null) {
        break;
      }
    }
    return title;
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
    return (tdbPublisherMap != null) ? tdbPublisherMap : Collections.EMPTY_MAP;
  }

}
