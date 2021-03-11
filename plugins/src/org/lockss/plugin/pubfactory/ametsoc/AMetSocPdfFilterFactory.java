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

package org.lockss.plugin.pubfactory.ametsoc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.ExtractingPdfFilterFactory.BaseDocumentExtractingTransform;
import org.lockss.pdf.*;
import org.lockss.plugin.*;

/**
 * <p>
 * AMetSoc PDFs receive a small watermark similar to this:
 * </p>
 * 
 * <pre>
Brought to you by CLOCKSS | Unauthenticated | Downloaded 03/04/21 01:12 AM UTC
 * </pre>
 * <p>
 * But, at least in PDFBox 1.8.16, the above string looks like this (written in
 * Java notation) in the token stream:
 * </p>
<pre>
"\uFEFF\u0000B\u0000r\u0000o\u0000u\u0000g\u0000h\u0000t\u0000 \u0000t\u0000o\u0000 \u0000y\u0000o\u0000u\u0000\u0000b\u0000y\u0000 \u0000C\u0000L\u0000O\u0000C\u0000K\u0000S\u0000S\u0000 \u0000|\u0000 \u0000U\u0000n\u0000a\u0000u\u0000t\u0000h\u0000e\u0000n\u0000t\u0000i\u0000c\u0000a\u0000t\u0000e\u0000d\u0000 \u0000|\u0000 \u0000D\u0000o\u0000w\u0000n\u0000l\u0000o\u0000a\u0000d\u0000e\u0000d\u0000 \u00000\u00003\u0000/\u00000\u00004\u0000/\u00002\u00001\u0000 \u00000\u00001\u0000\u02DB\u00001\u00002\u0000 \u0000A\u0000M\u0000 \u0000U\u0000T\u0000C"
</pre>
 * <p>
 * Ostensibly, this is the UTF-16BE encoding (with a byte order mark), with the
 * exception that the timestamp sometimes has mis-mapped characters (which may
 * improve in PDFBox 2):
 * </p>
<pre>
Brought to you by CLOCKSS | Unauthenticated | Downloaded 03/11/21 0??0? AM UTC
</pre>
 * <p>
 * This appears in a BT-ET block at the very end of the page token stream. In
 * preparation for 1.76, process this iterator-style with a primitive iterator
 * similar to SplitOperator/MergeOperator in PDFBox 0.7.3.
 * </p>
 * <p>
 * This is not a {@link SimplePdfFilterFactory} because the watermark string
 * uses a font with a gensym name and variable contents. (Perhaps an operation
 * to delete fonts from Resources dictionaries would help in PDFBox 2.)
 * </p>
 * <p>
 * Fortuitously, one of our seed examples had an unparseable creation date, so
 * this transform also includes ignoring the document information.
 * </p>
 * 
 * @see SubsequenceIterator
 */
public class AMetSocPdfFilterFactory extends ExtractingPdfFilterFactory {

  /*
   * Examples (which redirect to PDF files in spite of .xml):
   * 
   * https://journals.ametsoc.org/downloadpdf/journals/eint/24/1/EI-D-19-0015.1.xml
   * https://journals.ametsoc.org/downloadpdf/journals/wcas/12/1/wcas-d-19-0031.1.xml
   * https://journals.ametsoc.org/downloadpdf/journals/atsc/60/24/1520-0469_2003_060_3009_tiowac_2.0.co_2.xml
   * https://journals.ametsoc.org/downloadpdf/journals/bams/94/7/bams-d-13-00013.1.xml
   * 
   * The first one (EI-D-19-0015.1.xml) has an example of a huge token stream
   * with drawing operations on page 6 (page index 5).
   */
  
