/*
 * $Id: NormalizeXObjects.java,v 1.1 2007-11-21 02:04:18 thib_gc Exp $
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

package uk.org.lockss.plugin.annualreviews;

import java.io.IOException;
import java.util.List;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.pdfbox.util.operator.OperatorProcessor;

public class NormalizeXObjects extends AggregatePageTransform {

  public static class NormalizeDownloadedFrom extends PageStreamTransform {

    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

      @Override
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

      @Override
      public boolean identify(List tokens) {
        boolean ret = false;
        int progress = 0;
        // Iterate from the end
        iteration: for (int tok = tokens.size() - 1 ; tok >= 0 ; --tok) {
          switch (progress) {
            case 0:
              // End of subsequence
              if (tok != tokens.size() - 1) { break iteration; }
              // ET
              if (PdfUtil.isEndTextObject(tokens, tok)) { ++progress; }
              break;
            case 1:
              // Not BT
              if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
              // Tj and its argument ends with the string "Downloaded from " followed by a domain string
              if (PdfUtil.matchShowTextMatches(tokens, tok, ".*Downloaded from (?:http://)?[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+")) { ++progress; }
              break;
            case 2:
              // Not BT
              if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
              // Tm
              if (PdfUtil.matchSetTextMatrix(tokens, tok)) { ++progress; }
              break;
            case 3:
              // BT; beginning of subsequence
              if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
              break;
          }
        }
        if (AnnualReviewsPdfFilterFactory.logger.isDebug3()) {
          AnnualReviewsPdfFilterFactory.logger.debug3("NormalizeDownloadedFrom.ProcessEndTextObject candidate match: " + ret);
        }
        return ret;
      }

    }

    public NormalizeDownloadedFrom(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class,
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, NormalizeDownloadedFrom.ProcessEndTextObject.class);
    }

  }

  public static class NormalizePersonalUse extends PageStreamTransform {

    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

      @Override
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

      @Override
      public boolean identify(List tokens) {
        boolean ret = false;
        int progress = 0;
        // Iterate from the end
        iteration: for (int tok = tokens.size() - 1 ; tok >= 0 ; --tok) {
          switch (progress) {
            case 0:
              // End of subsequence
              if (tok != tokens.size() - 1) { break iteration; }
              // ET
              if (PdfUtil.isEndTextObject(tokens, tok)) { ++progress; }
              break;
            case 1:
              // Not BT
              if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
              // Tj and its argument begins with the string "by", contains a date, and ends with the string "For personal use only."
              if (PdfUtil.matchShowTextMatches(tokens, tok, "by .* on [0-9]{2}/[0-9]{2}/[0-9]{2}. For personal use only.")) { ++progress; }
              break;
            case 2:
              // Not BT
              if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
              // Tm
              if (PdfUtil.matchSetTextMatrix(tokens, tok)) { ++progress; }
              break;
            case 3:
              // BT; beginning of subsequence
              if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
              break;
          }
        }
        if (AnnualReviewsPdfFilterFactory.logger.isDebug3()) {
          AnnualReviewsPdfFilterFactory.logger.debug3("NormalizePersonalUse.ProcessEndTextObject candidate match: " + ret);
        }
        return ret;
      }

    }

    public NormalizePersonalUse(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                               OperatorProcessor.class);
              }
            },
            PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class,
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, NormalizePersonalUse.ProcessEndTextObject.class);
    }

  }

  public NormalizeXObjects(ArchivalUnit au) throws IOException {
    super(new NormalizeDownloadedFrom(au),
          new NormalizePersonalUse(au));
  }

}