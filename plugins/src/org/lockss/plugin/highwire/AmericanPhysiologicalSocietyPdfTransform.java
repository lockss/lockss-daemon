/*
 * $Id: AmericanPhysiologicalSocietyPdfTransform.java,v 1.12 2006-09-10 07:50:51 thib_gc Exp $
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
public class AmericanPhysiologicalSocietyPdfTransform extends AggregateDocumentTransform {

  public static class EraseDateString extends PageStreamTransform {
    public EraseDateString() throws IOException {
      super(PdfUtil.SHOW_TEXT, ProcessDateString.class);
    }
  }

  public static class EraseVerticalText extends PageStreamTransform {
    public EraseVerticalText() throws IOException {
      super(PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject.class);
    }
  }

  public static class EraseVerticalText2 extends PageStreamTransform {
    public EraseVerticalText2() throws IOException {
      super(PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject2.class);
    }
  }

  public static class FixHyperlink implements PageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      int s = ((COSArray)pdfPage.getDictionary().getDictionaryObject(COSName.ANNOTS)).size();
      COSArray array = (COSArray)pdfPage.getDictionary().getObjectFromPath("Annots/[" + (s-1) + "]/Rect");
      array.set(1, new COSFloat(300.0f));
      array.set(3, new COSFloat(525.0f));
      return true;
    }
  }

  public static class ProcessDateString extends ProcessShowText {
    public String getReplacement(String match) {
      return " ";
    }
    public boolean identify(String candidate) {
      return candidate.startsWith("This information is current as of ");
    }
  }

  public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {
    public List getReplacement(List tokens) {
      return ListUtil.list(tokens.get(0), tokens.get(29));
    }
    public boolean identify(List tokens) {
      boolean ret = tokens.size() == 30
      && PdfUtil.isEndTextObject(tokens.get(29))
      && PdfUtil.isBeginTextObject(tokens.get(0))
      && PdfUtil.isPdfFloat(tokens.get(9))
      && PdfUtil.isShowText(tokens.get(12))
      && PdfUtil.isPdfString(tokens.get(11))
      && PdfUtil.isShowText(tokens.get(21))
      && PdfUtil.isPdfString(tokens.get(20))
      && PdfUtil.isShowText(tokens.get(28))
      && PdfUtil.isPdfString(tokens.get(27))
      && PdfUtil.getPdfString(tokens.get(27)).equals("Downloaded from ");
      logger.debug3("ProcessEndTextObject candidate match: " + ret);
      return ret;
    }
  }

  public static class ProcessEndTextObject2 extends ConditionalSubsequenceOperatorProcessor {
    public List getReplacement(List tokens) {
      return ListUtil.list(tokens.get(0), tokens.get(51));
    }
    public int getSubsequenceLength() {
      return 52;
    }
    public boolean identify(List tokens) {
      boolean ret = tokens.size() == 52
      && PdfUtil.isEndTextObject(tokens.get(51))
      && PdfUtil.isBeginTextObject(tokens.get(0))
      && PdfUtil.isPdfFloat(tokens.get(9))
      && PdfUtil.isShowText(tokens.get(12))
      && PdfUtil.isPdfString(tokens.get(11))
      && PdfUtil.isEndTextObject(tokens.get(17))
      && PdfUtil.isBeginTextObject(tokens.get(20))
      && PdfUtil.isPdfFloat(tokens.get(29))
      && PdfUtil.isShowText(tokens.get(32))
      && PdfUtil.isPdfString(tokens.get(31))
      && PdfUtil.isEndTextObject(tokens.get(35))
      && PdfUtil.isBeginTextObject(tokens.get(38))
      && PdfUtil.isPdfFloat(tokens.get(47))
      && PdfUtil.isShowText(tokens.get(50))
      && PdfUtil.isPdfString(tokens.get(49))
      && PdfUtil.getPdfString(tokens.get(49)).equals("Downloaded from ");
      logger.debug3("ProcessEndTextObject2 candidate match: " + ret);
      return ret;
    }
  }

  public static class SanitizeMetadata implements DocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.removeModificationDate();
      COSArray idArray = (COSArray)pdfDocument.getTrailer().getItem(COSName.getPDFName("ID"));
      idArray.set(1, idArray.get(0));
      pdfDocument.setMetadata(" ");
      return true;
    }
  }

  public AmericanPhysiologicalSocietyPdfTransform() throws IOException {
    super(new TransformFirstPage(new AggregatePageTransform(new EraseVerticalText(),
                                                            new EraseDateString())),
          new TransformEachPageExceptFirst(new AggregatePageTransform(PdfUtil.OR,
                                                                      new EraseVerticalText(),
                                                                      new EraseVerticalText2())),
          new TransformEachPage(new FixHyperlink()),
          new SanitizeMetadata());
  }

  protected static Logger logger = Logger.getLogger("AmericanPhysiologicalSocietyPdfTransform");

}
