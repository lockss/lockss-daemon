/*
 * $Id: TdbAu.java,v 1.20 2013-03-18 23:21:36 pgust Exp $
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

import java.io.*;
import java.util.*;

import org.apache.commons.collections.map.Flat3Map;
import org.lockss.config.Tdb.TdbException;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.plugin.PluginManager;
import org.lockss.util.*;


/**
 * This class represents a title database archival unit (AU).
 *
 * @author  Philip Gust
 */
public class TdbAu implements BibliographicItem {
  /**
   * Set up logger
   */
  protected final static Logger logger = Logger.getLogger("TdbAu");

  /**
   * The name of this instance
   */
  private final String name;
  
  /**
   * The Title to which this AU belongs
   */
  private TdbTitle title;
  
  /**
   * The plugin ID of the instance
   */
  private String pluginId;
  
  /**
   * The plugin params for this instance
   */
  private Map<String, String> params;
 
  /**
   * The plugin attrs for this AU
   */
  private Map<String, String> attrs;

  /**
   * Additional properties
   */
  private Map<String,String> props;

  /**
   * The key for identity testing
   */
  private final Id tdbAuId = new Id();

  /**
   * This class encapsulates the key for a TdbAu.  As with
   * the Plugin, it uses the pluginId and params.  Since the
   * Plugin is not available, it uses all the params rather
   * than just the definitional ones.
   * 
   * @author phil
   */
  public class Id {
    private int hash = 0;

    /** 
     * Return the TdbAu for this ID.
     * @return the TdbAu for this ID
     */
    public TdbAu getTdbAu() {
      return TdbAu.this;
    }
    
    /**
     * Determines this ID is equal to another object
     * @param obj the other object
     * @return <code>true</code> if this ID equals the other object
     */
    public boolean equals(Object obj) {
      if (!(obj instanceof Id)) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      TdbAu.Id auId = (TdbAu.Id)obj;
      return (   TdbAu.this.getPluginId().equals(auId.getTdbAu().getPluginId())
              && TdbAu.this.getParams().equals(auId.getTdbAu().getParams()));
    }
    /**
     * Force hashcode to be recomputed because of a TdbAu change.
     */
    private void invalidateHashCode() {
      hash = 0;
    }
    /**
     * Returns the hashcode for this ID.
     * @return the hashcode for this ID
     */
    public int hashCode() {
      if (hash == 0) {
        hash = TdbAu.this.getParams().hashCode();;
//        hash = au.getPluginId().hashCode() + au.getParams().hashCode();
      } 
      return hash;
    }
    /**
     * Returns the string value for this ID.
     * @return the string value of this ID
     */
    public String toString() {
      Properties props = new Properties();
      props.putAll(TdbAu.this.getParams());
      return PluginManager.generateAuId(TdbAu.this.getPluginId(), props);
    }
  }
  
  /**
   * Create a new instance of an au.
   * 
   * @param name the name of the au
   * @param pluginId the id of the plugin.
   * @param pluginId the plugin ID of this AU
   */
  protected TdbAu(String name, String pluginId) {
    if (name == null) {
      throw new IllegalArgumentException("au name cannot be null");
    }
    
    if (pluginId == null) {
      throw new IllegalArgumentException("au pluginId cannot be null");
    }
    
    this.name = name;
    this.pluginId = pluginId;
//     params = new HashMap<String,String>();
    params = new Flat3Map();
  }

  /**
   * Determines two TdbsAus are equal. Equality is based on 
   * equality of their Ids, plus the equality of their attributes
   * and properties. The parent hierarchy is not checked.
   * 
   * @param o the other object
   * @return <code>true</code> iff they are equal TdbAus
   */
  public boolean equals(Object o) {
    // check for identity
    if (this == o) {
      return true;
    }

    if (o instanceof TdbAu) {
      TdbAu other = (TdbAu)o;
      return tdbAuId.equals(other.getId()) &&
             getAttrs().equals(other.getAttrs()) &&
             getProperties().equals(other.getProperties());
    }
    return false;
  }

  /**
   * Return the hashcode.  The hashcode of this instance
   * is the hashcode of its Id.
   * 
   * @returns hashcode of this instance
   */
  public int hashCode() {
      return getId().hashCode();
  }

