/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.tdb;

import java.util.*;

/**
 * <p>
 * A lightweight class (struct) to represent a title during TDB processing.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Title {

  /**
   * <p>
   * Makes a new title instance with the given parent publisher (useful for
   * tests).
   * </p>
   * 
   * @param publisher A parent publisher for the title.
   * @since 1.67
   */
  protected Title(Publisher publisher) {
    this(publisher, new LinkedHashMap<String, String>());
  }
  
  /**
   * <p>
   * Makes a new title instance with the given parent publisher and map.
   * </p>
   * 
   * @param publisher A parent publisher for the title.
   * @param map A map of key-value pairs for the title.
   * @since 1.67
   */
  public Title(Publisher publisher, Map<String, String>map) {
    this.publisher = publisher;
    this.map = map;
  }
  
  /**
   * <p>
   * Parent publisher.
   * </p>
   * 
   * @since 1.67
   */
  protected Publisher publisher;
  
  /**
   * <p>
   * Retrieves the title's parent publisher.
   * </p>
   * 
   * @return The title's parent publisher.
   * @since 1.67
   */
  public Publisher getPublisher() {
    return publisher;
  }

  /**
   * <p>
   * Internal storage map.
   * </p>
   * 
   * @since 1.67
   */
  protected Map<String, String> map;
  
  /**
   * <p>
   * Retrieves a value from the internal storage map.
   * </p>
   * 
   * @param key A key.
   * @return The value for the key, or <code>null</code> if none is set.
   * @since 1.67
   */
  public String getArbitraryValue(String key) {
    return map.get(key);
  }
  
  /**
   * <p>
   * Title's DOI (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String DOI = "doi";
  
  /**
   * <p>
   * Title's DOI (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _doi = false;
  
  /**
   * <p>
   * Title's DOI (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String doi = null;
  
  /**
   * <p>
   * Retrieves the title's DOI.
   * </p>
   * 
   * @return The title's DOI, or <code>null</code> if not set.
   * @since 1.67
   */
  public String getDoi() {
    if (!_doi) {
      _doi = true;
      doi = map.get(DOI);
    }
    return doi;
  }
  
  /**
   * <p>
   * Title's eISSN (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String EISSN = "eissn";
  
  /**
   * <p>
   * Title's eISSN (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _eissn = false;
  
  /**
   * <p>
   * Title's eISSN (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String eissn = null;
  
  /**
   * <p>
   * Retrieves the title's eISSN.
   * </p>
   * 
   * @return The title's eISSN, or <code>null</code> if not set.
   * @since 1.67
   */
  public String getEissn() {
    if (!_eissn) {
      _eissn = true;
      eissn = map.get(EISSN);
    }
    return eissn;
  }
  
  /**
   * <p>
   * Title's ISSN (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ISSN = "issn";
  
  /**
   * <p>
   * Title's ISSN (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _issn = false;

  /**
   * <p>
   * Title's ISSN (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String issn = null;
  
  /**
   * <p>
   * Retrieves the title's ISSN.
   * </p>
   * 
   * @return The title's ISSN, or <code>null</code> if not set.
   * @since 1.67
   */
  public String getIssn() {
    if (!_issn) {
      _issn = true;
      issn = map.get(ISSN);
    }
    return issn;
  }
  
  /**
   * <p>
   * Title's ISSN-L (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ISSNL = "issnl";

  /**
   * <p>
   * Title's ISSN-L (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _issnl = false;
  
  /**
   * <p>
   * Title's ISSN-L (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String issnl = null;
  
  /**
   * <p>
   * Retrieves the title's ISSN-L.
   * </p>
   * 
   * @return The title's ISSN-L, or <code>null</code> if not set.
   * @since 1.67
   */
  public String getIssnl() {
    if (!_issnl) {
      _issnl = true;
      issnl = map.get(ISSNL);
    }
    return issnl;
  }

  /**
   * <p>
   * Title's name (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String NAME = "name";

  /**
   * <p>
   * Title's name (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _name = false;

  /**
   * <p>
   * Title's name (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String name = null;
  
  /**
   * <p>
   * Retrieves the title's name.
   * </p>
   * 
   * @return The title's name.
   * @since 1.67
   */
  public String getName() {
    if (!_name) {
      _name = true;
      name = map.get(NAME);
    }
    return name;
  }
  
  /**
   * <p>
   * Title's type (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String TYPE = "type";

  /**
   * <p>
   * Title's type (flag).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean _type = false;

  /**
   * <p>
   * Title's type (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String type = null;

  /**
   * <p>
   * The book title type ({@value}).
   * </p>
   * 
   * @since 1.67
   * @deprecated As of 1.68, this constant is deprecated in favor of
   *             {@link #TYPE_BOOK_SERIES}, because the title type was meant to
   *             refer to the type of a publication (title), not the type of a
   *             publication's individual component (AU).
   * @see #TYPE_BOOK_SERIES
   */
  @Deprecated
  public static final String TYPE_BOOK = "book";

  /**
   * <p>
   * The book series title type ({@value}). 
   * </p>
   * 
   * @since 1.68
   */
  public static final String TYPE_BOOK_SERIES = "bookSeries";

  /**
   * <p>
   * The journal title type ({@value}). 
   * </p>
   * 
   * @since 1.67
   */
  public static final String TYPE_JOURNAL = "journal";

  /**
   * <p>
   * The proceedings title type ({@value}). 
   * </p>
   * 
   * @since 1.68
   */
  public static final String TYPE_PROCEEDINGS = "proceedings";

  /**
   * <p>
   * The default title type ({@link #TYPE_JOURNAL}).
   * </p>
   * 
   * @since 1.67
   * @see #TYPE_JOURNAL
   */
  public static final String TYPE_DEFAULT = TYPE_JOURNAL;

  /**
   * <p>
   * Retrieves the title's type.
   * </p>
   * 
   * @return The title's type, by default {@link #TYPE_DEFAULT}.
   * @since 1.67
   * @see #TYPE_DEFAULT
   * @see #TYPE_BOOK
   * @see #TYPE_BOOK_SERIES
   * @see #TYPE_JOURNAL
   * @see #TYPE_PROCEEDINGS
   */
  public String getType() {
    if (!_type) {
      _type = true;
      type = map.get(TYPE);
      if (type == null) {
        type = TYPE_DEFAULT;
      }
    }
    return type;
  }
  
}