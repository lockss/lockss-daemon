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

package org.lockss.plugin.hindawi;

import java.io.*;

import org.apache.commons.io.input.ProxyInputStream;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HindawiOctetStreamFilterFactory implements FilterFactory {

  private static final Logger logger = Logger.getLogger(HindawiOctetStreamFilterFactory.class);
  
  protected FilterFactory pdfFilterFactory = new HindawiPdfFilterFactory();

  public static class PdfPeekInputStream extends ProxyInputStream {

    protected boolean isPdf;
    
    protected byte[] buffer;
    
    protected int consumed;
    
    protected int returned;
    
    protected static final int bufferSize = 4;
    
    public PdfPeekInputStream(InputStream in) throws IOException {
      super(in);
      buffer = new byte[bufferSize];
      consumed = super.read(buffer);
      returned = 0;
      isPdf = (   (consumed == bufferSize)
               && (buffer[0] == '%')
               && (buffer[1] == 'P')
               && (buffer[2] == 'D')
               && (buffer[3] == 'F'));
    }
    
    @Override
    public int read() throws IOException {
      if (returned >= consumed) {
        return super.read();
      }
      int ret = buffer[returned];
      ++returned;
      return ret;
    }    

    @Override
    public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (returned >= consumed) {
        return super.read(b, off, len);
      }
      int available = consumed - returned; 
      int toBeProcessed = ((available <= len) ? available : len);
      for (int i = 0 ; i < toBeProcessed ; ++i) {
        b[off + i] = buffer[returned + i];
      }
      returned += toBeProcessed;
      return toBeProcessed;
    }
    
  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    try {
      PdfPeekInputStream pdfpeek = new PdfPeekInputStream(in);
      if (pdfpeek.isPdf) {
        logger.debug2("Detected PDF"); // expected to be PDF
        return pdfFilterFactory.createFilteredInputStream(au, pdfpeek, encoding);
      }
      else {
        logger.debug2("Unfiltered"); // expected to be EPUB
        return pdfpeek;
      }
    }
    catch (IOException ioe) {
      throw new PluginException("Error while peeking into the input stream", ioe);
    }
  }

}
