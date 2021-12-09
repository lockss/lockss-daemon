/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.extractor;

import java.util.*;
import org.apache.commons.collections4.*;
import org.apache.commons.collections4.map.*;
import org.lockss.util.*;

/**
 * Collection of metadata about a single article or other feature. Consists of
 * two maps that associate one or more string values with a string key. The raw
 * map should hold the raw keys and values extracted from one or more (html,
 * xml, etc.) files; the cooked map holds standard metadata (DOI, ISSN, etc.)
 * associated with well-known keys.
 */
public class ArticleMetadata {
  private static Logger log = Logger.getLogger("ArticleMetadata");

  private MultiValueMap rawMap = new MultiValueMap();
  private MultiValueMap cookedmap = new MultiValueMap();
  private Locale locale;

  public ArticleMetadata() {
  }

  /**
   * Set the Locale in which Locale-dependent values (e.g., dates) in this
   * metadata should be interpreted. Plugin metadata extractors must set this to
   * the appropriate Locale if, e.g., the values stored in the metadata are in a
   * language other than the default. If used, must be called before any cooked
   * data is stored (because validators and splitters may refer to the Locale).
   * 
   * @throws IllegalStateException
   *           if any cooked data has already been stored
   * @see MetadataUtil#getDefaultLocale()
   */
  public void setLocale(Locale locale) {
    if (cookedmap.isEmpty()) {
      this.locale = locale;
    } else {
      throw new IllegalStateException(
          "Cannot set locale after storing any cooked metadata");
    }
  }

  /**
   * Return the Locale in which values (e.g., dates) in this metadata should be
   * interpreted. Returns the systemwide default from (@link
   * MetadataUtil#getDefaultLocale()} if no Locale has't been explicitly set
   * with {@link #setLocale(Locale)}.
   */
  public Locale getLocale() {
    return locale != null ? locale : MetadataUtil.getDefaultLocale();
  }

  /*
   * Accessors ensure that metadata keys are case-insensitive strings
   * (lowercased).
   */

  // Raw map

  /**
   * Set or add to the value associated with the key in the raw metadata map.
   */
  public void putRaw(String key, String value) {
    rawMap.put(key.toLowerCase(), value);
  }

  /**
   * Set the value associated with the key in the raw metadata map.
   */
  public void putRaw(String key, Map<String, String> value) {
    rawMap.put(key.toLowerCase(), value);
  }

  /**
   * Return the list of raw values associated with a key, or an empty list.
   */
  public List<String> getRawList(String key) {
    return getRawCollection(key);
  }

  /**
   * Return the map of raw values associated with a key, or an empty map if none.
   */
  public Map<String, String> getRawMap(MetadataField field) {
    return getRawMap(field.getKey());
  }

  /**
   * Return the map of raw values associated with a key, or an empty map if none.
   */
  public Map<String, String> getRawMap(String key) {
    List<Map<String, String>> lst = (List<Map<String, String>>)rawMap.get(key);
    if (lst == null || lst.isEmpty()) {
      return Collections.<String, String> emptyMap();
    }
      return lst.get(0);
  }

