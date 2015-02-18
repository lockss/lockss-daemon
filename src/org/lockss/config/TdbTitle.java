/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.apache.commons.collections.iterators .*;

import org.lockss.config.Tdb.TdbException;
import org.lockss.util.*;

/**
 * This class represents a title database title.
 *
 * @author  Philip Gust
 * @version $Id$
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
      return values[(ordinal() < values.length/2) 
               ? (ordinal() + values.length/2) : (ordinal()-values.length/2)];
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
  private final HashMap<TdbAu.Id,TdbAu> tdbAus = 
      new HashMap<TdbAu.Id, TdbAu>(4, 1F);

  /**
   * A map of link types to a collection of title IDs
   */
  private Map<LinkType, Collection<String>> linkTitles;
  
  /**
   * Create a new instance for the specified name and id.
   * The title ID must be globally unique. For example, 
   * the ID of a journal may be its ISSN.
   * 
   * @param name the title name
   * @param id the title id
   */
  protected TdbTitle(String name, String id)
  {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }

    this.name = name;
    this.id = id;
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
   * Return the publisher name of this title.
   * 
   * @return the publisher name of this title
   */
  public String getPublisherName() {
    return (publisher == null) ? null : publisher.getName();
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
   * @throws TdbException if publisher already set
   */
  protected void setTdbPublisher(TdbPublisher publisher) throws TdbException{
    if (publisher == null) {
      throw new IllegalArgumentException("title publisher cannot be null");
    }
    if (this.publisher != null) {
      throw new TdbException("publisher cannot be reset");
    }
    
    this.publisher = publisher;
  }

  /**
   * Return the complete list of proprietary identifiers for this title.
   * 
   * @return a String[] with the proprietary identifiers for this title
   */
  public String[] getProprietaryIds() {
    Set<String> proprietaryIds = new HashSet<String>();

    for (TdbAu tdbau : tdbAus.values()) {
      String[] auProprietaryIds = tdbau.getProprietaryIds();

      if (auProprietaryIds != null && auProprietaryIds.length > 0) {
	String auPropId = auProprietaryIds[0];

	if (auPropId != null) {
	  proprietaryIds.add(auPropId);
	}
      }
    }

    return proprietaryIds.toArray(new String[proprietaryIds.size()]);
  }

  /**
   * Return best coverage depth of the content in this title. Values include 
   * "fulltext" for full-text coverage, "abstracts" for abstracts-only 
   * coverage, and "tablesofcontents" for tables of contents-only coverage.
   * 
   * @return best coverage depth this title
   */
  public String getCoverageDepth() {
    // count coverage depths
    Map<String,Integer> coverageMap = new HashMap<String,Integer>();
    for (TdbAu tdbau : tdbAus.values()) {
      String value = tdbau.getCoverageDepth();
      Integer count = coverageMap.get(value);
      coverageMap.put(value, (count == null) ? 1 : count+1);
    }
    
    // find the best coverage depth -- one with the highest count
    // (is this the best algorithm if 40% are abstracts and 60% are fulltext?)
    int bestValue = 0;
    String bestCoverage = "fulltext"; 
    for (Map.Entry<String,Integer> entry : coverageMap.entrySet()) {
      if (bestValue < entry.getValue()) {
        bestValue= entry.getValue();
        bestCoverage = entry.getKey();
      }
    }
    return bestCoverage;
  }

  /**
   * Return publication type this title. Values include "journal" for a journal,
   * "book" if each AU is an individual book that is not part of a series, and
   * "bookSeries" if each AU is an individual book that is part of a series.
   * For a "bookSeries" the journalTitle() is returns the name of the series.
   * For a "book" the journalTitle() is simply descriptive of the collection
   * of books (e.g. "Springer Books") and can be ignored for bibliographic
   * purposes.
   * 
   * @return publication type this title or "journal" if not specified
   */
  public String getPublicationType() {
    String value = null;
    for (TdbAu tdbau : tdbAus.values()) {
      value = tdbau.getPublicationType();
      if (value != null) {
        break;
      }
    }
    return value;
  }

  /**
   * Return print ISSN for this title.
   * 
   * @return the print ISSN for this title or <code>null</code> if not specified
   */
  public String getPrintIssn() {
    String value = null;
    for (TdbAu tdbau : tdbAus.values()) {
      value = tdbau.getPrintIssn();
      if (value != null) {
        break;
      }
    }
    return value;
  }
  
  /**
   * Return eISSN for this title.
   * 
   * @return the eISSN for this title or <code>null</code> if not specified
   */
  public String getEissn() {
    String value = null;
    for (TdbAu tdbau : tdbAus.values()) {
      value = tdbau.getEissn();
      if (value != null) {
        break;
      }
    }
    return value;
  }
  
  /**
   * Return ISSN-L for this title.
   * 
   * @return the ISSN-L for this title or <code>null</code> if not specified
   */
  public String getIssnL() {
    String value = null;
    for (TdbAu tdbau : tdbAus.values()) {
      value = tdbau.getIssnL();
      if (value != null) {
        break;
      }
    }
    return value;
  }
  
  /**
   * Return representative ISSN for this title. 
   * Uses ISSN-L, then eISSN, and finally print ISSN.
   * 
   * @return representative ISSN for this title or <code>null</code> 
   * if not specified
   */
  public String getIssn() {
    String value = null;
    for (TdbAu tdbau : tdbAus.values()) {
      value = tdbau.getIssn();
      if (value != null) {
        break;
      }
    }
    return value;
  }

  /**
   * Return the complete list of unique ISSNs for this title.
   * 
   * @return an array of unique ISSNs for this title
   */
  public String[] getIssns() {
    HashSet<String> issns = new HashSet<String>();
    String issn = getPrintIssn();
    if (issn != null) issns.add(issn);
    issn = getEissn();
    if (issn != null) issns.add(issn);
    issn = getIssnL();
    if (issn != null) issns.add(issn);
    return issns.toArray(new String[issns.size()]);
  }
  
  /**
   * Return a collection of TdbAus for this title that match the ISBN.
   * 
   * @return a colleciton of TdbAus for this title that match the ISBN
   */
  public Collection<TdbAu> getTdbAusByIsbn(String isbn) {
    Collection<TdbAu> tdbAus = new ArrayList<TdbAu>();
    getTdbAusByIsbn(tdbAus, isbn);
    return tdbAus;
  }
  
  /**
   * Add to a collection of TdbAus for this title that match the ISBN.
   * 
   * @return <code>true</code> if TdbAus were added to the collectiion
   */
  public boolean getTdbAusByIsbn(Collection<TdbAu> matchingTdbAus, String isbn) {
    boolean added = false;
    isbn = isbn.replaceAll("-", "");
    for (TdbAu tdbau : tdbAus.values()) {
      String anIsbn = tdbau.getPrintIsbn();
      if (anIsbn != null) {
        if (anIsbn.replaceAll("-", "").equals(isbn)) {
          added |= matchingTdbAus.add(tdbau);
          continue;
        }
      }
      anIsbn = tdbau.getEisbn();
      if (anIsbn != null) {
        if (anIsbn.replaceAll("-", "").equals(isbn)) {
          added |= matchingTdbAus.add(tdbau);
        }
      }
    }
    return added;
  }

  /**
   * Return the edition for this title.
   * 
   * @return the edition for this title or <code>null</code> if not specified
   */
  public String getEdition() {
    String value = null;
    for (TdbAu tdbau : tdbAus.values()) {
      value = tdbau.getEdition();
      if (value != null) {
        break;
      }
    }
    return value;
  }

  /**
   * Get the start year for this title. Holdings may not be continuous
   * between the start and end year.
   * @return the start year or <code>null</code> if not specified
   *   in at least one TdbAu for this title
   */
  public String getStartYear() {
    int sYear = Integer.MAX_VALUE;
    for (TdbAu tdbau : tdbAus.values()) {
      try {
        sYear = Math.min(sYear, Integer.parseInt(tdbau.getStartYear()));
      } catch (Throwable ex) {
        return null;
      }
    }
    return (sYear == Integer.MAX_VALUE) ? null : Integer.toString(sYear);
  }

  /**
   * Get the end year for this title. Holdings may not be continuous
   * between the start and end year.
   * @return the end year or <code>null</code> if not specified
   *   in at least one TdbAu for this title
   */
  public String getEndYear() {
    int eYear = Integer.MIN_VALUE;
    for (TdbAu tdbau : tdbAus.values()) {
      try {
        eYear = Math.max(eYear, Integer.parseInt(tdbau.getEndYear()));
      } catch (Throwable ex) {
        return null;
      }
    }
    return (eYear == Integer.MIN_VALUE) ? null : Integer.toString(eYear);
  }

  /**
   * Determine whether year(s) for this TdbTitle include a given year.
   * @param aYear a year
   * @return <code>true</code> if at least one TdbAu for this title 
   *   includes the year
   */
  public boolean includesYear(String aYear) {
    for (TdbAu tdbau : tdbAus.values()) {
      if (tdbau.includesYear(aYear)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Get the start volume for this title. Holdings may not be continuous
   * between the start and end volume.
   * @return the start volume or <code>null</code> if not specified
   *   in at least one TdbAu for this title
   */
  public String getStartVolume() {
    int sVolume = Integer.MAX_VALUE;
    for (TdbAu tdbau : tdbAus.values()) {
      try {
        sVolume = Math.min(sVolume, Integer.parseInt(tdbau.getStartVolume()));
      } catch (Throwable ex) {
        return null;
      }
    }
    return (sVolume == Integer.MAX_VALUE) ? null : Integer.toString(sVolume);
  }

  /**
   * Get the end volume for this title. Holdings may not be continuous
   * between the start and end volume.
   * @return the end volume or <code>null</code> if not specified
   *   in at least one TdbAu for this title
   */
  public String getEndVolume() {
    int eVolume = Integer.MIN_VALUE;
    for (TdbAu tdbau : tdbAus.values()) {
      try {
        eVolume = Math.max(eVolume, Integer.parseInt(tdbau.getEndVolume()));
      } catch (Throwable ex) {
        return null;
      }
    }
    return (eVolume == Integer.MIN_VALUE) ? null : Integer.toString(eVolume);
  }

  /**
   * Determine whether volumes(s) for this TdbTitle include a given volume.
   * @param aVolume a volume
   * @return <code>true</code> if at least one TdbAu for this title 
   *   includes the year
   */
  public boolean includesVolume(String aVolume) {
    for (TdbAu tdbau : tdbAus.values()) {
      if (tdbau.includesVolume(aVolume)) {
        return true;
      }
    }
    return false;
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
      if (System.getProperty("org.lockss.unitTesting","false").equals("true")) {
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
   * @return collection of title IDs
   */
  public Collection<String> getLinkedTdbTitleIdsForType(LinkType linkType) {
    if (linkType == null) {
      throw new IllegalArgumentException("linkType cannot be null");
    }
    if (linkTitles == null) {
      return Collections.emptyList();
    }
    Collection<String> titleIds = linkTitles.get(linkType);
    return (titleIds != null) ? titleIds : Collections.<String>emptyList();
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
    return (linkTitles != null) 
      ? linkTitles : Collections.<LinkType,Collection<String>>emptyMap();
  }

  /**
   * Return all TdbAus for this title.
   * <p>
   * Note: the collection should be treated as read-only.
   *
   * @return a collection of TdbAus for this title
   */
  public Collection<TdbAu> getTdbAus() {
    return tdbAus.values();
  }

  /**
   * Return all TdbProviders for this title.
   * <p>
   * Note: the collection should be treated as read-only.
   *
   * @return a collection of TdbProviders for this title
   */
  public Collection<TdbProvider> getTdbProviders() {
    Set<TdbProvider> providers = new HashSet<TdbProvider>();
    getTdbProviders(providers);
    return providers;
  }
  
  /**
   * Add to a collection of TdbProviders for this title.
   * @param providers a collection of TdbProviders to add to.
   * @return <code>true</code> if any TdbProviders were added
   */
  public boolean getTdbProviders(Collection<TdbProvider> providers) {
    boolean added = false;
    for (TdbAu tdbAu : tdbAus.values()) {
      TdbProvider provider = tdbAu.getTdbProvider();
      if (provider != null) {
        added |= providers.add(provider);
      }
    }
    return added;
  }

  /**
   * Return the number of TdbProviders for this title.
   *
   * @return the number of providers for this title
   */
  public int getTdbProviderCount() {
    return getTdbProviders().size();
  }

  public Iterator<TdbAu> tdbAuIterator() {
    return new ObjectGraphIterator(tdbAus.values().iterator(),
				   Tdb.AU_ITER_XFORM);
  }

  /**
   * Add a new TdbAu for this title.  All params must be set prior
   * to adding tdbAu to this title.
   * 
   * @param tdbAu a new TdbAus
   * @throws TdbException if trying to add different TdbAu with the same id as
   *   an existing TdbAu
   */
  public void addTdbAu(TdbAu tdbAu) throws TdbException {
    if (tdbAu == null) {
      throw new IllegalArgumentException("au cannot be null");
    }
    if (tdbAu.getPluginId() == null) {
      throw new IllegalArgumentException(
                        "cannot add au because its plugin ID is not set: \"" 
                      + tdbAu.getName() + "\"");
    }
    TdbTitle otherTitle = tdbAu.getTdbTitle();
    if (otherTitle == this) {
      throw new IllegalArgumentException(
                        "au entry \"" + tdbAu.getName() 
                      + "\" already exists in title \"" + name + "\"");
    } else if (otherTitle != null) {
      throw new IllegalArgumentException(
               "au entry \"" + tdbAu.getName() 
             + "\" already in another title: \"" + otherTitle.getName() + "\"");
    }

    // add au assuming that it is not a duplicate
    TdbAu.Id id = tdbAu.getId();
    TdbAu existingAu = tdbAus.put(id, tdbAu);
    if (existingAu == tdbAu) {
      // au is already added
      return;
    } else if (existingAu != null) {
      
      // au already added -- restore and report existing au
      tdbAus.put(id, existingAu);
      if (tdbAu.getName().equals(existingAu.getName())) {
        throw new TdbException(
                        "Cannot add duplicate au entry: \"" + tdbAu.getName() 
                      + "\" to title \"" + name + "\"");
      } else {
        // error because could lead to a missing su -- one probably has a typo
        throw new TdbException(
               "Cannot add duplicate au entry: \"" + tdbAu.getName() 
             + "\" with the same id as existing au entry \"" 
             + existingAu.getName()
             + "\" to title \"" + name + "\"");
      } 
    }
    
    try {
      tdbAu.setTdbTitle(this);
    } catch (TdbException ex) {
      // if we can't set the title, remove the au and re-throw exception
      tdbAus.remove(id);
      throw ex;
    }
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
   * Return the TdbAus for with the specified TdbAu volume.
   * 
   * @param tdbAuVolume the volume of the AU to select
   * @return the TdbAu for the specified name
   */
  public Collection<TdbAu> getTdbAusByVolume(String tdbAuVolume)
  {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    for (TdbAu tdbAu : tdbAus.values()) {
      if ((tdbAuVolume == null) || tdbAu.includesVolume(tdbAuVolume)) {
        aus.add(tdbAu);
      }
    }
    aus.trimToSize();
    return aus;
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
    for (TdbAu tdbAu : tdbAus.values()) {
      if (StringUtil.equalStringsIgnoreCase(tdbAu.getName(), tdbAuName)) {
        added |= aus.add(tdbAu);
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
    for (TdbAu tdbAu : tdbAus.values()) {
      if (StringUtil.startsWithIgnoreCase(tdbAu.getName(), tdbAuName)) {
        added |= aus.add(tdbAu);
      }
    }
    return added;
  }
  
  /**
   * Return the TdbAu for with the specified TdbAu volume.
   * 
   * @param tdbAuYear the year of the AU to select
   * @return the TdbAu for the specified name
   */
  public Collection<TdbAu> getTdbAusByYear(String tdbAuYear)
  {
    ArrayList<TdbAu> aus = new ArrayList<TdbAu>();
    for (TdbAu tdbAu : tdbAus.values()) {
      if ((tdbAuYear == null) || tdbAu.includesYear(tdbAuYear)) {
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
    return tdbAus.get(tdbAuId);
  }

  /**
   * Add all plugin IDs for this title.  This method is used by {@link
   * Tdb#addDifferences(Tdb.Differences,Tdb)}.
   * 
   * @param diffs the {@link Tdb.Differences} to add to.
   */
  protected void addAllPluginIds(Tdb.Differences diffs) {
    if (diffs == null) {
      throw new IllegalArgumentException("diffs cannot be null");
    }
    
    for (TdbAu au : tdbAus.values()) {
      diffs.addPluginId(au.getPluginId());
    }
  }
  
  /**
   * Determines two TdbsTitles are equal. Equality is based on having
   * equal TdbTitles and their child TdbAus.   The parent hierarchy is 
   * not checked.
   * 
   * @param o the other object
   * @return <code>true</code> iff they are equal TdbTitles
   */
  public boolean equals(Object o) {
    // check for identity
    if (this == o) {
      return true;
    }

    if (o instanceof TdbTitle) {
      try {
        // if no exception thrown, there are no differences
        // because the method did not try to modify the set
	addDifferences(new Tdb.Differences.Unmodifiable(), (TdbTitle)o);
        return true;
      } catch (UnsupportedOperationException ex) {
        // differences because method tried to add to unmodifiable set
      } catch (IllegalArgumentException ex) {
        // if something was wrong with the other title
      } catch (TdbException ex) {
        // if something is wrong with this title
      }
    }
    return false;
  }

  /**
   * Return the hashcode.  The hashcode of this instance is the
   * hashcode of its Id.
   * 
   * @throws the hashcode of this instance
   */
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * Add all plugin IDs for TdbAus that are different between this and
   * the specified title.
   * <p>
   * This method is used by {@link Tdb#addDifferences(Tdb.Differences,Tdb)}.
   * @param diffs the {@link Tdb.Differences} to add to.
   * @throws TdbException if this TdbTitle's ID not set
   */
  protected void addDifferences(Tdb.Differences diffs, TdbTitle oldTitle) 
    throws TdbException {
    
    if (diffs == null) {
      throw new IllegalArgumentException("diffs cannot be null");
    }
    
    if (oldTitle == null) {
      throw new IllegalArgumentException("oldTitle cannot be null");
    }
    
    if (!oldTitle.getId().equals(id)) {
      throw new IllegalArgumentException(
                      "title ID \"" + oldTitle.getId() 
                    + "\" different than \"" + getId() + "\"");
    }
    
    if (   !oldTitle.getName().equals(name)
        || !oldTitle.getAllLinkedTitleIds().equals(this.getAllLinkedTitleIds())) {
      // titles have changed if they don't have the same names or links
      diffs.addTitle(oldTitle, Tdb.Differences.Type.Old);
      diffs.addTitle(this, Tdb.Differences.Type.New);
    } else {

      // pluginIDs for TdbAus that only appear in oldTitle
      for (TdbAu oldAu : oldTitle.getTdbAus()) {
	if (!oldAu.equals(tdbAus.get(oldAu.getId()))) {
          // add pluginID for title AU that is not in this TdbTitle
	  diffs.addAu(oldAu, Tdb.Differences.Type.Old);
        }
      }
      for (TdbAu thisAu : this.getTdbAus()) {
	if (!thisAu.equals(oldTitle.getTdbAuById(thisAu.getId()))) {
          // add pluginId for AU in this TdbTitle that is not in oldTitle 
	  diffs.addAu(thisAu, Tdb.Differences.Type.New);
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
   * @throws TdbException if publisher already has this title
   */
  protected TdbTitle copyForTdbPublisher(TdbPublisher publisher) 
    throws TdbException {
    TdbTitle title = new TdbTitle(name, id);
    publisher.addTdbTitle(title);
    title.linkTitles = linkTitles;  // immutable: no need to copy
    return title;
  }

  /** Print a full description of the title and all its AUs */
  public void prettyPrint(PrintStream ps, int indent) {
    ps.println(StringUtil.tab(indent) + "Title: " + name);
    TreeMap<String, TdbAu> sorted =
      new TreeMap<String, TdbAu>(CatalogueOrderComparator.SINGLETON);
    for (TdbAu au : getTdbAus()) {
      sorted.put(au.getName(), au);
    }
    for (TdbAu au : sorted.values()) {
      au.prettyPrint(ps, indent + 2);
    }
  }

  /**
   * Return a String representation of the title.
   * 
   * @return a String representation of the title
   */
  public String toString() {
    return "[TdbTitle: " + name + "]";
  }

  /**
   * TODO: Where do the sorted archival units come from?
   * Provides a sorted list of the archival units for this title.
   *
   * @return a List<TdbAu> with the sorted archival units.
   */
  public List<TdbAu> getSortedTdbAus() {
    final String DEBUG_HEADER = "getSortedTdbAus(): ";
    List<TdbAu> sortedTdbAus = new ArrayList<TdbAu>(getTdbAus());
    Collections.sort(sortedTdbAus);

    if (logger.isDebug3())
      logger.debug3(DEBUG_HEADER + "sortedTdbAus = " + sortedTdbAus);
    return sortedTdbAus;
  }

  /**
   * Provides an indication of whether this title is a serial publication.
   * 
   * @return <code>true</code> if this title is a serial publication,
   *         <code>false</code> otherwise.
   */
  public boolean isSerial() {
    boolean result = false;
    TdbAu tdbau = CollectionUtil.getAnElement(tdbAus.values());
    if (tdbau != null) {
      result = tdbau.isSerial();
    }
    return result;
  }
}
