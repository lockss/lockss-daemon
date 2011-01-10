/*
 * $Id: ArticleMetadata.java,v 1.6 2011-01-10 09:12:40 tlipkis Exp $
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;

import org.apache.commons.collections .*;
import org.apache.commons.collections .map.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import static org.lockss.extractor.MetadataField.*;


/**
 * Collection of metadata about a single article or other feature.
 * Consists of two maps that associate one or more string values with a
 * string key.  The raw map should hold the raw keys and values extracted
 * from one or more (html, xml, etc.) files; the cooked map holds standard
 * metadata (DOI, ISSN, etc.) associated with well-known keys.
 */
public class ArticleMetadata {
  private static Logger log = Logger.getLogger("Metadata");

  private MultiValueMap rawMap = new MultiValueMap();
  private MultiValueMap cookedmap = new MultiValueMap();

  public ArticleMetadata() {
  }

  /*
   * Accessors ensure that metadata keys are case-insensitive strings
   * (lowercased).
   */

  // Raw map

  /** Set or add to the value associated with the key in the raw metadata
   * map. */
  public void putRaw(String key, String value) {
    rawMap.put(key.toLowerCase(), value);
  }

  /** Return a non-empty list of raw values associated with a key, else
   * null if none. */
  public List<String> getRawList(String key) {
    return getRawCollection(key);
  }

  /** Return the first or only raw value associated with a key, else null if
   * none. */
  public String getRaw(String key) {
    List<String> lst = getRawCollection(key);
    if (lst == null) {
      return null;
    } else {
      return lst.get(0);
    }
  }

  /** Return true if the key has an associated value in the raw map */
  public boolean containsRawKey(String key) {
    return rawMap.containsKey(key.toLowerCase());
  }

  /** Return the set of keys in the raw map */
  public Set<String> rawKeySet() {
    return rawMap.keySet();
  }

  /** Return the raw Entry set */
  public Set<Map.Entry<String,List<String>>> rawEntrySet() {
    return rawMap.entrySet();
  }

  /** Return the size of the raw map */
  public int rawSize() {
    return rawMap.size();
  }

  private List<String> getRawCollection(String key) {
    List<String> res = (List)rawMap.getCollection(key.toLowerCase());
    if (res == null || res.isEmpty()) {
      return null;
    }
    return res;
  }

  // Cooked map

  /** Set or add to the value associated with the key.  If the field has a
   * validator/normalizer it will be applied to the value first.  Returns
   * true iff the value validates and is stored successfully.
   *
   * <h4>Single-valued fields</h4>A valid value will be stored if either no
   * value is already present or the new value is equal to the current
   * value.  If a different value is present nothing is stored.  <br>A raw
   * value that doesn't validate will be stored as an {@link InvalidValue}
   * along with the validation exception, iff no valid value is already
   * present.  (See {@link #hasValidValue(MetadataField)}, {@link
   * #hasInvalidValue(MetadataField)} and {@link #get(MetadataField)}.)
   *
   * <h4>Multi-valued fields</h4>A valid value will be added and true
   * returned; an invalid valid will not be stored, and false returned
   */
  /** Set or add to the value associated with the key. Returns true iff the
   * value was stored without error */
  public boolean put(MetadataField field, String value) {
    MetadataException ex = put0(field, value);
    return ex == null;
  }

  /** Set or add to the value associated with the key.  If the field has a
   * validator/normalizer it will be applied to the value first.  Throws
   * MetadataException if the value does not validate or is not stored
   * successfully
   *
   * <h4>Single-valued fields</h4>A valid value will be stored if either no
   * value is already present or the new value is equal to the current
   * value.  If a different value is present nothing is stored and a
   * MetadataException.CardinalityException is thrown.  <br>A raw value
   * that doesn't validate will cause a
   * MetadataException.CardinalityException to be thrown.  If no valid
   * value is already present. the invalid raw value will be stored as an
   * {@link InvalidValue} along with the validation exception.  (See {@link
   * #hasValidValue(MetadataField)}, {@link
   * #hasInvalidValue(MetadataField)} and {@link #get(MetadataField)}.)
   *
   * <h4>Multi-valued fields</h4>A valid value will be added.  An invalid
   * valid will not be added, and a validation exception will be thrown.
   */
  public void putValid(MetadataField field, String value)
      throws MetadataException {
    MetadataException ex = put0(field, value);
    if (ex != null) {
      throw ex;
    }
  }

