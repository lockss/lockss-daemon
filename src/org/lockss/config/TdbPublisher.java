/*
 * $Id: TdbPublisher.java,v 1.2 2010-04-05 16:27:00 pgust Exp $
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
import java.util.Set;

/**
 * This class represents a title database publisher.
 *
 * @author  Philip Gust
 * @version $Id: TdbPublisher.java,v 1.2 2010-04-05 16:27:00 pgust Exp $
 */
public class TdbPublisher {
  /**
   * The name of the publisher
   */
  private final String name;
  
  /**
   * The set of titles for this publisher
   */
  private final ArrayList<TdbTitle> titles = new ArrayList<TdbTitle>();


  /**
   * Create a new instance for the specified publisher name.
   * 
   * @param name the publisher name
   */
  protected TdbPublisher(String name)
  {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    this.name = name;
  }
  
  /**
   * Return the name of this publisher.
   * 
   * @return the name of this publisher
   */
  public String getName() {
    return name;
  }
  
  /**
   * Return the number of TdbTitles for this publisher.
   * 
   * @return the number of TdbTitles for this publisher
   */
  public int getTdbTitleCount() {
    return titles.size();
  }

  /**
   * Return the number of TdbTitles for this publisher.
   * 
   * @return the number of TdbTitles for this publisher
   */
  public int getTdbAuCount() {
    int auCount = 0;
    for (TdbTitle title : titles) {
      auCount += title.getTdbAuCount();
    }
    return auCount;
  }

  /**
   * Return the collection of TdbTitles for this publisher.
   * <p>
   * Note: The returned collection should not be modified.
   * 
   * @return the collection of TdbTitles for this publisher
   */
  public Collection<TdbTitle> getTdbTitles() {
    return titles;
  }
  
  /**
   * Return the collection of TdbTitles for this publisher
   * with the specified title name.
   * <p>
   * Note: the returned collection should note be modified.
   * 
   * @param titleName the title name
   * @return the set of TdbTitles with the specified title name
   */
  public Collection<TdbTitle> getTdbTitlesByName(String titleName) {
    Collection<TdbTitle> theTitles = new ArrayList<TdbTitle>();
    for (TdbTitle title : titles) {
      if (title.getName().equals(titleName)) {
        theTitles.add(title);
      }
    }
    return theTitles;
  }
  
  /**
   * Return the TdbTitle for this publisher with the 
   * specified title ID.
   * 
   * @param titleId the title ID 
   * @return the TdbTitle with the specified title ID. 
   */
  public TdbTitle getTitleById(String titleId) {
    for (TdbTitle title : titles) {
      if (title.getId().equals(titleId)) {
        return title;
      }
    }
    return null;
  }
  
  /**
   * Add a new TdbTitle for this publisher. Sets the title ID to a
   * generated ID if the title ID is not already set.
   * 
   * @param title a new TdbTitle
   * @throws IllegalStateException if title already added to a publisher
   */
  protected void addTdbTitle(TdbTitle title) {
    if (title == null) {
      throw new IllegalArgumentException("published title cannot be null");
    }
    
    String id = title.getId();
    if (id == null) {
      id = genTdbTitleId(title);
    }
    TdbTitle existingTitle = getTitleById(id);
    if (existingTitle != null) {
      if (title.getName().equals(existingTitle.getName())) {
        throw new IllegalStateException("cannot add duplicate title entry: \"" + title.getName() + "\"");
      } else {
        // error because it could lead to a missing AU -- one probably has a typo
        throw new IllegalStateException("cannot add duplicate title entry: \"" + title.getName() 
                     + "\" with the same id as existing title \"" + existingTitle.getName() + "\"");
      }
    }

    if (title.getId() == null) {
      // generate titleID for title
      title.setId(id);
    }
    title.setTdbPublisher(this);
    
    titles.add(title); 
  }
  
  /**
   * Generate a unique title ID.  This method generates the same
   * title ID for the same title every time it is called.
   * 
   * @return a unique title ID
   */
  protected String genTdbTitleId(TdbTitle title) {
    // use a hash of publisher name and title name for now
    // todo: should we verify this and retry if collision found
    // with other titles in this publisher?
    return Integer.toString((title.getName() + name).hashCode());  
  }
  
  /**
   * Add plugin IDs of all TdbAus in this publisher to the input set.
   * This method is used by {@link Tdb#getChangedPluginIds(Tdb)}.
   * 
   * @param pluginIds the set of plugin IDs to add to.
   */
  protected void addAllPluginIds(Set<String>pluginIds) {
    for (TdbTitle title : titles) {
      title.addAllPluginIds(pluginIds);
    }
  }
  
  /**
   * Add plugin IDs for all TdbAus in this publisher that are
   * different between this and the specified publisher.
   * <p>
   * This method is used by {@link Tdb#getChangedPluginIds(Tdb)}.
   * @param pluginIds the set of plugin IDs to add to 
   */
  protected void addPluginIdsForDifferences(Set<String>pluginIds, TdbPublisher publisher) {
    if (pluginIds == null) {
      throw new IllegalArgumentException("pluginIds cannot be null");
    }
    
    if (publisher == null) {
      throw new IllegalArgumentException("pubisher cannot be null");
    }
    
    // add pluginIDs for TdbTitles that only appear in publisher
    for (TdbTitle publisherTitle : publisher.getTdbTitles()) {
      if (this.getTitleById(publisherTitle.getId()) == null) {
        // add all pluginIDs for publisher title not in this TdbPublisher
        publisherTitle.addAllPluginIds(pluginIds);
      }
    }
    
    // search titles in this publisher
    for (TdbTitle thisTitle : this.titles) {
      TdbTitle publisherTitle = publisher.getTitleById(thisTitle.getId());
      if (publisherTitle == null) {
        // add all pluginIDs for title not in publisher
        thisTitle.addAllPluginIds(pluginIds);
      } else {
        // add pluginIDs for differences in titles with the same title ID
        thisTitle.addPluginIdsForDifferences(pluginIds, publisherTitle);
      }
    }
  }
  
  /**
   * Return a String representation of the publisher.
   * 
   * @return a String representation of the publisher
   */
  public String toString() {
    return "[TdbPublisher: " + name + "]";
  }
}
