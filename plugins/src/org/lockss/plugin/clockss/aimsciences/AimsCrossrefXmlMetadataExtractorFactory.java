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

package org.lockss.plugin.clockss.aimsciences;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.CrossRefSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;



/*
 * An extractor for an XML file using the schmea for delivering data to CrossRef
 * <doi_batch
 *   <head>...</head>
 *   <body>
 *     <journal>
 *        <journal_issue
 *           <journal_article
 */

public class AimsCrossrefXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(AimsCrossrefXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper AimsCRPublishingHelper = null;
  private static final String AIMS_PUBLISHER = "American Institute of Mathematical Sciences";

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new AimsCRXmlMetadataExtractor();
  }

  public class AimsCRXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (AimsCRPublishingHelper == null) {
          AimsCRPublishingHelper = new CrossRefSchemaHelper();
      }
      return AimsCRPublishingHelper;
    }
    


    /* In this case, the filename is found using the doi as directory levels to "paper.pdf"
     * so in foo.zip with a doi of '10.3934/proc.2015.0085" defined in 
     * foo.zip!/crossref.xml
     *     foo.zip!/10.3934/proc.2015.0085/paper.pdf
     * but we've also seen this variation 
     *     foo.zip!/proc.2015.0085.pdf
     * which is at the same level as the crossref.xml and uses the 2nd part of the doi as filename
     * 
     * 2018 the file naming changed completely. 
     * Now the PDF filename is
     * doi_first/JID.YEAR.ISSUE.FIRSTPAGE/Paper.pdf
     * 
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String doiValue = oneAM.getRaw(helper.getFilenameXPathKey());
      String doiSecond = StringUtils.substringAfter(doiValue, "/");
      String doiFirst = StringUtils.substringBefore(doiValue, "/");
      //2017 options
      String pdfName = cuBase + doiValue + "/paper.pdf";
      String altName = cuBase + doiSecond + ".pdf";
      //2018 options
      String pubJID = oneAM.getRaw(CrossRefSchemaHelper.pub_abbrev);
      String pubJIDAlt = oneAM.getRaw(CrossRefSchemaHelper.pub_abbrev).toUpperCase();
      String pubYear = oneAM.getRaw(CrossRefSchemaHelper.pub_year);
      String pubIssue = oneAM.getRaw(CrossRefSchemaHelper.pub_issue);
      String artPage = oneAM.getRaw(CrossRefSchemaHelper.art_sp);
      String name2018 = cuBase + doiFirst + "/" + pubJID + "." + pubYear + "." + pubIssue + "." + artPage + "/Paper.pdf";
      String name2018Alt = cuBase + doiFirst + "/" + pubJIDAlt + "." + pubYear + "." + pubIssue + "." + artPage + "/Paper.pdf";
      String name2018Alt2 = cuBase + doiFirst + "/" + pubJID + "." + pubYear + "." + pubIssue + "." + artPage + "/paper.pdf";
      String name2018Alt3 = cuBase + doiFirst + "/" + pubJIDAlt + "." + pubYear + "." + pubIssue + "." + artPage + "/paper.pdf";
      log.debug3("looking for: " + pdfName + " or " + altName + " or " + name2018 +  " or "
              + name2018Alt + " or " + name2018Alt2 +  " or " + name2018Alt3);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      returnList.add(altName);
      returnList.add(name2018);
      returnList.add(name2018Alt);
      returnList.add(name2018Alt2);
      returnList.add(name2018Alt3);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in AIMS postCookProcess");
      String pname = thisAM.get(MetadataField.FIELD_PUBLISHER);
      if (!(AIMS_PUBLISHER.equals(pname))) {
    	  	// the CrossRef schema helper cooks this so be sure
            thisAM.replace(MetadataField.FIELD_PUBLISHER,AIMS_PUBLISHER);
        }
    }

  }
}
