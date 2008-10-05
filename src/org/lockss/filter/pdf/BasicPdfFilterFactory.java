/*
 * $Id: BasicPdfFilterFactory.java,v 1.5 2007-07-31 22:40:24 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.pdf;

import java.io.InputStream;

import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.util.*;

/**
 * <p>A default implementation of {@link FilterFactory} that assumes
 * that the name of an {@link OutputDocumentTransform} implementor
 * can be found in the {@link ArchivalUnit}'s attributes under the
 * following key:
 * <code>PdfUtil.PDF_FILTER_FACTORY_HINT_PREFIX + PdfUtil.PDF_MIME_TYPE + DefinableArchivalUnit.AU_FILTER_FACTORY_SUFFIX</code></p>
 * @author Thib Guicherd-Callin
 * @see PdfUtil#PREFIX_PDF_FILTER_FACTORY_HINT
 * @see PdfUtil#PDF_MIME_TYPE
 * @see DefinableArchivalUnit#SUFFIX_FILTER_FACTORY
 */
public class BasicPdfFilterFactory implements FilterFactory {

  /* Inherit documentation */
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    logger.debug2("Basic PDF filter factory for: " + au.getName());
    OutputDocumentTransform documentTransform = PdfUtil.getOutputDocumentTransform(au);
    if (documentTransform == null) {
      logger.debug2("Unfiltered");
      return in;
    }
    else {
      logger.debug2("Filtered with " + documentTransform.getClass().getName());
      return PdfUtil.applyFromInputStream(documentTransform, in);
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("BasicPdfFilterFactory");

}
