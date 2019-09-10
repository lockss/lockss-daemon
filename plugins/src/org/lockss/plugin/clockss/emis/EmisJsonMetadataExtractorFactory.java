/*
 * Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 */

package org.lockss.plugin.clockss.emis;

import com.jayway.jsonpath.DocumentContext;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceJsonMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceJsonSchemaHelper;
import org.lockss.util.Logger;

public class EmisJsonMetadataExtractorFactory extends SourceJsonMetadataExtractorFactory {

  static Logger log = Logger.getLogger(EmisJsonMetadataExtractorFactory.class);

  private static SourceJsonSchemaHelper jsonPublishingHelper = null;

  /**
   * Create a FileMetadataExtractor
   *
   * @param target the purpose for which metadata is being extracted
   * @param contentType the MIME type from which to extract URLs
   */
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new EmisSourceJsonMetadataExtractor();
  }

  public class EmisSourceJsonMetadataExtractor extends SourceJsonMetadataExtractor {

    @Override
    protected void postCookProcess(SourceJsonSchemaHelper schemaHelper, CachedUrl cu,
                                   ArticleMetadata thisAm) {
      log.debug3("in EMIS postCookProcess");
      String substance_urls = thisAm.getRaw(EmisSourceJsonSchemaHelper.mif_aurl);
      if (substance_urls != null) {
        // this field is a series of ';' seperated values we split them and assign as needed
        String[] urls = substance_urls.split(";");
        // per email first field in list
        thisAm.replace(MetadataField.FIELD_ACCESS_URL, urls[0].trim());
      }

      if(thisAm.hasInvalidValue(MetadataField.FIELD_ISSN)) {
        String raw_issn = thisAm.getRaw(EmisSourceJsonSchemaHelper.mif_issn);
        if(raw_issn.length() > 8) {
          String issn = raw_issn.substring(0,9);
          thisAm.replace(MetadataField.FIELD_ISSN, issn);
        }
      }
    }


    @Override
    protected SourceJsonSchemaHelper setUpSchema(CachedUrl cu, DocumentContext doc) {
      // Once you have it, just keep returning the same one. It won't change.
      if (jsonPublishingHelper == null) {
        jsonPublishingHelper = new EmisSourceJsonSchemaHelper();
      }
      return jsonPublishingHelper;
    }
  }

}