  /**
   * Get the name of the AU.  The name normally consists of the the TdbTitle
   * plus a volume or date specifier.
   * 
   * @return the name of this AU
   */
  public String getName() {
    return name;
  }
  
  /**
   * Array of props/attrs that can be used as a publisher-specified
   * journal id.
   */
  private final String[] journal_ids = {
      "journal_id",
      "journal_code",
      "journal_abbr"
  };
  
  /**
   * Get the proprietary ID of the AU. This is a publisher spececified
   * value found in the 'journal_id' attribute or parameter.
   * 
   * @return the name of this AU
   */
  public String getProprietaryId() {
    // give priority to attributes over params for overriding
    for (String journal_id : journal_ids) {
      String propId = getAttr(journal_id);
      if (!StringUtil.isNullString(propId)) return propId;
    }
    for (String journal_id : journal_ids) {
      String propId = getParam(journal_id);
      if (!StringUtil.isNullString(propId)) return propId;
    }
    return null;
  }
  
  /**
   * Get the key for this instance. Two instances represent
   * the same TdbAu if their keys are equal.
   * 
   * @return the key for this instance
   */
  public TdbAu.Id getId() {
    return tdbAuId;
  }
  /**
   * Get the TdbPublisher for this AU.
   * 
   * @return the TdbPublisher for this AU
   */
  public TdbPublisher getTdbPublisher()
  {
    return (title == null) ? null : title.getTdbPublisher();
  }
  
  /**
   * Return the TdbTitle for this AU.
   * 
   * @return the title for this AU.
   */
  public TdbTitle getTdbTitle() {
    return title;
  }
  
  /**
   * Set the title for this AU.
   * 
   * @param title the title for this AU
   * @throws TdbException if the title is already set
   */
  protected void setTdbTitle(TdbTitle title) throws TdbException{
    if (title == null) {
      throw new IllegalArgumentException("au title cannot be null");
    }
    if (this.title != null) {
      throw new TdbException("cannot reset title for au \"" + name + "\"");
    }
    
    this.title = title;
  }
  
  /**
   * Determines whether this AU is marked "down".
   * 
   * @return <code>true</code> if this AU is marked "down"
   */
  public boolean isDown() {
    return "true".equals(getParam("pub_down"));

  }
  
  /**
   * Return the ID of the plugin for this AU.  If the AU plugin ID
   * is not set, returns the title default plugin ID.
   * 
   * @return the ID of the plugin for this AU
   */
  public String getPluginId() {
    return pluginId;
  }
  
  /**
   * Get the properties for this instance.
   * <p>
   * Note: The returned map should be treated as read-only
   * 
   * @param name the property name
   * @return the property value or <code>null</code> if undefined
   */
  public Map<String,String> getProperties()
  {
    return (props != null) ? props : Collections.<String,String>emptyMap();
  }
  
  /**
   * Get a property by name.
   * 
   * @param name the property name
   * @return the property value or <code>null</code> if undefined
   */
  public String getPropertyByName(String name)
  {
    if (name == null) {
      throw new IllegalArgumentException("property name cannot be null");
    }
    if (name.equals("pluginId")) {
      return getPluginId();
    } else if (name.equals("name")) {
      return getName();
    } else if (props != null) {
      return props.get(name);
    }
    return null;
  }
  
  /**
   * Set AU properties by name.
   * 
   * @param name the property name
   * @param value the property value
   * @throws TdbException if cannot set property
   */
  protected void setPropertyByName(String name, String value) throws TdbException {
    if (name == null) {
      throw new IllegalArgumentException("property name cannot be null");
    }
    if (name.equals("pluginId")) {
      throw new TdbException("cannot reset pluginId property \"" + pluginId + "\" for au \"" + this.name + "\"");
    } else if (name.equals("name")) {
      throw new TdbException("cannot reset name property \"" + name + "\" for au \"" + this.name + "\"");
    } else {
      if (value == null) {
        throw new TdbException("value cannot be null for property \"" + name + "\" for au \"" + this.name + "\"");
      }
      if (props == null) {
//         props = new HashMap<String,String>();
        props = new Flat3Map();
      }
      props.put(StringPool.AU_CONFIG_PROPS.intern(name), value);
    }
  }
  
  /**
   * Get the params for this instance.
   * <p>
   * Note: this map should be treated as unmodifiable
   * 
   * @return the params for this instance
   */
  public Map<String, String> getParams() {
    return params;
  }
  
