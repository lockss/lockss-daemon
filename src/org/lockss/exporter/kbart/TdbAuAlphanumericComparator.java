/*
 * $Id: TdbAuAlphanumericComparator.java,v 1.5 2011-08-11 16:52:38 easyonthemayo Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import org.lockss.config.TdbAu;
import org.lockss.util.CachingComparator;

/**
 * A comparator for sorting <code>TdbAu</code>s alphanumerically, based on the 
 * specified comparison string. The class is abstract so instances must each 
 * explicitly implement the method to provide their comparison string. This
 * class does not provide any singletons for caching behaviour as the number
 * of AUs is considered too large. 
 *	
 * @author neil
 */
public abstract class TdbAuAlphanumericComparator extends CachingComparator<TdbAu> {

  /**
   * Create a comparator with the default case-sensitivity of KbartTitle fields.
   */
  public TdbAuAlphanumericComparator() {
    this(KbartTitle.Field.CASE_SENSITIVITY_DEFAULT);
  }

  /**
   * Create a comparator with the specified case-sensitivity of KbartTitle fields.
   * 
   * @param caseSensitive whether alphanumeric comparison should be case sensitive 
   */
  public TdbAuAlphanumericComparator(boolean caseSensitive) {
    super(caseSensitive);
  }
  
  @Override
  protected String getComparisonString(TdbAu tdbAu) {
    String s = getTdbAuComparisonString(tdbAu);
    return s==null ? "" : s;
  }

  /**
   * The <code>getComparisonString()</code> method delegates to this one
   * so that we can make it abstract, forcing subclasses to override it 
   * and explicitly specify their comparison string as an alternative to 
   * the default. 
   * 
   * @return the string which is subjected to alphanumeric comparison
   */
  protected abstract String getTdbAuComparisonString(TdbAu tdbAu);
  
}