  private MetadataException put0(MetadataField field, String value) {
    switch (field.getCardinality()) {
    case Single:
      return putSingle(field, value);
    case Multi:
      return putMulti(field, value);
    }
    return new MetadataException("Unknown field type: " +
				 field.getCardinality()).setField(field);
  }

  private String getKey(MetadataField field) {
    return field.getKey().toLowerCase();
  }

  private MetadataException putSingle(MetadataField field, String value) {
    String key = getKey(field);
    MetadataException valEx = null;
    String normVal;
    try {
      normVal = field.validate(value);
      List curval = getCollection(key);
      if (curval == null) {
	cookedmap.put(key, normVal);
	return null;
      } else if (isInvalid(curval.get(0))) {
	cookedmap.remove(key);
	cookedmap.put(key, normVal);
	return null;
      } else if (value.equals(curval.get(0))) {
	return null;
      }
      MetadataException ex =
	new MetadataException.CardinalityException("Attempt to reset single-valued key: " + key + " to " + value);
      ex.setField(field);
      ex.setNormalizedValue(normVal);
      ex.setRawValue(value);
      return ex;
    } catch (MetadataException ex) {
      if (getCollection(key) == null) {
	InvalidValue ival = new InvalidValue(value, null, ex);
	cookedmap.put(key, ival);
      }
      ex.setField(field);
      ex.setRawValue(value);
      return ex;
    }
  }

  private MetadataException putMulti(MetadataField field, String value) {
    String key = getKey(field);
    MetadataException valEx = null;
    String normVal;
    try {
      normVal = field.validate(value);
      cookedmap.put(key, normVal);
      return null;
    } catch (MetadataException ex) {
      ex.setRawValue(value);
      return ex;
    }
  }

  /** Return true iff the field has either a valid value or an invalid
   * value */
  public boolean hasValue(MetadataField field) {
    List curval = getCollection(getKey(field));
    return curval != null;
  }

  /** Return true iff the field has a valid value */
  public boolean hasValidValue(MetadataField field) {
    List curval = getCollection(getKey(field));
    return curval != null && !isInvalid(curval.get(0));
  }

  /** Return true iff the field has a value, which is invalid */
  public boolean hasInvalidValue(MetadataField field) {
    List curval = getCollection(getKey(field));
    return curval != null && isInvalid(curval.get(0));
  }

  private boolean isValid(Object obj) {
    return !(obj instanceof InvalidValue);
  }

  private boolean isInvalid(Object obj) {
    return obj instanceof InvalidValue;
  }

  /** Return the value associated with a key, else null if no valid value */
  private String get(String key) {
    return get(key, null);
  }

  /** Return the value associated with a key, else dfault if no valid
   * value */
  private String get(String key, String dfault) {
    List lst = getCollection(key);
    if (lst == null) {
      return dfault;
    }
    Object res = lst.get(0);
    if (res instanceof String) {
      return (String)res;
    }
    if (res instanceof InvalidValue) {
      return null;
    }
    return null;
  }

  /** If the field has an invalid value, return the {@link InvalidValue}
   * describing it, else null */
  public InvalidValue getInvalid(MetadataField field) {
    return getInvalid(field.getKey());
  }

