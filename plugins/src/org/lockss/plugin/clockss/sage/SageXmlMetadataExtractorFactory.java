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

package org.lockss.plugin.clockss.sage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;



public class SageXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SageXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper SageBooksHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new SageBooksPublishingSourceXmlMetadataExtractor();
  }

  public class SageBooksPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {

      if (SageBooksHelper == null) {
        SageBooksHelper = new SageBooksXmlSchemaHelper();
      }
      return SageBooksHelper;
    }

     


    /* In this case, the filename is the same as the xml filename
     * or, if that's not there could be the print isbn
     * but it might have an _v# after it. 
     * worst case use this very xml file - because it's full text
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String url_string = cu.getUrl();
      String cuBase = FilenameUtils.getFullPath(url_string);
      String cuFileName = FilenameUtils.getBaseName(url_string);

      String pdf_samename = cuBase + "pdf/" + cuFileName + ".pdf";
      String pdf_isbn = cuBase + "pdf/" + oneAM.getRaw(helper.getFilenameXPathKey());
      List<String> returnList = new ArrayList<String>();
      log.debug3("looking for: " + pdf_samename + "," + pdf_isbn + ".pdf," + pdf_isbn + "_v1.pdf," + url_string);
      returnList.add(pdf_samename);
      returnList.add(pdf_isbn + ".pdf");
      returnList.add(pdf_isbn + "_v1.pdf");
      returnList.add(url_string); // fallback - just use this file
      return returnList;
    }
    

  }
}