  /**
   * Return the param value for this instance for the specified name.
   * 
   * @param name the param name
   * @return the param value, or <code>null</code> if not defined
   */
  public String getParam(String name) {
    return params.get(name);
  }
  
  /**
   * Set the value of a param.  All params must be set before adding this TdbAu
   * to its TdbTitle because changing params could change the Id of this TdbAu.
   * 
   * @param name the param name
   * @param value the non-null param value
   * @throws TdbException if param is already set, or 
   *   au has been added to its title (could change its Id);
   */
  protected void setParam(String name, String value) throws TdbException {
    if (name == null) {
      throw new IllegalArgumentException("au param name cannot be null");
    }
    if (value == null) {
      throw new IllegalArgumentException("au param value cannot be null");
    }
    
    if (title != null) {
      throw new TdbException("cannot add param once au has been added to its title");
    }
    if (params.containsKey(name)) {
      throw new TdbException("cannot replace value of au param \"" + name + "\" for au \"" + this.name + "\"");
    }
    params.put(StringPool.AU_CONFIG_PROPS.intern(name), value);
    getId().invalidateHashCode();  // setting params modifies ID hashcode
  }
  
  /**
   * Get the attrs for this instance.
   * <p>
   * Note: the returned map should be treated as unmodifiable.
   * 
   * @return the attrs for this instance
   */
  public Map<String, String> getAttrs() {
    return (attrs != null) ? attrs : Collections.<String,String>emptyMap();
  }
  
  /**
   * Return the attr value for this AU for the specified name.
   * 
   * @param name the attr name
   * @return the attr value or <code>null</code> if not defined
   */
  public String getAttr(String name) {
    return (attrs != null) ? attrs.get(name) : null;
  }
  
  /**
   * Set the value of an attribute.
   * 
   * @param name the attr name
   * @param value the non-null attr value
   * @throws TdbException if attr already set
   */
  protected void setAttr(String name, String value) throws TdbException {
    if (name == null) {
      throw new IllegalArgumentException("attr name cannot be null for au \"" + this.name + "\"");
    }
    if (value == null) {
      throw new IllegalArgumentException("value of attr \"" + name + "\" cannot be null for au \"" + this.name + "\"");
    }
    
    if (attrs == null) {
//       attrs = new HashMap<String,String>();
      attrs = new Flat3Map();
    }
    
    if (attrs.containsKey(name)) {
      throw new TdbException("cannot replace value of au attr \"" + name + "\" for au \"" + this.name + "\"");
    }
    attrs.put(StringPool.AU_CONFIG_PROPS.intern(name), value);
  }
  
  /**
   * Convenience method returns the minimum plugin version from 
   * the "pluginVersion" property.
   * 
   * @return pluginVersion the plugin version
   */
  public String getPluginVersion() {
    return getPropertyByName("pluginVersion");
  }

  /**
   * Convenience method sets the minimum plugin version from 
   * the "pluginVersion" property.
   * 
   * @return pluginVersion the pluginVersion
   * @throws TdbException if plugin version already set
   */
  public void setPluginVersion(String pluginVersion) throws TdbException {
    setPropertyByName("pluginVersion", pluginVersion);
  }

  /**
   * Convenience method sets the "estSize" property to the estimated size.
   * 
   * @param size estimated size in bytes
   * @throws TdbException if size already set
   */
  public void setEstimatedSize(long size) throws TdbException {
    if (size < 0) {
      throw new IllegalArgumentException("estimated size cannot be negative");
    }
    setPropertyByName("estSize", Long.toString(size));
  }

  /**
   * Convenience method gets the "estSize" property as an estimated size.
   * If the current value represents fraction. it is truncated to the 
   * nearest whole number.  The "estSize" property can be a number, or
   * a number followed by a "MB" (megabytes) or "KB" (kilobytes) suffix.
   * 
   * @return the estimated size in bytes
   * @throws NumberFormatException if the "estSize" attribute is not an valid long
   */
  public long getEstimatedSize() {
     String size = getPropertyByName("estSize");
     if (size == null) {
       return 0;
     } else {
       if (size.toUpperCase().endsWith("MB")) {
         return (long)(Float.parseFloat(size.substring(0, size.length()-2))*1000000);
       } else if (size.toUpperCase().endsWith("KB")) {
         return (long)(Float.parseFloat(size.substring(0, size.length()-2))*1000);
       }
       return (long)Float.parseFloat(size);
     }
  }

