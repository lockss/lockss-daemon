/*
 * $Id: ProjectMusePdfFilterFactory.java,v 1.3 2012-05-14 22:46:16 wkwilson Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.io.*;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.common.PDStream;

public class ProjectMusePdfFilterFactory implements FilterFactory {

  public static class FilteringException extends PluginException {
    public FilteringException() { super(); }
    public FilteringException(String msg, Throwable cause) { super(msg, cause); }
    public FilteringException(String msg) { super(msg); }
    public FilteringException(Throwable cause) { super(cause); }
  }

  /**
   * <p>The result is written to a temporary file output stream; this
   * constant defines the size up to which it is handled entirely in
   * memory.</p>
   */
  protected static final int TEMP_FILE_THRESHOLD = 10*1024*1024; // 10MB
  
  protected static final Logger logger = Logger.getLogger("ProjectMusePdfFilterFactory");
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    try {
      PdfDocument pdfDocument = new PdfDocument(in);
      PdfPage frontPage = pdfDocument.getPage(0);
      List tokens = frontPage.getStreamTokens();
      
      int indexOfEt = -1;
      int indexOfBt = -1;
      int progress = 0;
      
      iteration_label: for (int index = tokens.size() - 1 ; index >= 0 ; --index) {
        
        switch (progress) {

          case 0:
          
            if (PdfUtil.isEndTextObject(tokens, index)) {
              indexOfEt = index;
              progress = 1;
            }
            break;
          
          case 1:
            
            if (PdfUtil.isBeginTextObject(tokens, index)) {
              progress = 0;
              indexOfEt = -1;
              break;
            }
            if (PdfUtil.isShowTextGlyphPositioning(tokens, index)) {
              progress = 2;
            }
            break;
          
          case 2:
            
            if (PdfUtil.isBeginTextObject(tokens, index)) {
              progress = 0;
              indexOfEt = -1;
              break;
            }
            Object obj = tokens.get(index);
            if (!(obj instanceof COSArray)) {
              progress = 1;
              break;
            }
            COSArray cosArray = (COSArray)obj;
            internal_label: for (int i = 0 ; i < cosArray.size() ; ++i) {
              if (PdfUtil.matchPdfStringMatches(cosArray.get(i), ".*Access Provided by .*")) {
                progress = 3;
                break internal_label;
              }
            }
            if (progress != 3) {
              progress = 1;
            }
            break;
          
          case 3:
            
            if (PdfUtil.isBeginTextObject(tokens, index)) {
              indexOfBt = index;
              progress = 4;
            }
            break;
            
          case 4:
            
            break iteration_label;
        
        }
        
      }
        
      if (progress == 4) {
        // We have identified that this PDF document has dynamic elements

        // Remove the dynamic elements on the front page
        PDStream resultStream = frontPage.getPdfDocument().makePdStream();
        OutputStream tokenOutputStream = resultStream.createOutputStream();
        ContentStreamWriter tokenWriter = new ContentStreamWriter(tokenOutputStream);
        tokenWriter.writeTokens(tokens, 0, indexOfBt);
        tokenWriter.writeTokens(tokens, indexOfEt + 1, tokens.size());
        frontPage.setContents(resultStream);
        pdfDocument.removeCreationDate();
        pdfDocument.removeModificationDate();
        // Normalize the trailer ID
        COSDictionary trailer = pdfDocument.getTrailer();
        if (trailer != null) {
          // Put bogus ID to prevent autogenerated (variable) ID
          COSArray id = new COSArray();
          id.add(new COSString("12345678901234567890123456789012"));
          id.add(id.get(0));
          trailer.setItem(COSName.getPDFName("ID"), id);
        }
      }

      DeferredTempFileOutputStream pdfOutputStream = new DeferredTempFileOutputStream(TEMP_FILE_THRESHOLD);
      pdfDocument.save(pdfOutputStream);
      pdfDocument.close();
      if (pdfOutputStream.isInMemory()) {
        return new ByteArrayInputStream(pdfOutputStream.getData());
      }
      else {
        File tempFile = null;
        try {
          tempFile = pdfOutputStream.getFile();
          InputStream fileStream = new BufferedInputStream(new FileInputStream(tempFile));
          CloseCallbackInputStream.Callback cb = new CloseCallbackInputStream.Callback() {
            public void streamClosed(Object file) {
              FileUtils.deleteQuietly(((File)file));
            }
          };
          return new CloseCallbackInputStream(fileStream, cb, tempFile);
        }
        catch (FileNotFoundException fnfe) {
          FileUtils.deleteQuietly(tempFile);
          throw new FilteringException("Error while creating a temporary file output stream", fnfe);
        }
        
      }

    }
    catch (IOException ioe) {
      throw new FilteringException("Error thrown by the PDF framework", ioe);
    }

  }
  
}