  /**
   * Return the first or only raw value associated with a key, else null if
   * none.
   */
  public String getRaw(String key) {
    List<String> lst = getRawCollection(key);
    if (lst.isEmpty()) {
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
  public Set<Map.Entry<String, List<String>>> rawEntrySet() {
    return rawMap.entrySet();
  }

  /** Return the size of the raw map */
  public int rawSize() {
    return rawMap.size();
  }

  private List<String> getRawCollection(String key) {
    List<String> res = getMapCollection(rawMap, key);
    if (res == null || res.isEmpty()) {
      return Collections.<String> emptyList();
    }
    return res;
  }

  private List<String> getMapCollection(MultiValueMap map, String key) {
    return (List<String>) map.getCollection(key.toLowerCase());
  }

  // Cooked map

  /**
   * Set or add to the value associated with the key. If the field has a
   * validator/normalizer it will be applied to the value first. Returns true
   * iff the value validates and is stored successfully.
   * 
   * <h4>Single-valued fields</h4>A valid value will be stored if either no
   * value is already present or the new value is equal to the current value. If
   * a different value is present nothing is stored. <br>
   * A raw value that doesn't validate will be stored as an {@link InvalidValue}
   * along with the validation exception, iff no valid value is already present.
   * (See {@link #hasValidValue(MetadataField)},
   * {@link #hasInvalidValue(MetadataField)} and {@link #get(MetadataField)}.)
   * 
   * <h4>Multi-valued fields</h4>A valid value will be added and true returned;
   * an invalid valid will not be stored, and false returned. If the field has a
   * splitter it will be invoked to convert the value into a list of values. If
   * there is also a validator/normalizer, it will be invoked on each split
   * value. If any of them fails to validate the behavior is undefined.
   */
  public boolean put(MetadataField field, String value) {
    MetadataException ex = put0(field, value);
    return ex == null;
  }

  /**
   * Set or add to the value associated with the key. If the field has a
   * validator/normalizer it will be applied to the value first. Throws
   * MetadataException if the value does not validate or is not stored
   * successfully
   * 
   * <h4>Single-valued fields</h4>A valid value will be stored if either no
   * value is already present or the new value is equal to the current value. If
   * a different value is present nothing is stored and a
   * MetadataException.CardinalityException is thrown. <br>
   * A raw value that doesn't validate will cause a
   * MetadataException.ValidationException to be thrown. If no valid value is
   * already present. the invalid raw value will be stored as an
   * {@link InvalidValue} along with the validation exception. (See
   * {@link #hasValidValue(MetadataField)},
   * {@link #hasInvalidValue(MetadataField)} and {@link #get(MetadataField)}.)
   * 
   * <h4>Multi-valued fields</h4>A valid value will be added. An invalid valid
   * will not be added, and a validation exception will be thrown. If the field
   * has a splitter it will be invoked to convert the value into a list of
   * values. If there is also a validator/normalizer, it will be invoked on each
   * split value. If any of them fails to validate the behavior is undefined.
   */
  public void putValid(MetadataField field, String value)
      throws MetadataException {
    MetadataException ex = put0(field, value);
    if (ex != null) {
      throw ex;
    }
  }

  /** Store the value in the ArticleMetadata unless it's null or the field
   * already has a valid value.  I.e., store the value iff it's better than
   * the one that's already there.
   * @param field The MetadataField into which to store
   * @param val the value to store
   * @returns true if the value was stored
   */
  public boolean putIfBetter(MetadataField field, String value) {
    if (value != null && !hasValidValue(field)) {
      put(field, value);
      return true;
    }
    return false;
  }

  /** Store the value in the ArticleMetadata, replacing any previous value.
   * @param field The MetadataField into which to store
   * @param val the value to store
   * @returns true
   */
  public boolean replace(MetadataField field, String value) {
    putSingle(field, value, true);
    return true;
  }

  private MetadataException put0(MetadataField field, String value) {
    switch (field.getCardinality()) {
    case Single:
      return putSingle(field, value, false);
    case Multi:
      return putMulti(field, value);
    }
    return new MetadataException("Unknown field type: "
        + field.getCardinality()).setField(field);
  }

  private String getKey(MetadataField field) {
    return field.getKey().toLowerCase();
  }

  private MetadataException putSingle(MetadataField field,final String value,
				      boolean force) {
    String key = getKey(field);
    MetadataException valEx = null;
    String normVal = null;
    try {
      if(field.hasExtractor()){
        try{
          String val = field.extract(this, value);
          normVal = field.validate(this, val);
        }catch(IndexOutOfBoundsException e){
          MetadataException ex = new MetadataException.CardinalityException(value,
              "Attempt to reset single-valued key: " + key + " to " + value);
          ex.setField(field);
          ex.setNormalizedValue(normVal);
          ex.setRawValue(value);
          throw ex;
        }
      }
      else{
        normVal = field.validate(this, value);
      }
      List curval = getCollection(key);
      if (curval.isEmpty()) {
        cookedmap.put(key, normVal);
        return null;
      } else if (force || isInvalid(curval.get(0))) {
        cookedmap.remove(key);
        cookedmap.put(key, normVal);
        return null;
      } else if (value.equals(curval.get(0))) {
        return null;
      }
      MetadataException ex = new MetadataException.CardinalityException(value,
          "Attempt to reset single-valued key: " + key + " to " + value);
      ex.setField(field);
      ex.setNormalizedValue(normVal);
      ex.setRawValue(value);
      return ex;
    } catch (MetadataException ex) {
      if (getCollection(key).isEmpty()) {
        InvalidValue ival = new InvalidValue(value, ex);
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
    if (field.hasSplitter()) {
      MetadataException elemEx = null;
      for (String elem : field.split(this, value)) {
        try {
          String normElem = field.validate(this, elem);
          cookedmap.put(key, normElem);
        } catch (MetadataException.ValidationException ex) {
          if (elemEx == null) {
            elemEx = ex;
          }
        }
      }
      return elemEx;
    }
    try {
      normVal = field.validate(this, value);
      cookedmap.put(key, normVal);
      return null;
    } catch (MetadataException.ValidationException ex) {
      ex.setRawValue(value);
      ex.setField(field);
      return ex;
    }
  }

  /**
   * Return true iff the field has either a valid value or an invalid value
   */
  public boolean hasValue(MetadataField field) {
    List curval = getCollection(getKey(field));
    return !curval.isEmpty();
  }

  /** Return true iff the field has a valid value */
  public boolean hasValidValue(MetadataField field) {
    List curval = getCollection(getKey(field));
    return !curval.isEmpty() && !isInvalid(curval.get(0));
  }

  /** Return true iff the field has a value, which is invalid */
  public boolean hasInvalidValue(MetadataField field) {
    List curval = getCollection(getKey(field));
    return !curval.isEmpty() && isInvalid(curval.get(0));
  }

  private boolean isValid(Object obj) {
    return !(obj instanceof InvalidValue);
  }

  private boolean isInvalid(Object obj) {
    return obj instanceof InvalidValue;
  }

  /** If the field has an invalid value, return the {@link InvalidValue}
   * describing it, else null */
  public InvalidValue getInvalid(MetadataField field) {
    return getInvalid(field.getKey());
  }

  /**
   * If the key has an invalid value, return the {@link InvalidValue}
   *  describing it, else null 
   */
  private InvalidValue getInvalid(String key) {
    List lst = getCollection(key);
    if (lst.isEmpty()) {
      return null;
    }
    Object res = lst.get(0);
    if (res instanceof InvalidValue) {
      return (InvalidValue) res;
    }
    return null;
  }

  /** Return the value associated with a key, else null if no valid value */
  public String get(MetadataField field) {
    return get(field.getKey());
  }

  /**
   * Return the list of values associated with a key, or an empty list if none.
   */
  public List<String> getList(String key) {
    List lst = getCollection(key);
    if (lst.isEmpty() || lst.get(0) instanceof InvalidValue) {
      return Collections.<String> emptyList();
    }
    return lst;
  }

  /**
   * Return the list of values associated with a key,or an empty list if none
   */
  public List<String> getList(MetadataField field) {
    return getList(field.getKey());
  }

  /** Return the value associated with a key, else null if no valid value */
  public String get(String key) {
    return get(key, null);
  }

  /**
   * Return the value associated with a key, else dfault if no valid value
   */
  private String get(String key, String dfault) {
    List lst = getCollection(key);
    if (lst.isEmpty()) {
      return dfault;
    }
    Object res = lst.get(0);
    if (res instanceof String) {
      return (String) res;
    }
    if (res instanceof InvalidValue) {
      return null;
    }
    return null;
  }

  /** Return the keyset of the cooked map */
  public Set<String> keySet() {
    return cookedmap.keySet();
  }

  // Simple entrySet would return keys of invalid values, not clear that's good.
  // public Set<Map.Entry<String,List<String>>> entrySet() {
  // return cookedmap.entrySet();
  // }

  /** Return the size of the cooked map */
  public int size() {
    return cookedmap.size();
  }

  /** Return true if the cooked map is empty */
  public boolean isEmpty() {
    return cookedmap.isEmpty();
  }

  /**
   * Copies values from the raw metadata map to the cooked map according to the
   * supplied map. Any MetadataExceptions thrown while storing into the cooked
   * map are returned in a List.
   * 
   * @param rawToCooked
   *          maps raw key -> cooked MatadataField.
   */
  public List<MetadataException> cook(org.apache.commons.collections4.MultiMap rawToCooked) {
    List<MetadataException> errors = new ArrayList<MetadataException>();
    for (Map.Entry ent :
      (Collection<Map.Entry<String, Collection<MetadataField>>>)
        (rawToCooked.entrySet())) {
      String rawKey = (String) ent.getKey();
      Collection<MetadataField> fields = (Collection) ent.getValue();
      for (MetadataField field : fields) {
        cookField(rawKey, field, errors);
      }
    }
    return errors;
  }

  /**
   * Copies values from the raw metadata map to the cooked map according to the
   * supplied map. Any MetadataExceptions thrown while storing into the cooked
   * map are returned in a List.
   *
   * @param rawToCooked
   *          maps raw key -> cooked MatadataField.
   * @deprecated Switch to commons collections4 and use {@link
   *             #cook(org.apache.commons.collections4.map.MultiMap)}
   */
  @Deprecated
  public List<MetadataException> cook(MultiMap rawToCooked) {
    List<MetadataException> errors = new ArrayList<MetadataException>();
    for (Map.Entry ent :
      (Collection<Map.Entry<String, Collection<MetadataField>>>)
        (rawToCooked.entrySet())) {
      String rawKey = (String) ent.getKey();
      Collection<MetadataField> fields = (Collection) ent.getValue();
      for (MetadataField field : fields) {
        cookField(rawKey, field, errors);
      }
    }
    return errors;
  }

  /**
   * Copies values from the raw metadata map to the cooked map according to the
   * supplied map. Any MetadataExceptions thrown while storing into the cooked
   * map are returned in a List.
   * 
   * @param rawToCooked
   *          maps raw key -> cooked MatadataField.
   */
  public List<MetadataException> cook(org.apache.commons.collections.MultiMap rawToCooked) {
    List<MetadataException> errors = new ArrayList<MetadataException>();
    for (Map.Entry ent : 
      (Collection<Map.Entry<String, Collection<MetadataField>>>)
        (rawToCooked.entrySet())) {
      String rawKey = (String) ent.getKey();
      Collection<MetadataField> fields = (Collection) ent.getValue();
      for (MetadataField field : fields) {
        cookField(rawKey, field, errors);
      }
    }
    return errors;
  }

  private void cookField(String rawKey, MetadataField field,
    List<MetadataException> errors) {
    List<String> rawlst = getRawCollection(rawKey);
    if (!rawlst.isEmpty()) {
      for (String rawval : rawlst) {
        try {
          putValid(field, rawval);
        } catch (MetadataException ex) {
          errors.add(ex);
        }
      }
    }
  }

  private MetadataField findField(String key) {
    key = key.toLowerCase();
    MetadataField res = MetadataField.findField(key);
    return (res != null) ? res : new MetadataField.Default(key);
  }

  private List getCollection(String key) {
    List<String> res = getMapCollection(cookedmap, key);
    if (res == null || res.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    return res;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[md:");
    for (String key : keySet()) {
      sb.append(" [");
      sb.append(key);
      sb.append(": ");
      List lst = getCollection(key);
      if (lst.isEmpty()) {
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

  /** Return a pretty printed String */
  public String ppString(int indent) {
    StringBuilder sb = new StringBuilder();
    String tab = StringUtil.tab(indent);
    sb.append(tab);
    if (cookedmap.isEmpty()) {
      sb.append("Metadata (empty)\n");
    } else {
      sb.append("Metadata\n");
      dumpMap(sb, cookedmap, indent + 2);
    }
    sb.append(tab);
    if (rawMap.isEmpty()) {
      sb.append("Raw Metadata (empty)\n");
    } else {
      sb.append("Raw Metadata\n");
      dumpMap(sb, rawMap, indent + 2);
    }
    return sb.toString();
  }

  private void dumpMap(StringBuilder sb, MultiValueMap map, int indent) {
    String tab = StringUtil.tab(indent);
    for (String key :
      StringUtil.caseIndependentSortedSet((Set<String>)map.keySet())) {
      sb.append(tab);
      sb.append(key);
      sb.append(": ");
      List lst = getMapCollection(map, key);
      if (lst.isEmpty()) {
        sb.append("(null)");
      } else if (lst.size() == 1 || lst.get(0) instanceof InvalidValue) {
        sb.append(lst.get(0));
      } else {
        sb.append("[");
        sb.append(StringUtil.separatedString(lst, "; "));
        sb.append("]");
      }
      sb.append("\n");
    }
  }

  /**
   * Record of a failed attempt to store a value in the cooked map, either
   * because the value didn't validate or a second store isn't allowed in a
   * Single cardinality field
   */
  public static class InvalidValue {
    private String rawValue;
    private MetadataException ex;

    public InvalidValue(String rawValue, MetadataException ex) {
      this.rawValue = rawValue;
      this.ex = ex;
    }

    /** Return the raw value that was attempted to be stored. */
    public String getRawValue() {
      return rawValue;
    }

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
