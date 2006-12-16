/*
 * $Id: HighWirePdfFilterFactory.java,v 1.10 2006-12-16 00:37:44 thib_gc Exp $
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
            // Tj and its argument is the string "Downloaded from "
            if (PdfUtil.matchShowText(tokens, tok, "Downloaded from ")) { ++progress; }
            break;
          case 2:
            // Not BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
            // Tj and its argument is a domain name string
            if (PdfUtil.matchShowTextMatches(tokens, tok, "[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+")) { ++progress; }
            break;
          case 3:
            // Not BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
            // Tj and its string argument
            if (PdfUtil.matchShowText(tokens, tok)) { ++progress; }
            break;
          case 4:
            // BT; beginning of subsequence
            if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
            break;
        }
      }
      logger.debug3("AbstractOnePartDownloadedFromOperatorProcessor candidate match: " + ret);
      return ret;
    }
      
  }
  
  public static abstract class AbstractThreePartDownloadedFromOperatorProcessor
      extends ConditionalSubsequenceOperatorProcessor {
    
    /* Inherit documentation */
    public int getSubsequenceLength() {
      // Examine the last 54 tokens in the output sequence
      return 54;
    }

    /* Inherit documentation */
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
            // Tj and its argument is the string "Downloaded from "
            if (PdfUtil.matchShowText(tokens, tok, "Downloaded from ")) { ++progress; }
            break;
          case 2:
            // BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { ++progress; }
            break;
          case 3:
            // ET
            if (PdfUtil.isEndTextObject(tokens,tok)) { ++progress; }
            break;
          case 4:
            // Not BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
            // Tj and its argument is a domain name string
            if (PdfUtil.matchShowTextMatches(tokens, tok, "[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+")) { ++progress; }
            break;
          case 5:
            // BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { ++progress; }
            break;
          case 6:
            // ET
            if (PdfUtil.isEndTextObject(tokens,tok)) { ++progress; }
            break;
          case 7:
            // Not BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
            // Tj and its string argument
            if (PdfUtil.matchShowText(tokens, tok)) { ++progress; }
            break;
          case 8:
            // BT
            if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = true; break iteration; }
            break;
        }
      }
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
