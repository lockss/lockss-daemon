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
public class TdbProvider {
  /**
   * Set up logger
   */
  protected final static Logger logger = Logger.getLogger("TdbProvider");

  /**
   * The name of the publisher
   */
  private final String name;
  
  /**
   * The map of title IDs to titles for this publisher
   */
  private final HashMap<TdbAu.Id, TdbAu> ausById = 
      new HashMap<TdbAu.Id, TdbAu>(4, 1F);


  /**
   * Create a new instance for the specified publisher name.
   * 
   * @param name the publisher name
   */
  protected TdbProvider(String name)
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
  public boolean isUnknownProvider() {
    return name.startsWith(Tdb.UNKNOWN_PROVIDER_PREFIX);
  }
  
  /**
   * Add plugin IDs of all TdbAus in this publisher to the input set.
   * This method is used by {@link Tdb#addDifferences(Tdb.Differences,Tdb)}.
   * 
   * @param diffs the {@link Tdb.Differences} to add to.
   */
  protected void addAllPluginIds(Tdb.Differences diffs) {
    for (TdbAu tdbAu: ausById.values()) {
      diffs.addPluginId(tdbAu.getPluginId());
    }
  }

  /**
   * Add plugin IDs for all TdbAus in this provider that are
   * different between this and the specified provider.
   * <p>
   * This method is used by {@link Tdb#addDifferences(Tdb.Differences,Tdb)}.
   * @param diffs the {@link Tdb.Differences} to add to.
   * @param oldProvider the other TdbProvider to compare to.
   */
  protected void addDifferences(Tdb.Differences diffs,
                                TdbProvider oldProvider) {
    if (diffs == null) {
      throw new IllegalArgumentException("diffs cannot be null");
    }
    
    if (oldProvider == null) {
      throw new IllegalArgumentException("provider cannot be null");
    }
    
    if (!oldProvider.getName().equals(name)) {
      // publishers have changed if they don't have the same names
      diffs.addProvider(oldProvider, Tdb.Differences.Type.Old);
      diffs.addProvider(this, Tdb.Differences.Type.New);
    } else {
      // pluginIDs for TdbAus that only appear in oldProvider
      for (TdbAu oldAu : oldProvider.getTdbAus()) {
	if (!oldAu.equals(ausById.get(oldAu.getId()))) {
          // add pluginID for provider AU that is not in this TdbProvider
          diffs.addAu(oldAu, Tdb.Differences.Type.Old);
        }
      }
      for (TdbAu thisAu : this.getTdbAus()) {
	if (!thisAu.equals(oldProvider.getTdbAuById(thisAu.getId()))) {
          // add pluginId for AU in this TdbProvider that is not in oldProvider 
          diffs.addAu(thisAu, Tdb.Differences.Type.New);
        }
      }
    }
  }

  /**
   * Return the number of TdbAus for this provider.
   * 
   * @return the number of TdbAus for this publisher
   */
  public int getTdbAuCount() {
    return ausById.size();
  }

  /**
   * Returns an iterator to the collecion of TdbAus for this provider
   * @return iterator to collection of AUs for this provider
   */
  public Iterator<TdbAu> tdbAuIterator() {
    return ausById.values().iterator();
  }
  
  /**
   * Return the TdbAu for with the specified TdbAu.Key.
   * 
   * @param tdbAuId the Id of the TdbAu to select
   * @return the TdbAu for the specified key
   */
  public TdbAu getTdbAuById(TdbAu.Id tdbAuId)
  {
    return ausById.get(tdbAuId);
  }

  /**
   * Return the collection of TdbAus for this provider.
   * <p>
   * Note: The returned collection should not be modified.
   * 
   * @return the collection of TdbAus for this provider
   */
  public Collection<TdbAu> getTdbAus() {
    return ausById.values();
  }
  
  /**
   * Returns a collection of TdbPublishers for this provider.
   * @return collection of TdbPublishers for this provider
   */
  public Collection<TdbPublisher> getTdbPublishers() {
    Collection<TdbPublisher> publishers = new HashSet<TdbPublisher>();
    for (TdbAu tdbAu : ausById.values()) {
      publishers.add(tdbAu.getTdbPublisher());
    }
    return publishers;
  }
  
  /**
   * Return the number of TdbTitles for this publisher.
   * 
   * @return the number of TdbTitles for this publisher
   */
  public int getTdbPublisherCount() {
    return getTdbPublishers().size();
  }

  /**
   * Returns a collection of TdbPublishers for this provider.
   * @return collection of TdbPublishers for this provider
   */
  public TdbPublisher getTdbPublisherByName(String publisherName) {
    if (publisherName != null) {
      for (TdbAu tdbAu : ausById.values()) {
        TdbPublisher publisher = tdbAu.getTdbPublisher();
        if (publisherName.equals(publisher.getName())) {
          return publisher;
        }
      }
    }
    return null;
  }
  
