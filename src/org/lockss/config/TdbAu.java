/*
 * $Id: TdbAu.java,v 1.3 2010-04-06 18:19:02 pgust Exp $
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.lockss.util.OrderedProperties;

/**
 * This class represents a title database archive unit (AU).
 *
 * @author  Philip Gust
 */
public class TdbAu {
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
  private final Id tdbAuId = new Id(this);
  
  /**
   * This class encapsulates the key for a TdbAu.  As with
   * the Plugin, it uses the pluginId and params.  Since the
   * Plugin is not available, it uses all the params rather
   * than just the definitional ones.
   * 
   * @author phil
   */
  static public class Id {
    final private TdbAu au;
    public Id(TdbAu au) {
      this.au = au;
    }
    public boolean equals(Object obj) {
      if (!(obj instanceof Id)) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      TdbAu.Id auId = (TdbAu.Id)obj;
      if ((au.getPluginId() == null) && (auId.au.getPluginId() == null)) {
        // compare names only if neither pluginIDs not set
        return au.getName().equals(auId.au.getName());
      } else if ((au.getPluginId() == null) || (auId.au.getPluginId() == null)) {
        // not equal if one but not both pluginIDs not set
        return false;
      }
      return (   au.getPluginId().equals(auId.au.getPluginId())
              && au.getParams().equals(auId.au.getParams()));
    }
    public int hashCode() {
      throw new UnsupportedOperationException();
    }
    public String toString() {
      Properties props = new Properties();
      props.putAll(au.getParams());
      return org.lockss.plugin.PluginManager.generateAuId(au.getPluginId(), props);
    }
  }

    /**
   * Create a new instance of an AU.
   * 
   * @param name the name of the AU
   * @param pluginId the plugin ID of this AU
   */
  protected TdbAu(String name) {
    if (name == null) {
      throw new IllegalArgumentException("au name cannot be null");
    }
    
    this.name = name;

    if (System.getProperty("org.lockss.unitTesting", "false").equals("true")) {
      // use LinkedHashMap to preserve param order for testing
      params = new LinkedHashMap<String,String>();
    } else {
      params = new HashMap<String,String>();
    }
  }
  
  /**
   * Determines two TdbsAus are equal. Equality is based on 
   * equality of their Ids.  The parent hierarchy is not checked.
   * 
   * @param o the other object
   * @return <code>true</code> iff they are equal TdbTitles
   */
  public boolean equals(Object o) {
    // check for identity
    if (this == o) {
      return true;
    }

    if (o instanceof TdbAu) {
      return tdbAuId.equals(((TdbAu)o).getId());
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
   */
  protected void setTdbTitle(TdbTitle title) {
    if (title == null) {
      throw new IllegalArgumentException("au title cannot be null");
    }
    if (this.title != null) {
      throw new IllegalArgumentException("cannot reset title for au \"" + name + "\"");
    }
    
    this.title = title;
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
   * Set the ID for the plugin for this TdbAu.  The pluginId must be set before adding 
   * this TdbAu to its TdbTitle because changing pluginId could change the Id of this TdbAu.
   * 
   * @param pluginId the ID of the plugin for this AU
   * @throws IllegalStateException if pluginID is already set (would change au's Id) 
   */
  protected void setPluginId(String pluginId) {
    if (pluginId == null) {
      throw new IllegalArgumentException("au pluginId cannot be null");
    }
    if (this.pluginId != null) {
      throw new IllegalStateException("cannot reset au pluginId for au \"" + name + "\"");
    }
    
    // set plugin ID if different than default
    this.pluginId = pluginId; 
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
    return (props != null) ? props : Collections.EMPTY_MAP;
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
   */
  protected void setPropertyByName(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("property name cannot be null");
    }
    if (name.equals("pluginId")) {
      setPluginId(value);
    } else if (name.equals("name")) {
      throw new IllegalStateException("cannot reset name property \"" + name + "\" for au \"" + this.name + "\"");
    } else {
      if (value == null) {
        throw new IllegalArgumentException("value cannot be null for property \"" + name + "\" for au \"" + this.name + "\"");
      }
      if (props == null) {
        props = new HashMap<String,String>();
      }
      props.put(name, value);
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
    return (params != null) ? params : Collections.EMPTY_MAP;
  }
  
  /**
   * Return the param value for this instance for the specified name.
   * 
   * @param name the param name
   * @return the param value, or <code>null</code> if not defined
   */
  public String getParam(String name) {
    return (params != null) ? params.get(name) : null;
  }
  
  /**
   * Set the value of a param.  All params must be set before adding this TdbAu
   * to its TdbTitle because changing params could change the Id of this TdbAu.
   * 
   * @param name the param name
   * @param value the non-null param value
   * @throws IllegalStateException if param is already set, or 
   *   au has been added to its title (could change its Id);
   */
  protected void setParam(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("au param name cannot be null");
    }
    if (value == null) {
      throw new IllegalArgumentException("au param value cannot be null");
    }
    
    if (title != null) {
      throw new IllegalStateException("cannot add param once au has been added to its title");
    }
    if (params.containsKey(name)) {
      throw new IllegalStateException("cannot replace value of au param \"" + name + "\" for au \"" + this.name + "\"");
    }
    params.put(name, value);
  }
  
  /**
   * Get the attrs for this instance.
   * <p>
   * Note: the returned map should be treated as unmodifiable.
   * 
   * @return the attrs for this instance
   */
  public Map<String, String> getAttrs() {
    return (attrs != null) ? attrs : Collections.EMPTY_MAP;
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
   * @throws IllegalStateException if attr already set
   */
  protected void setAttr(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("attr name cannot be null for au \"" + this.name + "\"");
    }
    if (value == null) {
      throw new IllegalArgumentException("value of attr \"" + name + "\" cannot be null for au \"" + this.name + "\"");
    }
    
    if (attrs == null) {
      attrs = new HashMap<String,String>();
    }
    
    if (attrs.containsKey(name)) {
      throw new IllegalStateException("cannot replace value of au attr \"" + name + "\" for au \"" + this.name + "\"");
    }
    attrs.put(name, value);
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
   */
  public void setPluginVersion(String pluginVersion) {
    setPropertyByName("pluginVersion", pluginVersion);
  }

  /**
   * Convenience method sets the "estSize" property to the estimated size.
   * 
   * @param size estimated size in bytes
   */
  public void setEstimatedSize(long size) {
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
   * @return the name of this AU's TdbTitle
   */
  public String getJournalTitle() {
    return (title != null) ? title.getName() : null;
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
      if (title.getId() != null) {
        p.put("journal.id", title.getId());     // proposed new property
      }

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
   */
  protected TdbAu copyForTdbTitle(TdbTitle title) {
    TdbAu au = new TdbAu(name);
    au.setPluginId(pluginId);
    title.addTdbAu(au);

    // immutable -- no need to copy        
    au.attrs = attrs;  
    au.props = props;
    au.params = params; 

    return au;
    
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
