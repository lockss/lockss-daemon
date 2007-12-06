/*
 * $Id: HighWirePdfFilterFactory.java,v 1.19 2007-12-06 23:47:45 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.common.PDRectangle;
import org.pdfbox.pdmodel.interactive.action.type.*;
import org.pdfbox.pdmodel.interactive.annotation.*;
import org.pdfbox.util.operator.OperatorProcessor;

public class HighWirePdfFilterFactory implements FilterFactory {

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
            if (PdfUtil.matchShowTextMatches(tokens, tok, "(?:http://)?[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+")) { ++progress; }
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
      if (logger.isDebug3()) {
        logger.debug3("AbstractOnePartDownloadedFromOperatorProcessor candidate match: " + ret);
      }
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
            if (PdfUtil.matchShowTextMatches(tokens, tok, "(?:http://)?[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+")) { ++progress; }
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
      if (logger.isDebug3()) {
        logger.debug3("AbstractThreePartDownloadedFromOperatorProcessor candidate match: " + ret);
      }
      return ret;
    }
  }

  public static class CollapseDownloadedFrom extends AggregatePageTransform {

    public CollapseDownloadedFrom(ArchivalUnit au) throws IOException {
      super(PdfUtil.OR,
            new CollapseOnePartDownloadedFrom(au),
            new CollapseThreePartDownloadedFrom(au));
    }

  }

  public static class CollapseDownloadedFromAndNormalizeHyperlinks
      extends AggregatePageTransform {

    public CollapseDownloadedFromAndNormalizeHyperlinks(ArchivalUnit au) throws IOException {
      super(new CollapseDownloadedFrom(au),
            new NormalizeHyperlinks());
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

    public CollapseOnePartDownloadedFrom(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            // "BT" operator: split unconditionally
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            // "ET" operator: merge conditionally using CollapseOnePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, CollapseOnePartDownloadedFromOperatorProcessor.class);
    }

  }

  public static class CollapseThreePartDownloadedFrom extends PageStreamTransform {

    public static class CollapseThreePartDownloadedFromOperatorProcessor
        extends AbstractThreePartDownloadedFromOperatorProcessor {

      public List getReplacement(List tokens) {
        // Known to have at least three "BT" tokens
        int bt = -1; int counter = 0;
        for (int tok = tokens.size() - 1 ; counter < 3 && tok >= 0 ; --tok) {
          if (PdfUtil.isBeginTextObject(tokens, tok)) {
            bt = tok; ++counter;
          }
        }

        // Replace by an empty text object, preserving earlier tokens
        List ret = new ArrayList(bt + 2);
        ret.addAll(// Tokens before the three text objects
                   tokens.subList(0, bt));
        ret.addAll(ListUtil.list(// Known to be "BT"
                                 tokens.get(bt),
                                 // Known to be "ET"
                                 tokens.get(tokens.size() - 1)));
        return ret;
      }

    }

    public CollapseThreePartDownloadedFrom(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            // "ET" operator: inspect subsequences ending in "ET" using CollapseThreePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, CollapseThreePartDownloadedFromOperatorProcessor.class);
    }

  }

  public static class EraseMetadataSection implements DocumentTransform {

    public boolean transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.setMetadata(" ");
      return true;
    }

  }

  public static class NormalizeHyperlinks implements PageTransform {

    public boolean transform(PdfPage pdfPage) throws IOException {
      boolean ret = false; // Expecting at least one

      for (Iterator iter = pdfPage.getAnnotationIterator() ; iter.hasNext() ; ) {
        PDAnnotation pdAnnotation = (PDAnnotation)iter.next();
        if (pdAnnotation instanceof PDAnnotationLink) {
          PDAnnotationLink pdAnnotationLink = (PDAnnotationLink)pdAnnotation;
          PDAction pdAction = pdAnnotationLink.getAction();
          if (pdAction instanceof PDActionURI) {
            PDRectangle rect = pdAnnotation.getRectangle();
            rect.setLowerLeftY(12.34f);  // 12.34f is arbitrary
            rect.setUpperRightY(56.78f); // 56.78f is arbitrary
            ret = true; // Found at least one
          }
        }
      }

      return ret;
    }

  }

  public static class NormalizeMetadata extends AggregateDocumentTransform {

    public NormalizeMetadata() {
      super(// Remove the modification date
            new RemoveModificationDate(),
            // Remove the text in the metadata section
            new EraseMetadataSection(),
            // Remove the variable part of the document ID
            new NormalizeTrailerId());
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
            // [1] variable; replace arbitrarily by [0] which is not
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

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    logger.debug2("PDF filter factory for: " + au.getName());
    OutputDocumentTransform documentTransform = PdfUtil.getOutputDocumentTransform(au);
    if (documentTransform == null) {
      logger.debug2("Unfiltered");
      return in;
    }
    else {
      if (documentTransform instanceof ArchivalUnitDependent) {
        logger.debug2("Filtered with " + documentTransform.getClass().getName());
        ArchivalUnitDependent aud = (ArchivalUnitDependent)documentTransform;
        aud.setArchivalUnit(au);
      }
      else {
        logger.debug2("Filtered with " + documentTransform.getClass().getName() + " but not AU-dependent");
      }
      return PdfUtil.applyFromInputStream(documentTransform, in);
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("HighWirePdfFilterFactory");

}
