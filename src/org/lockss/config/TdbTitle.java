/*
 * $Id: TdbTitle.java,v 1.1 2010-04-02 23:13:42 pgust Exp $
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.lockss.util.Logger;

/**
 * This class represents a title database publisher.
 *
 * @author  Philip Gust
 * @version $Id: TdbTitle.java,v 1.1 2010-04-02 23:13:42 pgust Exp $
 */
public class TdbTitle {
  /**
   * Set up logger
   */
  protected final static Logger logger = Logger.getLogger("TdbTitle");

  /**
   * This enumeration defines a relationship between two titles,
   * corresponding to field 785 of a MARC record.
   * <p>
   * The first half of the enumeration represents forward relationships,
   * while the second half represents corresponding backward relationships
   * @author phil
   *
   */
  static public enum LinkType{
    // forward relationships
    continuedBy,
    continuedInPartBy,
    supersededBy,
    supersededInPartBy,
    absorbedBy,
    absorbedInPartBy,
    splitInto,
    mergedWith,
    changedBackTo,
    
    // backward relationships
    continues,
    continuesInPart,
    supersedes,
    supersedesInPart,
    absorbs,
    absorbsInPart,
    splitFrom,
    mergedFrom,
    changedBackFrom;
    
    /**
     * Return the inverse link type for this link type.  If this is a forward
     * link type, returns the backward link type.  If this is a backward
     * link type, returns the forward one.
     * 
     * @return the inverse link type
     */
    public LinkType inverseLinkType() {
      LinkType[] values = LinkType.values();
      return values[(ordinal() < values.length/2) ? (ordinal() + values.length/2) : (ordinal()-values.length/2)];
    }
    
    /**
     * Determines whether this is a forward link type.
     * 
     * @return <code>true</code> if a forward link type
     */
    public boolean isForwardLinkType() {
      return (ordinal() < LinkType.values().length/2);
    }
  }

  /**
   * The title name
   */
  final private String name;
  
  /**
   * The title ID
   */
  private String id;
  
  /**
   * The publisher for this title
   */
  TdbPublisher publisher = null;
  
  /**
   * A collection of AUs for this title
   */
  private final ArrayList<TdbAu> tdbAus = new ArrayList<TdbAu>();

  /**
   * A map of link types to a collection of title IDs
   */
  private Map<LinkType, Collection<String>> linkTitles;
  
  /**
   * Create a new instance for the specified name.
   * 
   * @param name the title name
   */
  protected TdbTitle(String name)
  {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    
    this.name = name;
  }
  
  /**
   * Return the name of this title. The name is not guaranteed
   * to be unique within a given publisher
   * 
   * @return the name of this title
   */
  public String getName() {
    return name;
  }
  
  /**
   * Return the ID of this title. The ID is guaranteed to be
   * globally unique.  For example, the ID of a journal may be
   * its ISSN.
   * <p>
   * If ID is not set by the time this instance is added to its
   * TdbPublisher, the publisher will generate and assign one
   * to this instance.
   * 
   * @return the ID of this title or <code>null</code> if not set
   */
  public String getId() {
    return id;
  }
  
