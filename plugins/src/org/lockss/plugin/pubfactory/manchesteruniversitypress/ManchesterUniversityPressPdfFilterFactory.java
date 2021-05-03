package org.lockss.plugin.pubfactory.manchesteruniversitypress;

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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class ManchesterUniversityPressPdfFilterFactory extends ExtractingPdfFilterFactory {

  private static final Logger log = Logger.getLogger(ManchesterUniversityPressPdfFilterFactory.class);
  /*
   *
   */

  protected static final Pattern WATERMARK_LINE_1 =
      Pattern.compile("^Downloaded from manchester");
  protected static final Pattern WATERMARK_LINE_2_VARIANTS =
      Pattern.compile("via (c?lockss archive|communal account|c?lockks archive)", Pattern.CASE_INSENSITIVE);

  protected static boolean SEEN_WATERMARK_LINE_1 = false;
  Integer count = 0;

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
      Iterator<PdfToken> filteredIter = new ManchesterUniversityPressPdfFilterFactory.SubsequenceIterator<PdfToken>(tokenIter) {
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
                if (WATERMARK_LINE_1.matcher(rewritten).find()) {
                  accumulator.clear();
                  // now that we've seen watermark line 1, we set to true
                  SEEN_WATERMARK_LINE_1 = true;
                  return;
                }
              } else if (SEEN_WATERMARK_LINE_1) {
                // the next line must be WATERMARK_LINE_2, if it exists
                SEEN_WATERMARK_LINE_1 = false;
                // still check the pattern, just in case the line 2 was actually part of line 1
                if (WATERMARK_LINE_2_VARIANTS.matcher(str).find()) {
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
      FilterFactory fact = new ManchesterUniversityPressPdfFilterFactory();
      IOUtils.copy(fact.createFilteredInputStream(null, new FileInputStream(fileStr), null),
          new FileOutputStream(fileStr + ".bin"));
    }
  }

}