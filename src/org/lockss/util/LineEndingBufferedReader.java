/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;

/**
 * <p>
 * A buffered {@link Reader} that also gives a way to consume input line by
 * line like {@link BufferedReader} but with the line endings preserved.
 * </p>
 * <p>
 * Line endings are defined as <code>"\r\n"</code> (Windows-style), or
 * <code>"\r"</code> not followed by <code>"\n"</code> (Macintosh-style), or
 * <code>"\n"</code> not preceded by <code>"\r"</code> (Unix-style). The last
 * line returned will not have a line ending if the input does not end with one.
 * </p>
 * <p>
 * This class also gives a way to count lines as it goes, similarly to
 * {@link LineNumberReader}. The documentation of
 * {@link LineNumberReader#getLineNumber()} does not make it perfectly clear
 * that the line number being returned is that of the line most recently read,
 * but it is, and this is what this class does, via
 * {@link #getLineCount()}. That number is zero before any input is read and
 * remains the same after end of file as just before end of file, and in this
 * class comes from {@link #getLineCount()}. There is no equivalent to
 * {@link LineNumberReader#setLineNumber(int)} but it could be achieved with
 * a subclass.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.66
 * @see BufferedReader
 * @see LineNumberReader
 */
public class LineEndingBufferedReader extends Reader {

  /**
   * <p>
   * The underlying {@link Reader} this class consumes.
   * </p>
   * 
   * @since 1.66
   */
  protected Reader underlyingReader;
  
  /**
   * <p>
   * This class' internal buffer.
   * </p>
   * 
   * @since 1.66
   */
  protected char[] myBuffer;
  
  /**
   * <p>
   * The current index into the internal buffer, {@link #myBuffer}.
   * </p>
   * 
   * @since 1.66
   */
  protected int myIndex;
  
  /**
   * <p>
   * A flag set when end of file has been reached in the underlying
   * {@link Reader}.
   * </p>
   * 
   * @since 1.66
   * @see #isEof()
   */
  protected boolean seenEof;
  
  /**
   * <p>
   * A flag set when this instance has been closed.
   * </p>
   * 
   * @since 1.66
   * @see #close()
   */
  protected boolean isClosed;
  
  /**
   * <p>
   * The length of {@link #myBuffer}, except when the underlying {@link Reader}
   * reaches end of file and there is not enough input left to fill the
   * internal buffer, in which case it is the maximum index into the internal
   * buffer.
   * </p>
   * 
   * @since 1.66
   * @see #isEof()
   */
  protected int maxBufferIndex;
  
  /**
   * <p>
   * The total number of lines read so far by {@link #readLine()}.
   * </p>
   * 
   * @see #getLineCount()
   */
  protected int lineCount;
  
  /**
   * <p>
   * The default internal buffer size ({@value #BUFFER_SIZE_DEFAULT} bytes).
   * </p>
   * 
   * @since 1.66
   */
  public static final int BUFFER_SIZE_DEFAULT = 8192;
  
  /**
   * <p>
   * Makes a new instance with a default internal buffer size.
   * </p>
   * 
   * @param underlyingReader
   *          The {@link Reader} being consumed.
   * @since 1.66
   * @see #BUFFER_SIZE_DEFAULT
   */
  public LineEndingBufferedReader(Reader underlyingReader) {
    this(underlyingReader, BUFFER_SIZE_DEFAULT);
  }
  
  /**
   * <p>
   * Makes a new instance with the specified internal buffer size.
   * </p>
   * 
   * @param underlyingReader
   *          The {@link Reader} being consumed.
   * @param bufferSize
   *          The desired internal buffer size.
   * @since 1.66
   */
  public LineEndingBufferedReader(Reader underlyingReader,
                                  int bufferSize) {
    this.underlyingReader = underlyingReader;
    this.myBuffer = new char[bufferSize];
    this.myIndex = myBuffer.length;
    this.maxBufferIndex = myBuffer.length;
    this.seenEof = false;
    this.isClosed = false;
    this.lineCount = 0;
  }
  
  @Override
  public void close() throws IOException {
    if (isClosed) {
      return;
    }
    isClosed = true;
    underlyingReader.close();
  }

  @Override
  public int read(char[] buf, int off, int len) throws IOException {
    throwIfClosed();
    if (isEof()) {
      return -1;
    }
    refill();
    int charsAvailable = maxBufferIndex - myIndex;
    int charsRequested = Math.min(buf.length - off, len);
    int charsToProcess = Math.min(charsAvailable, charsRequested);
    System.arraycopy(myBuffer, myIndex, buf, off, charsToProcess);
    myIndex += charsToProcess;
    return charsToProcess;
  }
  
