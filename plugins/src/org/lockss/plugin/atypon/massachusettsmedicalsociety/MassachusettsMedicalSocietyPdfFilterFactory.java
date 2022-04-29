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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.atypon.BaseAtyponPdfFilterFactory;
import org.lockss.util.Logger;

import java.io.FileInputStream;
import java.util.List;

/**
 * <p>
 * This class replaces equivalent functionality in a class formerly known as
 * MassachusettsMedicalSocietyPdfTransform. This PdfFilterFactory differs from
 * the BaseAtypon parent by additionally normalizing all token streams.
 * Additionally, a check is performed to search the first page for a characteristic title.
 * if this title is found, the transform/filtering is not performed.
 * </p>
 */
public class MassachusettsMedicalSocietyPdfFilterFactory extends BaseAtyponPdfFilterFactory {

  private static final Logger log = Logger.getLogger(MassachusettsMedicalSocietyPdfFilterFactory.class);

  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    boolean doTransform = true;
    PdfTokenStream pt = pdfDocument.getPage(0).getPageTokenStream();
    List<PdfToken> tokens = pt.getTokens();
    for (PdfToken tok: tokens) {
      if (tok.isString()) {
        String str = tok.getString();
        log.info(str);
        if (str.matches("\\s*Protocol\\s*") ||
            str.matches("\\s*Supplementary Appendix\\s*") ||
            str.matches("\\s*ICMJE Form for Disclosure of Potential Conflicts of Interest\\s*")) {
          doTransform = false;
          log.debug3("Skipping the transform, found a '" + str + "'");
          break;
        }
      }
    }
    if (doTransform) {
      doBaseTransforms(pdfDocument);
      PdfUtil.normalizeAllTokenStreams(pdfDocument);
    }
  }
  public static void main(String[] args) throws Exception {
    String[] fileStrs = {
        //"nejmoa1816885_disclosures.pdf",
        //"nejmoa1817083_protocol(2).pdf",
        //"nejmoa1816897_protocol.pdf",
        //"nejmoa1805435_appendix.pdf"
    };
    for (String fileStr : fileStrs) {
      FilterFactory fact = new MassachusettsMedicalSocietyPdfFilterFactory();
      fact.createFilteredInputStream(null, new FileInputStream(fileStr), null);
    }
  }
  
}
