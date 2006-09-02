/*
 * $Id: AmericanPhysiologicalSocietyPdfTransform.java,v 1.11 2006-09-02 00:18:12 thib_gc Exp $
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

import org.apache.commons.lang.StringUtils;
import org.lockss.filter.pdf.*;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.util.PDFOperator;

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
public class AmericanPhysiologicalSocietyPdfTransform extends ConditionalPdfTransform {

  protected static class FirstPage {

    public static class FixHyperlink implements PdfPageTransform {
      public void transform(PdfDocument pdfDocument, PdfPage pdfPage) throws IOException {
        int s = ((COSArray)pdfPage.getDictionary().getDictionaryObject(COSName.ANNOTS)).size();
        COSArray array = (COSArray)pdfPage.getDictionary().getObjectFromPath("Annots/[" + (s-1) + "]/Rect");
        array.set(1, new COSFloat(300.0f));
        array.set(3, new COSFloat(525.0f));
      }
    }

    public static class ModifyEndTextObject extends SimpleOperatorProcessor {
      public void process(PDFOperator operator,
                          List operands,
                          PdfPageStreamTransform pdfPageStreamTransform)
          throws IOException {
        super.process(operator, operands, pdfPageStreamTransform);
        Object[] pdfTokens = pdfPageStreamTransform.getOutputList().toArray();
        if (recognizeEndTextObject(pdfTokens)) {
          pdfPageStreamTransform.signalChange();
          List replacement = new ArrayList();
          modifyEndTextObject(pdfTokens, replacement);
          pdfPageStreamTransform.mergeOutputList(replacement);
        }
        else {
          pdfPageStreamTransform.mergeOutputList();
        }
      }
    }

    public static class RecognizeEndTextObject extends SimpleOperatorProcessor {
      public void process(PDFOperator operator,
                          List operands,
                          PdfPageStreamTransform pdfPageStreamTransform)
          throws IOException {
        super.process(operator, operands, pdfPageStreamTransform);
        Object[] pdfTokens = pdfPageStreamTransform.getOutputList().toArray();
        if (recognizeEndTextObject(pdfTokens)) {
          pdfPageStreamTransform.signalChange();
        }
        else {
          pdfPageStreamTransform.mergeOutputList();
        }
      }
    }

    public static class ReplaceString extends ShowTextProcessor {
      public boolean candidateMatches(String candidate) {
        return candidate.startsWith(BEGINNING);
      }
      public String getReplacement(String match) {
        return BEGINNING;
      }
      protected static final String BEGINNING = "This information is current as of ";
    }

    protected static final int LENGTH = 30;

    public static Properties getMatcherProperties() throws IOException {
      Properties properties = new Properties();
      properties.setProperty(PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class.getName());
      properties.setProperty(PdfUtil.END_TEXT_OBJECT, RecognizeEndTextObject.class.getName());
      return properties;
    }

    public static Properties getMutatorProperties() throws IOException {
      Properties properties = new Properties();
      properties.setProperty(PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class.getName());
      properties.setProperty(PdfUtil.END_TEXT_OBJECT, ModifyEndTextObject.class.getName());
      properties.setProperty(PdfUtil.SHOW_TEXT, ReplaceString.class.getName());
      return properties;
    }

    public static void modifyEndTextObject(Object[] pdfTokens,
                                           List outputList) {
      for (int tok = 0 ; tok < LENGTH ; ++tok) {
        switch (tok) {
          case 9:  outputList.add(new COSFloat(300.0f)); break;
          case 11: outputList.add(new COSString(" ")); break;
          default: outputList.add(pdfTokens[tok]); break;
        }
      }
    }

    public static boolean recognizeEndTextObject(Object[] pdfTokens) {
      boolean ret = pdfTokens.length == LENGTH
      && PdfUtil.isEndTextObject(pdfTokens[29])
      && PdfUtil.isBeginTextObject(pdfTokens[0])
      && PdfUtil.isPdfFloat(pdfTokens[9])
      && PdfUtil.isShowText(pdfTokens[12])
      && PdfUtil.isPdfString(pdfTokens[11])
      && PdfUtil.isShowText(pdfTokens[21])
      && PdfUtil.isPdfString(pdfTokens[20])
      && PdfUtil.isShowText(pdfTokens[28])
      && PdfUtil.isPdfString(pdfTokens[27])
      && PdfUtil.getPdfString(pdfTokens[27]).equals("Downloaded from ");
      logger.debug3("FirstPageShowTextProcessor candidate match: " + Boolean.toString(ret));
      return ret;
    }

  }

  protected static class OtherPages {

    public static class EndTextObjectProcessor extends SubsequenceOperatorProcessor {

      public boolean identify(Object[] pdfTokens) {
        boolean ret = pdfTokens.length == LENGTH
        && PdfUtil.isEndTextObject(pdfTokens[51])
        && PdfUtil.isBeginTextObject(pdfTokens[0])
        && PdfUtil.isPdfFloat(pdfTokens[9])
        && PdfUtil.isShowText(pdfTokens[12])
        && PdfUtil.isPdfString(pdfTokens[11])
        && PdfUtil.isEndTextObject(pdfTokens[17])
        && PdfUtil.isBeginTextObject(pdfTokens[20])
        && PdfUtil.isPdfFloat(pdfTokens[29])
        && PdfUtil.isShowText(pdfTokens[32])
        && PdfUtil.isPdfString(pdfTokens[31])
        && PdfUtil.isEndTextObject(pdfTokens[35])
        && PdfUtil.isBeginTextObject(pdfTokens[38])
        && PdfUtil.isPdfFloat(pdfTokens[47])
        && PdfUtil.isShowText(pdfTokens[50])
        && PdfUtil.isPdfString(pdfTokens[49])
        && PdfUtil.getPdfString(pdfTokens[49]).equals("Downloaded from ");
        logger.debug3("OtherPagesEndTextObjectProcessor candidate match: " + Boolean.toString(ret));
        return ret;
      }

      public void modify(Object[] pdfTokens, List outputList) {
        for (int tok = 0 ; tok < LENGTH ; ++tok) {
          switch (tok) {
            case 9:  outputList.add(new COSFloat(300.0f)); break;
            case 11: outputList.add(new COSString(" ")); break;
            case 29: outputList.add(new COSFloat(525.0f)); break;
            case 47: outputList.add(new COSFloat(600.0f)); break;
            default: outputList.add(pdfTokens[tok]); break;
          }
        }
      }

      public int subsequenceLength() {
        return LENGTH;
      }

      protected static final int LENGTH = 52;

    }

    public static class FixHyperlink implements PdfPageTransform {
      public void transform(PdfDocument pdfDocument, PdfPage pdfPage) throws IOException {
        COSArray array = (COSArray)pdfPage.getDictionary().getObjectFromPath("Annots/[0]/Rect");
        array.set(1, new COSFloat(300.0f));
        array.set(3, new COSFloat(525.0f));
      }
    }

    public static Properties getProperties() throws IOException {
      Properties properties = new Properties();
      properties.setProperty(PdfUtil.END_TEXT_OBJECT, EndTextObjectProcessor.class.getName());
      return properties;
    }

  }

  protected static class TransformMetadata implements PdfTransform {

    public void transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.removeModificationDate();

      COSArray idArray = (COSArray)pdfDocument.getTrailer().getItem(COSName.getPDFName("ID"));
      idArray.set(1, idArray.get(0));

      int begin = 0;
      int end = 0;
      String metadata = pdfDocument.getMetadataAsString();

      begin = metadata.indexOf(BEGIN_MODIFY_DATE, end) + BEGIN_MODIFY_DATE.length();
      end = metadata.indexOf(END_MODIFY_DATE, begin);
      metadata = StringUtils.overlay(metadata, "", begin, end);

      begin = metadata.indexOf(BEGIN_DOCUMENT_ID, end) + BEGIN_DOCUMENT_ID.length();
      end = metadata.indexOf(END_DOCUMENT_ID, begin);
      metadata = StringUtils.overlay(metadata, "", begin, end);

      begin = metadata.indexOf(BEGIN_INSTANCE_ID, end) + BEGIN_INSTANCE_ID.length();
      end = metadata.indexOf(END_INSTANCE_ID, begin);
      metadata = StringUtils.overlay(metadata, "", begin, end);

      pdfDocument.setMetadata(metadata);
    }

    protected static final String BEGIN_DOCUMENT_ID = "<xapMM:DocumentID>";

    protected static final String BEGIN_INSTANCE_ID = "<xapMM:InstanceID>";

    protected static final String BEGIN_MODIFY_DATE = "<xap:ModifyDate>";

    protected static final String END_DOCUMENT_ID = "</xapMM:DocumentID>";

    protected static final String END_INSTANCE_ID = "</xapMM:InstanceID>";

    protected static final String END_MODIFY_DATE = "</xap:ModifyDate>";

  }

  /**
   * <p>This class cannot be publicly instantiated.</p>
   */
  private AmericanPhysiologicalSocietyPdfTransform(PdfTransform underlyingTransform) {
    super(underlyingTransform);
  }

  public boolean identify(PdfDocument pdfDocument) throws IOException {
    return PdfPageStreamTransform.identifyPageStream(pdfDocument,
                                                     pdfDocument.getPage(0),
                                                     FirstPage.getMatcherProperties());
  }

  protected static Logger logger = Logger.getLoggerWithInitialLevel("AmericanPhysiologicalSocietyPdfTransform", Logger.LEVEL_DEBUG3);

  /**
   * <p>A singleton instance of this class.</p>
   */
  private static AmericanPhysiologicalSocietyPdfTransform singleton;

  /**
   * <p>A singleton instance of this class' underlying transform.</p>
   */
  private static CompoundPdfTransform underlyingTransform;

  /**
   * <p>Gets a singleton instance of this class.</p>
   * @return An instance of this class.
   */
  public static synchronized AmericanPhysiologicalSocietyPdfTransform makeTransform() throws IOException {
    if (singleton == null) {
      singleton = new AmericanPhysiologicalSocietyPdfTransform(makeUnderlyingTransform());
    }
    return singleton;
  }

  protected static synchronized CompoundPdfTransform makeUnderlyingTransform() throws IOException {
    if (underlyingTransform == null) {
      // First page
      CompoundPdfPageTransform firstPageTransform = new CompoundPdfPageTransform();
      firstPageTransform.add(new PdfPageStreamTransform(FirstPage.getMutatorProperties()));
      firstPageTransform.add(new FirstPage.FixHyperlink());
      // Other pages
      CompoundPdfPageTransform otherPagesTransform = new CompoundPdfPageTransform();
      otherPagesTransform.add(new PdfPageStreamTransform(OtherPages.getProperties()));
      otherPagesTransform.add(new OtherPages.FixHyperlink());
      // Overall
      underlyingTransform = new CompoundPdfTransform();
      underlyingTransform.add(new TransformFirstPage(firstPageTransform));
      underlyingTransform.add(new TransformEachPageExceptFirst(otherPagesTransform));
      underlyingTransform.add(new TransformMetadata());
    }
    return underlyingTransform;
  }

}
