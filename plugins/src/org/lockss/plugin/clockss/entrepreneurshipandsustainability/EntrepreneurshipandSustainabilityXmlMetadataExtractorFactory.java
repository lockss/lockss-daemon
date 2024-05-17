/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.entrepreneurshipandsustainability;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.CrossRefSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

public class EntrepreneurshipandSustainabilityXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory{

    private static final Logger log = Logger.getLogger(EntrepreneurshipandSustainabilityXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper EandSSchemaHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
            throws PluginException {
      return new EntrepreneurshipandSustainabilityXmlMetadataExtractor();
    }
    
    public class EntrepreneurshipandSustainabilityXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // Once you have it, just keep returning the same one. It won't change.
            if (EandSSchemaHelper == null) {
                EandSSchemaHelper = new CrossRefSchemaHelper();
                log.info("Setting up schema helper.");
            }
            return EandSSchemaHelper;
        }

        /*
         * One issue will be mapped to one xml file. So, for example, 
         * an xml file will look like this: 10.9770_IRD.2019.1.1.xml
         * and map to the following pdfs: 
         * 10.9770_IRD.2019.1.1(1).pdf, 10.9770_IRD.2019.1.1(2).pdf, 10.9770_IRD.2019.1.1(3).pdf, 
         * 10.9770_IRD.2019.1.1(4).pdf, 10.9770_IRD.2019.1.1(5).pdf
         * The doi matches the pdf filename- the only thing that needs to be updated is the '/'
         * needs to be replaced with '_'.
         */
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());
            log.debug3("CU Base is: " + cuBase);
            String doi = oneAM.getRaw(helper.getFilenameXPathKey());
            doi = doi.replace("/","_").trim();
            log.info("DOI: " + doi);
            String pdfName = cuBase + doi + ".pdf";
            log.info("The pdf is: " + pdfName);
            List<String> returnList = new ArrayList<String>();
            returnList.add(pdfName);
            return returnList;
        }
    }
}