  /**
   * Convenience method returns the AU's TdbTitle's name.
   *
   * @return the name of this AU's TdbTitle, or <tt>null</tt>
   */
  public String getJournalTitle() {
    return (title != null) ? title.getName() : null;
  }

  /**
   * Convenience method returns the AU's TdbPublisher's name.
   *
   * @return the name of this AU's TdbPublisher, or <tt>null</tt>
   */
  public String getPublisherName() {
    try {
      return title.getTdbPublisher().getName();
    } catch (NullPointerException e) {
      return null;
    }
  }

  /**
   * Get the start issue for this AU. Allows for the issue string to represent
   * a delimited list of ranges.
   * @return the start issue or <code>null</code> if not specified
   */
  public String getStartIssue() {
    return BibliographicUtil.getRangeSetStart(getIssue());
  }

  /**
   * Get the end issue for this AU. Allows for the issue string to represent
   * a delimited list of ranges.
   * @return the end issue or <code>null</code> if not specified
   */
  public String getEndIssue() {
    return BibliographicUtil.getRangeSetEnd(getIssue());
  }

  /**
   * Determine whether the issue string for this AU include a given issue.
   * Uses {@link BibliographicUtil.coverageIncludes} to check each range
   * specified in the string.
   *
   * @param anIssue an issue
   * @return <code>true</code> if this AU includes the issue
   */
  public boolean includesIssue(String anIssue) {
    return BibliographicUtil.coverageIncludes(getIssue(), anIssue);
  }

  /**
   * Convenience method returns issue for this AU. Uses the issue attribute 
   * as preferred bibliographic value because parameter values are sometimes 
   * not used correctly. Otherwise tries one of the several parameter formats,
   * in order from most to least likely.
   *
   * @return issue for for this AU or <code>null</code> if not specified
   */
  public String getIssue() {
    String issue = getAttr("issue");
    for (String key : new String[] {
        "issue",
        "num_issue_range",
        "issue_set",
        "issue_no",
        "issues",
        "issue_no.",
        "issue_dir"
    }) {
      if (issue == null) {
        issue = getParam(key);
      } else return issue;
    }
    return issue;
  }

  
  /**
   * Return print ISSN for this AU.
   * 
   * @return the print ISSN for this title or <code>null</code> if not specified
   */
  public String getPrintIssn() {
    return (props == null) ? null : props.get("issn");
  }
  
  /**
   * Return eISSN for this title. Checks that the ISSN is well-formed (but
   * does not check checksum).
   * 
   * @return the eISSN for this title or <code>null</code> if not specified
   */
  public String getEissn() {
    return (props == null) ? null : props.get("eissn");
  }
  
  /**
   * Return ISSN-L for this title. Checks that the ISSN is well-formed (but
   * does not check checksum).
   * 
   * @return the ISSN-L for this title or <code>null</code> if not specified
   */
  public String getIssnL() {
    return (props == null) ? null : props.get("issnl");
  }
  
  /**
   * Return representative ISSN for this title.  Returns ISSN-L if available, 
   * then print ISSN, and finally eISSN. This approximates the way ISSN-Ls
   * are assigned. The returned ISSN is checked for well-formedness, but is 
   * not checksummed.
   * 
   * @return representative for this title or <code>null</code> if not 
   *  specified or is ill-formed
   */
  public String getIssn() {
    String issn = getIssnL();
    if (!MetadataUtil.isIssn(issn)) {
      issn = getPrintIssn();
      if (!MetadataUtil.isIssn(issn)) {
        issn = getEissn();
        if (!MetadataUtil.isIssn(issn)) {
          issn = null;
        }
      }
    }
    return issn;
  }
  
  /**
   * Return the print ISBN for this TdbAu.
   * 
   * @return the print ISBN for this TdbAu or <code>null</code> if not specified
   */
  public String getPrintIsbn() {
    String printIsbn = (attrs == null) ? null : attrs.get("isbn");
    if (printIsbn != null) {
      logger.debug("Found " + printIsbn + " for " + getName());
    }
    return printIsbn;
  }

