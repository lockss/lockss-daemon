/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.filter.pdf;

import java.io.InputStream;

import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.util.*;

/**
 * <p>A default implementation of {@link FilterFactory} that assumes
 * that the name of an {@link OutputDocumentTransform} implementor
 * can be found in the {@link ArchivalUnit}'s attributes under the
 * following key:
 * <code>PdfUtil.PDF_FILTER_FACTORY_HINT_PREFIX + PdfUtil.PDF_MIME_TYPE + DefinableArchivalUnit.AU_FILTER_FACTORY_SUFFIX</code></p>
 * @author Thib Guicherd-Callin
 * @see PdfUtil#PREFIX_PDF_FILTER_FACTORY_HINT
 * @see PdfUtil#PDF_MIME_TYPE
 * @see DefinableArchivalUnit#SUFFIX_HASH_FILTER_FACTORY
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class BasicPdfFilterFactory implements FilterFactory {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public BasicPdfFilterFactory() {}
  
  /* Inherit documentation */
  @Deprecated
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    logger.debug2("Basic PDF filter factory for: " + au.getName());
    OutputDocumentTransform documentTransform = PdfUtil.getOutputDocumentTransform(au);
    if (documentTransform == null) {
      logger.debug2("Unfiltered");
      return in;
    }
    else {
      logger.debug2("Filtered with " + documentTransform.getClass().getName());
      return PdfUtil.applyFromInputStream(documentTransform, in);
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(BasicPdfFilterFactory.class);

}
