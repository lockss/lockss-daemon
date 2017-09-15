/*
 * $Id$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.stanforduniversitypress;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix2BooksSchemaHelper;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;


public class SUPressOnixXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SUPressOnixXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper Onix2Helper = null;
  private static SourceXmlSchemaHelper Onix3Helper = null;
 
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new IopOnixXmlMetadataExtractor();
  }

  public static class IopOnixXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }
    
    
/*
 * SUPress uses at least Onix2 long and short
 * Leave in support for Onix3, just in case they start to deliver that
 */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {
      // Once you have it, just keep returning the same one. It won't change.
      String system = null;
      String release = null;
      
      if (doc == null) return null;
      DocumentType doctype = doc.getDoctype();
      if (doctype != null) {
        system  = doctype.getSystemId();
        log.debug3("DOCTYPE URI: " + doctype.getSystemId());
      }
      Element root = doc.getDocumentElement();
      if (root != null) {
        release = root.getAttribute("release");
        if (release != null) {
          log.debug3("releaseNum : " + release);
        }
      }
      if( ((system != null) && system.contains("/onix/2")) ||
           (( release != null) && release.startsWith("2")) ) {
        if (Onix2Helper == null) {
          Onix2Helper = new Onix2BooksSchemaHelper();
        }
        return Onix2Helper;
      } else if ( ((system != null) && system.contains("/onix/3")) ||
        (( release != null) && release.startsWith("3")) ) {
        if (Onix3Helper == null) {
          Onix3Helper = new Onix3BooksSchemaHelper();
        }
        return Onix3Helper;
      } else {
        log.warning("guessing at XML schema - using ONIX2");
      }
      if (Onix2Helper == null) {
        Onix2Helper = new Onix2BooksSchemaHelper();
      }
      return Onix2Helper;
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

  }
}
