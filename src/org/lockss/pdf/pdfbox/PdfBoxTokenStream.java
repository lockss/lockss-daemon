/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University
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

import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
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
  
  @Override
  public PdfBoxPage getPage() {
    return pdfBoxPage;
  }
  
  @Override
  public List<PdfToken> getTokens() throws PdfException {
    List<PdfToken> tokens = new ArrayList<PdfToken>();
    PDStream pdStream = getPdStream();
    if (pdStream == null) {
      return tokens; // Blank page with null stream
    }
    PDFStreamParser pdfStreamParser = null;
    try {
      pdfStreamParser = new PDFStreamParser(pdStream.getStream());
      int fileBackedThreshold =
	(CurrentConfig.getBooleanParam(PARAM_ENABLE_FILE_BACKED_LISTS,
				       DEFAULT_ENABLE_FILE_BACKED_LISTS))
	? CurrentConfig.getIntParam(PARAM_FILE_BACKED_LISTS_THRESHOLD,
				    DEFAULT_FILE_BACKED_LISTS_THRESHOLD)
	: Integer.MAX_VALUE;
      Iterator<Object> iter = pdfStreamParser.getTokenIterator();
      while (iter.hasNext()) {
        if (tokens.size() == fileBackedThreshold) {
          // List becoming too large for main memory
          FileBackedList<PdfToken> newList = new FileBackedList<PdfToken>(tokens);
          // Clean up old list
          tokens.clear();
          ((ArrayList<PdfToken>)tokens).trimToSize();
          // Put new list in cleanup queue
          getPage().getDocument().autoCloseables.add(new WeakReference<AutoCloseable>(newList));
          // Start using this new list
          tokens = newList;
        }
        tokens.add(PdfBoxTokens.convertOne(iter.next()));
      }
      decodeStringsWithFontContext(tokens);
      return tokens;
    }
    catch (IOException ioe) {
      throw new PdfException(ioe);
    }
    finally {
      // "safeClose()" for a PDFStreamParser
      if (pdfStreamParser != null) {
        try {
          pdfStreamParser.close();
        }
        catch (IOException ioe) {
          // ignore
        }
      }
    }
  }

  /**
   * <p>
   * Decodes strings using font contexts, in place in the given list.
   * </p>
   * 
   * @param tokens
   *          A list of tokens.
   * @param currentFont
   *          The current font in the surrounding context of the list of tokens.
   * @throws PdfException
   *           if a referenced font cannot be found.
   * @since 1.62
   */
  protected void decodeStringsWithFontContext(List<PdfToken> tokens) throws PdfException {
    decodeStringsWithFontContext(tokens, null);
  }

  /**
   * <p>
   * Decodes strings using font contexts, in place in the given list, using a
   * current font at the beginning.
   * </p>
   * 
   * @param tokens
   *          A list of tokens.
   * @param currentFont
   *          The current font in the surrounding context of the list of tokens.
   * @throws PdfException
   *           if a referenced font cannot be found.
   * @since 1.62
   */
  protected void decodeStringsWithFontContext(List<PdfToken> tokens,
                                              PDFont currentFont)
      throws PdfException {
    PdfTokenFactory factory = PdfUtil.getTokenFactory(this);
    for (int i = 0 ; i < tokens.size() ; ++i) {
      PdfToken token = tokens.get(i);
      if (token.isOperator() && PdfOpcodes.SET_TEXT_FONT.equals(token.getOperator())) {
        if (i == 0 || i == 1) {
          continue; // Malformed; ignore
        }
        PdfToken fontName = tokens.get(i - 2);
        PdfToken fontSize = tokens.get(i - 1);
        if (!fontName.isName() || !(fontSize.isFloat() || fontSize.isInteger())) {
          continue; // Malformed; ignore
        }
        PDResources streamResources = getStreamResources();
        if (streamResources == null) {
          throw new PdfException("Current context has no PDResources instance");
        }
        currentFont = (PDFont)streamResources.getFonts().get(fontName.getName());
        if (currentFont == null) {
          throw new PdfException(String.format("Font '%s' not found", fontName.getName()));              
        }
      }
      else if (token.isString()) {
        if (currentFont == null) {
          throw new PdfException(String.format("No font set at index %d", i));              
        }
        // See PDFBox 1.8.2, PDFStreamEngine, lines 387-514
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new COSString(token.getString()).getBytes();
        int codeLength = 1;
        for (int j = 0; j < bytes.length; j += codeLength) {
          codeLength = 1;
          String str;
          try {
            str = currentFont.encode(bytes, j, codeLength);
            if (str == null && j + 1 < bytes.length) {
              codeLength++;
              str = currentFont.encode(bytes, j, codeLength);
            }
          } catch (IOException ioe) {
            throw new PdfException(String.format("Error decoding string at index %d", i), ioe);
          }
          if (str != null) {
            sb.append(str);
          }
        }
        tokens.set(i, factory.makeString(sb.toString()));
      }
      else if (token.isArray()) {
        List<PdfToken> array = token.getArray();
        try {
          decodeStringsWithFontContext(array, currentFont);
        } catch (PdfException pdfe) {
          throw new PdfException(String.format("Error processing list at index %d", i), pdfe);
        }
        tokens.set(i, factory.makeArray(array));
      }
    }
    
  }
  
  /**
   * <p>
   * Retrieves the {@link PDStream} instance underpinning this PDF token stream.
   * </p>
   * 
   * @return The {@link PDStream} instance this instance represents.
   * @since 1.56
   */
  protected abstract PDStream getPdStream();

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
   * <p>
   * Convenience method to create a new {@link PDStream} instance.
   * </p>
   * 
   * @return A new {@link PDStream} instance based on this document.
   * @since 1.56
   */
  protected PDStream makeNewPdStream() {
    return new PDStream(pdfBoxPage.pdfBoxDocument.pdDocument);
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
