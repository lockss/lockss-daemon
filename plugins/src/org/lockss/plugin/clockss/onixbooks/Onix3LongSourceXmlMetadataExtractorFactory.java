/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.onixbooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.common.util.StringUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class Onix3LongSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(Onix3LongSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper Onix3Helper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new Onix3LongSourceXmlMetadataExtractor();
  }

  public class Onix3LongSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (Onix3Helper == null) {
        Onix3Helper = new Onix3LongSchemaHelper();
      }
      return Onix3Helper;
    }


    /* In this case, build up the filename from just the isbn13 value of the AM 
     * with suffix either .pdf or .epub
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String filenameValue = oneAM.getRaw(helper.getFilenameXPathKey());
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      List<String> returnList = new ArrayList<String>();
      returnList.add(cuBase + filenameValue + ".pdf");
      returnList.add(cuBase + filenameValue + ".epub");
      return returnList;
    }

    /*
     * For this plugin, if the schema sets the deDupKey use that to determine
     * if two records should be combined.
     * When combining one or more records, if the consolidationXPathKey is set,
     * append the raw values of that key in the one combined record.
     */
    @Override
    protected Collection<ArticleMetadata> modifyAMList(SourceXmlSchemaHelper helper, CachedUrl cu,
        List<ArticleMetadata> allAMs) {


      String deDupKey = helper.getDeDuplicationXPathKey(); 
      boolean deDuping = (!StringUtils.isEmpty(deDupKey));
      if (deDuping) {
        Map<String,ArticleMetadata> uniqueRecordMap = new HashMap<String,ArticleMetadata>();
        String consolidationXPathKey = helper.getConsolidationXPathKey();

        // Look at each item in AM list and put those with unique values
        // associated with the deDupKey in a map of unique records.
        // For duplicates, use the consolidateRecords() method to combine
        for ( ArticleMetadata oneAM : allAMs) {
          String deDupRawVal = oneAM.getRaw(deDupKey); 
          ArticleMetadata prevDupRecord = uniqueRecordMap.get(deDupRawVal);
          if (prevDupRecord == null) {
            log.debug3("no record already existed with that raw val");
            uniqueRecordMap.put(deDupRawVal,  oneAM);
          } else if (!StringUtils.isEmpty(consolidationXPathKey)){
            log.debug3("combining two AM records");
            // ArticleMetadata.putRaw appends if a value(s) exists
            prevDupRecord.putRaw(consolidationXPathKey,  oneAM.getRaw(consolidationXPathKey));
          } 
        }
        log.debug3("After consolidation, " + uniqueRecordMap.size() + "records");
        return uniqueRecordMap.values();
      }
      return allAMs;
    }

  }
}
