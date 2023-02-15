/*
Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.clockss.isecs;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(InternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper CrossRefHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new InternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractor();
  }

  public class InternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (CrossRefHelper != null) {
        return CrossRefHelper;
      }
      CrossRefHelper = new InternationalStructuralEngineeringConstructionSocietyCrossRefQuerySchemaHelper();
      return CrossRefHelper;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {
      // we need to convert the external pdf url to the pdf url in the AU.
      String curr_access_url = thisAM.get(MetadataField.FIELD_ACCESS_URL);
      Pattern xml_url_pat = Pattern.compile("/XML/.+\\.xml");
      Pattern pdf_url_pat = Pattern.compile("/.+\\.pdf");
      // match on the XML file path for replacing with the pdf file name
      Matcher xml_mat = xml_url_pat.matcher(cu.getUrl());
      // make sure the pdf url points to a pdf.
      Matcher pdf_mat = pdf_url_pat.matcher(curr_access_url);
      if (xml_mat.find() && pdf_mat.find()) {
        int idx = curr_access_url.lastIndexOf("/");
        if (idx > -1) {
          String pdf_in_au = xml_mat.replaceAll(curr_access_url.substring(idx));
          CachedUrl pdfcu = cu.getArchivalUnit().makeCachedUrl(pdf_in_au);
          if (pdfcu.hasContent()) {
            // replace xml path and filename with pdf filename.
            thisAM.replace(MetadataField.FIELD_ACCESS_URL, pdf_in_au);
          }
        }
      }
    }
  }
}