  /**
   * Returns a collection of TdbPublishers for this provider.
   * @return collection of TdbPublishers for this provider
   */
  public Collection<TdbTitle> getTdbTitles() {
    Collection<TdbTitle> titles = new HashSet<TdbTitle>();
    for (TdbAu tdbAu : ausById.values()) {
      titles.add(tdbAu.getTdbTitle());
    }
    return titles;
  }
  
  /**
   * Return the number of TdbTitles for this provider.
   * 
   * @return the number of TdbTitles for this provider
   */
  public int getTdbTitleCount() {
    return getTdbTitles().size();
  }

  /**
   * Return the collection of TdbTitles for this provider
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
   * Adds to the collection of TdbTitles for this provider
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
      for (TdbAu tdbAu : ausById.values()) {
        TdbTitle title = tdbAu.getTdbTitle();
        if (title.getName().equalsIgnoreCase(titleName)) {
          added |= titles.add(title);
        }
      }
    }
    return added;
  }
  
  /**
   * Return the collection of TdbTitles for this provider
   * with a name like (starts with) the specified title name.
   * 
   * @param titleName the title name
   * @return the set of TdbTitles with the specified title name
   */
  public Collection<TdbTitle> getTdbTitlesLikeName(String titleName) {
    Collection<TdbTitle> matchTitles = new HashSet<TdbTitle>();
    getTdbTitlesLikeName(titleName, matchTitles);
    return matchTitles;
  }
  
  /**
   * Return the collection of TdbTitles for this provider
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
      for (TdbAu tdbAu : ausById.values()) {
        TdbTitle title = tdbAu.getTdbTitle();
        if (StringUtil.startsWithIgnoreCase(title.getName(), titleName)) {
          added |= titles.add(title);
        }
      }
    }
    return added;
  }
  
  /**
   * Return the TdbAus for this provider like the specified TdbAu volume.
   * 
   * @param tdbAuName the name of the AU to select
   * @return the TdbAu like the specified name
   */
  public Collection<TdbAu> getTdbAusByName(String tdbAuName)
  {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    getTdbAusByName(tdbAuName, aus);
    aus.trimToSize();
    return aus;
  }
  
  /**
   * Add TdbAus for this provider with the specified TdbAu name.
   * 
   * @param tdbAuName the name of the AU to select
   * @param aus the collection to add to
   * @return <code>true</code> if TdbAus were added to the collection
   */
  public boolean getTdbAusByName(String tdbAuName, Collection<TdbAu> aus)
  {
    boolean added = false;
    if (tdbAuName != null) {
      for (TdbAu tdbAu : ausById.values()) {
        if (tdbAuName.equals(tdbAu.getName()))
          added |= aus.add(tdbAu);
      }
    }
    return added;
  }
  
  /**
   * Return the TdbAus for this provider like the specified TdbAu volume.
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
   * Add TdbAus for this provider like the specified TdbAu name.
   * 
   * @param tdbAuName the name of the AU to select
   * @param aus the collection to add to
   * @return <code>true</code> if TdbAus were added to the collection
   */
  public boolean getTdbAusLikeName(String tdbAuName, Collection<TdbAu> aus)
  {
    boolean added = false;
    if (tdbAuName != null) {
      for (TdbAu tdbAu : ausById.values()) {
        if (StringUtil.startsWithIgnoreCase(tdbAuName, tdbAu.getName())) {
          added |= aus.add(tdbAu);
        }
      }
    }
    return added;
  }
  
