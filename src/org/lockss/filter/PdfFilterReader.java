/*
 * $Id: PdfFilterReader.java,v 1.1 2006-07-25 00:15:38 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import java.io.*;

import org.lockss.util.*;

/**
 * <p>A specialized {@link Reader} that gives access to a programmatic
 * representation of a PDF document, for filtering.</p>
 * @author Thib Guicherd-Callin
 * @see PdfTransform
 */
public class PdfFilterReader extends FilterReader {

  protected static Logger logger = Logger.getLogger("PdfFilterReader");

  public PdfFilterReader(Reader reader, PdfTransform pdfTransform) {
    super(reader);
    this.pdfTransform = pdfTransform;
    this.parsed = false;
    this.closed = false;
  }

  protected PdfTransform pdfTransform;

  protected boolean parsed;

  protected boolean closed;

  public synchronized void close() throws IOException {
    this.closed = true;
    super.close();
  }

  public synchronized int read(char[] cbuf, int off, int len) throws IOException {
    try {
      if (closed) {
        throw new IOException("Attempting to read from a closed Reader");
      }
      if (!parsed) {
        parse();
        parsed = true;
      }
      return super.read(cbuf, off, len);
    }
    catch (IOException ioe) {
      close(); // invalidate reader
      throw ioe;
    }
  }

  protected void parse() throws IOException {
    /* Assumes synchronized */
    InputStream pdfInputStream = new ReaderInputStream(in);
    ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
    PdfTransformUtil.parse(pdfInputStream, pdfOutputStream, pdfTransform, logger);
    in = new InputStreamReader(new ByteArrayInputStream(pdfOutputStream.toByteArray()));
  }

}
