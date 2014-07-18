/*
 * $Id: RSC2014PdfFilterFactory.java,v 1.5.2.2 2014-07-18 15:56:31 wkwilson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class RSC2014PdfFilterFactory extends SimplePdfFilterFactory {
  
  static Logger log = Logger.getLogger(RSC2014PdfFilterFactory.class);
  
  public RSC2014PdfFilterFactory() {
    super();
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument)
      throws PdfException {
    try {
      PdfUtil.normalizeAllTokenStreams(pdfDocument);
    }
    catch (NullPointerException e) {
      // catch Exception that was thrown, but when ignored did not seem to be a problem
      // XXX talked thru with W, may need to revisit if problems appear, but for now no problems
      log.warning("caught/ignore exception from pdf framework", e);
    }
    PdfUtil.normalizeTrailerId(pdfDocument);
  }
  
}