  /**
   * Return the eISBN for this TdbAu.
   * 
   * @return the print ISBN for this TdbAu or <code>null</code> if not specified
   */
  public String getEisbn() {
    String eisbn = (attrs == null) ? null : attrs.get("eisbn");
    if (eisbn != null) {
      logger.debug("Found " + eisbn + " for " + getName());
    }
    return eisbn;
  }

  /**
   * Return a representative ISBN for this TdbAu; the eISBN if specified or the
   * print ISBN. Each ISBN is check for well-formedness but is not checksummed.
   * Also checks deprecated "isbn' title property if ISBN or eISBN attribute
   * are not present. 
   * 
   * @return the ISBN for this title or <code>null</code> if not specified
   *  or is malformed
   */
  public String getIsbn() {
    String isbn = getEisbn();
    if (!MetadataUtil.isIsbn(isbn)) {
      isbn = getPrintIsbn();
      if (!MetadataUtil.isIsbn(isbn)) {
        isbn = (props == null) ? null : props.get("isbn");
        if ((isbn != null) && !MetadataUtil.isIsbn(isbn)) {
          isbn = null;
        }
      }
    }
    return isbn;
  }

  /**
   * Get the start year for this AU. Allows for the year string to represent
   * a delimited list of ranges.
   * @return the start year or <code>null</code> if not specified
   */
  public String getStartYear() {
    return BibliographicUtil.getRangeSetStart(getYear());
  }

  /**
   * Get the end year for this AU. Allows for the year string to represent
   * a delimited list of ranges.
   * @return the end year or <code>null</code> if not specified
   */
  public String getEndYear() {
    return BibliographicUtil.getRangeSetEnd(getYear());
  }

  /**
   * Determine whether year(s) for this AU include a given date.
   * Uses {@link BibliographicUtil.coverageIncludes} to check each range
   * specified in the string.
   * @param aYear a year
   * @return <code>true</code> if this AU includes the date
   */
  public boolean includesYear(String aYear) {
    //return NumberUtil.rangeIncludes(getYear(), aYear);
    return BibliographicUtil.coverageIncludes(getYear(), aYear);
  }

  /**
   * Get year for this AU. Uses the year attribute as preferred bibliographic 
   * value because the parameter values are sometimes not used correctly.
   * <p>
   * Note: The year field may be a range (e.g. 2003-2004) rather than a
   * single year.
   * 
   * @return the year for this AU or <code>null</code> if not specified
   */
  public String getYear() {
    String auYear = getAttr("year");
    if (auYear == null) {
      auYear = getParam("year");
    }
    return auYear;
  }

  /**
   * Get the start volume for this AU. First the method checks whether the volume
   * string looks like a genuine range. If it looks like a single identifier
   * with a hyphen, the string is returned whole.
   * @return the start volume or <code>null</code> if not specified
   */
  public String getStartVolume() {
    return BibliographicUtil.getRangeSetStart(getVolume());
  }

  /**
   * Get the end volume for this AU. First the method checks whether the volume
   * string looks like a genuine range. If it looks like a single identifier
   * with a hyphen, the string is returned whole.
   * @return the end volume or <code>null</code> if not specified
   */
  public String getEndVolume() {
    return BibliographicUtil.getRangeSetEnd(getVolume());
  }

  /**
   * Determine whether the volume string for this AU include a given volume.
   * Uses {@link BibliographicUtil.coverageIncludes} to check each range
   * specified in the string.
   * @param aVolume a volume
   * @return <code>true</code> if this AU includes the volume
   */
  public boolean includesVolume(String aVolume) {
    //return NumberUtil.rangeIncludes(getVolume(), aVolume);
    return BibliographicUtil.coverageIncludes(getVolume(), aVolume);
    // TODO Use a volume-aware method instead, that allows for example
    // s2ii to not include s2iii
    // s2v-s2x to include s2ix but not s2w
  }

  /**
   * Return the volume from a TdbAu. Uses the volume attribute as preferred 
   * bibliographic value because the parameter values are sometimes not used 
   * correctly within TDB files (e.g. they're really years)
   * <p>
   * Note: The volume field may be a range (e.g. 85-87) rather than a
   * single volume.
   * 
   * @param tdbau the TdbAu
   * @return the volume name or <code>null</code> if not specified.
   */
  public String getVolume() {
    String auVolume = getAttr("volume");
    if (auVolume == null) {
      auVolume = getParam("volume_name");
    }
    if (auVolume == null) {
      auVolume = getParam("volume_str");
    }
    if (auVolume == null) {
      auVolume = getParam("volume");
    }
    return auVolume;
  }

