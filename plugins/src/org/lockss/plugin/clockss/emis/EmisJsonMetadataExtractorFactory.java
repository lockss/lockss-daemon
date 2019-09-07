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
import java.util.ArrayList;
import java.util.List;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceJsonMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceJsonSchemaHelper;
import org.lockss.util.Logger;

public class EmisJsonMetadataExtractorFactory extends SourceJsonMetadataExtractorFactory {
  static Logger log = Logger.getLogger(EmisJsonMetadataExtractorFactory.class);

  private static SourceJsonSchemaHelper jsonPublishingHelper =null;

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
                                   ArticleMetadata thisAM) {
      super.postCookProcess(schemaHelper, cu, thisAM);
    }

    /**
     * A routine used by preEmitCheck to know which files to check for
     * existence.
     * It returns a list of strings, each string is a
     * complete url for a file that could be used to check for whether a cu
     * with that name exists and has content.
     * If the returned list is null, preEmitCheck returns TRUE
     * If any of the files in the list is found and exists, preEmitCheck
     * returns TRUE. It stops after finding one.
     * If the list is not null and no file exists, preEmitCheck returns FALSE
     * The first existing file from the list gets set as the access URL.
     * The child plugin could override preEmitCheck for different results.
     * The base version of this returns the value of the schema helper's value at
     * getFilenameXPathKey in the same directory as the JSON file.
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceJsonSchemaHelper helper,
                                                            CachedUrl cu, ArticleMetadata oneAM) {
      String pdfPath = "";
      String url_string = cu.getUrl();
      int last_slash = url_string.lastIndexOf("/");
      String path = url_string.substring(0, last_slash);
      String pdf = path + "/001.pdf";
      String html = path + "/000.html";

      log.debug3("pdf path: " + pdf + " html path: " + html) ;

      List<String> returnList = new ArrayList<String>();
      returnList.add(pdf);
      returnList.add(html);

      if(url_string.indexOf(".json") > -1 && url_string.indexOf("mif.json") == -1) {
        returnList.remove(url_string);
      }
      return returnList;
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
