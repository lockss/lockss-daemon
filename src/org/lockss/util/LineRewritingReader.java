/*
 * $Id: LineRewritingReader.java,v 1.1 2014-07-22 02:08:41 thib_gc Exp $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.io.IOException;
import java.io.Reader;

/**
 * <p>
 * This abstract {@link Reader} consumes an underlying {@link Reader} line by
 * line, giving concrete implementations a chance to rewrite each line to apply
 * some transformation on the incoming character data via the
 * {@link #rewriteLine(String)} method.
 * </p>
 * <p>
 * A {@link LineEndingBufferedReader} is used to read from the underlying
 * {@link Reader}, which keeps line terminators.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.66
 */
public abstract class LineRewritingReader extends Reader {

  /**
   * <p>
   * The underlying {@link Reader}, wrapped if necessary in a
   * {@link LineEndingBufferedReader}.
   * </p>
   */
  protected LineEndingBufferedReader underlyingReader;
  
  /**
   * <p>
   * The index into the current line.
   * </p>
   */
  protected int currentIndex;
  
  /**
   * <p>
   * The line most recently read from the underlying {@link Reader}.
   * </p>
   */
  protected String currentLine;
  
  /**
   * <p>
   * A flag denoting whether the underlying {@link Reader} has reached end of
   * file.
   * </p>
   */
  protected boolean isEof;
  
  /**
   * <p>
   * A flag denoting whether {@link #close()} has been called on this instance.
   * </p>
   */
  protected boolean isClosed;
  
  /**
   * <p>
   * The maximum length allowed for a line consumed from the underlying
   * {@link Reader} (<code>0</code> for unlimited line length).
   * </p>
   */
  protected int maxLineLength;
  
  /**
   * <p>
   * Builds a new rewriting reader based on the given underlying reader, using
   * the supplied maximum line length.
   * </p>
   * 
   * @param underlyingReader
   *          An underlying {@link Reader} instance.
   * @param maxLineLength
   *          The maximum line length allowed, <code>0</code> for unlimited
   *          length.
   * @since 1.66
   */
  public LineRewritingReader(Reader underlyingReader,
                             int maxLineLength) {
    if (underlyingReader instanceof LineEndingBufferedReader) {
      this.underlyingReader = (LineEndingBufferedReader)underlyingReader;
    }
    else {
      this.underlyingReader = new LineEndingBufferedReader(underlyingReader);
    }
    setMaxLineLength(maxLineLength);
    this.currentLine = null;
    this.currentIndex = 0;
    this.isEof = false;
    this.isClosed = false;
  }
  
  /**
   * <p>
   * Builds a new rewriting reader based on the given underlying reader, with
   * unlimited line length.
   * </p>
   * 
   * @param underlyingReader
   *          An underlying {@link Reader} instance.
   * @since 1.66
   * @see LineRewritingReader#RewritingReader(Reader, int)
   */
  public LineRewritingReader(Reader underlyingReader) {
    this(underlyingReader, 0);
  }
  
  @Override
  public void close() throws IOException {
    if (isClosed) {
      return;
    }
    isClosed = true;
    underlyingReader.close();
  }
  
  /**
   * <p>
   * Gets the current maximum line length.
   * </p>
   * 
   * @return The maximum line length, <code>0</code> if unlimited.
   * @since 1.66
   */
  public int getMaxLineLength() {
    return maxLineLength;
  }

  @Override
  public int read(char[] buf, int off, int len) throws IOException {
    getSomeInput();
    if (isEof) {
      return -1;
    }
    int charsAvailable = currentLine.length() - currentIndex;
    int charsRequested = Math.min(buf.length - off, len);
    int charsToProcess = Math.min(charsAvailable, charsRequested);
    currentLine.getChars(currentIndex, currentIndex + charsToProcess, buf, off);
    advance(charsToProcess);
    return charsToProcess;
  }
  
  /**
   * <p>
   * Sets the maximum line length, applying to the next time a line is consumed
   * from the underlying {@link Reader}.
   * </p>
   * 
   * @param maxLineLength
   *          The maximum line length allowed, <code>0</code> for unlimited
   *          length. Note that the underlying {@link Reader} is consumed via a
   *          {@link LineEndingBufferedReader} which keeps the line terminator.
   * @since 1.66
   */
  public void setMaxLineLength(int maxLineLength) {
    this.maxLineLength = maxLineLength;
  }
  
  /**
   * <p>
   * Throws {@link IOException} if this instance has already been closed.
   * </p>
   * 
   * @throws IOException
   *           if this instance has already been closed.
   * @since 1.66
   */
  protected void throwIfClosed() throws IOException {
    if (isClosed) {
      throw new IOException("stream closed");
    }
  }

  /**
   * <p>
   * Records that the given number of characters have been consumed from the
   * current line, and resets the current line if it has been completely
   * consumed.
   * </p>
   * 
   * @param charsConsumed
   *          The number of characters consumed from the current line.
   * @since 1.66
   */
  protected void advance(int charsConsumed) {
    currentIndex = currentIndex + charsConsumed;
    if (currentIndex == currentLine.length()) {
      currentLine = null;
    }
  }
  
  /**
   * <p>
   * May get a new line from the underlying {@link Reader}. If so, detects end
   * of file and applies the maximum line length if necessary. The incoming line
   * is then rewritten by {@link #rewriteLine(String)}.
   * </p>
   * 
   * @throws IOException
   *           if this instance is already closed, or if the underlying
   *           {@link Reader} throws an {@IOException}, or if the
   *           maximum line length is exceeded.
   * @since 1.66
   */
  protected void getSomeInput() throws IOException {
    throwIfClosed();
    while (!isEof && currentLine == null) {
      currentLine = underlyingReader.readLine();
      currentIndex = 0;
      if (currentLine == null) {
        isEof = true;
      }
      else {
        if (maxLineLength > 0 && currentLine.length() > maxLineLength) {
          throw new IOException(String.format("Line length (%d) exceeds maximum line length (%d)", currentLine.length(), maxLineLength));
        }
        currentLine = rewriteLine(currentLine);
        if (currentLine != null) {
          return;
        }
        // otherwise skip this line and get another
      }
    }
  }
  
  /**
   * <p>
   * Rewrites an incoming line of input. Concrete implementations are free to do
   * whatever they need here, including returning a line much longer than the
   * maximum line length (if set).
   * </p>
   * <p>
   * Returning <code>null</code> means that the line should be skipped (ignored)
   * entirely.
   * </p>
   * 
   * @param line
   *          The incoming line of character data from the underlying
   *          {@link Reader}, with its line ending.
   * @return A rewritten line of data.
   * @since 1.66
   */
  public abstract String rewriteLine(String line);

}