  /** If the key has an invalid value, return the {@link InvalidValue}
   * describing it, else null */
  public InvalidValue getInvalid(String key) {
    List lst = getCollection(key);
    if (lst == null) {
      return null;
    }
    Object res = lst.get(0);
    if (res instanceof InvalidValue) {
      return (InvalidValue)res;
    }
    return null;
  }


//   /** Return the value associated with a key */
//   public String get(String key) {
//     return get(key, null);
//   }

//   /** Return the value associated with a key */
//   public String get(String key, String def) {
//     List lst = getCollection(key);
//     if (lst == null) {
//       return def;
//     }
//     if (lst.size() > 1) {
//       return StringUtil.separatedString(lst, ";");
//     } else {
//       return (String)lst.get(0);
//     }
//   }

  /** Return the value associated with a key */
  public String get(MetadataField field) {
    return get(field.getKey());
  }

  /** Return the list of values associated with a key */
  public List<String> getList(String key) {
    List lst = getCollection(key);
    if (lst == null || lst.get(0) instanceof InvalidValue) {
      return null;
    }
    return lst;
  }

  /** Return the list of values associated with a key */
  public List<String> getList(MetadataField field) {
    return getList(field.getKey());
  }

  /** Return the keyset of the cooked map */
  public Set<String> keySet() {
    return cookedmap.keySet();
  }

//  Simple entrySet would return keys of invalid values, not clear that's good.
//   public Set<Map.Entry<String,List<String>>> entrySet() {
//     return cookedmap.entrySet();
//   }

  /** Return the size of the cooked map */
  public int size() {
    return cookedmap.size();
  }

  /** Return true if the cooked map is empty */
  public boolean isEmpty() {
    return cookedmap.isEmpty();
  }

  /** Copies values from the raw metadata map to the cooked map according
   * to the supplied map.  Validation errors are ignored
   * @param rawToCooked maps raw key -> cooked MatadataField.
   */
  public void cook(Map<String,Object> rawToCooked) {
    for (Map.Entry ent : rawToCooked.entrySet()) {
      String rawKey = (String)ent.getKey();
      Object val = ent.getValue();
      if (val instanceof Collection) {
	for (MetadataField field : (Collection<MetadataField>)val) {
	  cookField(rawKey, field);
	}
      } else if (val instanceof MetadataField) {
	cookField(rawKey, (MetadataField)val);
      }
    }
  }

  private void cookField(String rawKey, MetadataField field) {
    List<String> rawlst = getRawCollection(rawKey);
    if (rawlst != null) {
      for (String rawval : rawlst) {
	put(field, rawval);
      }
    }
  }

  private MetadataField findField(String key) {
    key = key.toLowerCase();
    MetadataField res = MetadataField.findField(key);
    return (res != null) ? res : new MetadataField.Default(key);
  }

  private List getCollection(String key) {
    List res = (List)cookedmap.getCollection(key.toLowerCase());
    if (res == null || res.isEmpty()) {
      return null;
    }
    return res;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[md:");
    for (String key : keySet()) {
      sb.append(" [");
      sb.append(key);
      sb.append(", ");
      List lst = getCollection(key);
      if (lst == null) {
	sb.append("(null)");
      } else if (lst.get(0) instanceof InvalidValue) {
	sb.append(lst.get(0));
      } else {
	sb.append(lst);
      }
      sb.append("]");
    }
    return sb.toString();
  }

  /** Record of a failed attempt to store a value in the cooked map, either
   * because the value didn't validate or a second store isn't allowed in a
   * Single cardinality field */
  public static class InvalidValue {
    private String rawValue;
    private String invValue;
    private MetadataException ex;

    public InvalidValue(String rawValue, String invalidValue,
			MetadataException ex) {
      this.rawValue = rawValue;
      this.invValue = invalidValue;
      this.ex = ex;
    }

    /** Return the raw value that was attempted to be stored. */
    public String getRawValue() {
      return rawValue;
    }
//     public String getInvalidValue() {
//       return invValue;
//     }

    /** Return the exception thrown by the validator. */
    public MetadataException getException() {
      return ex;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[inv: ");
      sb.append(rawValue);
      sb.append(", ");
      sb.append(ex.toString());
      sb.append("]");
      return sb.toString();
    }
  }
}
