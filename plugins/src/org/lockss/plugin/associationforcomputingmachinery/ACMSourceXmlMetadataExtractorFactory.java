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

package org.lockss.plugin.associationforcomputingmachinery;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class ACMSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ACMSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper ACMHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ACMSourceXmlMetadataExtractor();
  }

  public class ACMSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if (ACMHelper != null) {
        return ACMHelper;
      }
      ACMHelper = new ACMXmlSchemaHelper();
      return ACMHelper;
    }
    
    
    /*
     * starting in late Nov 2014 ACM started to change the location of the identified
     * fulltext filenames. 
     * Originally (and occasionally) the listed filename is at the same level as
     * the XML file in which it is named.
     * Now, usually, the listed filename is in a subdirectory of the specific 
     * article_rec article_id name.  
     * That is, if the article_id is 2366543, then that is the name of the subedirectory.
     * In the interest of being flexible - we'll look in both places! 
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      // get the key for a piece of metadata used in building the filename
      String fn_key = helper.getFilenameXPathKey();  
      // the schema doesn't define a filename so don't do a default preEmitCheck
      if (fn_key == null) {
        return null; // no preEmitCheck 
      }
      String filenameValue = oneAM.getRaw(helper.getFilenameXPathKey());
      log.debug3("PreEmit filename is " + filenameValue);
      // we expected a value, but didn't get one...we need to return something
      // for preEmitCheck to fail
      if (filenameValue == null) {
        filenameValue = "NOFILEINMETADATA"; // we expected a value, but got none
      }
      if (filenameValue.endsWith(".html")) {
        filenameValue = filenameValue.replace("\\", "/");
        log.debug3("html filename is now " + filenameValue);
      }
      
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String article_id_directory = oneAM.getRaw(ACMXmlSchemaHelper.ACM_article_id);
      List<String> returnList = new ArrayList<String>();
      // default version is just the filename associated with the key, in this directory
      returnList.add(cuBase + filenameValue);
      // next option is same filename in subdirectory of the article_id
      returnList.add(cuBase + article_id_directory + "/" + filenameValue);
      return returnList;
    }
    

    @Override
    public boolean getDoXmlFiltering() {
      return true;
    }
  }
}
