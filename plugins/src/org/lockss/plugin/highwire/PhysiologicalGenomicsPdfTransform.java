/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.IOException;
import java.util.Iterator;

import org.lockss.filter.PdfTransform;
import org.lockss.util.*;
import org.lockss.util.PdfTransformUtil.PdfTokenSubstreamMutator;
import org.pdfbox.pdmodel.PDPage;

/**
 * <p>This PDF transform identifies and processes PDF documents that
 * match a template found in certain HighWire titles, for instance
 * Physiological Genomics.</p>
 * @author Thib Guicherd-Callin
 * @see <a href="http://physiolgenomics.physiology.org/">Physiological
 *      Genomics</a>
 * @see HighWirePdfFilterRule
 */
public class PhysiologicalGenomicsPdfTransform implements PdfTransform {

  public static class OtherPages implements PdfTokenSubstreamMutator {

    public boolean isLastSubstreamToken(Object candidateToken) {
      return PdfTransformUtil.isPdfOperator(candidateToken, PdfTransformUtil.END_TEXT_OBJECT);
    }

    public void processTokenSubstream(Object[] pdfTokens, Logger logger) throws IOException {
      PdfTransformUtil.replacePdfString(pdfTokens[11], " ", logger);
    }

    public boolean tokenSubstreamMatches(Object[] pdfTokens, Logger logger) {
      boolean ret = PdfTransformUtil.isPdfOperator(pdfTokens[0], PdfTransformUtil.BEGIN_TEXT_OBJECT)
      && PdfTransformUtil.isPdfOperator(pdfTokens[17], PdfTransformUtil.END_TEXT_OBJECT)
      && PdfTransformUtil.isPdfOperator(pdfTokens[20], PdfTransformUtil.BEGIN_TEXT_OBJECT)
      && PdfTransformUtil.isPdfOperator(pdfTokens[35], PdfTransformUtil.END_TEXT_OBJECT)
      && PdfTransformUtil.isPdfOperator(pdfTokens[38], PdfTransformUtil.BEGIN_TEXT_OBJECT)
      && PdfTransformUtil.isPdfOperator(pdfTokens[12], PdfTransformUtil.SHOW_TEXT)
      && PdfTransformUtil.isPdfString(pdfTokens[11])
      && PdfTransformUtil.isPdfOperator(pdfTokens[32], PdfTransformUtil.SHOW_TEXT)
      && PdfTransformUtil.isPdfString(pdfTokens[31])
      && PdfTransformUtil.isPdfOperator(pdfTokens[50], PdfTransformUtil.SHOW_TEXT)
      && PdfTransformUtil.isPdfString(pdfTokens[49])
      && "Downloaded from ".equals(PdfTransformUtil.getPdfString(pdfTokens[49]));
      logger.debug3("Evaluating candidate match: " + Boolean.toString(ret));
      return ret;
    }

    public int tokenSubstreamSize() {
      return 52;
    }

    private static OtherPages singleton;

    public static synchronized OtherPages getInstance() {
      if (singleton == null) {
        singleton = new OtherPages();
      }
      return singleton;
    }

  }

  protected static class FirstPage implements PdfTokenSubstreamMutator {

    private FirstPage() {}

    public boolean isLastSubstreamToken(Object candidateToken) {
      return PdfTransformUtil.isPdfOperator(candidateToken, PdfTransformUtil.END_TEXT_OBJECT);
    }

    public void processTokenSubstream(Object[] pdfTokens, Logger logger) throws IOException {
      PdfTransformUtil.replacePdfString(pdfTokens[11], " ", logger);
    }

    public boolean tokenSubstreamMatches(Object[] pdfTokens, Logger logger) {
      boolean ret = PdfTransformUtil.isPdfOperator(pdfTokens[0], PdfTransformUtil.BEGIN_TEXT_OBJECT)
      && PdfTransformUtil.isPdfOperator(pdfTokens[12], PdfTransformUtil.SHOW_TEXT)
      && PdfTransformUtil.isPdfString(pdfTokens[11])
      && PdfTransformUtil.isPdfOperator(pdfTokens[21], PdfTransformUtil.SHOW_TEXT)
      && PdfTransformUtil.isPdfString(pdfTokens[20])
      && PdfTransformUtil.isPdfOperator(pdfTokens[28], PdfTransformUtil.SHOW_TEXT)
      && PdfTransformUtil.isPdfString(pdfTokens[27])
      && "Downloaded from ".equals(PdfTransformUtil.getPdfString(pdfTokens[27]));
      logger.debug3("Evaluating candidate match: " + Boolean.toString(ret));
      return ret;
    }

    public int tokenSubstreamSize() {
      return 30;
    }

    private static FirstPage singleton;

    public static synchronized FirstPage getInstance() {
      if (singleton == null) {
        singleton = new FirstPage();
      }
      return singleton;
    }

  }

  private PhysiologicalGenomicsPdfTransform() {}

  public void transform(PdfDocument pdfDocument, Logger logger) throws IOException {
    logger.debug2("Applying transform");
    if (identify(pdfDocument, logger)) {
      logger.debug2("Positive identification");
      Iterator iter = pdfDocument.getPageIterator();

      logger.debug3("Removing vertical text from first page");
      PdfTransformUtil.runPdfTokenSubstreamMutator(FirstPage.getInstance(),
                                                   pdfDocument,
                                                   (PDPage)iter.next(),
                                                   true,
                                                   logger);

      while (iter.hasNext()) {
        logger.debug3("Removing vertical text from another page");
        PdfTransformUtil.runPdfTokenSubstreamMutator(OtherPages.getInstance(),
                                                     pdfDocument,
                                                     (PDPage)iter.next(),
                                                     true,
                                                     logger);
      }
    }
  }

  protected boolean identify(PdfDocument pdfDocument, Logger logger) throws IOException {
    return PdfTransformUtil.runPdfTokenSubstreamMatcher(FirstPage.getInstance(),
                                                        pdfDocument.getPage(0),
                                                        logger);
  }

  private static PhysiologicalGenomicsPdfTransform singleton;

  public static synchronized PdfTransform getInstance() {
    if (singleton == null) {
      singleton = new PhysiologicalGenomicsPdfTransform();
    }
    return singleton;
  }

}