  protected static final Pattern BROUGHT_TO_YOU_ETC =
      Pattern.compile("^Brought to you by.*Downloaded \\d{2}/\\d{2}/\\d{2}");
  
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    List<PdfPage> pdfPages = pdfDocument.getPages();
    Iterator<PdfPage> pageIter = pdfPages.iterator(); // in preparation for 1.76
    while (pageIter.hasNext()) {
      PdfPage pdfPage = pageIter.next();
      PdfTokenStream pageTokenStream = pdfPage.getPageTokenStream();
      List<PdfToken> tokens = pageTokenStream.getTokens();
      Iterator<PdfToken> tokenIter = tokens.iterator(); // in preparation for 1.76
      Iterator<PdfToken> filteredIter = new SubsequenceIterator<PdfToken>(tokenIter) {
        @Override
        protected boolean isBegin(PdfToken item) {
          return item.isOperator() && PdfOpcodes.isBeginTextObject(item);
        }
        @Override
        protected boolean isEnd(PdfToken item) {
          return item.isOperator() && PdfOpcodes.isEndTextObject(item);
        }
        @Override
        protected void hasAccumulated() {
          for (PdfToken tok : accumulator) {
            if (tok.isString()) {
              String str = tok.getString();
              if (str.startsWith("\ufeff")) {
                String rewritten = new String(str.substring(1).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_16BE);
                if (BROUGHT_TO_YOU_ETC.matcher(rewritten).find()) {
                  accumulator.clear();
                  return;
                }
              }
            }
          }
        }
      };
      pageTokenStream.setTokens(IteratorUtils.toList(filteredIter));
    }
  }
  
  @Override
  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au, OutputStream os) {
    // Ignore unparseable creation dates
    return new BaseDocumentExtractingTransform(os) {
      @Override
      public void outputCreationDate() throws PdfException {
        // Intentionally made blank
      }
    };
  }
  
  /**
   * <p>
   * An iterator wrapper that consumes its underlying iterator but attempts to
   * identify subsequences identified as starting and ending by elements
   * recognized by {@link #isBegin(Object)} and {@link #isEnd(Object)}
   * respectively.
   * </p>
   * <p>
   * When the underlying iterator returns an element recognized by
   * {@link #isBegin(Object)}, this iterator continues to consume the underlying
   * iterator into the list {@link #accumulator} until the end of the underlying
   * iterator has been reached or until an element is recognized by
   * {@link #isEnd(Object)}. If an ending element is found,
   * {@link #hasAccumulated()} is invoked, to give a subclass the opportunity to
   * alter the sequence of tokens that will be returned by this iterator,
   * possibly by suppressing the entire subsequence
   * ({@link Collection#clear()}). If the end of the underlying iterator is
   * reached before an ending element is found, {@link #hasAccumulated()} is not
   * invoked. By default, {@link #isBegin(Object)} and {@link #isEnd(Object)}
   * return false and {@link #hasAccumulated()} does nothing.
   * </p>
   */
  public class SubsequenceIterator<E> implements Iterator<E> {
    
    protected Iterator<E> iterator;
    
    protected Deque<E> accumulator;
    
    public SubsequenceIterator(Iterator<E> iterator) {
      this.iterator = iterator;
      this.accumulator = new ArrayDeque<>();
    }
    
    @Override
    public boolean hasNext() {
      if (iterator == null) {
        return false;
      }
      findNext();
      return !accumulator.isEmpty();
    }

    @Override
    public E next() {
      return accumulator.removeFirst();
    }
    
    protected void findNext() {
      if (!accumulator.isEmpty()) {
        return;
      }
      outer_loop: while (iterator.hasNext()) {
        E item = iterator.next();
        accumulator.addLast(item);
        if (!isBegin(item)) {
          return;
        }
        while (iterator.hasNext()) {
          item = iterator.next();
          accumulator.addLast(item);
          if (isEnd(item)) {
            hasAccumulated();
            if (!accumulator.isEmpty()) {
              return;
            }
            continue outer_loop;
          }
        }
      }
      iterator = null;
      
    }
    
    protected boolean isBegin(E item) {
      return false;
    }
    
    protected boolean isEnd(E item) {
      return false;
    }
    
    protected void hasAccumulated() {
      // Intentionally left blank
    }
    
  }
  
  public static void main(String[] args) throws Exception {
    String[] fileStrs = {

    };
    for (String fileStr : fileStrs) {
      FilterFactory fact = new AMetSocPdfFilterFactory();
      IOUtils.copy(fact.createFilteredInputStream(null, new FileInputStream(fileStr), null),
                   new FileOutputStream(fileStr + ".bin"));
    }
  }
  
}
