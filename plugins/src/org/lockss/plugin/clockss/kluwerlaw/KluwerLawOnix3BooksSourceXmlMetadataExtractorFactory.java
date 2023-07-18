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

package org.lockss.plugin.clockss.kluwerlaw;

import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.common.util.StringUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.*;

public class KluwerLawOnix3BooksSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(KluwerLawOnix3BooksSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper Onix3Helper = null;
  private final Map<String,HashSet<String>> allRecordsSet = new HashMap<>();

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new Onix3LongSourceXmlMetadataExtractor();
  }

  public class Onix3LongSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    public void resetRecordsSet(String auid) {
      // we make a hashset for each AU, this is to avoid resetting another AUs
      // hashset in the middle of metadata operation.
      HashSet<String> auidsSet = allRecordsSet.get(auid);
      if (auidsSet != null) {
          auidsSet.clear();
      } else {
        allRecordsSet.put(auid, new HashSet<>());
      }
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (Onix3Helper == null) {
        Onix3Helper = new Onix3BooksSchemaHelper();
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
      List<String> returnList = new ArrayList<>();

      String pdf = cuBase + filenameValue + ".pdf";
      String epub = cuBase + filenameValue + ".epub";

      returnList.add(cuBase + filenameValue + ".pdf");
      returnList.add(cuBase + filenameValue + ".epub");

      log.debug3("File pdf = " + pdf + ", epub = " + epub);

      return returnList;
    }

    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in KluwerLawOnix3BooksSourceXmlMetadataExtractorFactory preEmitCheck");

      List<String> filesToCheck;

      // If no files get returned in the list, nothing to check
      if ((filesToCheck = getFilenamesAssociatedWithRecord(schemaHelper, cu,thisAM)) == null) {
        return true;
      }

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
      String directory = null;

      if (tdbau != null) {
        directory  = tdbau.getParam("directory");
        log.debug3("Suppressing emit of metadata directory = " + directory );
      } else {
        log.debug3("Suppressing emit of metadata in Null extractor tdbau directory is null");
      }

      if ( directory != null && (directory.equals("2023_02"))) {
        log.debug3("Suppressing emit of metadata in Null extractor. contains 2023_02 back content " + directory);

        return true; // Do not check existances of 2023_02 back content folder
      } else {
        log.debug3("Suppressing emit of metadata in Null extractor. Not contains 2023_02 back content " + directory);
      }

      log.debug3("Let parent check existance of file for directory : " + cu.getUrl());

      return super.preEmitCheck(schemaHelper, cu, thisAM);
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
        Map<String,ArticleMetadata> uniqueRecordMap = new HashMap<>();
        HashSet<String> auidsSet = allRecordsSet.get(cu.getArchivalUnit().getAuId());
        if (auidsSet == null) { auidsSet = new HashSet<>(); } // for testing

        // Look at each item in AM list and compare them to a running set of records
        // to only return unique records from this AM
        for ( ArticleMetadata oneAM : allAMs) {
          String deDupRawVal = oneAM.getRaw(deDupKey);
          log.debug3("anArticleMD deDupKey: " + deDupRawVal);
          if (!auidsSet.contains(deDupRawVal)) {
            log.debug3("no record already existed with that raw val");
            uniqueRecordMap.put(deDupRawVal,  oneAM);
            auidsSet.add(deDupRawVal);
          }
        }
        log.debug3("After consolidation, added " + uniqueRecordMap.size() + " for a total of " + allRecordsSet.size() + " records");
        return uniqueRecordMap.values();
      }
      return allAMs;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
    		CachedUrl cu, ArticleMetadata thisAM) {

    	log.debug3("setting publication type in postcook process");
    	// this is a book volume
    	thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_BOOK);
    	thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_BOOKVOLUME);
    }

  }
}
