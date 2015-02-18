/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.Serializable;

/**
 * A simple class for encapsulating customisation options for KBART reports.
 *
 * @author Neil Mayo
 */
public class KbartCustomOptions implements Serializable {

  private boolean omitEmptyColumns;
  private boolean omitHeader;
  private boolean excludeNoIdTitles;
  private boolean showHealthRatings;
  private KbartExportFilter.ColumnOrdering columnOrdering;

  public KbartCustomOptions(boolean omitEmptyColumns,
                            boolean omitHeader,
                            boolean excludeNoIdTitles,
                            boolean health,
                            KbartExportFilter.ColumnOrdering ord) {
    this.omitEmptyColumns = omitEmptyColumns;
    this.omitHeader = omitHeader;
    this.excludeNoIdTitles = excludeNoIdTitles;
    this.showHealthRatings = health;
    this.columnOrdering = ord;
  }

  public static KbartCustomOptions getDefaultOptions() {
    return new KbartCustomOptions(
        KbartExporter.omitEmptyFieldsByDefault,
        KbartExporter.omitHeaderRowByDefault,
        KbartExporter.excludeNoIdTitlesByDefault,
        KbartExporter.showHealthRatingsByDefault,
        KbartExportFilter.CustomColumnOrdering.getDefaultOrdering()
    );
  }

  public boolean isOmitEmptyColumns() {
    return omitEmptyColumns;
  }

  public void setOmitEmptyColumns(boolean omitEmptyColumns) {
    this.omitEmptyColumns = omitEmptyColumns;
  }

  public boolean isOmitHeader() {
    return omitHeader;
  }

  public void setOmitHeader(boolean omitHeader) {
    this.omitHeader = omitHeader;
  }

  public boolean isExcludeNoIdTitles() {
    return excludeNoIdTitles;
  }

  public void setExcludeNoIdTitles(boolean excludeNoIdTitles) {
    this.excludeNoIdTitles = excludeNoIdTitles;
  }

  public boolean isShowHealthRatings() {
    return showHealthRatings;
  }

  public void setShowHealthRatings(boolean showHealthRatings) {
    this.showHealthRatings = showHealthRatings;
  }

  public KbartExportFilter.ColumnOrdering getColumnOrdering() {
    return columnOrdering;
  }

  public void setColumnOrdering(KbartExportFilter.ColumnOrdering columnOrdering) {
    this.columnOrdering = columnOrdering;
  }

}
