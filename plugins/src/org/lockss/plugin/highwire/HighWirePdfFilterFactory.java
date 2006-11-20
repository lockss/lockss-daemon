/*
 * $Id: HighWirePdfFilterFactory.java,v 1.5 2006-11-20 22:37:28 thib_gc Exp $
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
      // The output list is 30 tokens long
      boolean ret = tokens.size() == 30
      // Token [0] is "BT" and token [29] is "ET" (text object containing three strings)
      && PdfUtil.matchTextObject(tokens, 0, 29)
      // Token [12] is "Tj" and its operand is a string (date/institution)
      && PdfUtil.matchShowText(tokens, 12)
      // Token [16] is "rg" and its operands are the RGB triple for blue (color of URL)
      && PdfUtil.matchSetRgbColorNonStroking(tokens, 16, 0, 0, 1)
      // Token [21] is "Tj" and its operand is a string (URL)
      && PdfUtil.matchShowText(tokens, 21)
      // Token [28] is "Tj" and its operand is "Downloaded from "
      && PdfUtil.matchShowText(tokens, 28, "Downloaded from ");
      logger.debug3("AbstractOnePartDownloadedFromOperatorProcessor candidate match: " + ret);
      return ret;
    }
    
  }
  
  public static abstract class AbstractThreePartDownloadedFromOperatorProcessor
      extends ConditionalSubsequenceOperatorProcessor {
    
    /* Inherit documentation */
    public int getSubsequenceLength() {
      // Examine the last 52 tokens in the output sequence
      return 52;
    }

    /* Inherit documentation */
    public boolean identify(List tokens) {
      // The output list is 52 tokens long
      boolean ret = tokens.size() == 52
      // Token [0] is "BT" and token [17] is "ET" (text object containing date/institution)
      && PdfUtil.matchTextObject(tokens, 0, 17)
      // Token [12] is "Tj" and its operand is a string (date/institution)
      && PdfUtil.matchShowText(tokens, 12)
      // Token [16] is "rg" and its operands are the RGB triple for blue (color of URL)
      && PdfUtil.matchSetRgbColorNonStroking(tokens, 16, 0, 0, 1)
      // Token [20] is "BT" and token [35] is "ET" (text object containing URL)
      && PdfUtil.matchTextObject(tokens, 20, 35)
      // Token [32] is "Tj" and its operand is a string (URL)
      && PdfUtil.matchShowText(tokens, 32)
      // Token [38] is "BT" and token [51] is "ET" (text object containing "Downloaded from ")
      && PdfUtil.matchTextObject(tokens, 38, 51)
      // Token [50] is "Tj" and its operand is "Downloaded from "
      && PdfUtil.matchShowText(tokens, 50, "Downloaded from ");
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
                             tokens.get(29));
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
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(51));
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
        // Only replace variable string in token [11]
        List list = new ArrayList(tokens);
        list.set(11, new COSString(" "));
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
        // Only replace variable string in token [11]
        List list = new ArrayList(tokens);
        list.set(11, new COSString(" "));
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
