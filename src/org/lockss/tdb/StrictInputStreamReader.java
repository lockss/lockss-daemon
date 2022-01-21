/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.tdb;

import java.io.*;
import java.nio.charset.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.*;

/**
 * <p>
 * An {@link InputStreamReader}-like class that throws
 * {@link MalformedInputRangeException} when it encounters malformed input for
 * the underlying input stream's declared character set. By default,
 * {@link InputStreamReader} ignores malformed input (see
 * {@link CodingErrorAction#IGNORE}).
 * </p>
 * <p>
 * This class is essentially a character-oriented version of
 * {@link CountingInputStream}, applied to an {@link InputStreamReader}. The
 * index ranges are approximate because of buffering and byte-level lookahead
 * within {@link InputStreamReader}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.76
 * @see MalformedInputRangeException
 * @see #handleIOException(IOException)
 * @see CodingErrorAction#IGNORE
 * @see CodingErrorAction#REPORT
 */
public class StrictInputStreamReader extends ProxyReader {

  /**
   * <p>
   * A subclass of {@link MalformedInputException} that includes an approximate
   * index range for where malformed input was encountered.
   * </p>
   * <p>
   * The indices are in the style of Java and Python slices, with {@link #start}
   * being inclusive and {@link #end} being exclusive.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.76
   */
  public static class MalformedInputRangeException extends MalformedInputException {
    
    /**
     * <p>
     * The approximate starting index (inclusive).
     * </p>
     * 
     * @since 1.76
     * @see #getStart()
     */
    public final long start;
    
    /**
     * <p>
     * The approximate ending index (exclusive).
     * </p>
     * 
     * @since 1.76
     * @see #getEnd()
     */
    public final long end;
    
    /**
     * <p>
     * Makes a new {@link MalformedInputRangeException} instance.
     * </p>
     * 
     * @param mie
     *          An underlying {@link MalformedInputException} instance.
     * @param start
     *          An approximate starting index (inclusive).
     * @param end
     *          An approximate ending index (exclusive).
     * @since 1.76
     */
    public MalformedInputRangeException(MalformedInputException mie,
                                        long start,
                                        long end) {
      super(mie.getInputLength());
      initCause(mie);
      this.start = start;
      this.end = end;
    }
    
    @Override
    public String getMessage() {
      return String.format("Malformed input between character index %d (0x%X) inclusive and %d (0x%X) exclusive",
                           start, start, end, end);
    }
    
    /**
     * <p>
     * Returns the approximate starting index (inclusive).
     * </p>
     * 
     * @since 1.76
     * @see #start
     */
    public long getStart() {
      return start;
    }
    
    /**
     * <p>
     * Returns the approximate ending index (exclusive).
     * </p>
     * 
     * @since 1.76
     * @see #end
     */
    public long getEnd() {
      return end;
    }
    
  }
  
  /**
   * <p>
   * The number of characters returned so far.
   * </p>
   * 
   * @since 1.76
   * @see #afterRead(int)
   */
  protected long count;
  
  /**
   * <p>
   * The number of characters most recently requested.
   * </p>
   * 
   * @since 1.76
   * @see #beforeRead(int)
   */
  protected int requested;
  
  /**
   * <p>
   * Makes a new instance from a given underlying input stream and its declared
   * character set.
   * </p>
   * 
   * @param underlying
   *          An underlying input stream.
   * @param charset
   *          The underlying input stream's declared character set.
   * @since 1.76
   */
  public StrictInputStreamReader(InputStream underlying,
                                 Charset charset) {
    super(new InputStreamReader(underlying, charset.newDecoder()));
    this.count = 0L;
    this.requested = 0;
  }
  
  @Override
  public long skip(long n) throws IOException {
    long skipped = super.skip(n);
    count += skipped;
    return skipped;
  }

  @Override
  protected void beforeRead(int n) throws IOException {
    super.beforeRead(n);
    requested = n;
  }
  
  @Override
  protected void afterRead(int n) throws IOException {
    super.afterRead(n);
    if (n != IOUtils.EOF) {
      count += n;
      requested = 0;
    }
  }

  @Override
  protected void handleIOException(IOException ioe) throws IOException {
    try {
      throw ioe;
    }
    catch (MalformedInputException mie) {
      throw new MalformedInputRangeException(mie, count, count + requested);
    }
    catch (IOException e) {
      super.handleIOException(e);
    }
  }
  
}
