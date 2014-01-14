/*
 * $Id: SourceXmlMetadataExtractorFactory.java,v 1.11 2014-01-14 20:04:23 alexandraohlson Exp $
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

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.common.util.StringUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.io.IOException;
import java.util.*;
import javax.xml.xpath.XPathExpressionException;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;


/**
 *  A factory to create an generic SourceXmlMetadataExtractor
 *  @author alexohlson
 */
public class SourceXmlMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(SourceXmlMetadataExtractorFactory.class);

  public static final String PLUGIN_SOURCE_XML_HELPER_CLASS_KEY = "plugin_source_xml_metadata_extractor_helper";
  private static LRUMap schemaHelperMap = new LRUMap(10); /*<helperClassName, helperClassInstance> */ 

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new SourceXmlMetadataExtractor();
  }

  /** Class to set up specific schema information for the
   * SourceXmlMetadataExtractro. A helper class defines the schema
   * and provides the information to the extractor via the get methods.
   */
  public interface SourceXmlMetadataExtractorHelper {

    /**
     * Return the map for metadata that carries across all records in this XML
     * schema. It can be null. <br/>
     * If null, only article level information is retrieved.
     * @return
     */
    public Map<String,XPathValue> getGlobalMetaMap();

    /**
     * Return the map for metadata that is specific to each "article" record.
     * @return
     */
    public Map<String,XPathValue> getArticleMetaMap();

    /**
     * Return the xPath string which defines one "article" record
     * It can be null.<br/>
     * If null, xPath matching starts at the root of the document.
     * @return
     */
    public String getArticleNode();

    /**
     * Return the map to translate from raw metadata to cooked metadata
     * This must be set or no metadata gets emitted
     * @return
     */
    public MultiValueMap getCookMap();


    /**
     * Return the xPath key for an item in the record that identifies a 
     * unique article, even if multiple records provide information about 
     * different manifestations of that article. For example, an ISBN13 will
     * be consistent, even if there are different records for each book 
     * type (eg. pdf, epub, etc)
     * The key will be used to combine metadata from multiple records in to
     * one ArticleMetadata for all associated records. 
     * It can be null - no record consolidation will happen.
     * @return
     */
    public String getDeDuplicationXPathKey();

    /**
     * Used only in consolidateRecords() 
     * Return the xPath key for the item in the record that should be combined
     * when multiple records are consolidated because the refer to the same
     * item. For example - when different formats of a book are described in
     * separate records, the format description would be the item to 
     * consolidate when deduping.
     * It can be null. Consolidation may occur, but the record field will
     * not be combined.
     * @return
     */
    public String getConsolidationXPathKey();

    /**
     * Used only in preEmitCheck() which may be overridden by a child
     * Return the filename prefix to be used when building up a filename to determine
     * if a file exists before emitting its metadata. It can also contain
     * path information (relative to current directory for XML file) if needed. 
     * It can be null if not needed.
     * @return
     */
    public String getFilenamePrefix();

    /**
     * Used only in preEmitCheck() which may be overridden by a child
     * Return a list of suffixes for the filenames to be looked for when 
     * checking for existence before emitting. It could be just the filetype
     * (eg. [.pdf][.epub]) or it could include additional text if needed
     * The preEmitCheck will check the suffixes in order until it finds one.
     * It can be null. 
     * @return
     */
    public ArrayList<String> getFilenameSuffixList();

    /**
     * Used only in preEmitCheck() which may be overridden by a child
     * Return the xPath key to use an item from the raw metadata to build the
     * filename for a preEmitCheck. For example if the ISBN13 value is used
     * as the filename.
     * It can be null if the filename doesn't include metadata information.
     * @return
     */
    public String getFilenameXPathKey();
  }


  /**
   * The generic class to handle metadata extraction for source content
   * that uses XML with a set schema for metadata.
   * The class gets its schema definition from an implemenation of a
   * SourceXmlMetadataExtractorHelper 
   * which must be provided and is defined in a plugin using the key
   * plugin_source_xml_metadata_extractor_helper
   * 
   * @author alexohlson
   *
   */
  public static class SourceXmlMetadataExtractor 
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

        SourceXmlMetadataExtractorHelper schemaHelper;
        // 1. figure out which XmlMetadataExtractorHelper class to use to get
        // the schema specific information
        if ((schemaHelper = setUpSchema(cu)) == null) {
          log.debug("Unable to set up XML schema. Cannot extract from XML");
          throw new PluginException("XML schema not set up for " + cu.getUrl());
        }     

        // 2. Gather all the metadata in to a list of AM records
        // XPathXmlMetadataParser is not thread safe, must be called each time
        List<ArticleMetadata> amList = 
            new XPathXmlMetadataParser(schemaHelper.getGlobalMetaMap(), 
                schemaHelper.getArticleNode(), 
                schemaHelper.getArticleMetaMap()).extractMetadata(target, cu);

        
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
      }

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
    protected void updateRecordMap(SourceXmlMetadataExtractorHelper schemaHelper,
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
    protected void consolidateRecords(SourceXmlMetadataExtractorHelper schemaHelper,
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
    protected boolean preEmitCheck(SourceXmlMetadataExtractorHelper schemaHelper, 
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
    protected void postCookProcess(SourceXmlMetadataExtractorHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in SourceXmlMetadataExtractor postEmitProcess");
    }

    /* 
     * Use the helper class defined in the plugin to set up the necessary
     * information for extraction. If this doesn't happen sucessfully, no
     * metadata is emitted. 
     * Validate the return values where necessary.
     */
    private SourceXmlMetadataExtractorHelper setUpSchema(CachedUrl cu) {
      ArchivalUnit au = cu.getArchivalUnit();

      TypedEntryMap auDefMap = AuUtil.getPluginDefinition(au);
      String helperName = null;
      // must check for key first or getString would throw exception
      if(auDefMap.containsKey(PLUGIN_SOURCE_XML_HELPER_CLASS_KEY)){
        helperName = auDefMap.getString(PLUGIN_SOURCE_XML_HELPER_CLASS_KEY);
      } else {
        return null;
      }

      SourceXmlMetadataExtractorHelper helper = (SourceXmlMetadataExtractorHelper)schemaHelperMap.get(helperName);
      if (helper != null) {
        return helper;
      }

      try {
        helper = 
            (SourceXmlMetadataExtractorHelper)au.getPlugin().newAuxClass(helperName, 
                SourceXmlMetadataExtractorHelper.class);
        schemaHelperMap.put(helperName, helper);
        return helper;
      } catch (PluginException.InvalidDefinition ide) {
        log.error("Couldn't load xml definition helper " + helperName +
            ": ", ide);
        return null;
      }
    }
  }
}