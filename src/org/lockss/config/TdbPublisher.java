/*
 * $Id$
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

import org.apache.commons.collections.iterators .*;

import org.lockss.config.Tdb.TdbException;
import org.lockss.util.*;

/**
 * This class represents a title database publisher.
 *
 * @author  Philip Gust
 * @version $Id$
 */
public class TdbPublisher {
  /**
   * Set up logger
   */
  protected final static Logger logger = Logger.getLogger("TdbPublisher");

  /**
   * The name of the publisher
   */
  private final String name;
  
  /**
   * The map of title IDs to titles for this publisher
   */
  private final HashMap<String, TdbTitle> titlesById = 
      new HashMap<String, TdbTitle>(4, 1F);


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
   * Return true if this publisher was given a gensym name
   * 
   * @return  true if this publisher was given a gensym name
   */
  public boolean isUnknownPublisher() {
    return name.startsWith(Tdb.UNKNOWN_PUBLISHER_PREFIX);
  }
  
  /**
   * Return the number of TdbTitles for this publisher.
   * 
   * @return the number of TdbTitles for this publisher
   */
  public int getTdbTitleCount() {
    return titlesById.size();
  }

  /**
   * Return the number of TdbTitles for this publisher.
   * 
   * @return the number of TdbTitles for this publisher
   */
  public int getTdbAuCount() {
    int auCount = 0;
    for (TdbTitle title : titlesById.values()) {
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
    return titlesById.values();
  }
  
  /**
   * Return all TdbProviders for this publisher.
   * <p>
   * Note: the collection should be treated as read-only.
   *
   * @return a collection of TdbAus for this publisher
   */
  public Collection<TdbProvider> getTdbProviders() {
    Set<TdbProvider> providers = new HashSet<TdbProvider>();
    getTdbProviders(providers);
    return providers;
  }
  
  /**
   * Add to a collection of TdbProviders for this publisher.
   * @param providers a collection of TdbProviders to add to.
   * @return <code>true</code> if any TdbProviders were added
   */
  public boolean getTdbProviders(Collection<TdbProvider> providers) {
    boolean added = false;
    for (TdbTitle title : titlesById.values()) {
      added |= title.getTdbProviders(providers);
    }
    return added;
  }

  /**
   * Return the number of TdbProviders for this publisher.
   * @return the number of TdbProviders for this publisher
   */
  public int getTdbProviderCount() {
    return getTdbProviders().size();
  }

  public Iterator<TdbTitle> tdbTitleIterator() {
    return new ObjectGraphIterator(titlesById.values().iterator(),
				   Tdb.TITLE_ITER_XFORM);
  }

  public Iterator<TdbAu> tdbAuIterator() {
    return new ObjectGraphIterator(titlesById.values().iterator(),
				   Tdb.AU_ITER_XFORM);
  }

  /**
   * Return the collection of TdbTitles for this publisher
   * with the specified title name.
   * 
   * @param titleName the title name
   * @return the set of TdbTitles with the specified title name
   */
  public Collection<TdbTitle> getTdbTitlesByName(String titleName) {
    ArrayList<TdbTitle> matchTitles = new ArrayList<TdbTitle>();
    getTdbTitlesByName(titleName, matchTitles);
    matchTitles.trimToSize();
    return matchTitles;
  }
  
  /**
   * Adds to the collection of TdbTitles for this publisher
   * with the specified title name.
   * 
   * @param titleName the title name
   * @param titles a collection of matching titles
   * @return <code>true</code> if titles were added to the collection
   */
  public boolean getTdbTitlesByName(String titleName, 
                                    Collection<TdbTitle> titles) {
    boolean added = false;
    if (titleName != null) {
      for (TdbTitle title : titlesById.values()) {
        if (title.getName().equalsIgnoreCase(titleName)) {
          added |= titles.add(title);
        }
      }
    }
    return added;
  }
  
  /**
   * Return the collection of TdbTitles for this publisher
   * with a name like (starts with) the specified title name.
   * 
   * @param titleName the title name
   * @return the set of TdbTitles with the specified title name
   */
  public Collection<TdbTitle> getTdbTitlesLikeName(String titleName) {
    ArrayList<TdbTitle> matchTitles = new ArrayList<TdbTitle>();
    getTdbTitlesLikeName(titleName, matchTitles);
    matchTitles.trimToSize();
    return matchTitles;
  }
  
  /**
   * Return the collection of TdbTitles for this publisher
   * with a name like (starts with) the specified title name.
   * 
   * @param titleName the title name
   * @param titles a collection of matching titles
   * @return <code>true</code> if titles were added to the collection
   */
  public boolean getTdbTitlesLikeName(String titleName,
                                      Collection<TdbTitle> titles) {
    boolean added = false;
    if (titleName != null) {
      for (TdbTitle title : titlesById.values()) {
        if (StringUtil.startsWithIgnoreCase(title.getName(), titleName)) {
          added |= titles.add(title);
        }
      }
    }
    return added;
  }
  
  /**
   * Return the TdbAus like the specified TdbAu volume.
   * 
   * @param tdbAuName the name of the AU to select
   * @return the TdbAu like the specified name
   */
  public Collection<TdbAu> getTdbAusByName(String tdbAuName)
  {
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
  public boolean getTdbAusByName(String tdbAuName, Collection<TdbAu> aus)
  {
    boolean added = false;
    for (TdbTitle title : titlesById.values()) {
      added |= title.getTdbAusByName(tdbAuName, aus);
    }
    return added;
  }
  
  /**
   * Return the TdbAus like the specified TdbAu volume.
   * 
   * @param tdbAuName the name of the AU to select
   * @return the TdbAu like the specified name
   */
  public Collection<TdbAu> getTdbAusLikeName(String tdbAuName)
  {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    getTdbAusLikeName(tdbAuName, aus);
    return aus;
  }
  
  /**
   * Add TdbAus for like the specified TdbAu name.
   * 
   * @param tdbAuName the name of the AU to select
   * @param aus the collection to add to
   * @return <code>true</code> if TdbAus were added to the collection
   */
  public boolean getTdbAusLikeName(String tdbAuName, Collection<TdbAu> aus)
  {
    boolean added = false;
    for (TdbTitle title : titlesById.values()) {
      added |= title.getTdbAusLikeName(tdbAuName, aus);
    }
    return added;
  }
  
  /**
   * Returns a TdbTitle with the specified ISSN as its ISSN-L,
   * eISSN, or print ISSN. Only returns the first one found.
   * There could be several if the publisher changes the name
   * of the title without changing the ISSN -- it happens!
   * 
   * @param issn the ISSN
   * @return the TdbTitle or <code>null</code> if not found
   */
  public TdbTitle getTdbTitleByIssn(String issn) {
    if (issn != null) {
      for (TdbTitle title : titlesById.values()) {
        if (issn.equals(title.getIssnL())) {
          return title;
        }
        if (issn.equals(title.getEissn())) {
          return title;
        }
        if (issn.equals(title.getPrintIssn())) {
          return title;
        }
      }
    }
    return null;
  }
  
  /**
   * Return a collection of TdbTitles that match the ISSN.
   * @param issn the ISSN
   * @return a colleciton of TdbTitles that match the ISSN
   */
  public Collection<TdbTitle> getTdbTitlesByIssn(String issn) {
    Collection<TdbTitle> tdbTitles = new ArrayList<TdbTitle>();
    getTdbTitlesByIssn(issn, tdbTitles);
    return tdbTitles;
  }
  
  /**
   * Add a collection of TdbTitles that match the ISBN.
   * @param issn the ISSN
   * @param matchingTdbTitles the collection of TdbTitles
   * @return <code>true</code> if titles were added, else <code>false</code>
   */
  public boolean getTdbTitlesByIssn(String issn, 
                                    Collection<TdbTitle> matchingTdbTitles) {
    boolean added = false;
    if (issn != null) {
      for (TdbTitle title : titlesById.values()) {
        if (issn.equals(title.getIssnL())) {
          added |= matchingTdbTitles.add(title);
        } else if (issn.equals(title.getEissn())) {
          added |= matchingTdbTitles.add(title);
        } else if (issn.equals(title.getPrintIssn())) {
          added |= matchingTdbTitles.add(title);
        }
      }
    }    
    return added;
  }
  
  /**
   * Return a collection of TdbAus for this publisher that match the ISBN.
   * 
   * @return a collection of TdbAus for this publisher that match the ISBN
   */
  public Collection<TdbAu> getTdbAusByIsbn(String isbn) {
    Collection<TdbAu> tdbAus = new ArrayList<TdbAu>();
    getTdbAusByIsbn(tdbAus, isbn);
    return tdbAus;
  }
  
  /**
   * Add to a collection of TdbAus for this publisher that match the ISBN.
   * 
   * @return <code>true</code> if TdbAus were added to the collectiion
   */
  public boolean getTdbAusByIsbn(Collection<TdbAu> matchingTdbAus, String isbn) {
    boolean added = false;
    for (TdbTitle tdbTitle : getTdbTitles()) {
      added |= tdbTitle.getTdbAusByIsbn(matchingTdbAus, isbn);
    }
    return added;
  }

  /**
   * Return the TdbTitle for this publisher with the 
   * specified title ID.
   * 
   * @param titleId the title ID 
   * @return the TdbTitle with the specified title ID. 
   */
  public TdbTitle getTdbTitleById(String titleId) {
    return titlesById.get(titleId);
  }
  
  /**
   * Add a new TdbTitle for this publisher. 
   * 
   * @param title a new TdbTitle
   * @throws IllegalArgumentException if the title ID is not set
   * @throws TdbException if trying to add different TdbTitle with same id
   *   as existing TdbTitle
   */
  protected void addTdbTitle(TdbTitle title) throws TdbException{
    if (title == null) {
      throw new IllegalArgumentException("published title cannot be null");
    }

    // add the title assuming that is not a duplicate
    String titleId = title.getId();
    TdbTitle existingTitle = titlesById.put(titleId, title);
    if (existingTitle == title) {
      // title already added
      return;
      
    } else if (existingTitle != null) {
      
      // title already added -- restore and report existing title
      titlesById.put(titleId, existingTitle);
      if (title.getName().equals(existingTitle.getName())) {
        throw new TdbException("Cannot add duplicate title entry: \"" + title.getName() 
                               + "\" for title id: " + titleId
                               + " to publisher \"" + name + "\"");
      } else {
        // error because it could lead to a missing AU -- one probably has a typo
        throw new TdbException(
                       "Cannot add duplicate title entry: \"" + title.getName() 
                     + "\" with the same id: " + titleId + " as existing title \"" 
                     + existingTitle.getName() + "\" to publisher \"" + name + "\"");
      }
    }

    try {
      title.setTdbPublisher(this);
    } catch (TdbException ex) {
      // if can't set the publisher, remove title and re-throw exception
      titlesById.remove(title.getId());
      throw ex;
    }
  }
  
  /**
   * Add plugin IDs of all TdbAus in this publisher to the input set.
   * This method is used by {@link Tdb#addDifferences(Tdb.Differences,Tdb)}.
   * 
   * @param diffs the {@link Tdb.Differences} to add to.
   */
  protected void addAllPluginIds(Tdb.Differences diffs) {
    for (TdbTitle title : titlesById.values()) {
      title.addAllPluginIds(diffs);
    }
  }
  


  /**
   * Add plugin IDs for all TdbAus in this publisher that are
   * different between this and the specified publisher.
   * <p>
   * This method is used by {@link Tdb#addDifferences(Tdb.Differences,Tdb)}.
   * @param diffs the {@link Tdb.Differences} to add to.
   * @param oldPublisher the other TdbPublisher to compare to.
   */
  protected void addDifferences(Tdb.Differences diffs,
				TdbPublisher oldPublisher) {
    if (diffs == null) {
      throw new IllegalArgumentException("diffs cannot be null");
    }
    
    if (oldPublisher == null) {
      throw new IllegalArgumentException("pubisher cannot be null");
    }
    
    if (!oldPublisher.getName().equals(name)) {
      // publishers have changed if they don't have the same names
      diffs.addPublisher(oldPublisher, Tdb.Differences.Type.Old);
      diffs.addPublisher(this, Tdb.Differences.Type.New);
    }
    
    // add the TdbTitles that only appear in publisher
    for (TdbTitle oldTitle : oldPublisher.getTdbTitles()) {
      if (this.getTdbTitleById(oldTitle.getId()) == null) {
	diffs.addTitle(oldTitle, Tdb.Differences.Type.Old);
      }
    }
    
    // search titles in this publisher
    for (TdbTitle thisTitle : this.titlesById.values()) {
      TdbTitle oldTitle = oldPublisher.getTdbTitleById(thisTitle.getId());
      if (oldTitle == null) {
        // add all pluginIDs for title not in publisher
	diffs.addTitle(thisTitle, Tdb.Differences.Type.New);
	thisTitle.addAllPluginIds(diffs);
      } else {
        try {
	  // add pluginIDs for differences in titles with the same title ID
	  thisTitle.addDifferences(diffs, oldTitle);
        } catch (TdbException ex) {
          // won't happen because all titles for publisher have an ID
          logger.error("Internal error: title with no id: " + thisTitle, ex);
        }
      }
    }
  }
  
  /**
   * Determines two TdbsPublshers are equal. The parent hierarchy is not checked.
   * 
   * @param o the other object
   * @return <code>true</code> iff they are equal TdbPubishers
   */
  public boolean equals(Object o) {
    // check for identity
    if (this == o) {
      return true;
    }

    if (o instanceof TdbPublisher) {
      try {
        // if no exception thrown, there are no differences
        // because the method did not try to modify the set
	addDifferences(new Tdb.Differences.Unmodifiable(), (TdbPublisher)o);
        return true;
      } catch (UnsupportedOperationException ex) {
        // differences because method tried to add to unmodifiable set
      } catch (IllegalArgumentException ex) {
        // if something was wrong with the other publisher
      } catch (IllegalStateException ex) {
        // if something is wrong with this publisher
      }
    }
    return false;
  }

  /** Print a full description of the publisher and all its titles */
  public void prettyPrint(PrintStream ps, int indent) {
    ps.println(StringUtil.tab(indent) + "Publisher: " + name);
    TreeMap<String, TdbTitle> sorted =
      new TreeMap<String, TdbTitle>(CatalogueOrderComparator.SINGLETON);
    for (TdbTitle title : getTdbTitles()) {
      sorted.put(title.getName(), title);
    }
    for (TdbTitle title : sorted.values()) {
      title.prettyPrint(ps, indent + 2);
    }
  }

  /**
   * Returns the hashcode.  The hashcode for this instance
   * is the hashcode of its name.
   * 
   * @return the hashcode for this instance
   */
  public int hashCode() {
    return name.hashCode();
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
