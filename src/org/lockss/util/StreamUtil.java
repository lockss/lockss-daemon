/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.zip.*;
import java.security.MessageDigest;

import org.lockss.daemon.LockssWatchdog;

/**
 * This is a class to contain generic stream utilities
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class StreamUtil {

  static Logger log = Logger.getLogger("StreamUtil");

  private static final int BUFFER_SIZE = 256;
  static final int COPY_WDOG_CHECK_EVERY_BYTES = 1024 * 1024;

  /**
   * Copy bytes from an InputStream to an Outputstream until EOF.  The
   * OutputStream is flushed, neither stream is closed.
   * @param is input stream
   * @param os output stream
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os) throws IOException {
    return copy(is, os, -1);
  }

  /**
   * Copy bytes from an InputStream to an Outputstream until EOF,
   * occasionally poking the watchdog.  The OutputStream is flushed,
   * neither stream is closed.
   * @param is input stream
   * @param os output stream
   * @param wdog if non-null, a LockssWatchdog that will be poked at
   * approximately twice its required rate.
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os,
			  LockssWatchdog wdog) throws IOException {
    return copy(is, os, -1, wdog);
  }

  /**
   * Copy up to len bytes from an InputStream to an Outputstream.  The
   * OutputStream is flushed, neither stream is closed.
   * @param is input stream
   * @param os output stream
   * @param len number of bytes to copy; -1 means copy to EOF
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os, long len)
      throws IOException {
    return copy(is, os, len, null);
  }

  /**
   * Copy up to len bytes from InputStream to Outputstream, occasionally
   * poking a watchdog.  The OutputStream is flushed, neither stream is
   * closed.
   * @param is input stream
   * @param os output stream
   * @param len number of bytes to copy; -1 means copy to EOF
   * @param wdog if non-null, a LockssWatchdog that will be poked at
   * approximately twice its required rate.
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os, long len,
			  LockssWatchdog wdog)
      throws IOException {
    return copy(is, os, len, wdog, false, null);
  }

  /**
   * Copy up to len bytes from InputStream to Outputstream, occasionally
   * poking a watchdog.  The OutputStream is flushed, neither stream is
   * closed.
   * @param is input stream
   * @param os output stream
   * @param len number of bytes to copy; -1 means copy to EOF
   * @param wdog if non-null, a LockssWatchdog that will be poked at
   * approximately twice its required rate.
   * @param wrapExceptions if true, exceptions that occur while reading
   * from the input stream will be wrapped in a {@link
   * StreamUtil#InputException} and exceptions that occur while writing to
   * or closing the output stream will be wrapped in a {@link
   * StreamUtil#OutputException}.
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os, long len,
		  LockssWatchdog wdog, boolean wrapExceptions) 
	  throws IOException {
    return copy(is, os, len, wdog, wrapExceptions, null);
  }
  
  /**
   * Copy up to len bytes from InputStream to Outputstream, occasionally
   * poking a watchdog.  The OutputStream is flushed, neither stream is
   * closed.
   * @param is input stream
   * @param os output stream
   * @param len number of bytes to copy; -1 means copy to EOF
   * @param wdog if non-null, a LockssWatchdog that will be poked at
   * approximately twice its required rate.
   * @param wrapExceptions if true, exceptions that occur while reading
   * from the input stream will be wrapped in a {@link
   * StreamUtil#InputException} and exceptions that occur while writing to
   * or closing the output stream will be wrapped in a {@link
   * StreamUtil#OutputException}.
   * @param md a MessageDigest algorithm that, when not null, receives all input
   * @return number of bytes copied
   * @throws IOException
   */
  public static long copy(InputStream is, OutputStream os, long len,
			  LockssWatchdog wdog, boolean wrapExceptions, MessageDigest md)
      throws IOException {
    if (is == null || os == null || len == 0) {
      return 0;
    }
    long wnext = 0, wcnt = 0, wint = 0;
    if (wdog != null) {
      wint = wdog.getWDogInterval() / 4;
      wnext = TimeBase.nowMs() + wint;
    }
    byte[] buf = new byte[BUFFER_SIZE];
    long rem = (len > 0) ? len : Long.MAX_VALUE;
    long ncopied = 0;
    int nread;
    while (rem > 0) {
      try {
	nread = is.read(buf, 0, rem > BUFFER_SIZE ? BUFFER_SIZE : (int)rem);
      } catch (IOException e) {
	if (wrapExceptions) {
	  throw new InputException(e);
	} else {
	  throw e;
	}
      }
      if (nread <= 0) {
	break;
      }
      if (md != null) {
        md.update(buf, 0, nread);
      }
      try {
	os.write(buf, 0, nread);
      } catch (IOException e) {
	if (wrapExceptions) {
	  throw new OutputException(e);
	} else {
	  throw e;
	}
      }
      ncopied += nread;
      rem -= nread;
      if (wdog != null) {
	if ((wcnt += nread) > COPY_WDOG_CHECK_EVERY_BYTES) {
	  log.debug2("checking: "+ wnext);
	  if (TimeBase.nowMs() > wnext) {
	    log.debug2("poke: " + wcnt);
	    wdog.pokeWDog();
	    wnext = TimeBase.nowMs() + wint;
	  }
	  wcnt = 0;
	}
      }
    }
    try {
      os.flush();
    } catch (IOException e) {
      if (wrapExceptions) {
	throw new OutputException(e);
      } else {
	throw e;
      }
    }
    return ncopied;
  }

  /**
   * This function copies the contents of a Reader to a Writer
   * It buffers the copying, and closes neither.
   * @param reader reader
   * @param writer writer
   * @return number of charscopied
   * @throws IOException
   */
  public static long copy(Reader reader, Writer writer) throws IOException {
    if (reader == null || writer == null) {
      return 0;
    }
    long totalCharCount = 0;
    char[] chars = new char[BUFFER_SIZE];
    int count;
    while ((count = reader.read(chars)) > 0) {
      totalCharCount += count;
      writer.write(chars, 0, count);
    }
    writer.flush();
    return totalCharCount;
  }

  /** Return the number of bytes that can be read from the InputStream.
   * The stream is consumed but not closed. */
  public static long countBytes(InputStream is) throws IOException {
    return copy(is, new org.apache.commons.io.output.NullOutputStream());
  }

  /** Return the number of characters that can be read from the Reader
   * The reader is consumed but not closed. */
  public static long countChars(Reader reader) throws IOException {
    return copy(reader, new org.apache.commons.io.output.NullWriter());
  }

  public static class InputException extends IOException {
    public InputException(IOException cause) {
      super(cause);
    }

    public IOException getIOCause() {
      return (IOException)getCause();
    }
  }

  public static class OutputException extends IOException {
    public OutputException(IOException cause) {
      super(cause);
    }

    public IOException getIOCause() {
      return (IOException)getCause();
    }
  }

  /** Read size bytes from stream into buf.  Keeps trying to read until
   * enough bytes have been read or EOF or error.
   * @param ins stream to read from
   * @param buf buffer to read into
   * @param size number of bytes to read
   * @return number of bytes read, which will be less than size iff EOF is
   * reached
   * @throws IOException
   */
  public static int readBytes(InputStream ins, byte[] buf, int size)
      throws IOException {
    int off = 0;
    while ( off < size) {
      int nread = ins.read(buf, off, size - off);
      if (nread == -1) {
	return off;
      }
      off += nread;
    }
    return off;
  }

  /** Read size chars from reader into buf.  Keeps trying to read until
   * enough chars have been read or EOF or error.
   * @param reader reader to read from
   * @param buf buffer to read into
   * @param size number of chars to read
   * @return number of chars read, which will be less than size iff EOF is
   * reached
   * @throws IOException
   */
  public static int readChars(Reader reader, char[] buf, int size)
      throws IOException {
    int off = 0;
    while (off < size) {
      int nread = reader.read(buf, off, size - off);
      if (nread == -1) {
	return off;
      }
      off += nread;
    }
    return off;
  }

  /** Read from two input streams and compare their contents.  The streams
   * are not closed, and may get left at any position.
   * @param ins1 1st stream
   * @param ins2 2nd stream
   * @return true iff streams have same contents and reach EOF at the same
   * point.
   * @throws IOException
   */
  public static boolean compare(InputStream ins1, InputStream ins2)
      throws IOException {
    byte[] b1 = new byte[BUFFER_SIZE];
    byte[] b2 = new byte[BUFFER_SIZE];
    while (true) {
      int len1 = readBytes(ins1, b1, BUFFER_SIZE);
      int len2 = readBytes(ins2, b2, BUFFER_SIZE);
      if (len1 != len2) return false;
      if (len1 == 0) return true;
      for (int ix = 0; ix < len1; ix++) {
	if (b1[ix] != b2[ix]) return false;
      }
    }
  }

  /** Return an InputStream that's guaranteed to be reset()able.  If
   * the supplied InputStream supports mark() and reset(), return it,
   * else wrap it in a BufferedInputStream.
   */
  public static InputStream getResettableInputStream(InputStream ins)
      throws IOException {
    if (!ins.markSupported()) {
      return new BufferedInputStream(ins);
    }
    return ins;
  }

  /** Return an InputStream that uncompresses the data on the input stream
   * (normally an HTTP response stream) according to the contentEncoding
   * @param instr raw InputStream
   * @param contentEncoding value of HTTP Content-Encoding: header
   * @return The wrapped stream, or the original stream if contentEncoding
   * is null or "identity"
   * @throws UnsupportedEncodingException
   */
  public static InputStream getUncompressedInputStream(InputStream instr,
						       String contentEncoding)
      throws IOException, UnsupportedEncodingException {
    InputStream res;
    if (StringUtil.isNullString(contentEncoding) ||
	contentEncoding.equalsIgnoreCase("identity")) {
      res = instr;
    } else if (contentEncoding.equalsIgnoreCase("gzip") ||
	contentEncoding.equalsIgnoreCase("x-gzip")) {
      log.debug3("Wrapping in GZIPInputStream");
      res = new GZIPInputStream(instr);
    } else if (contentEncoding.equalsIgnoreCase("deflate")) {
      log.debug3("Wrapping in InflaterInputStream");
      res = new InflaterInputStream(instr);
    } else {
      throw new UnsupportedEncodingException(contentEncoding);
    }
    return res;
  }

  /** Return an InputStream that uncompresses the data on the input stream
   * (normally an HTTP response stream) according to the contentEncoding
   * (see {@link #getUncompressedInputStream(InputStream, String)}).  If
   * decompression fails (e.g., because instr stream wasn't actually
   * compressed), a stream equivalent to (but not == to) the original
   * stream is returned.  The client may test identity of the result to
   * determine whether the characteristics of the stream *may* have changed.
   * @param instr raw InputStream
   * @param contentEncoding value of HTTP Content-Encoding: header
   * @param label A string (e.g., URL) to include in the logged warning if
   * decompression fails
   * @return The wrapped stream, or the original stream if contentEncoding
   * is null or "identity", or a new stream with the same content as the
   * original if decompression fails.
   */
  public static InputStream
    getUncompressedInputStreamOrFallback(InputStream instr,
					 String contentEncoding,
					 String label) {
    if (StringUtil.isNullString(contentEncoding) ||
	contentEncoding.equalsIgnoreCase("identity")) {
      return instr;
    }
    InputStream bin = new BufferedInputStream(instr);
    bin.mark(1024);
    try {
      InputStream res =
	StreamUtil.getUncompressedInputStream(bin, contentEncoding);
      if (contentEncoding.equalsIgnoreCase("deflate")) {
	// InflaterInputStream doesn't throw on bad input until first byte
	// is read.  (GZIPInputStream throws on construction.)
	res = new BufferedInputStream(res);
	res.mark(1);
	res.read();
	res.reset();
      }
      return res;
    } catch (IOException e) {
      log.warning("Decompression (" + contentEncoding +
		  ") failed, returning raw stream: " + label,
		  e);
      try {
	bin.reset();
	return bin;
      } catch (IOException e2) {
	log.warning("Reset (after decompression error) failed", e2);
	throw new RuntimeException("Internal error: please report \"Insufficient buffering for reset\".");
      }
    }
  }

  /** Return a Reader that reads from the InputStream.  If the specified
   * encoding is not found, tries {@link Constants#DEFAULT_ENCODING}.  If
   * the supplied InputStream is a ReaderInputStream, returns the
   * underlying Reader.
   * @param in the InputStream to be wrapped
   * @param encoding the charset
   */
  public static Reader getReader(InputStream in, String encoding) {
    if (in instanceof ReaderInputStream) {
      ReaderInputStream ris = (ReaderInputStream)in;
      return ris.getReader();
    }
    if (encoding == null) {
      encoding = Constants.DEFAULT_ENCODING;
    }
    try {
      return new InputStreamReader(in, encoding);
    } catch (UnsupportedEncodingException e1) {
      log.error("No such encoding: " + encoding + ", trying " +
		Constants.DEFAULT_ENCODING);
      try {
	return new InputStreamReader(in, Constants.DEFAULT_ENCODING);
      } catch (UnsupportedEncodingException e2) {
	log.critical("Default encoding not found: " +
		     Constants.DEFAULT_ENCODING);
	throw new RuntimeException(("UnsupportedEncodingException for both " +
				    encoding + " and " +
				    Constants.DEFAULT_ENCODING),
				   e1);
      }
    }
  }

  /**
   * Write to stream a byte-order mark (BOM) for Excel, helping it to recognise UTF-8.
   * Works and is necessary for Excel in Windows, not recognised in Excel on Mac.
   * @param os the output stream to write to
   * @throws IOException
   */
  public static void writeUtf8ByteOrderMark(OutputStream os) throws IOException {
    os.write(0xEF);   // 1st byte of BOM
    os.write(0xBB);
    os.write(0xBF);   // last byte of BOM
  }

  /** Wrapper InputStream which swallows calls to <code>close()</code>,
   * leaving the wrapped stream open */


  public static class IgnoreCloseInputStream extends FilterInputStream {
    public IgnoreCloseInputStream(InputStream stream) {
      super(stream);
    }
    public void close() throws IOException {
      // ignore
    }
  }


}
