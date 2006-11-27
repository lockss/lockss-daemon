/*
 * $Id: HighWirePdfFilterFactory.java,v 1.8 2006-11-27 03:27:09 thib_gc Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.util.*;
import org.pdfbox.cos.*;

public class HighWirePdfFilterFactory extends BasicPdfFilterFactory {

  public static abstract class AbstractOnePartDownloadedFromOperatorProcessor
      extends ConditionalMergeOperatorProcessor {
    
    /* Inherit documentation */
    public boolean identify(List tokens) {
      // Look back from the end
      int last = tokens.size() - 1;
      // There are at least 20 tokens to look at
      boolean ret = tokens.size() >= 20
      // Token [0] is "BT" and token [last] is "ET" (text object containing three strings)  
      && PdfUtil.matchTextObject(tokens, 0, last)
      // Token [last-17] is "Tj" and its operand is a string (date/institution)
      && PdfUtil.matchShowText(tokens, last - 17)
      // Token [last-13] is "rg" and its operands are the RGB triple for blue (color of URL)
      && PdfUtil.matchSetRgbColorNonStroking(tokens, last - 13, 0, 0, 1)
      // Token [last-8] is "Tj" and its operand is a string (URL)
      && PdfUtil.matchShowText(tokens, last - 8)
      // Token [last-1] is "Tj" and its operand is "Downloaded from "
      && PdfUtil.matchShowText(tokens, last - 1, "Downloaded from ");
      logger.debug3("AbstractOnePartDownloadedFromOperatorProcessor candidate match: " + ret);
      return ret;
    }
    
  }
  
  public static abstract class AbstractThreePartDownloadedFromOperatorProcessor
      extends ConditionalSubsequenceOperatorProcessor {
    
    /**
     * <p>The (fixed) length of the output sequence being examined.</p>
     */
    public static final int LENGTH = 54;

    /* Inherit documentation */
    public int getSubsequenceLength() {
      // Examine the last LENGTH tokens in the output sequence
      return LENGTH;
    }

    /* Inherit documentation */
    public boolean identify(List tokens) {
      // Look back from the end
      int last = tokens.size() - 1;
      // The output list is 54 tokens long
      boolean ret = tokens.size() == LENGTH
      // Token [2] or [0] is "BT" and token [last-34] is "ET" (text object containing date/institution)
      && (PdfUtil.matchTextObject(tokens, 2, last - 34)
          || PdfUtil.matchTextObject(tokens, 0, last - 34))
      // Token [last-39] is "Tj" and its operand is a string (date/institution)
      && PdfUtil.matchShowText(tokens, last - 39)
      // Token [last-35] is "rg" and its operands are the RGB triple for blue (color of URL)
      && PdfUtil.matchSetRgbColorNonStroking(tokens, last - 35, 0, 0, 1)
      // Token [last-31] is "BT" and token [last-16] is "ET" (text object containing URL)
      && PdfUtil.matchTextObject(tokens, last - 31, last - 16)
      // Token [last-19] is "Tj" and its operand is a string (URL)
      && PdfUtil.matchShowText(tokens, last - 19)
      // Token [last-13] is "BT" and token [last] is "ET" (text object containing "Downloaded from ")
      && PdfUtil.matchTextObject(tokens, last - 13, last)
      // Token [last-1] is "Tj" and its operand is "Downloaded from "
      && PdfUtil.matchShowText(tokens, last - 1, "Downloaded from ");
      logger.debug3("AbstractThreePartDownloadedFromOperatorProcessor candidate match: " + ret);
      return ret;
    }
    
  }
  
  public static class CollapseDownloadedFrom extends AggregatePageTransform {
    
    public CollapseDownloadedFrom() throws IOException {
      super(new AggregatePageTransform(PdfUtil.OR,
                                       new CollapseOnePartDownloadedFrom(),
                                       new CollapseThreePartDownloadedFrom()),
            new NormalizeDownloadedFromHyperlink());
    }
    
  }
  
  public static class CollapseOnePartDownloadedFrom extends PageStreamTransform {
    
    public static class CollapseOnePartDownloadedFromOperatorProcessor
        extends AbstractOnePartDownloadedFromOperatorProcessor {
      
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }
      
    }
    
    public CollapseOnePartDownloadedFrom() throws IOException {
      super(// "BT" operator: split unconditionally
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            // "ET" operator: merge conditionally using CollapseOnePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, CollapseOnePartDownloadedFromOperatorProcessor.class);
    }
    
  }
  
  public static class CollapseThreePartDownloadedFrom extends PageStreamTransform {

    public static class CollapseThreePartDownloadedFromOperatorProcessor
        extends AbstractThreePartDownloadedFromOperatorProcessor {

      public List getReplacement(List tokens) {
        // Find first "BT"
        int bt = PdfUtil.isBeginTextObject(tokens, 2) ? 2 : 0;
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(bt),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

    }

    public CollapseThreePartDownloadedFrom() throws IOException {
      super(// "ET" operator: inspect subsequences ending in "ET" using CollapseThreePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, CollapseThreePartDownloadedFromOperatorProcessor.class);
    }

  }
  
  public static class EraseMetadataSection implements DocumentTransform {
    
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.setMetadata(" ");
      return true;
    }
    
  }
  
  public static class NormalizeDownloadedFrom extends AggregatePageTransform {
    
    public NormalizeDownloadedFrom() throws IOException {
      super(PdfUtil.OR,
            new NormalizeOnePartDownloadedFrom(),
            new NormalizeThreePartDownloadedFrom());
    }
    
  }

  public static class NormalizeDownloadedFromHyperlink implements PageTransform {

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      if (pdfPage.getNumberOfAnnotations() > 0) {
        COSArray array = pdfPage.getAnnotation(pdfPage.getNumberOfAnnotations()-1).getRectangle().getCOSArray();
        array.set(1, new COSFloat(1.0f)); // 1.0f is arbitrary
        array.set(3, new COSFloat(2.0f)); // 2.0f is arbitrary
        return true; // success
      }
      return false; // all other cases are unexpected
    }

  }
  
  public static class NormalizeMetadata extends AggregateDocumentTransform {

    public NormalizeMetadata() {
      super(// Remove the modification date
            new RemoveModificationDate(),
            // Remove the text in the metadat section
            new EraseMetadataSection(),
            // Remove the variable part of the document ID
            new NormalizeTrailerId());
    }
  
  }
  
  public static class NormalizeOnePartDownloadedFrom extends PageStreamTransform {
    
    public static class NormalizeOnePartDownloadedFromOperatorProcessor
        extends AbstractOnePartDownloadedFromOperatorProcessor {
      
      public List getReplacement(List tokens) {
        // Look back from the end
        int last = tokens.size() - 1;
        // Only replace variable string in token [last-18]
        List list = new ArrayList(tokens);
        list.set(last - 18, new COSString(" "));
        return list;
      }
      
    }
    
    public NormalizeOnePartDownloadedFrom() throws IOException {
      super(// "BT" operator: split unconditionally
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            // "ET" operator: merge conditionally using NormalizeOnePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, NormalizeOnePartDownloadedFromOperatorProcessor.class);
    }
    
  }
  
  public static class NormalizeThreePartDownloadedFrom extends PageStreamTransform {

    public static class NormalizeThreePartDownloadedFromOperatorProcessor
        extends AbstractThreePartDownloadedFromOperatorProcessor {

      public List getReplacement(List tokens) {
        // Look back from the end
        int last = tokens.size() - 1;
        // Only replace variable string in token [last-40]
        List list = new ArrayList(tokens);
        list.set(last - 40, new COSString(" "));
        return list;
      }

    }

    public NormalizeThreePartDownloadedFrom() throws IOException {
      super(// "ET" operator: inspect subsequences ending in "ET" using NormalizeThreePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, NormalizeThreePartDownloadedFromOperatorProcessor.class);
    }

  }
  
  public static class NormalizeTrailerId implements DocumentTransform {
    
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      COSDictionary trailer = pdfDocument.getTrailer();
      if (trailer != null) {
        COSBase idObj = trailer.getItem(COSName.getPDFName("ID"));
        if (idObj != null && idObj instanceof COSArray) {
          COSArray idArray = (COSArray)idObj;
          if (idArray.size() == 2) {
            idArray.set(1, idArray.get(0));
            return true; // success
          }
        }
      }
      return false; // all other cases are unexpected
    }
    
  }
  
  public static class RemoveModificationDate implements DocumentTransform {
    
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.removeModificationDate();
      return true;
    }
    
  }
  
  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("HighWirePdfFilterFactory");

}
