/*
 * $Id: SourceXmlMetadataExtractorFactory.java,v 1.4 2013-12-06 19:32:15 alexandraohlson Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    //log.setLevel("debug3");
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
    public String getUniqueIDKey();
    /**
     * Return the xPath key for the item in the record that should be combined
     * when multiple records are consolidated because the refer to the same
     * item. For example - when different formats of a book are described in
     * separate records, the format description would be the item to 
     * consolidate when deduping.
     * It can be null. Consolidation may occur, but the record field will
     * not be combined.
     * @return
     */
    public String getConsolidationKey();
    /**
     * Return the filename prefix to be used when building up a filename to determine
     * if a file exists before emitting its metadata. It can also contain
     * path information (relative to current directory for XML file) if needed. 
     * It can be null if not needed.
     * @return
     */
    public String getFilenamePrefix();
    /**
     * Return a list of suffixes for the filenames to be looked for when 
     * checking for existence before emitting. It could be just the filetype
     * (eg. [.pdf][.epub]) or it could include additional text if needed
     * The preEmitCheck will check the suffixes in order until it finds one.
     * It can be null. 
     * @return
     */
    public ArrayList<String> getFilenameSuffixList();
    /**
     * Return the xPath key to use an item from the raw metadata to build the
     * filename for a preEmitCheck. For example if the ISBN13 value is used
     * as the filename.
     * It can be null if the filename doesn't include metadata information.
     * @return
     */
    public String getFilenameKey();
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
        Map<String,ArticleMetadata> uniqueRecordMap = new HashMap<String,ArticleMetadata>();

        //log.setLevel("debug3");
        SourceXmlMetadataExtractorHelper schemaHelper;
        // 1. figure out which XmlMetadataExtractorHelper class to use to get
        // the schema specific information
        if ((schemaHelper = setUpSchema(cu)) == null) {
          log.debug3("Unable to set up XML schema. Cannot extract from XML");
          return;
        }     

        // 2. Gather all the metadata in to a list of AM records
        List<ArticleMetadata> amList = 
            new XPathXmlMetadataParser(schemaHelper.getGlobalMetaMap(), schemaHelper.getArticleNode(), schemaHelper.getArticleMetaMap()).extractMetadata(target, cu);


        // 3. Consolidate identical records based on uniqueIDKey
        // and consolidate values of consolidationKey
        String uniqueIDKey = schemaHelper.getUniqueIDKey(); 
        if (uniqueIDKey == null) uniqueIDKey = ""; //cannot be null
        String consolidationKey = schemaHelper.getConsolidationKey(); //can be null

        for ( ArticleMetadata oneAM : amList) {
          updateRecordMap(uniqueIDKey, consolidationKey, uniqueRecordMap, oneAM);
        }
        if ( (uniqueRecordMap == null) || (uniqueRecordMap.isEmpty()) ) {
          log.debug3("After consolidation, no resulting records");
          return;
        }

        // 4. Cook & Emit all the records in the unique AM list after optional
        // check for file existence
        Iterator<Entry<String, ArticleMetadata>> it = uniqueRecordMap.entrySet().iterator();

        while (it.hasNext()) {
          ArticleMetadata nextAM = (ArticleMetadata)(it.next().getValue());
          // pre-emit check could be overridden by a child with different layout/naming
          // check for each files existence before cook/emit
          if (preEmitCheck(schemaHelper, cu,nextAM)) {
            nextAM.cook(schemaHelper.getCookMap());
            emitter.emitMetadata(cu,nextAM);
          }
        }
      } catch (XPathExpressionException e) {
        log.debug3("Xpath expression exception:" + e.getMessage());
      }

    }

    /**
     *  XML might have multiple <Product/> records for the same item, because
     *  each format might get its own record (eg epub, pdf, etc.)
     *  This method consolidates AM records for items that have the same 
     *  uniqueIDKey (schema defined), combining their consolidationKey 
     *  information <br/>
     *  note - if two versions of the same item are in two different XML files
     *  they won't get consolidated. <br/>
     *  If the uniqueIDKey is null, no consoidation occurs. <br/>
     *  If the consolidationKey is null, consolidation may occur, but no other
     *  fields get combined while doing so.<br/>
     * @param uniqueRecordMap
     * @param nextAM
     */
    protected void updateRecordMap(String uniqueIDKey, String consolidationKey,
        Map<String, ArticleMetadata> uniqueRecordMap,
        ArticleMetadata nextAM) {


      //String formDetailKey = "DescriptiveDetail/ProductFormDetail";
      String nextID = nextAM.getRaw(uniqueIDKey); //will not be null

      log.debug3("updateRecordMap nextID = " + nextID);

      ArticleMetadata prevAM = uniqueRecordMap.get(nextID);
      if (prevAM == null) {
        log.debug3("no record already existed with that id");
        uniqueRecordMap.put(nextID,  nextAM);
      } else if (consolidationKey != null) {
        log.debug3("combining two AM records under that id");
        prevAM.putRaw(consolidationKey, nextAM.getRaw(consolidationKey));
        // assume for now that the metadata is the same, just add to the product form information
        //TODO: Once support is implemented, we'll need to update the FIELD_FACET_URL_MAP information
        // so we can tell the database what format files are available.
      } // otherwise just ignore the redundant record
    }

    /**
     * Verify that a content file exists described in the xml record actually
     * exists base on the XML path location.This also works even if content is 
     * zipped.<br/>
     * The schema helper defines a filename prefix (which could include path
     * information for a subdirectory or sibling directory) as well as a 
     * possible filename component based on a record item (using filenameKey) 
     * and possibly multiple suffixes to handle filetype options.
     * If filenamePrefix, filenameKey and filenameSuffixes are all null, 
     * no preEmitCheck occurs.<br/>
     * For more complicated situations, a child  might want to override this 
     * function entirely.
     * @param cu
     * @param thisAM
     * @return
     */
    protected boolean preEmitCheck(SourceXmlMetadataExtractorHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;


      log.debug3("in SourceXmlMetadataExtractor preEmitCheck");
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
       *        suffix[0] is ".pdf"
       *     filename is Foo_isbn13_Bar{.pdf,.epub} in the same directory
       *        prefix is "Foo_"
       *        key is raw key for ISBN13
       *        suffix is {"_Bar.pdf", "_Bar.epub"}   
       *     
       */
      String filenamePrefix = schemaHelper.getFilenamePrefix(); //can be null
      ArrayList<String> filenameSuffixList = schemaHelper.getFilenameSuffixList(); //can be null
      String filenameKey = schemaHelper.getFilenameKey(); //can be null

      // no pre-emit check required if values are all null, just return
      if (((filenamePrefix == null) && (filenameSuffixList == null) && (filenameKey == null))) return false;

      String filename = 
          (filenamePrefix != null ? filenamePrefix : "") + 
          (filenameKey != null ? thisAM.getRaw(filenameKey) : "");

      //Check in order for at least existing file from among the suffixes
      if (filenameSuffixList == null) {
        // just check for the one version using the other items
        fileCu = B_au.makeCachedUrl(cuBase + filename);
        if(fileCu != null && (fileCu.hasContent())) {
          // Set a cooked value for an access file. Otherwise it would get set to xml file
          thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
          return true;
        } else {
          log.debug3(filename + " does not exist in this AU");
          return false; //No file found to match this record
        }
      } else {
        for (int i=0; i < filenameSuffixList.size(); i++) 
        { 
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
      if(auDefMap.containsKey(PLUGIN_SOURCE_XML_HELPER_CLASS_KEY)){
        helperName = auDefMap.getString(PLUGIN_SOURCE_XML_HELPER_CLASS_KEY);
      } else
        return null;

      Class helperClass;
      try {
        helperClass = Class.forName(helperName);
      } catch (ClassNotFoundException e) {
        log.error("Couldn't load xml definition helper " + helperName +
            ": " + e.toString());
        return null;
      }
      SourceXmlMetadataExtractorHelper helper;
      try {
        helper = (SourceXmlMetadataExtractorHelper)helperClass.newInstance();
      } catch (Exception e) {
        log.error("Couldn't instantiate xml definition helper " + helperName +
            ": " + e.toString());
        return null;
      }

      return helper;
    }
  }
}