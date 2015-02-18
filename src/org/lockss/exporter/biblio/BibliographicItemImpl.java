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

/**
 * A basic implementation of the <code>BibliographicItem</code> interface which
 * is mutable. All bibliographic data is passed though setters. Extends the 
 * {@link BibliographicItemAdapter}, so setters return BibliographicItemAdapter 
 * so they can be chained.
 *
 * @author Neil Mayo
 */
public class BibliographicItemImpl extends BibliographicItemAdapter {

  /**
   * Create a BibliographicItem with no properties.
   */
  public BibliographicItemImpl() {}

  /**
   * Create a BibliographicItemImpl by copying all the field values from the
   * supplied BibliographicItemAdapter.
   * @param other another BibliographicItemAdapter
   */
  public BibliographicItemImpl(BibliographicItemAdapter other) {
    // initialize by copying properties from another BibliographicItemAdapter
    this.copyFrom(other);
  }

  /**
   * Clone all the fields of this item and into a new item and return it.
   * @return
   */
  public BibliographicItemImpl clone() {
    return new BibliographicItemImpl(this);
  }

}
