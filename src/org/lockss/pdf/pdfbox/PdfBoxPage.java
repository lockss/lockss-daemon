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

package org.lockss.pdf.pdfbox;

import java.io.*;
import java.util.*;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.xobject.*;
import org.lockss.pdf.*;
import org.lockss.pdf.pdfbox.PdfBoxTokens.*;
import org.lockss.util.Logger;

/**
 * <p>
 * A {@link PdfPage} implementation based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDPage} class.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxPage implements PdfPage {

  /**
   * <p>
   * Logger for use by this class.
   * </p>
   * 
   * @since 1.56
   */
  private static final Logger log = Logger.getLogger(PdfBoxPage.class);
  
  /**
   * <p>
   * Returns an XObject from the given resources, as a {@link PDXObject}
   * instance.
   * </p>
   * 
   * @param pdResources
   *          A {@link PDResources} instance.
   * @param name
   *          The name of the desired XObject.
   * @return The requested XObject, or <code>null</code> if not found.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.61
   */
  protected static PDXObject getPDXObjectByName(PDResources pdResources,
                                                String name)
      throws PdfException {
    /*
     * IMPLEMENTATION NOTE
     * 
     * This map contains objects of type PDXObject (PDFBox 1.6.0:
     * see PDResources lines 157 and 160), which are null, or of
     * type either PDXObjectForm (see PDXObject line 162) or PDJpeg
     * (line 140) or PDCcitt (line 144) or PDPixelMap (line 153).
     * The latter three have a common supertype, PDXObjectImage.
     */
    return (PDXObject)(pdResources.getXObjects().get(name));
  }

  /**
   * <p>
   * Determines if the given {@link PDXObject} instance is a byte
   * stream (as opposed to a token stream).
   * </p>
   * 
   * @param xObject A {@link PDXObject} instance.
   * @return <code>true</code> if and only if the argument is a byte
   *         stream.
   * @since 1.56.3
   * @see #isTokenStream(PDXObject)
   */
  private static boolean isByteStream(PDXObject xObject) {
    return !isTokenStream(xObject);
  }
  
  /**
   * <p>
   * Determines if the given {@link PDXObject} instance is a token
   * stream (as opposed to a byte stream).
   * </p>
   * 
   * @param xObject A {@link PDXObject} instance.
   * @return <code>true</code> if and only if the argument is a token
   *         stream.
   * @since 1.56.3
   */
  private static boolean isTokenStream(PDXObject xObject) {
    /*
     * IMPLEMENTATION NOTE
     * 
     * PDXObject is an abstract class whose subtypes all represent
     * image XObjects (PDJpeg, PDCcitt or PDPixelMap) i.e. a byte
     * stream, except one, PDXObjectForm, which represents a form
     * XObject i.e. a token stream.
     */
    return xObject instanceof PDXObjectForm;
  }

  /**
   * <p>
   * The parent {@link PdfBoxDocument} instance.
   * </p>
   * 
   * @since 1.56
   */
  protected final PdfBoxDocument pdfBoxDocument;

  /**
   * <p>
   * The {@link PDPage) instance this instance represents.
   * </p>
   * 
   * @since 1.56
   */
  protected final PDPage pdPage;

  /**
   * <p>
   * Constructor.
   * </p>
   * 
   * @param pdfBoxDocument The parent {@link PdfBoxDocument} instance.
   * @param pdPage The {@link PDPage} instance underpinning this PDF
   *          page.
   * @since 1.56
   */
  public PdfBoxPage(PdfBoxDocument pdfBoxDocument,
                    PDPage pdPage) {
    this.pdfBoxDocument = pdfBoxDocument;
    this.pdPage = pdPage;
  }

  @Override
  public List<InputStream> getAllByteStreams() throws PdfException {
    List<InputStream> ret = new ArrayList<InputStream>();
    PdfTokenStream pageTokenStream = getPageTokenStream();
    // Use findResources(), not getResources() (inspired by getAllTokenStreams() below)
    recursivelyFindByteStreams(pageTokenStream, pdPage.findResources(), ret);
    return ret;
  }
  
  @Override
  public List<PdfTokenStream> getAllTokenStreams() throws PdfException {
    List<PdfTokenStream> ret = new ArrayList<PdfTokenStream>();
    PdfTokenStream pageTokenStream = getPageTokenStream();
    ret.add(pageTokenStream);
    // Use findResources(), not getResources() (e.g. PDFBox 1.8.7 PDFTextStripper.java line 460)
    recursivelyFindTokenStreams(pageTokenStream, pdPage.findResources(), ret);
    return ret;
  }

  @Override
  public List<PdfToken> getAnnotations() {
    /*
     * IMPLEMENTATION NOTE
     * 
     * Annotations are just dictionaries, but because there are many
     * types, the PDFBox API defines a vast hierarchy of objects to
     * represent them. At this time, this is way too much detail for
     * this API, because only one type of annotation has a foreseeable
     * use case (the Link type). So for now, we are only representing
     * annotations as the dictionaries they are by circumventing the
     * PDAnnotation factory call in getAnnotations() (PDFBox 1.6.0:
     * PDPage line 780).
     */
    COSDictionary pageDictionary = pdPage.getCOSDictionary();
    COSArray annots = (COSArray)pageDictionary.getDictionaryObject(COSName.ANNOTS);
    if (annots == null) {
      return new ArrayList<PdfToken>();
    }
    return PdfBoxTokens.convertOne(annots).getArray();
  }

  @Override
  public PdfBoxDocument getDocument() {
    return pdfBoxDocument;
  }
  
  @Override
  public PdfTokenStream getPageTokenStream() throws PdfException {
    try {
      return getDocument().getDocumentFactory().makePageTokenStream(this, pdPage.getContents());
    }
    catch (IOException ioe) {
      throw new PdfException("Failed to get the page content stream", ioe);
    }
  }
  
  @Override
  public void setAnnotations(List<PdfToken> annotations) {
    pdPage.getCOSDictionary().setItem(COSName.ANNOTS, (COSArray)Arr.of(annotations).toPdfBoxObject());
  }

  /**
   * <p>
   * Traverses the given token stream (and associated resources object), adding
   * any embedded image data to the given list, and recursively traversing any
   * referenced XObject streams.
   * </p>
   * 
   * @param pdfTokenStream
   *          A token stream.
   * @param pdResources
   *          The token stream's resources object,
   * @param ret
   *          A list into which byte streams are appended.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.61
   */
  private void recursivelyFindByteStreams(PdfTokenStream pdfTokenStream,
                                          final PDResources pdResources,
                                          final List<InputStream> ret)
        throws PdfException {
    PdfTokenStreamWorker worker = new PdfTokenStreamWorker() {
      @Override public void operatorCallback() throws PdfException {
        // 'ID' and 'BI'
        if (isBeginImageData() || isBeginImageObject()) {
          /*
           * IMPLEMENTATION NOTE
           * 
           * getImageData() (PDFBox 1.6.0: PDFOperator line 105) does
           * not copy the byte array, nor does ByteArrayInputStream
           * (http://docs.oracle.com/javase/1.5.0/docs/api/java/io/ByteArrayInputStream.html#ByteArrayInputStream%28byte[]%29).
           */
          ret.add(new ByteArrayInputStream(((Op)getOperator()).getImageData()));
        }
        // 'Do'
        else if (isInvokeXObject()) {
          PdfToken operand = getTokens().get(getIndex() - 1);
          if (operand.isName()) {
            PDXObject xObject = getPDXObjectByName(pdResources, operand.getName());
            if (isByteStream(xObject)) {
              try {
                ret.add(xObject.getCOSStream().getFilteredStream()); // unlike in LOCKSS, filtered in PDFBox means unmodified
              }
              catch (IOException ioe) {
                log.debug2("recursivelyFindByteStreams: Error retrieving a byte stream", ioe);
              }
            }
            else {
              PDXObjectForm pdxObjectForm = (PDXObjectForm)xObject;
              PdfBoxXObjectTokenStream referencedTokenStream =
                  getDocument().getDocumentFactory().makeXObjectTokenStream(PdfBoxPage.this,
                                                                            Arrays.asList(pdxObjectForm,
                                                                                          pdResources,
                                                                                          pdxObjectForm.getResources()));
              recursivelyFindByteStreams(referencedTokenStream,
                                         referencedTokenStream.getStreamResources(), // pdxObjectForm.getResources() or pdResources if null
                                         ret);
            }
          }
          else {
            log.debug2("recursivelyFindByteStreams: invalid input");
          }
        }
      }
    };
    worker.process(pdfTokenStream);
  }
  
  /**
   * <p>
   * Traverses the given token stream (and associated resources object), adding
   * any embedded XObject streams to the given list, and recursively traversing
   * them. Note that the given stream itself is not appended to the list.
   * </p>
   * 
   * @param pdfTokenStream
   *          A token stream.
   * @param pdResources
   *          The token stream's resources object,
   * @param ret
   *          A list into which byte streams are appended.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.61
   */
  private void recursivelyFindTokenStreams(PdfTokenStream pdfTokenStream,
                                           final PDResources pdResources,
                                           final List<PdfTokenStream> ret)
      throws PdfException {
    PdfTokenStreamWorker worker = new PdfTokenStreamWorker() {
      @Override public void operatorCallback() throws PdfException {
        if (isInvokeXObject()) {
          PdfToken operand = getTokens().get(getIndex() - 1);
          if (operand.isName()) {
            PDXObject xObject = getPDXObjectByName(pdResources, operand.getName());
            if (isTokenStream(xObject)) {
              PDXObjectForm pdxObjectForm = (PDXObjectForm)xObject;
              PdfBoxXObjectTokenStream referencedTokenStream =
                  getDocument().getDocumentFactory().makeXObjectTokenStream(PdfBoxPage.this,
                                                                            Arrays.asList(pdxObjectForm,
                                                                                          pdResources,
                                                                                          pdxObjectForm.getResources()));
              ret.add(referencedTokenStream);
              recursivelyFindTokenStreams(referencedTokenStream,
                                          referencedTokenStream.getStreamResources(), // pdxObjectForm.getResources() or pdResources if null
                                          ret);
            }
          }
          else {
            log.debug2("recursivelyFindTokenStreams: invalid input");
          }
        }
      }
    };
    worker.process(pdfTokenStream);
  }
  
}
