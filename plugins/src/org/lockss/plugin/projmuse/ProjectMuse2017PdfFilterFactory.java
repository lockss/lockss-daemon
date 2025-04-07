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
      // Watermark found in 2025: "Access provided by Stanford Libraries (31 May 2018 09:30 GMT)"
      Pattern.compile("Access provided at .+ from ");

  protected static final Pattern WATERMARK_LINE_2 = 
      Pattern.compile("\\[[0-9.]+\\] +Project +MUSE +\\([0-9-]+");

  protected static final Pattern WATERMARK_LINE_3 = 
      Pattern.compile("Access provided by");

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

    PdfPage pdfPage = pdfDocument.getPage(0);
    List<PdfTokenStream> allTokenStream = pdfPage.getAllTokenStreams();
    for (PdfTokenStream pageTokenStream : allTokenStream) {
      PageWorker pw = new PageWorker();
      pw.process(pageTokenStream);
      if(pw.getResult()){
        pdfDocument.removePage(0);
        break;
      }
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

  public static class PageWorker extends PdfTokenStreamStateMachine {
     
    public PageWorker() {
      super(log);
    }
    
    @Override
    public void state0() throws PdfException {
      if (isBeginTextObject()) {
        setState(1);
      }
    }
    
    @Override
    public void state1() throws PdfException {
      if (isShowTextFind(WATERMARK_LINE_1) ||
          isShowTextFind(WATERMARK_LINE_2) ||
          isShowTextFind(WATERMARK_LINE_3) ||
          isShowTextGlyphPositioningFind(WATERMARK_LINE_1) ||
          isShowTextGlyphPositioningFind(WATERMARK_LINE_2) ||
          isShowTextGlyphPositioningFind(WATERMARK_LINE_3)) {
        setState(2);
      }
      else if (isEndTextObject()) { 
        setState(0);
      }
    }
    
    @Override
    public void state2() throws PdfException {
      if (isEndTextObject()) {
        setResult(true);
        stop(); 
      }
    }
  
  }

  public static void main(String[] args) throws Exception {
    
    //Reads in a pdf and applies the filters writing the contents to a new file <fileStr>.bin
     
    String[] fileStrs = {
        "/Users/crc10/Desktop/ProjMuse_delta1.pdf"
    };
    for (String fileStr : fileStrs) {
      FilterFactory fact = new ProjectMuse2017PdfFilterFactory();
      IOUtils.copy(fact.createFilteredInputStream(null, new FileInputStream(fileStr), null),
          new FileOutputStream(fileStr + ".bin"));
    }
  }
}