  /**
   * Sets the ID of this title.  The ID must be one that is
   * globally unique.  For example, the ID of a journal may be
   * its ISSN.
   * <p>
   * If ID is not set by the time this instance is added to its
   * TdbPublisher, the publisher will generate and assign one
   * to this instance.
   * 
   * @param id the ID of this title
   * @throws IllegalStateException if the ID is already set
   */
  protected void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if (this.id != null) {
      throw new IllegalStateException("id canot be reset"); 
    }
    this.id = id;
  }
  
  /**
   * Return the publisher of this title.
   * 
   * @return the publisher of this title
   */
  public TdbPublisher getTdbPublisher() {
    return publisher;
  }
  
  /**
   * Set the publisher of this title.  
   * 
   * @param publisher the publisher of this title
   * @throws IllegalStateException if publisher already set
   */
  protected void setTdbPublisher(TdbPublisher publisher) {
    if (publisher == null) {
      throw new IllegalArgumentException("title publisher cannot be null");
    }
    if (this.publisher != null) {
      throw new IllegalStateException("publisher cannot be reset");
    }
    
    this.publisher = publisher;
  }
  
  /**
   * Add a title link for a specified link type.  Link types
   * define evolutionary changes of title and publisher that
   * enable queries to examine predecessor or successor titles
   * as they change over time.
   * <p>
   * Link types correspond to field 785 of the MARC format.
   * Backward (e.g. continues) as well as forward (e.g. continuedBy)
   * links can be added to facilitate navigation.
   * 
   * @param linkType the link type
   * @param title the title for the link
   */
  public void addLinkToTdbTitle(LinkType linkType, TdbTitle title) {
    if (title == null) {
      throw new IllegalArgumentException("title cannot be null");
    }
    addLinkToTdbTitleId(linkType, title.getId());
  }
  
  /**
   * Add a title link for a specified link type.  Link types
   * define evolutionary changes of title and publisher that
   * enable queries to examine predecessor or successor titles
   * as they change over time.
   * <p>
   * Link types correspond to field 785 of the MARC format.
   * Backward (e.g. continues) as well as forward (e.g. continuedBy)
   * links can be added to facilitate navigation.
   * 
   * @param linkType the link type
   * @param titleId the title ID for the link
   */
  public void addLinkToTdbTitleId(LinkType linkType, String titleId) {
    if (linkType == null) {
      throw new IllegalArgumentException("linkType cannot be null");
    }
    if (titleId == null) {
      throw new IllegalArgumentException("titleId cannot be null");
    }
    
    if (linkTitles == null) {
      if (System.getProperty("org.lockss.unitTesting", "false").equals("true")) {
        // use an ordered map to facilitate testing
        linkTitles = new LinkedHashMap<LinkType, Collection<String>>();
      } else {
        // use a standard map when not testing
        linkTitles = new HashMap<LinkType, Collection<String>>();
      }
    }
    Collection<String> targets = linkTitles.get(linkType);
    if (targets == null) {
      targets = new ArrayList<String>();
      linkTitles.put(linkType, targets);
    } else if (targets.contains(titleId)) {
      // duplicate target
      return;
    }
    targets.add(titleId);
  }
  /**
   * Returns a collection of title IDs for the specified link type.
   * <p>
   * Note: The returned collection should be treated as read-only
   * 
   * @param linkType the specified link type
   * @return
   */
  public Collection<String> getLinkedTdbTitleIdsForType(LinkType linkType) {
    if (linkType == null) {
      throw new IllegalArgumentException("linkType cannot be null");
    }
    if (linkTitles == null) {
      return Collections.EMPTY_LIST;
    }
    Collection<String> titleIds = linkTitles.get(linkType);
    return (titleIds != null) ? titleIds : Collections.EMPTY_LIST;
  }
  
  /**
   * Get all linked titles by link type.
   * <p>
   * Note: The returned map and its collections should be treated 
   * as read-only.
   * 
   * @return all linked titles by link type
   */
  public Map<LinkType,Collection<String>> getAllLinkedTitleIds() {
    return (linkTitles != null) ? linkTitles : Collections.EMPTY_MAP;
  }
  
  /**
   * Return all TdbAus for this title.
   * <p>
   * Note: the collection should be treated as read-only.
   * 
   * @return a collection of TdbAus for this title
   */
  public Collection<TdbAu> getTdbAus() {
    return tdbAus;
  }
  
  /**
   * Add a new TdbAu for this title.
   * 
   * @param tdbAu a new TdbAus
   */
  public void addTdbAu(TdbAu tdbAu) {
    if (tdbAu == null) {
      throw new IllegalArgumentException("au for title \"" + name + "\" cannot be null");
    }

    tdbAu.setTdbTitle(this);
    tdbAus.add(tdbAu);
    
  }
  
  /**
   * Return the number of TdbAus for this title.
   * 
   * @return the number of TdbAus
   */
  public int getTdbAuCount()
  {
    return tdbAus.size();
  }

  /**
   * Return the TdbAu for with the specified TdbAu name.
   * 
   * @param tdbAuName the name of the AU to select
   * @return the TdbAu for the specified name
   */
  public Collection<TdbAu> getTdbAusByName(String tdbAuName)
  {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    for (TdbAu tdbAu : tdbAus) {
      if (tdbAu.getName().equals(tdbAuName)) {
        aus.add(tdbAu);
      }
    }
    aus.trimToSize();
    return aus;
  }
  
  /**
   * Return the TdbAu for with the specified TdbAu.Key.
   * 
   * @param tdbAuId the Id of the TdbAu to select
   * @return the TdbAu for the specified key
   */
  public TdbAu getTdbAuById(TdbAu.Id tdbAuId)
  {
    for (TdbAu tdbAu : tdbAus) {
      if (tdbAu.getId().equals(tdbAuId)) {
        return tdbAu;
      }
    }
    return null;
  }

  /**
   * Add all plugin IDs for this title.  This method is used
   * by {@link Tdb#getChangedPluginIds(Tdb)}.
   * 
   * @param pluginIds a set of plugin IDs
   */
  protected void addAllPluginIds(Set<String>pluginIds) {
    if (pluginIds == null) {
      throw new IllegalArgumentException("pluginIds cannot be null");
    }
    
    for (TdbAu au : tdbAus) {
      pluginIds.add(au.getPluginId());
    }
  }
  
  /**
   * Add all plugin IDs for TdbAus that are different between this and
   * the specified title.
   * <p>
   * This method is used by {@link Tdb#getChangedPluginIds(Tdb)}.
   * @param pluginIds the pluginIds for TdbAus that are different 
   * @throws IllegalStateException if this TdbTitle's ID not set
   */
  protected void addPluginIdsForDifferences(Set<String>pluginIds, TdbTitle title) {
    if (pluginIds == null) {
      throw new IllegalArgumentException("pluginIds cannot be null");
    }
    
    if (title == null) {
      throw new IllegalArgumentException("title cannot be null");
    }
    
    if (id == null) {
      throw new IllegalStateException("ID must be set");
    }
      
    if (title.getId() == null) {
      throw new IllegalArgumentException("title ID must be set");
    }
      
    if (!title.getId().equals(id)) {
      throw new IllegalArgumentException("title ID \"" + title.getId() + "\" different than \"" + getId() + "\"");
    }
    
    if (   !title.getName().equals(name)
        || !title.getAllLinkedTitleIds().equals(this.getAllLinkedTitleIds())) {
      // titles have changed if they don't have the same names or links
      title.addAllPluginIds(pluginIds);
      this.addAllPluginIds(pluginIds);
    } else {

      // pluginIDs for TdbAus that only appear in title
      for (TdbAu titleAu : title.getTdbAus()) {
        if (this.getTdbAuById(titleAu.getId()) == null) {
          // add pluginID for title AU that is not in this TdbTitle
          pluginIds.add(titleAu.getPluginId());
        }
      }

      for (TdbAu thisAu : this.tdbAus) {
        TdbAu titleAu = title.getTdbAuById(thisAu.getId()); 
        if (titleAu == null) {
          // add pluginId for AU in this TdbTitle that is not in title 
          pluginIds.add(thisAu.getPluginId());
        }
      }
    }
  }

  /**
   * Create a copy of this TdbTitle for the specified publisher.
   * TdbAus are not copied by this method.
   * <p>
   * This is method is used by Tdb to make a deep copy of a publisher.
   * 
   * @param publisher the publisher
   */
  protected TdbTitle copyForTdbPublisher(TdbPublisher publisher) {
    TdbTitle title = new TdbTitle(name);
    title.setId(id);
    publisher.addTdbTitle(title);
    title.linkTitles = linkTitles;  // immutable: no need to copy
    return title;
  }

  /**
   * Return a String representation of the title.
   * 
   * @return a String representation of the title
   */
  public String toString() {
    return "[TdbTitle: " + name + "]";
  }
}
