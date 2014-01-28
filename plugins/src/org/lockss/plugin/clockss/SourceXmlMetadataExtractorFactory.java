/*
 * $Id: SourceXmlMetadataExtractorFactory.java,v 1.14 2014-01-28 22:06:39 alexandraohlson Exp $
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.common.util.StringUtils;
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
import org.xml.sax.SAXException;


/**
 *  A factory to create an generic SourceXmlMetadataExtractor
 *  @author alexohlson
 */
public abstract class SourceXmlMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(SourceXmlMetadataExtractorFactory.class);


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
        // 1. figure out which XmlMetadataExtractorHelper class to use to get
        // the schema specific information
        if ((schemaHelper = setUpSchema()) == null) {
          log.debug("Unable to set up XML schema. Cannot extract from XML");
          throw new PluginException("XML schema not set up for " + cu.getUrl());
        }     

        // 2. Gather all the metadata in to a list of AM records
        // XPathXmlMetadataParser is not thread safe, must be called each time
        List<ArticleMetadata> amList = 
            new XPathXmlMetadataParser(schemaHelper.getGlobalMetaMap(), 
                schemaHelper.getArticleNode(), 
                schemaHelper.getArticleMetaMap(),
                schemaHelper.getDoXmlFiltering()).extractMetadata(target, cu);


        Collection<ArticleMetadata> AMCollection = amList; //if deduping, this will change

        // 3. Consolidate identical records based on DeDuplicationXPathKey
        // consolidating as specified by the consolidateRecords() method
        String deDupKey = schemaHelper.getDeDuplicationXPathKey(); 
        boolean deDuping = (!StringUtils.isEmpty(deDupKey));
        if (deDuping) {
          Map<String,ArticleMetadata> uniqueRecordMap = new HashMap<String,ArticleMetadata>();

          // Look at each item in AM list and put those with unique values
          // associated with the deDupKey in a map of unique records.
          // For duplicates, use the consolidateRecords() method to combine
          for ( ArticleMetadata oneAM : amList) {
            updateRecordMap(schemaHelper, deDupKey, uniqueRecordMap, oneAM);
          }
          log.debug3("After consolidation, " + uniqueRecordMap.size() + "records");
          AMCollection = uniqueRecordMap.values();
        }

        // 4. check, cook, and emit every item in resulting AM collection (list)
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
        handleSAXException(cu, ex);
      } catch (IOException ex) {
        handleIOException(cu, ex);
      }


    }

    // Overrideable method for plugins that want to catch and handle
    // a problem in the XML file
    protected void handleIOException(CachedUrl cu, IOException ex) {
      // Add an alert or more significant warning here
      log.debug3("IO exception loading XML file", ex);

    }

    // Overrideable method for plugins that want to catch and handle
    // a SAX parser problem in the XML file
    protected void handleSAXException(CachedUrl cu, SAXException ex) {
      // Add an alert or more significant warning here
      log.debug3("SAX exception loading XML file", ex);

    }

    /**
     *  XML might have multiple <Product/> records for the same item, because
     *  each format might get its own record (eg epub, pdf, etc.)
     *  This method consolidates AM records for items that have the same 
     *  deDuplicationXPath (schema defined), combining their consolidationXPathKey 
     *  information <br/>
     *  note - if two versions of the same item are in two different XML files
     *  they won't get consolidated. <br/>
     *  If the deDuplicationXPath is null or empty, this method isn't called.
     *  A child plugin might override this method to provide some other type of
     *  deDuplication. They would still need to set a deDuplicationXPathKey
     *  to trigger the call to the method..
     * @param schemaHelper for subroutines to access schema information
     * @param deDupRawKey - the raw metadata key on whose values to de dup
     * @param uniqueRecordMap - a String key, to ArticleMetadata map for storing
     * the dedup'd records
     * @param thisAM - determine if this AM is unique or a duplicate
     */
    protected void updateRecordMap(SourceXmlSchemaHelper schemaHelper,
        String deDupRawKey,
        Map<String, ArticleMetadata> uniqueRecordMap,
        ArticleMetadata thisAM) {

      // The deDuplicationXPathKey is neither null nor empty if we are in this method
      String deDupRawVal = thisAM.getRaw(deDupRawKey); 

      log.debug3("updateRecordMap deDupRawVal = " + deDupRawVal);

      ArticleMetadata prevDupRecord = uniqueRecordMap.get(deDupRawVal);
      if (prevDupRecord == null) {
        log.debug3("no record already existed with that raw val");
        uniqueRecordMap.put(deDupRawVal,  thisAM);
      } else {
        // A child can override this for different consolidation needs
        consolidateRecords(schemaHelper, prevDupRecord, thisAM);
      } 
    }

    /**
     * Two ArticleMetadata records are "combined" in to one by
     * appending the rawValue associated with the consolidationXPathKey 
     * of the second AM record to the rawValue of the first AM record.
     * If the consolidateionKey is null, the routine returns immediately.
     * This method could be overridden for a child plugin that needs to 
     * consolidate records in a different way.
     */
    protected void consolidateRecords(SourceXmlSchemaHelper schemaHelper,
        ArticleMetadata intoAM, ArticleMetadata fromAM) {

      String consolidationXPathKey = schemaHelper.getConsolidationXPathKey();
      if (StringUtils.isEmpty(consolidationXPathKey)) return;
      log.debug3("combining two AM records");
      // ArticleMetadata.putRaw appends if a value(s) exists
      intoAM.putRaw(consolidationXPathKey, fromAM.getRaw(consolidationXPathKey));
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

      String filenamePrefix = schemaHelper.getFilenamePrefix(); //can be null
      ArrayList<String> filenameSuffixList = schemaHelper.getFilenameSuffixList(); //can be null
      String filenameKey = schemaHelper.getFilenameXPathKey(); //can be null
      // no pre-emit check required if values are all null, just return and emit
      if (((filenamePrefix == null) && (filenameSuffixList == null) && (filenameKey == null))) return true;

      /* create a filename (relative to the current directory) from the 
       * optional items: 
       * filenamePrefix + thisAM.getRaw(filenameKey) + filenameSuffix
       * Any or all of them could be null. 
       * We don't get in to this routine if all three are null
       * examples:
       *     filename is just isbn13.pdf in the same directory
       *        prefix = null; key is raw key for ISBN13, suffix[0] is ".pdf"
       *     filename is online.pdf in a "content" subdirectory
       *        prefix = "/content/online"
       *        key = null
       *        suffix[0] is ".pdf"
       *     filename is Foo_isbn13_Bar{.pdf,.epub} in the same directory
       *        prefix is "Foo_"
       *        key is raw key for ISBN13
       *        suffix is {"_Bar.pdf", "_Bar.epub"}   
       *     
       */
      String filename = 
          (filenamePrefix != null ? filenamePrefix : "") + 
          (filenameKey != null ? thisAM.getRaw(filenameKey) : "");

      // Check for files by going through list of suffixes, stop at first success
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;
      if (filenameSuffixList == null) {
        //for simplicity, create an empty suffix 
        filenameSuffixList = new ArrayList<String>(); 
        filenameSuffixList.add("");
      }
      for (int i=0; i < filenameSuffixList.size(); i++) 
      { 
        String fullfn = cuBase + filename + filenameSuffixList.get(i);
        fileCu = B_au.makeCachedUrl(cuBase + filename + filenameSuffixList.get(i));
        if(fileCu != null && (fileCu.hasContent())) {
          // Set a cooked value for an access file. Otherwise it would get set to xml file
          thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
          return true;
        }
      }
      log.debug3(filename + " does not exist in this AU");
      return false; //No files found that match this record
    }

    // a routine to allow a child to add in some post-cook processing for each
    // AM record (eg. "putifbetter"
    // Default is to do nothing
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in SourceXmlMetadataExtractor postEmitProcess");
    }

    /* 
     * A particular XML extractor might inherit the rest of the base methods
     * but it MUST implement a definition for a specific schema
     */
    protected abstract SourceXmlSchemaHelper setUpSchema();
  }
}
