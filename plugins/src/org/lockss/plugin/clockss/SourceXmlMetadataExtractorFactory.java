/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import java.io.IOException;
import java.util.*;

import javax.xml.xpath.XPathExpressionException;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 *  A factory to create an generic SourceXmlMetadataExtractor
 *  @author alexohlson
 */
public abstract class SourceXmlMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SourceXmlMetadataExtractorFactory.class);


  /**
   * The generic class to handle metadata extraction for source content
   * that uses XML with a set schema.
   * A specific plugin must subclass this. While it might inherit and use
   * the vast majority, at a minimum it must define the schema by overriding
   * the "setUpSchema()" method 
   * @author alexohlson
   *
   */
  public static abstract class SourceXmlMetadataExtractor 
  implements FileMetadataExtractor {


    /**
     *  Look at the AU to determine which type of XML will be processed and 
     *  then create an XPathXmlMetadataParser to handle the specified XML files
     *  by using the appropriate helper to pass information to the parser.
     *  Take the resulting list of ArticleMetadata objects, 
     *  optionaly consolidate it to remove redundant records  
     *  and then check for content file existence based on file naming
     *  information set up by the helper before cooking and emitting
     *  the metadata.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      try {

        SourceXmlSchemaHelper schemaHelper;
        // 1. Create the xmlParser without setting a schema yet 
        // XPathXmlMetadataParser is not thread safe, must be called each time
        XPathXmlMetadataParser xmlParser = createXpathXmlMetadataParser();             
        // 2. create an XML document tree using the parser
        Document xmlDoc = xmlParser. createDocumentTree(cu);
        // 3. Set an xml parsing schema on the parser before extracting metadata
        // do this *after* creating the document tree in order to handle those cases
        // where one publisher/plugin handles schema variants (eg. books vs journals)
        // either based on information in the URL or the Document
        if ((schemaHelper = setUpSchema(cu, xmlDoc)) == null) {
          log.debug("Unable to set up XML schema. Cannot extract from XML");
          throw new PluginException("XML schema not set up for " + cu.getUrl());
        }     
        xmlParser.setXmlParsingSchema(schemaHelper.getGlobalMetaMap(), 
                schemaHelper.getArticleNode(), 
                schemaHelper.getArticleMetaMap());
        // 4. Gather all the metadata in to a list of AM records
        List<ArticleMetadata> amList = xmlParser.extractMetadataFromDocument(target,xmlDoc); 

        //5. Optional consolidation of duplicate records within one XML file
        // a child plugin can leave the default (no deduplication) or 
        // AMCollection pointing to just a subset of the full
        // AM list
        // This routine used to be called conslidateAMList(schemaHelper, amList) which is 
        // now deprecated in favor of this more general method which takes a cu as well.
        Collection<ArticleMetadata> AMCollection = modifyAMList(schemaHelper, cu, amList);

        // 6.. check, cook, and emit every item in resulting AM collection (list)
        for ( ArticleMetadata oneAM : AMCollection) {
          if (preEmitCheck(schemaHelper, cu, oneAM)) {
            oneAM.cook(schemaHelper.getCookMap());
            postCookProcess(schemaHelper, cu, oneAM); // hook for optional processing
            emitter.emitMetadata(cu,oneAM);
          }
        }

      } catch (XPathExpressionException e) {
        log.debug3("Xpath expression exception:",e);
      } catch (SAXException ex) {
        handleSAXException(cu, emitter, ex);
      } catch (IOException ex) {
        handleIOException(cu, emitter, ex);
      }


    }
    
    // overrideable method for creating the parser
    // this is used by plugins that need to create a parser that filters
    // sgml to legit xml as they are read in line by line.
    protected XPathXmlMetadataParser createXpathXmlMetadataParser() {
      return new XPathXmlMetadataParser(getDoXmlFiltering());
    }

    // Overrideable method for plugins that want to catch and handle
    // a problem in the XML file
    protected void handleIOException(CachedUrl cu, Emitter emitter, IOException ex) {
      // Add an alert or more significant warning here
      log.siteWarning("IO exception loading XML file", ex);
    }

    // Overrideable method for plugins that want to catch and handle
    // a SAX parser problem in the XML file
    protected void handleSAXException(CachedUrl cu, Emitter emitter, SAXException ex) {
      // Add an alert or more significant warning here
      log.siteWarning("SAX exception loading XML file", ex);

    }

 
    /**
     * Verify that a content file exists described in the xml record actually
     * exists based on the XML path location.This also works even if content is 
     * zipped.<br/>
     * The schema helper defines a filename prefix (which could include path
     * information for a subdirectory or sibling directory) as well as a 
     * possible filename component based on a record item (using filenameXPathKey) 
     * and possibly multiple suffixes to handle filetype options.
     * If filenamePrefix, filenameXPathKey and filenameSuffixes are all null, 
     * no preEmitCheck occurs.<br/>
     * For more complicated situations, a child  might want to override this 
     * function entirely.
     * @param schemaHelper for thread safe access to schema specific information 
     * @param cu
     * @param thisAM
     * @return
     */
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in SourceXmlMetadataExtractor preEmitCheck");
      
      List<String> filesToCheck;

      // If no files get returned in the list, nothing to check
      if ((filesToCheck = getFilenamesAssociatedWithRecord(schemaHelper, cu,thisAM)) == null) {
        return true;
      }
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;
      for (int i=0; i < filesToCheck.size(); i++) 
      { 
        fileCu = B_au.makeCachedUrl(filesToCheck.get(i));
        log.debug3("Check for existence of " + filesToCheck.get(i));
        if(fileCu != null && (fileCu.hasContent())) {
          // Set a cooked value for an access file. Otherwise it would get set to xml file
          thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
          return true;
        }
      }
      log.debug3("No file exists associated with this record");
      return false; //No files found that match this record
    }

    // a routine to allow a child to add in some post-cook processing for each
    // AM record (eg. "putifbetter"
    // Default is to do nothing
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in SourceXmlMetadataExtractor postEmitProcess");
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
     * getFilenameXPathKey in the same directory as the XML file.
     * @param cu
     * @param oneAM
     * @return
     */
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
      // we expected a value, but didn't get one...we need to return something
      // for preEmitCheck to fail
      if (filenameValue == null) {
        filenameValue = "NOFILEINMETADATA"; // we expected a value, but got none
      }
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArrayList<String> returnList = new ArrayList<String>();
      // default version is just the filename associated with the key, in this directory
      returnList.add(cuBase + filenameValue);
      return returnList;
    }
    
    /* default for a plugin is to NOT special filter the XML when loading */
    public boolean getDoXmlFiltering() {
      return false;
    }

    
    
    /*
     * Default behavior is to replace the overridable routine that used to be 
     * in its place
     */
    protected Collection<ArticleMetadata> modifyAMList(SourceXmlSchemaHelper helper,
        CachedUrl cu, List<ArticleMetadata> allAMs) {
        return allAMs; 
    }
    
    // A particular XML extractor might inherit the rest of the base methods
    // but it MUST implement a definition for a specific schema
    // of this version. Having the CU can allow a plugin to choose a schema
    // based on information in the URL. They can simply disregard the cu if there is only one schema 
    // See TaylorAndFrancisSourceXmlMetadataExtractor as an example where it varies based on CU
    protected abstract SourceXmlSchemaHelper setUpSchema(CachedUrl cu);
    
    // This version allows for decisions between schemas based on information
    // somewhere within the xml doc. 
    // See ElsevierDTD5SourceXmlMetadataExtractor as an example.
    // They can simply disregard the argument if there is only one schema
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {
      return setUpSchema(cu);
    }

  }
}