  /**
   * <p>
   * Read one line of input until a line ending (or end of file), incrementing
   * the total line count returned by {@link #getLineCount()} in the process.
   * </p>
   * <p>
   * Line endings are defined as <code>"\r\n"</code> (Windows-style), or
   * <code>"\r"</code> not followed by <code>"\n"</code> (Macintosh-style), or
   * <code>"\n"</code> not preceded by <code>"\r"</code> (Unix-style). The last
   * line returned will not have a line ending if the input does not end with
   * one. When end of file has been reached, this method returns
   * <code>null</code>.
   * </p>
   * 
   * @return A line of text including its line ending (or not, if it is the last
   *         line in the input and the input does not end with a line ending),
   *         or <code>null</code> if end of file has been reached.
   * @throws IOException
   *           if the underlying {@link Reader} encounters an I/O exception.
   * @since 1.66
   */
  public String readLine() throws IOException {
    throwIfClosed();
    StringBuilder sb = new StringBuilder(myBuffer.length);
    boolean carriageReturnAcrossRefill = false;
    int endIndex; // not inclusive, like in substring() and similar

    /*
     * There are basically five cases.
     * (1) "\n" (at the end of the buffer or not)
     * (2) "\r" not at the end of the buffer, followed by "\n"
     * (3) "\r" not at the end of the buffer, not followed by "\n" 
     * (4) "\r" at the end of the buffer, followed by "\n" after a refill
     * (5) "\r" at the end of the buffer, not followed by "\n" after a refill 
     */
    
    _read_loop: while (true) {
      // Step 1: refill buffer
      refill();
      if (isEof()) {
        if (sb.length() == 0) {
          // At EOF
          return null;
        }
        else {
          ++lineCount;
          return sb.toString();
        }
      }
      
      // Step 2: look for a line ending
      for (int i = myIndex ; i < maxBufferIndex ; ++i) {
        char ch = myBuffer[i];
        if (ch == '\n') {
          // Case (1) or (4)
          endIndex = i + 1;
          break _read_loop;
        }
        else if (!carriageReturnAcrossRefill && ch == '\r') {
          if (i + 1 < maxBufferIndex) {
            // Next character readily available; simply check it
            if (myBuffer[i + 1] == '\n') {
              endIndex = i + 2; // Case (2)
            }
            else {
              endIndex = i + 1; // Case (3)
            }
            break _read_loop;
          }
          else {
            // Need to refill buffer and check one character
            carriageReturnAcrossRefill = true;
          }
        }
        else if (carriageReturnAcrossRefill) {
          // Case (5)
          endIndex = i;
          break _read_loop;
        }
      }
      
      // Step 3: still in _read_loop; copy into result
      sb.append(myBuffer, myIndex, maxBufferIndex - myIndex);
      myIndex = maxBufferIndex;
    }
    
    // Broke out of _read_loop; endIndex is set
    sb.append(myBuffer, myIndex, endIndex - myIndex);
    myIndex = endIndex;
    ++lineCount;
    return sb.toString();
  }

  /**
   * <p>
   * Returns the number of lines returned by {@link #readLine()} so far, zero
   * before any input is read, and the same number after end of file is reached
   * as just before end of file.
   * </p>
   * <p>
   * Calling {@link #read(char[], int, int)} (et al.) some of the time and
   * {@link #readLine()} some of the time render the results of this method
   * meaningless.
   * </p>
   * 
   * @return The total line count so far.
   * @since 1.66
   */
  public int getLineCount() {
    return lineCount;
  }
  
  /**
   * <p>
   * Refills the internal buffer if necessary.
   * <p>
   * 
   * @throws IOException
   *           if the underlying {@link Reader} encounters an I/O exception
   * @since 1.66
   */
  protected void refill() throws IOException {
    if (myIndex == maxBufferIndex) {
      myIndex = 0;
      int totalRead = 0;
      int singleRead;
      do {
        singleRead = underlyingReader.read(myBuffer, totalRead, myBuffer.length - totalRead);
        if (singleRead == -1) {
          seenEof = true;
        }
        else {
          totalRead = totalRead + singleRead;
        }
      } while (singleRead != -1 && totalRead < myBuffer.length);
      maxBufferIndex = totalRead;
    }
  }
  
  /**
   * <p>
   * Throws if the instance is already closed.
   * </p>
   * 
   * @throws IOException
   *           if the instance is already closed.
   * @since 1.66
   * @see #isClosed
   */
  protected void throwIfClosed() throws IOException {
    if (isClosed) {
      throw new IOException("stream closed");
    }
  }

  /**
   * <p>
   * Determines if this instance has reached end of file. This is when the
   * underlying {@link Reader} has reached end of file and the index into the
   * this instance's internal buffer has reached the maximum number of
   * characters still available.
   * </p>
   * 
   * @return whether this instance has reached end of file
   */
  protected boolean isEof() {
    return seenEof && myIndex == maxBufferIndex;
  }
  
}
