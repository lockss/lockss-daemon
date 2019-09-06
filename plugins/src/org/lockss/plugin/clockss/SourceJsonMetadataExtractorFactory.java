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

package org.lockss.plugin.clockss;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/**
 * A factory to create an generic SourceJsonMetadataExtractor
 *
 * @author clairegriffin
 */
public abstract class SourceJsonMetadataExtractorFactory implements FileMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(SourceJsonMetadataExtractorFactory.class);

  /**
   * The generic class to handle metadata extraction for source content
   * that uses JSON with a set schema.
   * A specific plugin must subclass this. While it might inherit and use
   * the vast majority, at a minimum it must define the schema by overriding
   * the "setUpSchema()" method
   *
   * @author alexohlson
   */
  public static abstract class SourceJsonMetadataExtractor
      implements FileMetadataExtractor {


    /**
     * Look at the AU to determine which type of JSON will be processed and
     * then create an XPathJsonMetadataParser to handle the specified JSON files
     * by using the appropriate helper to pass information to the parser.
     * Take the resulting list of ArticleMetadata objects,
     * optionaly consolidate it to remove redundant records
     * and then check for content file existence based on file naming
     * information set up by the helper before cooking and emitting
     * the metadata.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      try {
        SourceJsonSchemaHelper schemaHelper;
        // 1. Create the jsonParser without setting a schema yet
        JsonPathJsonMetadataParser jsonParser = createJpathJsonMetadataParser();
        // 2. create an Json document  using the parser
        DocumentContext doc = jsonParser.createDocument(cu);
        // 3. Set an json parsing schema on the parser before extracting metadata
        if ((schemaHelper = setUpSchema(cu, doc)) == null) {
          log.debug("Unable to set up JSON schema. Cannot extract from JSON");
          throw new PluginException("JSON schema not set up for " + cu.getUrl());
        }
        jsonParser.setJsonParsingSchema(schemaHelper.getGlobalMetaMap(),
            schemaHelper.getArticleNode(),
            schemaHelper.getArticleMetaMap());
        // 4. Gather all the metadata in to a list of AM records
        List<ArticleMetadata> amList = jsonParser.extractMetadataFromDocument(target, doc);

        //5. Optional consolidation of duplicate records within one JSON file
        // a child plugin can leave the default (no deduplication) or
        // This routine used to be called conslidateAMList(schemaHelper, amList) which is
        // now deprecated in favor of this more general method which takes a cu as well.
        Collection<ArticleMetadata> AMCollection = modifyAMList(schemaHelper, cu, amList);

        // 6.. check, cook, and emit every item in resulting AM collection (list)
        for (ArticleMetadata oneAM : AMCollection) {
          if (preEmitCheck(schemaHelper, cu, oneAM)) {
            oneAM.cook(schemaHelper.getCookMap());
            postCookProcess(schemaHelper, cu, oneAM); // hook for optional processing
            emitter.emitMetadata(cu, oneAM);
          }
        }
      }
      catch (IOException ex) {
        handleIOException(cu, emitter, ex);
      }
    }

    // overrideable method for creating the parser
    // this is used by plugins that need to create a parser that filters
    // sgml to legit xml as they are read in line by line.
    protected JsonPathJsonMetadataParser createJpathJsonMetadataParser() {
      return new JsonPathJsonMetadataParser(getDoJsonFiltering());
    }

    // Overrideable method for plugins that want to catch and handle
    // a problem in the JSON file
    protected void handleIOException(CachedUrl cu, Emitter emitter, IOException ex) {
      // Add an alert or more significant warning here
      log.siteWarning("IO exception loading JSON file", ex);
    }

    /**
     * Verify that a content file exists described in the xml record actually
     * exists based on the JSON path location.This also works even if content is
     * zipped.<br/>
     * The schema helper defines a filename prefix (which could include path
     * information for a subdirectory or sibling directory) as well as a
     * possible filename component based on a record item (using filenameXPathKey)
     * and possibly multiple suffixes to handle filetype options.
     * If filenamePrefix, filenameXPathKey and filenameSuffixes are all null,
     * no preEmitCheck occurs.<br/>
     * For more complicated situations, a child  might want to override this
     * function entirely.
     *
     * @param schemaHelper for thread safe access to schema specific information
     */
    protected boolean preEmitCheck(SourceJsonSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in SourceJsonMetadataExtractor preEmitCheck");

      List<String> filesToCheck;

      // If no files get returned in the list, nothing to check
      if ((filesToCheck = getFilenamesAssociatedWithRecord(schemaHelper, cu, thisAM)) == null) {
        return true;
      }
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;
      for (String s : filesToCheck) {
        fileCu = B_au.makeCachedUrl(s);
        log.debug3("Check for existence of " + s);
        if (fileCu != null && (fileCu.hasContent())) {
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
    protected void postCookProcess(SourceJsonSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in SourceJsonMetadataExtractor postEmitProcess");
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
    protected List<String> getFilenamesAssociatedWithRecord(SourceJsonSchemaHelper helper,
                                                            CachedUrl cu,
                                                            ArticleMetadata oneAM) {

      // get the key for a piece of metadata used in building the filename
      String fn_key = helper.getFilenameJsonKey();
      // the schema doesn't define a filename so don't do a default preEmitCheck
      if (fn_key == null) {
        return null; // no preEmitCheck
      }
      String filenameValue = oneAM.getRaw(helper.getFilenameJsonKey());
      // we expected a value, but didn't get one...we need to return something
      // for preEmitCheck to fail
      if (filenameValue == null) {
        filenameValue = "NOFILEINMETADATA"; // we expected a value, but got none
      }
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArrayList<String> returnList = new ArrayList<>();
      // default version is just the filename associated with the key, in this directory
      returnList.add(cuBase + filenameValue);
      return returnList;
    }


    /* default for a plugin is to NOT special filter the JSON when loading */
    public boolean getDoJsonFiltering() {
      return false;
    }


    /*
     * Default behavior is to replace the overridable routine that used to be
     * in its place
     */
    protected Collection<ArticleMetadata> modifyAMList(SourceJsonSchemaHelper helper,
                                                       CachedUrl cu, List<ArticleMetadata> allAMs) {
      return allAMs;
    }

    // A particular Json extractor might inherit the rest of the base methods
    // but it MUST implement a definition for a specific schema
    // of this version. Having the CU can allow a plugin to choose a schema
    // based on information in the URL. They can simply disregard the cu if there is only one schema
    // See TaylorAndFrancisSourceJsonMetadataExtractor as an example where it varies based on CU
    protected abstract SourceJsonSchemaHelper setUpSchema(CachedUrl cu, DocumentContext doc);

  }
}
