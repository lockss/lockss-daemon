/*
 * $Id: AmericanPhysiologicalSocietyPdfTransform.java,v 1.17 2006-09-21 05:50:52 thib_gc Exp $
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
import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.SanitizeMetadata;
import org.lockss.util.*;
import org.pdfbox.cos.*;

// The following Javadoc comment needs rewriting
/**
 * <p>This PDF transform identifies and processes PDF documents that
 * match a template found in certain titles published by the
 * American Physiological Society.</p>
 * @author Thib Guicherd-Callin
 * @see <a href="http://www.physiology.org/">American Physiological
 * Society Journals Online</a>
 * @see HighWirePdfFilterRule
 */
public class AmericanPhysiologicalSocietyPdfTransform extends ConditionalDocumentTransform {

  public static class EraseDateString extends PageStreamTransform {

    public static class ProcessDateString extends ReplaceString {
      public String getReplacement(String match) {
        return " ";
      }
      public boolean identify(String candidate) {
        return candidate.startsWith("This information is current as of ");
      }
    }
    public EraseDateString() throws IOException {
      super(PdfUtil.SHOW_TEXT, ProcessDateString.class);
    }

  }

  public static class EraseVerticalText extends PageStreamTransform {

    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {
      public List getReplacement(List tokens) {
        return ListUtil.list(tokens.get(0), tokens.get(29));
      }
      public boolean identify(List tokens) {
        boolean ret = tokens.size() == 30
        && PdfUtil.matchTextObject(tokens, 0, 29)
        && PdfUtil.isPdfFloat(tokens, 9)
        && PdfUtil.matchShowText(tokens, 12)
        && PdfUtil.matchSetRgbColorNonStroking(tokens, 16, 0, 0, 1)
        && PdfUtil.matchShowText(tokens, 21)
        && PdfUtil.matchShowText(tokens, 28, "Downloaded from ");;
        logger.debug3("ProcessEndTextObject candidate match: " + ret);
        return ret;
      }
    }

    public EraseVerticalText() throws IOException {
      super(PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject.class);
    }

  }

  public static class EraseVerticalText2 extends PageStreamTransform {

    public static class ProcessEndTextObject2 extends ConditionalSubsequenceOperatorProcessor {
      public List getReplacement(List tokens) {
        return ListUtil.list(tokens.get(0), tokens.get(51));
      }
      public int getSubsequenceLength() {
        return 52;
      }
      public boolean identify(List tokens) {
        boolean ret = tokens.size() == 52
        && PdfUtil.matchTextObject(tokens, 0, 17)
        && PdfUtil.isPdfFloat(tokens, 9)
        && PdfUtil.matchShowText(tokens, 12)
        && PdfUtil.matchSetRgbColorNonStroking(tokens, 16, 0, 0, 1)
        && PdfUtil.matchTextObject(tokens, 20, 35)
        && PdfUtil.isPdfFloat(tokens, 29)
        && PdfUtil.matchShowText(tokens, 32)
        && PdfUtil.matchTextObject(tokens, 38, 51)
        && PdfUtil.isPdfFloat(tokens, 47)
        && PdfUtil.matchShowText(tokens, 50, "Downloaded from ");
        logger.debug3("ProcessEndTextObject2 candidate match: " + ret);
        return ret;
      }
    }

    public EraseVerticalText2() throws IOException {
      super(PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject2.class);
    }

  }

  public static class FixHyperlink implements PageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      COSArray array = pdfPage.getAnnotation(pdfPage.getNumberOfAnnotations()-1).getRectangle().getCOSArray();
      array.set(1, new COSFloat(1.0f));
      array.set(3, new COSFloat(2.0f));
      return true;
    }
  }

  public AmericanPhysiologicalSocietyPdfTransform() throws IOException {
    super(new TransformFirstPage(new EraseVerticalText(),
                                 new EraseDateString(),
                                 new FixHyperlink()),
          new TransformEachPageExceptFirst(new AggregatePageTransform(PdfUtil.OR,
                                                                      new EraseVerticalText(),
                                                                      new EraseVerticalText2()),
                                           new FixHyperlink()),
          new SanitizeMetadata());
  }

  protected static Logger logger = Logger.getLogger("AmericanPhysiologicalSocietyPdfTransform");

}
