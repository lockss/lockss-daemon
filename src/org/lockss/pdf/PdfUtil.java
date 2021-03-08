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

package org.lockss.pdf;

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.iterators.*;
import org.apache.commons.text.StringEscapeUtils;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.util.*;

/**
 * <p>
 * PDF-related utilities.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public class PdfUtil {

  /**
   * <p>
   * The configuration prefix for this class ({@value}).
   * </p>
   * 
   * @since 1.56
   */
  public static final String CONFIG_PREFIX = Configuration.PREFIX + "pdf.";

  /**
   * Default: 5MB
   */
  public static final int DEFAULT_PDF_MEMORY_LIMIT = 5 * 1024 * 1024;

  /**
   * Number of megabytes above which a filtered PDF file is transferred from
   * memory to a temporary file. May also be used by other PDF operations that
   * require in-memory processing.
   */
  public static final String PARAM_PDF_MEMORY_LIMIT =
      CONFIG_PREFIX + "pdfMemoryLimit";

  /**
   * <p>
   * A suggested prefix for title database attributes conveying hints about PDF
   * filter factories.
   * </p>
   * 
   * @see DefinableArchivalUnit#SUFFIX_FILTER_FACTORY
   */
  public static final String PREFIX_PDF_FILTER_FACTORY_HINT = "hint_";

  /**
   * <p>
   * A logger for use by this class.
   * </p>
   * 
   * @since 1.56
   */
  private static final Logger log = Logger.getLogger(PdfUtil.class);

  /**
   * <p>
   * Convenience call to {@link #asInputStream(PdfDocument, int)} using a
   * default memory limit defined by the parameter
   * {@link #PARAM_PDF_MEMORY_LIMIT}.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @return The saved PDF document, as an input stream.
   * @throws PdfException
   *           If processing fails at the PDF level.
   * @throws IOException
   *           If processing fails at the I/O level.
   * @see #asInputStream(PdfDocument, int)
   */
  public static InputStream asInputStream(PdfDocument pdfDocument)
      throws PdfException, IOException {
    return asInputStream(pdfDocument, getPdfMemoryLimit());
  }

  /**
   * <p>
   * Saves the given PDF documeSnt and returns the result as an input stream,
   * staying if possible in memory up to the given number of megabytes.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @param memoryLimitMb
   *          The number of megabytes of memory up to which processing can be
   *          done entirely in memory.
   * @return The saved PDF document, as an input stream.
   * @throws PdfException
   *           If processing fails at the PDF level.
   * @throws IOException
   *           If processing fails at the I/O level.
   * @see DeferredTempFileOutputStream
   */
  public static InputStream asInputStream(PdfDocument pdfDocument,
                                          int memoryLimitMb)
      throws PdfException, IOException {
    DeferredTempFileOutputStream os = new DeferredTempFileOutputStream(memoryLimitMb);
    try {
      pdfDocument.save(os);
      os.close();
      return os.getDeleteOnCloseInputStream();
    }
    catch (PdfException | IOException | RuntimeException e) {
      os.deleteTempFile();
      throw e;
    }
  }

  /**
   * <p>
   * Convenience method to get the token factory out of a document factory.
   * Equivalent to:
   * <code>pdfDocumentFactory.getTokenFactory()</code>
   * </p>
   * 
   * @param pdfDocumentFactory
   *          A PDF document factory instance.
   * @return A PDF token factory instance.
   * @since 1.70
   */
  public static PdfTokenFactory getTokenFactory(PdfDocumentFactory pdfDocumentFactory) {
    return pdfDocumentFactory.getTokenFactory();
  }
  
  /**
   * <p>
   * Convenience method to get the token factory out of a document.
   * Equivalent to:
   * <code>pdfDocument.getDocumentFactory().getTokenFactory()</code>
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document instance.
   * @return A PDF token factory instance.
   * @since 1.70
   */
  public static PdfTokenFactory getTokenFactory(PdfDocument pdfDocument) {
    return getTokenFactory(pdfDocument.getDocumentFactory());
  }
  
  /**
   * <p>
   * Convenience method to get the token factory out of a page.
   * Equivalent to:
   * <code>pdfPage.getDocument().getDocumentFactory().getTokenFactory()</code>
   * </p>
   * 
   * @param pdfPage
   *          A PDF page instance.
   * @return A PDF token factory instance.
   * @since 1.70
   */
  public static PdfTokenFactory getTokenFactory(PdfPage pdfPage) {
    return getTokenFactory(pdfPage.getDocument());
  }
  
  /**
   * <p>
   * Convenience method to get the token factory out of a token stream.
   * Equivalent to:
   * <code>pdfTokenStream.getPage().getDocument().getDocumentFactory().getTokenFactory()</code>
   * </p>
   * 
   * @param pdfTokenStream
   *          A PDF token stream instance.
   * @return A PDF token factory instance.
   * @since 1.70
   */
  public static PdfTokenFactory getTokenFactory(PdfTokenStream pdfTokenStream) {
    // Null check to accommodate fake token streams like MockPdfTokenStream
    PdfPage pdfPage = pdfTokenStream.getPage();
    return pdfPage == null ? null : getTokenFactory(pdfPage);
  }
  
  /**
   * <p>
   * Retrieves from the title database the value of a special attribute the
   * given AU may have, that is used by convention to direct a PDF filter
   * factory to use a particular PDF transformation for that AU. The special
   * attribute is the concatenation of {@link #PREFIX_PDF_FILTER_FACTORY_HINT},
   * {@link Constants#MIME_TYPE_PDF} and
   * {@link DefinableArchivalUnit#SUFFIX_ARTICLE_MIME_TYPE}.
   * </p>
   * 
   * @param au
   *          An archival unit.
   * @return The value of the PDF hint attribute, or <code>null</code> if unset.
   */
  public static String getPdfHint(ArchivalUnit au) {
    String key = PREFIX_PDF_FILTER_FACTORY_HINT + Constants.MIME_TYPE_PDF
        + DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY;
    return AuUtil.getTitleAttribute(au, key);
  }

  /**
   * <p>
   * Convenience method to retrieve the value of the daemon parameter
   * {@link #PARAM_PDF_MEMORY_LIMIT}/{@link #DEFAULT_PDF_MEMORY_LIMIT}.
   * </p>
   * 
   * @return The value of {@link #PARAM_PDF_MEMORY_LIMIT}/
   *         {@link #DEFAULT_PDF_MEMORY_LIMIT} in the current configuration.
   * @since 1.56
   */
  public static int getPdfMemoryLimit() {
    return CurrentConfig.getCurrentConfig().getInt(PARAM_PDF_MEMORY_LIMIT,
                                                   DEFAULT_PDF_MEMORY_LIMIT);
  }
  
  /**
   * <p>
   * Normalizes all token stream of each page of the given PDF document.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizeAllTokenStreams(PdfDocument pdfDocument)
      throws PdfException {
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      normalizeAllTokenStreams(pdfPage);
    }
  }

  /**
   * <p>
   * Normalizes all the token streams of the given PDF page.
   * </p>
   * 
   * @param pdfPage
   *          A PDF page.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizeAllTokenStreams(PdfPage pdfPage)
      throws PdfException {
    for (PdfTokenStream pdfTokenStream : pdfPage.getTokenStreamList()) {
      normalizeTokenStream(pdfTokenStream);
    }
  }

  /**
   * <p>
   * Normalizes the page token stream of the given PDF page.
   * </p>
   * 
   * @param pdfPage
   *          A PDF page.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizePageTokenStream(PdfPage pdfPage)
      throws PdfException {
    normalizeTokenStream(pdfPage.getPageTokenStream());
  }

  /**
   * <p>
   * Normalizes the page stream of each page of the given PDF document.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizePageTokenStreams(PdfDocument pdfDocument)
      throws PdfException {
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      normalizePageTokenStream(pdfPage);
    }
  }

  /**
   * <p>
   * Reads all the tokens from the given stream, then writes the result back to
   * it.
   * </p>
   * <p>
   * The purpose of this seemingly idempotent operation is to force the
   * underlying stream implementation to unravel any parts of the stream it may
   * have been able to delay interpreting until the stream is accessed, such as
   * decoding a filtered stream.
   * </p>
   * 
   * @param pdfTokenStream
   *          A token stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  public static void normalizeTokenStream(PdfTokenStream pdfTokenStream)
      throws PdfException {
    pdfTokenStream.setTokens(pdfTokenStream.getTokenIterator());
  }

  /**
   * <p>
   * Sets the ID array of the given PDF document to one consisting of the
   * arbitrary ID string <code>"12345678901234567890123456789012"</code> twice.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #setTrailerId(PdfDocument, String, String)
   */
  public static void normalizeTrailerId(PdfDocument pdfDocument)
      throws PdfException {
    setTrailerId(pdfDocument,
                 "12345678901234567890123456789012",
                 "12345678901234567890123456789012");
  }

  /**
   * <p>
   * Convenience method to convert the given PDF token to a human-readable
   * String.
   * </p>
   * 
   * @param pdfToken
   *          A PDF token.
   * @return A string representing the token.
   */
  public static String prettyPrint(PdfToken pdfToken) {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb, pdfToken);
    return sb.toString();
  }

  /**
   * <p>
   * Convenience method to output a human-readable version of the given token to
   * the given string builder.
   * </p>
   * 
   * @param sb
   *          A string builder.
   * @param pdfToken
   *          A PDF token.
   * @since 1.57
   */
  public static void prettyPrint(StringBuilder sb, PdfToken pdfToken) {
    if (pdfToken == null) {
      sb.append("null");
      return;
    }
    sb.append("[");
    if (pdfToken.isArray()) {
      sb.append("array:");
      for (PdfToken arrayToken : pdfToken.getArray()) {
        prettyPrint(sb,arrayToken);
      }
    } else if (pdfToken.isBoolean()) {
      sb.append("boolean:");
      sb.append(Boolean.toString(pdfToken.getBoolean()));
    } else if (pdfToken.isDictionary()) {
      boolean first = true;
      sb.append("dictionary:");
      for (Map.Entry<String, PdfToken> entry : pdfToken.getDictionary().entrySet()) {
        if (first) {
          first = false;
        } else {
          sb.append(";");
        }
        sb.append(StringEscapeUtils.escapeJava(entry.getKey()));
        sb.append("=");
        prettyPrint(sb,entry.getValue());
      }
    } else if (pdfToken.isFloat()) {
      sb.append("float:");
      sb.append(Float.toString(pdfToken.getFloat()));
    } else if (pdfToken.isInteger()) {
      sb.append("integer:");
      sb.append(Long.toString(pdfToken.getInteger()));
    } else if (pdfToken.isName()) {
      sb.append("name:");
      sb.append(StringEscapeUtils.escapeJava(pdfToken.getName()));
    } else if (pdfToken.isNull()) {
      sb.append("null");
    } else if (pdfToken.isObject()) {
      sb.append("object:");
      prettyPrint(sb, pdfToken.getObject());
    } else if (pdfToken.isOperator()) {
      sb.append("operator:");
      sb.append(StringEscapeUtils.escapeJava(pdfToken.getOperator()));
    } else if (pdfToken.isString()) {
      sb.append("string:\"");
      sb.append(StringEscapeUtils.escapeJava(pdfToken.getString()));
      sb.append("\"");
    }
    sb.append("]");
  }

  /**
   * <p>
   * If the given PDF document is not <code>null</code>, closes it ignoring any
   * exception thrown by {@link PdfDocument#close()}.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @since 1.56
   * @see PdfDocument#close()
   */
  public static void safeClose(PdfDocument pdfDocument) {
    try {
      if (pdfDocument != null) {
        pdfDocument.close();
      }
    } catch (PdfException pdfe) {
      log.debug2("Error closing a PDF document", pdfe);
    }
  }

  /**
   * <p>
   * Sets the trailer ID array of the given PDF document to one consisting of
   * the two given strings.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @param id0
   *          The first string in the trailer ID array.
   * @param id1
   *          The second string in the trailer ID array.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  public static void setTrailerId(PdfDocument pdfDocument,
                                  String id0,
                                  String id1)
        throws PdfException {
    PdfTokenFactory pdfTokenFactory = PdfUtil.getTokenFactory(pdfDocument);
    Map<String, PdfToken> trailerMapping = pdfDocument.getTrailer();
    trailerMapping.remove(PdfNames.ID);
    List<PdfToken> idArray = new ArrayList<PdfToken>(2);
    idArray.add(pdfTokenFactory.makeString(id0));
    idArray.add(pdfTokenFactory.makeString(id1));
    trailerMapping.put(PdfNames.ID, pdfTokenFactory.makeArray(idArray));
    pdfDocument.setTrailer(trailerMapping);
  }

  public static Iterator<? extends PdfOperandsAndOperator<? extends PdfToken>> toOperandsAndOperatorIterator(final Iterator<? extends PdfToken> tokenIter) {
    return new LazyIteratorChain<PdfOperandsAndOperator<? extends PdfToken>>() {
      @Override
      protected Iterator<? extends PdfOperandsAndOperator<? extends PdfToken>> nextIterator(int count) {
        if (!tokenIter.hasNext()) {
          return null;
        }
        PdfOperandsAndOperator<PdfToken> oao = new PdfOperandsAndOperator<>();
        PdfToken tok = null;
        while (tokenIter.hasNext()) {
          tok = tokenIter.next();
          if (tok.isOperator()) {
            oao.setOperator(tok);
            return new SingletonIterator<>(oao);
          }
          else {
            oao.getOperands().add(tok);
          }
        }
        throw new PdfRuntimeException("Token stream does not end with an operator; last token: " + prettyPrint(tok));
      }
    };
  }
  
  /**
   * <p>
   * This class cannot be instantiated.
   * </p>
   * 
   * @since 1.56
   */
  private PdfUtil() {
    // Prevent instantiation
  }

}