  /**
   * Return the edition from a TdbAu.
   * 
   * @param tdbau the TdbAu
   * @return the edition name or <code>null</code> if not specified
   */
  public String getEdition() {
    String auEdition = getAttr("edition");
    if (auEdition == null) {
      auEdition = getParam("edition");
    }
    return auEdition;
  }
  
  /** 
   * Convenience method generates Properties that will result in this 
   * TdbAu when loaded by Tdb. 
   * 
   * @return Properties equivalent to this TdbAu
   */
  public Properties toProperties() {
    Properties p = new OrderedProperties();
    
    // put fixed AU props
    p.put("title", name);
    p.put("plugin", pluginId);
    
    // put additional AU props
    if (props != null) {
      for (Map.Entry<String,String> entry : props.entrySet()) {
        p.put(entry.getKey(), entry.getValue());
      }
    }

    // put the journal title
    if (title != null) {
      // Put title properties on each AU
      // This will go away when the external
      // representation includes separate title records.
      p.put("journal.title", title.getName());  // proposed replacement for journalTitle
      p.put("journal.id", title.getId());     // proposed new property

      if (title.getTdbPublisher() != null) {
        // proposed new property to replace attribute.publisher
        p.put("publisher.id", title.getTdbPublisher().getName());
      }
      // put link properties
      // KLUDGE: put all title links on each AU.
      // During processing, links are aggregated from AUs
      // into the title.  This will go away when the external
      // representation includes separate title records. 
      int ix = 0;
      for (Map.Entry<TdbTitle.LinkType,Collection<String>> entry : title.getAllLinkedTitleIds().entrySet()) {
        TdbTitle.LinkType key = entry.getKey();
        for (String titleId : entry.getValue()) {
          String ppre = "link." + (++ix) + ".";
          p.put(ppre + "type", key.toString());
          p.put(ppre + "journalId", titleId);
        }
      }
    }
    
    // put param properties
    if (params != null) {
      int ix = 0;
      for (Map.Entry<String,String> entry : params.entrySet()) {
        String ppre = "param." + (++ix) + ".";
        String key = entry.getKey();
        p.put(ppre + "key", key);
        p.put(ppre + "value", entry.getValue());
      }
    }

    // put attr properties
    if (attrs != null) {
      for (Map.Entry<String,String> entry : attrs.entrySet()) {
        p.put("attributes."+entry.getKey(), entry.getValue());
      }
    }
    return p;
  }

  /**
   * Create a copy of this TdbAu for the specified title
   * <p>
   * This is method is used by Tdb to make a deep copy of a publisher.
   * 
   * @param publisher the publisher
   * @throws TdbException if trying to add a TdbAu  to title with the 
   *   same id as this one
   */
  protected TdbAu copyForTdbTitle(TdbTitle title) throws TdbException {
    TdbAu au = new TdbAu(name, pluginId);
    title.addTdbAu(au);

    // immutable -- no need to copy
    au.attrs = attrs;
    au.props = props;
    au.params = params;

    return au;
    
  }
  
  /** Print a full description of the AU */
  public void prettyPrint(PrintStream ps, int indent) {
    ps.println(StringUtil.tab(indent) + "AU: " + name);
    indent += 2;
    ps.println(StringUtil.tab(indent) + "Plugin: " + pluginId);
    pprintSortedMap("Params:", params, ps, indent);
    if (attrs != null && !attrs.isEmpty()) {
      pprintSortedMap("Attrs:", attrs, ps, indent);
    }
    if (props != null && !props.isEmpty()) {
      pprintSortedMap("Additional props:", props, ps, indent);
    }
  }

  void pprintSortedMap(String title, Map<String,String> map,
		       PrintStream ps, int indent) {
    ps.println(StringUtil.tab(indent) + title);
    indent += 2;
    TreeMap<String, String> sorted = new TreeMap<String, String>(map);
    for (Map.Entry<String,String> ent : sorted.entrySet()) {
      ps.println(StringUtil.tab(indent) +
		 ent.getKey() + " = " + ent.getValue());
    }
  }

  /**
   * Return a String representation of the title.
   * 
   * @return a String representation of the title
   */
  public String toString() {
    return "[TdbAu: " + name + "]";
  }
}
