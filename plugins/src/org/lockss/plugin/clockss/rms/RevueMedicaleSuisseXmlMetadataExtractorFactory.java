/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.rms;

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RevueMedicaleSuisseXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(RevueMedicaleSuisseXmlMetadataExtractorFactory.class);

    Pattern DOI_PAT = Pattern.compile("10[.][0-9a-z]{4,6}/.*");

    Pattern DECORATED_DOI_PAT =
            Pattern.compile("^(" +
                            "(?:doi:)|" +
                            "(?:doi/)|" +
                            "(?:doi\\.org:)|" +
                            "(?:doi\\.org/)|" +
                            "(?:https?://dx\\.doi\\.org/)|" +
                            "(?:https?://doi\\.org/)|" +
                            ")?" + "(" + DOI_PAT.toString() + ")",
                    Pattern.CASE_INSENSITIVE);



    @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new JatsPublishingSourceXmlMetadataExtractor();
  }

  public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }


    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {
      Element root = doc.getDocumentElement();
      DocumentType doctype = doc.getDoctype();

      // 1. Sniff for Erudit: Look for the specific 'idproprio' attribute
      // or the 'lang="fr"' on the root <article> tag.
      if (root.hasAttribute("idproprio") || "fr".equals(root.getAttribute("lang"))) {
          log.debug3("Detected Erudit (French) schema");
          return new EruditFrenchSchemaHelper();
      }

      // 2. Sniff for JATS: Check for the NLM Public ID in the DTD
      if (doctype != null && doctype.getPublicId() != null) {
          if (doctype.getPublicId().contains("-//NLM//DTD")) {
              log.debug3("Detected JATS schema via DTD");
              return new RMSJatsEnglishSchemaHelper();
          }
      }

      // 3. Optional: Fallback check for JATS xmlns if DTD is missing
      if ("http://www.w3.org/1999/xlink".equals(root.getAttribute("xmlns:xlink"))) {
          log.debug3("Detected JATS-like xlink namespace; defaulting to JATS");
          return new JatsPublishingSchemaHelper();
      }

      return null;
    }


    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      String volume = thisAM.get(MetadataField.FIELD_VOLUME);
      if (volume !=null ) {
          log.debug3(String.format("Volme before = %s", volume));
          thisAM.replace(MetadataField.FIELD_VOLUME,volume.replace("N&#x00B0; ", "")
                          .replace("N° ", "")
                  .replace("N\u00b0 ", "")  // Degree Sign
                  .replace("N\u00ba ", "")  // Masculine Ordinal
                  .replace("N\u02da ", "").trim());
      }

      String erudit_raw_doi = thisAM.getRaw("/article/admin/infoarticle/idpublic[@norme='doi']");
      String jats_raw_doi = thisAM.getRaw("/article/front/article-meta/article-id[@pub-id-type = \"doi\"]");

      String doi = null;

      if (erudit_raw_doi != null) {
          Matcher m1 = DECORATED_DOI_PAT.matcher(erudit_raw_doi);
          if (m1.find()) {
              doi = m1.group(2);
              thisAM.put(MetadataField.FIELD_DOI, doi);
          }
      }
      if (jats_raw_doi != null) {
            Matcher m1 = DECORATED_DOI_PAT.matcher(jats_raw_doi);
            if (m1.find()) {
                doi = m1.group(2);
                thisAM.put(MetadataField.FIELD_DOI, doi);
            }
        }
      log.debug3(String.format("rawdoi = %s, jats_raw_doi = %s, doi = %s ", erudit_raw_doi, jats_raw_doi, doi));
    }
  }
}
