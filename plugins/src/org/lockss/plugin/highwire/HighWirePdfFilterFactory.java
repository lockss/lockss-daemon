/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.PageTransformUtil.ExtractStringsToOutputStream;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.PDRectangle;
import org.pdfbox.pdmodel.interactive.action.type.*;
import org.pdfbox.pdmodel.interactive.annotation.*;
import org.pdfbox.util.operator.OperatorProcessor;

/**
 * <p>
 * Documenting how this class works and how it relates to
 * {@link HighWireNewPdfFilterFactory}, as part of phasing out PDFBox 0.7.3.
 * </p>
 * <p>
 * The four H10 plugins (3 GLN + 1 CLOCKSS) use this filter factory as their PDF
 * filter factory.
 * </p>
 * <p>
 * Using {@link PdfUtil#getOutputDocumentTransform(ArchivalUnit)}, this filter
 * factory looks up the {@code hint_application/pdf_filter_factory} attribute of
 * the AU, and if it's present, instantiates an instance of the
 * {@link OutputDocumentTransform} class it fully names, and uses (through
 * {@link PdfUtil#applyFromInputStream(OutputDocumentTransform, InputStream)})
 * to filter the input stream.
 * </p>
 * <p>
 * The seven {@link OutputDocumentTransform} classes are:
 * {@link AmericanAcademyOfPediatricsPdfTransform},
 * {@link AmericanMedicalAssociationPdfTransform},
 * {@link AmericanPhysiologicalSocietyPdfTransform},
 * {@link AmericanSocietyForNutritionPdfTransform},
 * {@link BritishMedicalJournalPublishingGroupPdfTransform},
 * {@link RockefellerUniversityPressPdfTransform},
 * {@link SagePublicationsPdfTransform}. Originally, there was an eighth one,
 * {@code NewEnglandJournalOfMedicinePdfTransform}, but it was not listed in
 * any TDB file and has been deleted.
 * </p>
 * <p>
 * This class defines the following building blocks.
 * </p>
 * <p>
 * {@link ResilientTextScrapingDocumentTransform}: a document transform that, if
 * {@link ResilientTextScrapingDocumentTransform#makePreliminaryTransform()}
 * (abstract) is "true", extracts all strings (with
 * {@link ExtractStringsToOutputStream}). That sounds like
 * {@link TextScrapingDocumentTransform}, but a comment says "Bypasses
 * {@link TextScrapingDocumentTransform}, which fails on zero-length results."
 * </p>
 * <p>
 * {@link AbstractOnePartDownloadedFromOperatorProcessor}: if, starting from the
 * end, the sequence of tokens matches ET, then Tj with the string "Downloaded
 * from ", then Tj with a string matching a URL, then Tj with a string, then BT,
 * then store a positive result.
 * </p>
 * <p>
 * {@link CollapseOnePartDownloadedFromOperatorProcessor} is a page transform
 * that, if {@link AbstractOnePartDownloadedFromOperatorProcessor} has a
 * positive result, reduces the whole BT-ET sequence to just BT and ET.
 * </p>
 * <p>
 * {@link AbstractThreePartDownloadedFromOperatorProcessor}: similar to
 * {@link AbstractOnePartDownloadedFromOperatorProcessor}, but instead of the
 * three Tj being in a single BT-ET, each is in its own BT-ET.
 * </p>
 * <p>
 * {@link CollapseThreePartDownloadedFromOperatorProcessor}, similarly to
 * {@link CollapseOnePartDownloadedFromOperatorProcessor}, is a page transform
 * that, if {@link AbstractThreePartDownloadedFromOperatorProcessor} has a
 * positive result, reduces the whole sequence from the first BT to the last ET
 * to just BT and ET.
 * </p>
 * <p>
 * {@link CollapseDownloadedFrom} is a page transform that expects either
 * {@link CollapseOnePartDownloadedFrom} or
 * {@link CollapseThreePartDownloadedFrom} to succeed.
 * </p>
 * <p>
 * {@link CollapseDownloadedFromAndNormalizeHyperlinks} is a page transform that
 * expects both {@link CollapseDownloadedFrom} and {@link NormalizeHyperlinks}
 * to succeed.
 * </p>
 * <p>
 * {@link RemoveModificationDate} is a document transform that unsets the
 * modification date.
 * </p>
 * <p>
 * {@link EraseMetadataSection} is a document transform that sets the Metadata
 * field to {@code " "}.
 * </p>
 * <p>
 * {@link NormalizeTrailerId} is a document transform that sets the document ID
 * to an arbitrary, fixed identifier.
 * </p>
 * <p>
 * {@link NormalizeMetadata} is a document transofrm that does all three of
 * {@link RemoveModificationDate}, {@link EraseMetadataSection} and
 * {@link NormalizeTrailerId}.
 * </p>
 * <p>
 * {@link NormalizeHyperlinks} is a page transform that enumerates all
 * annotations of type link with a URI action and repositions them to an
 * arbitrary, fixed (x,y) position.
 * </p>
 * <p>
 * See each of the seven output document transforms to learn what each does
 * using these building blocks.
 * 
 * @see HighWireNewPdfFilterFactory
 * @see AmericanAcademyOfPediatricsPdfTransform
 * @see AmericanMedicalAssociationPdfTransform
 * @see AmericanPhysiologicalSocietyPdfTransform
 * @see AmericanSocietyForNutritionPdfTransform
 * @see BritishMedicalJournalPublishingGroupPdfTransform
 * @see RockefellerUniversityPressPdfTransform
 * @see SagePublicationsPdfTransform
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class HighWirePdfFilterFactory implements FilterFactory {

  /**
   * <p>Bypasses {@link TextScrapingDocumentTransform}, which fails on
   * zero-length results. To be fixed in the PDF framework later.</p>
   * @see TextScrapingDocumentTransform
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static abstract class ResilientTextScrapingDocumentTransform extends OutputStreamDocumentTransform {
    @Deprecated public abstract DocumentTransform makePreliminaryTransform() throws IOException;
    @Deprecated public DocumentTransform makeTransform() throws IOException {
      return new ConditionalDocumentTransform(makePreliminaryTransform(),
                                              false, // Difference with TextScrapingDocumentTransform
                                              new TransformEachPage(new ExtractStringsToOutputStream(outputStream) {
                                                @Override
                                                public void processStream(PDPage arg0, PDResources arg1, COSStream arg2) throws IOException {
                                                  logger.debug3("ResilientTextScrapingDocumentTransform: unconditional signalChange()");
                                                  signalChange(); // Difference with TextScrapingDocumentTransform
                                                  super.processStream(arg0, arg1, arg2);
                                                }
                                              }));
    }
  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static abstract class AbstractOnePartDownloadedFromOperatorProcessor
      extends ConditionalMergeOperatorProcessor {

    /* Inherit documentation */
    @Deprecated
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

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static abstract class AbstractThreePartDownloadedFromOperatorProcessor
      extends ConditionalSubsequenceOperatorProcessor {

    /* Inherit documentation */
    @Deprecated
    public int getSubsequenceLength() {
      // Examine the last 100 tokens in the output sequence
      return 100;
    }

    /* Inherit documentation */
    @Deprecated
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

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class CollapseDownloadedFrom extends AggregatePageTransform {

    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public CollapseDownloadedFrom(ArchivalUnit au) throws IOException {
      super(PdfUtil.OR,
            new CollapseOnePartDownloadedFrom(au),
            new CollapseThreePartDownloadedFrom(au));
    }

  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class CollapseDownloadedFromAndNormalizeHyperlinks
      extends AggregatePageTransform {

    @Deprecated
    public CollapseDownloadedFromAndNormalizeHyperlinks(ArchivalUnit au) throws IOException {
      super(new CollapseDownloadedFrom(au),
            new NormalizeHyperlinks());
    }

  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class CollapseOnePartDownloadedFrom extends PageStreamTransform {

    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public static class CollapseOnePartDownloadedFromOperatorProcessor
        extends AbstractOnePartDownloadedFromOperatorProcessor {

      @Deprecated
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

    }

    @Deprecated
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

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class CollapseThreePartDownloadedFrom extends PageStreamTransform {

    @Deprecated
    public static class CollapseThreePartDownloadedFromOperatorProcessor
        extends AbstractThreePartDownloadedFromOperatorProcessor {

      @Deprecated
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

    @Deprecated
    public CollapseThreePartDownloadedFrom(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              @Deprecated public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            // "ET" operator: inspect subsequences ending in "ET" using CollapseThreePartDownloadedFromOperatorProcessor
            PdfUtil.END_TEXT_OBJECT, CollapseThreePartDownloadedFromOperatorProcessor.class);
    }

  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class EraseMetadataSection implements DocumentTransform {

    @Deprecated
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.setMetadata(" ");
      return true;
    }

  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class NormalizeHyperlinks implements PageTransform {

    @Deprecated
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

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class NormalizeMetadata extends AggregateDocumentTransform {

    @Deprecated
    public NormalizeMetadata() {
      super(// Remove the modification date
            new RemoveModificationDate(),
            // Remove the text in the metadata section
            new EraseMetadataSection(),
            // Remove the variable part of the document ID
            new NormalizeTrailerId());
    }

  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class NormalizeTrailerId implements DocumentTransform {

    @Deprecated
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      COSDictionary trailer = pdfDocument.getTrailer();
      if (trailer != null) {
        // Put bogus ID to prevent autogenerated (variable) ID
        COSArray id = new COSArray();
        id.add(new COSString("12345678901234567890123456789012"));
        id.add(id.get(0));
        trailer.setItem(COSName.getPDFName("ID"), id);
        return true; // success
      }
      return false; // all other cases are unexpected
    }

  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class RemoveModificationDate implements DocumentTransform {

    @Deprecated
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      pdfDocument.removeModificationDate();
      return true;
    }

  }

  @Deprecated
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
  @Deprecated
  private static Logger logger = Logger.getLogger(HighWirePdfFilterFactory.class);

}
