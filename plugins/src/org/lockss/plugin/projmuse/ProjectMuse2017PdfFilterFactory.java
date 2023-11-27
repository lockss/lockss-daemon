/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.projmuse;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.filter.pdf.PdfTransform;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

public class ProjectMuse2017PdfFilterFactory extends ExtractingPdfFilterFactory {

  private static final Logger log = Logger.getLogger(ProjectMuse2017PdfFilterFactory.class);

  protected static final Pattern WATERMARK_LINE_1 =
      //[ Access provided at 2 Dec 2022 15:10 GMT from Stanford LOCKSS (+1 other institution account) ]
      Pattern.compile("Access provided at .+ from Stanford LOCKSS");

  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    /* copied from ProjectMusePdfFilterFactory */
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetCreator();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetProducer();
    PdfUtil.normalizeTrailerId(pdfDocument);

    List<PdfPage> pdfPages = pdfDocument.getPages();
    Iterator<PdfPage> pageIter = pdfPages.iterator();
    while (pageIter.hasNext()) {
      PdfPage pdfPage = pageIter.next();
      List<PdfTokenStream> allTokenStream = pdfPage.getAllTokenStreams();
      for (PdfTokenStream pageTokenStream : allTokenStream) {

      List<PdfToken> tokens = pageTokenStream.getTokens();
      Iterator<PdfToken> tokenIter = tokens.iterator();
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
              if (WATERMARK_LINE_1.matcher(str).find()) {
                accumulator.clear();
                return;
              }
            }
          }
        }
      };
      pageTokenStream.setTokens(IteratorUtils.toList(filteredIter));
    }}
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
    /*
    Reads in a pdf and applies the filters writing the contents to a new file <fileStr>.bin
     */
    String[] fileStrs = {
        "/home/mark/Downloads/projmuse.pdf",
        "/home/mark/Downloads/projmuse6.pdf"
    };
    for (String fileStr : fileStrs) {
      FilterFactory fact = new ProjectMuse2017PdfFilterFactory();
      IOUtils.copy(fact.createFilteredInputStream(null, new FileInputStream(fileStr), null),
          new FileOutputStream(fileStr + ".bin"));
    }
  }
}
