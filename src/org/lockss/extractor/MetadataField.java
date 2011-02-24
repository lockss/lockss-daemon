/*
 * $Id: MetadataField.java,v 1.2.2.2 2011-02-24 00:28:01 pgust Exp $
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
import org.apache.commons.lang.StringUtils;

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
      @Override
      public String validate(ArticleMetadata am, String val)
	  throws MetadataException.ValidationException {
	// normalize away leading "doi:" before checking validity
	String doi = StringUtils.removeStartIgnoreCase(val, PROTOCOL_DOI);
	if (!MetadataUtil.isDOI(doi)) {
	  throw new MetadataException.ValidationException("Illegal DOI: "
							  + val);
	}
	return doi;
      }};	


  public static final String PROTOCOL_ISSN = "issn:";
  public static final String KEY_ISSN = "issn";
  public static final MetadataField FIELD_ISSN =
    new MetadataField(KEY_ISSN, Cardinality.Single) {
      @Override
      public String validate(ArticleMetadata am, String val)
	  throws MetadataException.ValidationException {
	// normalize away leading "issn:" before checking validity
	String issn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_ISSN);
	if (!MetadataUtil.isISSN(issn)) {
	  throw new MetadataException.ValidationException("Illegal ISSN: "
							  + val);
	}
	return issn;
      }};

  public static final String PROTOCOL_EISSN = "eissn:";
  public static final String KEY_EISSN = "eissn";
  public static final MetadataField FIELD_EISSN =
    new MetadataField(KEY_EISSN, Cardinality.Single) {
      @Override
      public String validate(ArticleMetadata am, String val)
	  throws MetadataException.ValidationException {
	// normalize away leading "eissn:" before checking validity
	String issn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_EISSN);
	if (!MetadataUtil.isISSN(issn)) {
	  throw new MetadataException.ValidationException("Illegal EISSN: "
							  + val);
	}
	return issn;
      }};
      
  public static final String PROTOCOL_ISBN = "isbn:";
  public static final String KEY_ISBN = "isbn";
  public static final MetadataField FIELD_ISBN =
    new MetadataField(KEY_ISBN, Cardinality.Single) {
    @Override
    public String validate(ArticleMetadata am, String val)
        throws MetadataException.ValidationException {
      // normalize away leading "isbn:" before checking validity
      String isbn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_ISBN);
      if (!MetadataUtil.isISBN(isbn, false)) {  // ignore publisher malformed ISBNs
        throw new MetadataException.ValidationException("Illegal ISBN: "
                                                        + val);
      }
      return isbn;
    }};

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

  /** Return the predefined MetadataField with the given key, or null if
   * none */
  public static MetadataField findField(String key) {
    return fieldMap.get(key.toLowerCase());
  }


  protected final String key;
  protected final Cardinality cardinality;
  protected final Validator validator;
  protected final Splitter splitter;

  /** Create a metadata field descriptor with Cardinality.Single
   * @param key the map key
   */
  public MetadataField(String key) {
    this(key, Cardinality.Single, null, null);
  }

  /** Create a metadata field descriptor
   * @param key the map key
   * @param cardinality
   */
  public MetadataField(String key, Cardinality cardinality) {
    this(key, cardinality, null, null);
  }

  /** Create a metadata field descriptor
   * @param key the map key
   * @param cardinality
   * @param validator
   */
  public MetadataField(String key, Cardinality cardinality,
		       Validator validator) {
    this(key, cardinality, validator, null);
  }

  /** Create a metadata field descriptor
   * @param key the map key
   * @param cardinality
   * @param splitter
   */
  public MetadataField(String key, Cardinality cardinality,
		       Splitter splitter) {
    this(key, cardinality, null, splitter);
  }

  /** Create a metadata field descriptor
   * @param key the map key
   * @param cardinality
   * @param validator
   */
  public MetadataField(String key, Cardinality cardinality,
		       Validator validator, Splitter splitter) {
    this.key = key;
    this.cardinality = cardinality;
    this.validator = validator;
    if (cardinality != Cardinality.Multi && splitter != null) {
      throw new IllegalArgumentException("Splitter legal only with Cardinality.Multi");
    }
    this.splitter = splitter;
  }

  /** Create a MetadataField that's a copy of another one
   * @param field the MetadataField to copy
   * @param splitter
   */
  public MetadataField(MetadataField field) {
    this(field.getKey(), field.getCardinality(), field.getValidator());
  }

  /** Create a MetadataField that's a copy of another one
   * @param field the MetadataField to copy
   */
  public MetadataField(MetadataField field, Splitter splitter) {
    this(field.getKey(), field.getCardinality(),
	 field.getValidator(), splitter);
  }

  /** Return the field's key. */
  public String getKey() {
    return key;
  }

  /** Return the field's cardinality. */
  public Cardinality getCardinality() {
    return cardinality;
  }

  private Validator getValidator() {
    return validator;
  }

  /** If a validator is present, apply it to the argument.  If valid,
   * return the argument or a normalized value.  If invalid, throw
   * MetadataException.ValidationException */
  public String validate(ArticleMetadata am, String value)
      throws MetadataException.ValidationException {
    if (validator != null) {
      return validator.validate(am, this, value);
    }
    return value;
  }

  /** If a splitter is present, apply it to the argument return a list of
   * strings.  If no splitter is present, return a singleton list of the
   * argument */
  public List<String> split(ArticleMetadata am, String value) {
    if (splitter != null) {
      return splitter.split(am, this, value);
    }
    return ListUtil.list(value);
  }

  public boolean hasSplitter() {
    return splitter != null;
  }


  /** Cardinality of a MetadataField: single-valued or multi-valued */
  public static enum Cardinality {Single, Multi};

  static class Default extends MetadataField {
    public Default(String key) {
      super(key, Cardinality.Single);
    }
  }

  /** Validator can be associated with a MetadataField to check and/or
   * normalize values when stored. */
  public interface Validator {
    /** Validate and/or normalize value.
     * @param am the ArticleMeta being stored into (source of Locale, if
     * necessary)
     * @param field the field being stored
     * @param value the value being stored
     * @return original value or a normalized value to store
     * @throws MetadataField.ValidationException if the value is illegal
     * for the field
     */
    public String validate(ArticleMetadata am,
			   MetadataField field,
			   String value)
	throws MetadataException.ValidationException;
  }

  /** Splitter can be associated with a MetadataField to split value
   * strings into substring to be stored into a multi-valued field. */
  public interface Splitter {
    /** Split a value into a list of values
     * @param am the ArticleMeta being stored into (source of Locale, if
     * necessary)
     * @param field the field being stored
     * @param value the value being stored
     * @return list of values
     */
    public List<String> split(ArticleMetadata am,
			      MetadataField field,
			      String value);
  }

  /** Return a Splitter that splits substrings separated by the separator
   * string.
   * @param separator the separator string
   */
  public static Splitter splitAt(String separator) {
    return new SplitAt(separator, null, null);
  }

  /** Return a Splitter that first removes the delimiter string from the
   * ends of the input, then splits substrings separated by the separator
   * string. 
   * @param separator the separator string
   * @param delimiter the delimiter string removed from both ends of the input
   */
  public static Splitter splitAt(String separator, String delimiter) {
    return new SplitAt(separator, delimiter, delimiter);
  }

  /** Return a Splitter that first removes the two delimiter strings from
   * the front and end of the input, respectively, then splits substrings
   * separated by the separator string.
   * @param separator the separator string
   * @param delimiter1 the delimiter string removed from the beginning of
   * the input
   * @param delimiter2 the delimiter string removed from the end of the
   * input
   */
  public static Splitter splitAt(String separator,
				 String delimiter1,
				 String delimiter2) {
    return new SplitAt(separator, delimiter1, delimiter2);
  }

  /** A Splitter that splits substrings separated by a separator string,
   * optionally after removing delimiters from the beginning and end of the
   * string.  Blanks are trimmed from the ends of the input string and from
   * each substring, and empty substrings are discarded. */
  public static class SplitAt implements Splitter {
    protected String splitSep;
    protected String splitDelim1;
    protected String splitDelim2;

    public SplitAt(String separator, String delimiter1, String delimiter2) {
      splitSep = separator;
      splitDelim1 = delimiter1;
      splitDelim2 = delimiter2;
    }

    public List<String> split(ArticleMetadata am,
			      MetadataField field,
			      String value) {
      value = value.trim();
      if (splitDelim1 != null) {
	if (splitDelim2 == null) {
	  splitDelim2 = splitDelim1;
	}
	value = StringUtils.removeStartIgnoreCase(value, splitDelim1);
	value = StringUtils.removeEndIgnoreCase(value, splitDelim2);
      }
      return StringUtil.breakAt(value, splitSep, -1, true, true);
    }
  }
}
