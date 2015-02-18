/*
 * $Id$
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.biblio;

import org.lockss.util.CachingComparator;

/**
 * A comparator for sorting <code>BibliographicItem</code>s alphanumerically,
 * based on the specified comparison string. The class is abstract so instances
 * must each explicitly implement the method to provide their comparison string.
 * This class does not provide any singletons for caching behaviour as the
 * likely number of <code>BibliographicItem</code>s is considered too large.
 *	
 * @author Neil Mayo
 */
public abstract class BibliographicComparator
    extends CachingComparator<BibliographicItem> {

  /**
   * The default case-sensitivity of string comparisons on bibliographic
   * item fields.
   */
  public static final boolean CASE_SENSITIVITY_DEFAULT = false;

  /**
   * Create a comparator with the default case-sensitivity.
   */
  public BibliographicComparator() {
    this(CASE_SENSITIVITY_DEFAULT);
  }

  /**
   * Create a comparator with the specified case-sensitivity on bibliographic
   * item fields.
   *
   * @param caseSensitive whether alphanumeric comparison should be case sensitive
   */
  public BibliographicComparator(boolean caseSensitive) {
    super(caseSensitive);
  }
  
  @Override
  protected String getComparisonString(BibliographicItem item) {
    String s = getBibliographicComparisonString(item);
    return s==null ? "" : s;
  }

  /**
   * The <code>getComparisonString()</code> method delegates to this one
   * so that we can make it abstract, forcing subclasses to override it 
   * and explicitly specify their comparison string as an alternative to 
   * the default. 
   *
   * @param item a BibliographicItem
   * @return the string which is subjected to alphanumeric comparison
   */
  protected abstract String getBibliographicComparisonString(BibliographicItem item);
  
}

