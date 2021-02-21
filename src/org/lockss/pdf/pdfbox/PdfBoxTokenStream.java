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
import java.lang.ref.WeakReference;
import java.util.*;

import org.apache.pdfbox.contentstream.*;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.lockss.config.CurrentConfig;
import org.lockss.pdf.*;
import org.lockss.util.FileBackedList;

/**
 * <p>
 * A {@link PdfTokenStream} implementation based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDStream} class, but
 * the origin of the wrapped instance comes from {@link #getPdStream()}
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public abstract class PdfBoxTokenStream implements PdfTokenStream {

  /**
   * <p>
   * The parent {@link PdfBoxPage} instance.
   * </p>
   * 
   * @since 1.56
   */
  protected PdfBoxPage pdfBoxPage;
  
  /**
   * <p>
   * Constructor.
   * </p>
   * 
   * @param pdfBoxPage The parent {@link PdfBoxPage} instance.
   * @since 1.56
   */
  public PdfBoxTokenStream(PdfBoxPage pdfBoxPage) {
    this.pdfBoxPage = pdfBoxPage;
  }
  
  @Override // Just for covariant default method return type
  public Iterable<PdfBoxOperandsAndOperator> getOperandsAndOperatorIterable() throws PdfException {
    // Just for covariant default method return type
    return (Iterable<PdfBoxOperandsAndOperator>)PdfTokenStream.super.getOperandsAndOperatorIterable();
  }
  
  @Override // Just for covariant default method return type
  public List<PdfBoxOperandsAndOperator> getOperandsAndOperatorList() throws PdfException {
    // Just for covariant default method return type
    return (List<PdfBoxOperandsAndOperator>)PdfTokenStream.super.getOperandsAndOperatorList();
  }
  
  @Override
  public PdfBoxPage getPage() {
    return pdfBoxPage;
  }

  @Override // Just for covariant default method return type
  public Iterable<PdfBoxToken> getTokenIterable() throws PdfException {
    return (Iterable<PdfBoxToken>)PdfTokenStream.super.getTokenIterable();
  }
  
  @Override // Just for covariant default method return type
  public Iterator<PdfBoxToken> getTokenIterator() throws PdfException {
    return (Iterator<PdfBoxToken>)PdfTokenStream.super.getTokenIterator();
  }
  
  /**
   *
   */
  @Override
  public List<PdfToken> getTokens() throws PdfException {
    return (List<PdfToken>)PdfTokenStream.super.getTokenList();
  }

  @Override
  public List<PdfBoxToken> getTokenList() throws PdfException {
    List<PdfBoxToken> tokens = new ArrayList<>();
    Iterator<PdfBoxToken> tokenIter = getTokenIterator();
    
    int fileBackedThreshold =
        (CurrentConfig.getBooleanParam(PARAM_ENABLE_FILE_BACKED_LISTS,
                                       DEFAULT_ENABLE_FILE_BACKED_LISTS))
        ? CurrentConfig.getIntParam(PARAM_FILE_BACKED_LISTS_THRESHOLD,
                                    DEFAULT_FILE_BACKED_LISTS_THRESHOLD)
        : Integer.MAX_VALUE;
    
    while (tokenIter.hasNext()) {
      if (tokens.size() == fileBackedThreshold) {
        // List becoming too large for main memory
        FileBackedList<PdfBoxToken> newList;
        try {
          newList = new FileBackedList<>(tokens);
        }
        catch (IOException ioe) {
          throw new PdfException(ioe);
        }
        // Clean up old list
        tokens.clear();
        ((ArrayList<PdfBoxToken>)tokens).trimToSize();
        // Put new list in cleanup queue
        getPage().getDocument().autoCloseables.add(new WeakReference<AutoCloseable>(newList));
        // Start using this new list
        tokens = newList;
      }
      tokens.add(tokenIter.next());
    }
    
    return tokens;
  }

  @Override
  public void setTokens(Iterator<? extends PdfToken> tokenIterator) throws PdfException {
    Iterator<PdfBoxToken> pdfBoxIter = (Iterator<PdfBoxToken>)tokenIterator;
    
  }
  
  /**
   * 
   * @return
   * @since 1.76
   */
  protected abstract PDContentStream getPdContentStream();
  
  /**
   * <p>
   * Retrieves a {@link PDResources} instance suitable for the context of this
   * token stream.
   * </p>
   * 
   * @return The {@link PDResources} instance for this token stream.
   * @since 1.64
   */
  protected abstract PDResources getStreamResources();
  
  /**
   * 
   * @param tokenIterator
   * @return
   * @throws PdfException
   * @since 1.76
   */
  protected PDStream makePdStreamFromTokens(Iterator<? extends PdfToken> tokenIterator) throws PdfException {
    Iterator<PdfBoxToken> iter = (Iterator<PdfBoxToken>)tokenIterator;
    PDStream newPdStream = new PDStream(pdfBoxPage.getDocument().getPdDocument());
    try {
      ContentStreamWriter tokenWriter = new ContentStreamWriter(newPdStream.createOutputStream());
      while (iter.hasNext()) {
        PdfBoxToken pdfBoxToken = iter.next();
        if (pdfBoxToken.isOperator()) {
          tokenWriter.writeToken(((PdfBoxToken.Op)pdfBoxToken).toPdfBoxObject());
        }
        else {
          tokenWriter.writeToken((COSBase)pdfBoxToken.toPdfBoxObject());
        }
      }
    }
    catch (IOException ioe) {
      throw new PdfException("Error writing token stream from token iterator", ioe);
    }
    return newPdStream;
  }

  /**
   * Configuration prefix for PDFBox-related parameters (may be moved upstream
   * later).
   */
  public static final String PREFIX = "org.lockss.pdfbox.";
  
  /**
   * Whether to allow excessively large lists of PDFBox tokens go to disk above
   * PARAM_FILE_BACKED_LISTS_THRESHOLD items.
   */
  public static final String PARAM_ENABLE_FILE_BACKED_LISTS = PREFIX + "enableFileBackedLists";
  
  public static final boolean DEFAULT_ENABLE_FILE_BACKED_LISTS = true;
  
  /**
   * When PARAM_ENABLE_FILE_BACKED_LISTS is true, number of items that triggers
   * the allocation of a file-backed list.
   */
  public static final String PARAM_FILE_BACKED_LISTS_THRESHOLD = PREFIX + "fileBackedListsThreshold";
  
  public static final int DEFAULT_FILE_BACKED_LISTS_THRESHOLD = 1_000_000;
 
}