  /**
   * Returns a TdbTitle for this provider with the specified ISSN as its 
   * ISSN-L, eISSN, or print ISSN. Only returns the first one found.
   * There could be several if the provider changes the name
   * of the title without changing the ISSN -- it happens!
   * 
   * @param issn the ISSN
   * @return the TdbTitle or <code>null</code> if not found
   */
  public TdbTitle getTdbTitleByIssn(String issn) {
    if (issn != null) {
      for (TdbAu tdbAu : ausById.values()) {
        TdbTitle title = tdbAu.getTdbTitle();
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
   * Return a collection of TdbTitles for this provider that match the ISSN.
   * @param issn the ISSN
   * @return a colleciton of TdbTitles that match the ISSN
   */
  public Collection<TdbTitle> getTdbTitlesByIssn(String issn) {
    Collection<TdbTitle> tdbTitles = new HashSet<TdbTitle>();
    getTdbTitlesByIssn(issn, tdbTitles);
    return tdbTitles;
  }
  
  /**
   * Add a collection of TdbTitles for this provider that match the ISBN.
   * @param issn the ISSN
   * @param matchingTdbTitles the collection of TdbTitles
   * @return <code>true</code> if titles were added, else <code>false</code>
   */
  public boolean getTdbTitlesByIssn(String issn, 
                                    Collection<TdbTitle> matchingTdbTitles) {
    boolean added = false;
    if (issn != null) {
      for (TdbAu tdbAu : ausById.values()) {
        TdbTitle title = tdbAu.getTdbTitle();
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
   * Return a collection of TdbAus for this provider that match the ISBN.
   * 
   * @return a collection of TdbAus for this provider that match the ISBN
   */
  public Collection<TdbAu> getTdbAusByIsbn(String isbn) {
    Collection<TdbAu> tdbAus = new ArrayList<TdbAu>();
    getTdbAusByIsbn(tdbAus, isbn);
    return tdbAus;
  }
  
  /**
   * Add to a collection of TdbAus for this provider that match the ISBN.
   * 
   * @return <code>true</code> if TdbAus were added to the collection
   */
  public boolean getTdbAusByIsbn(Collection<TdbAu> matchingTdbAus, String isbn) {
    boolean added = false;
    if (isbn != null) {
      for (TdbAu tdbAu : ausById.values()) {
        String anIsbn = tdbAu.getPrintIsbn();
        if (anIsbn != null) {
          if (anIsbn.replaceAll("-", "").equals(isbn)) {
            added |= matchingTdbAus.add(tdbAu);
            continue;
          }
        }
        anIsbn = tdbAu.getEisbn();
        if (anIsbn != null) {
          if (anIsbn.replaceAll("-", "").equals(isbn)) {
            added |= matchingTdbAus.add(tdbAu);
          }
        }
      }
    }
    return added;
  }

  /**
   * Return the TdbTitle for this provider with the 
   * specified title ID.
   * 
   * @param titleId the title ID 
   * @return the TdbTitle with the specified title ID. 
   */
  public TdbTitle getTdbTitleById(String titleId) {
    if (titleId != null) {
      for (TdbAu tdbAu : ausById.values()) {
        TdbTitle title = tdbAu.getTdbTitle();
        if (titleId.equals(title.getId())) {
          return title;
        }
      }
    }
    return null;
  }
  
  /**
   * Add a new TdbAu for this provider. 
   * 
   * @param tdbAu a new TdbAu
   * @throws IllegalArgumentException if the ID is not set
   * @throws TdbException if trying to add different TdbAu with same id
   *   as existing TdbAu
   */
  protected void addTdbAu(TdbAu tdbAu) throws TdbException{
    if (tdbAu == null) {
      throw new IllegalArgumentException("au cannot be null");
    }
    if (tdbAu.getPluginId() == null) {
      throw new IllegalArgumentException(
                        "cannot add au because its plugin ID is not set: \"" 
                      + tdbAu.getName() + "\"");
    }
    TdbProvider otherProvider = tdbAu.getTdbProvider();
    if (otherProvider == this) {
      throw new IllegalArgumentException(
                        "au entry \"" + tdbAu.getName() 
                      + "\" already exists in provider \"" + name + "\"");
    } else if (otherProvider != null) {
      throw new IllegalArgumentException(
               "au entry \"" + tdbAu.getName() 
             + "\" already in another provider: \"" 
             + otherProvider.getName() + "\"");
    }

    // add au assuming that it is not a duplicate
    TdbAu.Id id = tdbAu.getId();
    TdbAu existingAu = this.ausById.put(id, tdbAu);
    if (existingAu == tdbAu) {
      // au is already added
      return;
    } else if (existingAu != null) {
      
      // au already added -- restore and report existing au
      ausById.put(id, existingAu);
      if (tdbAu.getName().equals(existingAu.getName())) {
        throw new TdbException(
                        "Cannot add duplicate au entry: \"" + tdbAu.getName() 
                      + "\" to provider \"" + name + "\"");
      } else {
        // error because could lead to a missing su -- one probably has a typo
        throw new TdbException(
               "Cannot add duplicate au entry: \"" + tdbAu.getName() 
             + "\" with the same id as existing au entry \"" 
             + existingAu.getName()
             + "\" to provider \"" + name + "\"");
      } 
    }
    
    try {
      tdbAu.setTdbProvider(this);
    } catch (TdbException ex) {
      // if we can't set the title, remove the au and re-throw exception
      ausById.remove(id);
      throw ex;
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

    if (o instanceof TdbProvider) {
      try {
        // if no exception thrown, there are no differences
        // because the method did not try to modify the set
        addDifferences(new Tdb.Differences.Unmodifiable(), (TdbProvider)o);
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
    ps.println(StringUtil.tab(indent) + "Provider: " + name);
    TreeMap<String, TdbAu> sorted =
      new TreeMap<String, TdbAu>(CatalogueOrderComparator.SINGLETON);
    for (TdbAu tdbAu : ausById.values()) {
      sorted.put(tdbAu.getName(), tdbAu);
    }
    for (TdbAu tdbAu : sorted.values()) {
      tdbAu.prettyPrint(ps, indent + 2);
    }
  }

  /**
   * Returns the hashcode.  The hashcode for this instance
   * is the hashcode of its name.
   * 
   * @return the hashcode for this instance
   */
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * Return a String representation of the publisher.
   * 
   * @return a String representation of the publisher
   */
  public String toString() {
    return "[TdbProvider: " + name + "]";
  }
}
