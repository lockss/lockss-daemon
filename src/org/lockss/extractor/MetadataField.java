/*
 * $Id: MetadataField.java,v 1.1 2011-01-10 09:12:40 tlipkis Exp $
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

package org.lockss.extractor;

import java.util.*;

import org.lockss.util.*;

/**
 * Describes a field (key-value pair) in metadata: key name, required
 * cardinality and validator, if any.
 */
public class MetadataField {
  static Logger log = Logger.getLogger("MetadataField");

  /*
   * The canonical representation of a DOI has key "dc.identifier"
   * and starts with doi:
   */
  public static final String PROTOCOL_DOI = "doi:";
  public static final String KEY_DOI = "doi";
  public static final MetadataField FIELD_DOI =
    new MetadataField(KEY_DOI, Cardinality.Single) {
      public String validate(String val)
	  throws MetadataException.ValidationException {
	String doi = val;
	// normalize away leading "doi:" before checking validity
	if (StringUtil.startsWithIgnoreCase(val, PROTOCOL_DOI)) {
	  doi = val.substring(PROTOCOL_DOI.length());
	}
	if (!MetadataUtil.isDOI(doi)) {
	  throw new MetadataException.ValidationException("Illegal DOI: "
							  + val);
	}
	return doi;
      }};	


  public static final String KEY_ISSN = "issn";
  public static final MetadataField FIELD_ISSN =
    new MetadataField(KEY_ISSN, Cardinality.Single) {
      public String validate(String val) {
// 	if (!MetadataUtil.isISSN(val)) {
// 	  throw new MetadataException.ValidationException("Illegal ISSN: "
// 							  + val);
// 	}
	return val;
      }};

  public static final String KEY_EISSN = "eissn";
  public static final MetadataField FIELD_EISSN =
    new MetadataField(KEY_EISSN, Cardinality.Single) {
      public String validate(String val) {
// 	if (!MetadataUtil.isISSN(val)) {
// 	  throw new MetadataException.ValidationException("Illegal EISSN: "
// 							  + val);
// 	}
	return val;
      }};

  public static final String KEY_ISBN = "isbn";
  public static final MetadataField FIELD_ISBN =
    new MetadataField(KEY_ISBN, Cardinality.Single);

  public static final String KEY_VOLUME = "volume";
  public static final MetadataField FIELD_VOLUME =
    new MetadataField(KEY_VOLUME, Cardinality.Single);

  public static final String KEY_ISSUE = "issue";
  public static final MetadataField FIELD_ISSUE =
    new MetadataField(KEY_ISSUE, Cardinality.Single);

  public static final String KEY_START_PAGE = "startpage";
  public static final MetadataField FIELD_START_PAGE =
    new MetadataField(KEY_START_PAGE, Cardinality.Single);

  /*
   * A date can be just a year, a month and year, or a specific issue date.
   */
  public static final String KEY_DATE = "date";
  public static final MetadataField FIELD_DATE =
    new MetadataField(KEY_DATE, Cardinality.Single);

  public static final String KEY_ARTICLE_TITLE = "article.title";
  public static final MetadataField FIELD_ARTICLE_TITLE =
    new MetadataField(KEY_ARTICLE_TITLE, Cardinality.Single);

  public static final String KEY_JOURNAL_TITLE = "journal.title";
  public static final MetadataField FIELD_JOURNAL_TITLE =
    new MetadataField(KEY_JOURNAL_TITLE, Cardinality.Single);

  /* Author is currently a delimited list of one or more authors. */
  public static final String KEY_AUTHOR = "author";
  public static final MetadataField FIELD_AUTHOR =
    new MetadataField(KEY_AUTHOR, Cardinality.Multi);

  public static final String KEY_ACCESS_URL = "access.url";
  public static final MetadataField FIELD_ACCESS_URL =
    new MetadataField(KEY_ACCESS_URL, Cardinality.Single);

  public static final String KEY_KEYWORDS = "keywords";
  public static final MetadataField FIELD_KEYWORDS =
    new MetadataField(KEY_KEYWORDS, Cardinality.Multi);

  // Dublin code fields

  public static final String DC_KEY_IDENTIFIER = "dc.identifier";
  public static final MetadataField DC_FIELD_IDENTIFIER =
    new MetadataField(DC_KEY_IDENTIFIER, Cardinality.Single);

  public static final String DC_KEY_DATE = "dc.date";
  public static final MetadataField DC_FIELD_DATE =
    new MetadataField(DC_KEY_DATE, Cardinality.Single);

  public static final String DC_KEY_CONTRIBUTOR = "dc.contributor";
  public static final MetadataField DC_FIELD_CONTRIBUTOR =
    new MetadataField(DC_KEY_CONTRIBUTOR, Cardinality.Single);

  private static MetadataField[] fields = {
    FIELD_VOLUME,
    FIELD_ISSUE,
    FIELD_START_PAGE,
    FIELD_DATE,
    FIELD_ARTICLE_TITLE,
    FIELD_JOURNAL_TITLE,
    FIELD_AUTHOR,
    FIELD_ACCESS_URL,
    FIELD_KEYWORDS,
    DC_FIELD_IDENTIFIER.
    DC_FIELD_DATE,
    DC_FIELD_CONTRIBUTOR,
  };

  // maps keys to fields
  private static Map<String,MetadataField> fieldMap =
    new HashMap<String,MetadataField>();
  static {
    for (MetadataField f : fields) {
      fieldMap.put(f.getKey().toLowerCase(), f);
    }
  }

  public static MetadataField findField(String key) {
    return fieldMap.get(key.toLowerCase());
  }


  private final String key;
  private final Cardinality cardinality;
  private final Validator validator;

  /** Create a metadata field descriptor with Cardinality.Single
   * @param key the map key
   */
  public MetadataField(String key) {
    this(key, Cardinality.Single);
  }

  /** Create a metadata field descriptor
   * @param key the map key
   * @param cardinality
   */
  public MetadataField(String key, Cardinality cardinality) {
    this(key, cardinality, null);
  }

  /** Create a metadata field descriptor
   * @param key the map key
   * @param cardinality
   * @param validator
   */
  public MetadataField(String key, Cardinality cardinality,
		       Validator validator) {
    this.key = key;
    this.cardinality = cardinality;
    this.validator = validator;
  }

  /** Create a MetadataField that's a copy of another one
   * @param field the MetadataField to copy
   */
  public MetadataField(MetadataField field) {
    this(field.getKey(), field.getCardinality(), field.getValidator());
  }

  public String getKey() {
    return key;
  }

  public Cardinality getCardinality() {
    return cardinality;
  }

  public Validator getValidator() {
    return validator;
  }

  public String validate(String value)
      throws MetadataException.ValidationException {
    if (validator != null) {
      return validator.validate(this, value);
    }
    return value;
  }


  public static class Default extends MetadataField {
    public Default(String key) {
      super(key, Cardinality.Single);
    }
  }

  public static enum Cardinality {Single, Multi};

  public interface Validator {
    /** Validate and/or normalize value.
     * @param field the field being stored
     * @param value the value being stor
     * @return Original value or a normalized value to store
     * @throws MetadataField.ValidationException if the value is illegal
     * for the field
     */
    public String validate(MetadataField field, String value)
	throws MetadataException.ValidationException;
  }
}
