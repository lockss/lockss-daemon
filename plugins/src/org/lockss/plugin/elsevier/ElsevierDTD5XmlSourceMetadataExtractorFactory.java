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

package org.lockss.plugin.elsevier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  Elsevier DTD5 Metadata Extractor
 *  This is a little more complicated than other Clockss Source XML based plugins
 *  1. The deliveries are broken in to chunks. 
 *       CLKS000003A.tar, CLKS000003B.tar... combine to make directory CLKS000003/
 *       but we do not unpack the individual tars so we must figure out which tar
 *       contents live in
 *  2.  The A tarball contains a "dataset.xml" file describing
 *       all the contents for related tarballs as well as much of the metadata.
 *  3. The A tarball and any other letter variants of the same delivery name contain
 *       subdirectories which contain articles or book chapters. Each article/chapter "item"
 *       has a PDF of the content and a "main.xml" file which contains the remainder
 *       of the needed metadata
 *       
 *   The approach will be thus
 *       - use the dataset.xml to get the easy-to-get metadata for the delivery
 *       - use the item level main.xml files to get the rest of the metadata
 *       - use a custom data object attached to the ArticleFiles object to store data
 *       - use a custom emitter to bring together the two portions of the metadata for one article
 *       
 * The iterator will find all dataset.xml (there is only one per delivery set) and all 
 * main.xml files that live at the correct level down.
 * 
 * The extractor is a standard XML schema extractor but it is complicated by supporting several
 * different schema (books, journals/book-series). To simplify the xpath used we have four schema
 * and identify which one to use either through file url or namespace in the XML file.
 * 
 * The emitter is custom and will wait until both portions of metadata have been collected and the
 * existence of the PDF has been verified.
 *       
 *  @author alexohlson
 */
public class ElsevierDTD5XmlSourceMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ElsevierDTD5XmlSourceMetadataExtractorFactory.class);


  //ex: http://www.elsevier.com/xml/schema/transport/ew-xcr/book-2015.1/book-projects
  private static final Pattern BOOK_DATASET_XMLNS = Pattern.compile("/schema/transport/[^/]+/book-[^/]+/");
  // as opposed to journals and book series which would have a namespace like this 
  // ex: http://www.elsevier.com/xml/schema/transport/ew-xcr/journal-2014.6/issues
  // ex: http://www.elsevier.com/xml/schema/transport/ew-xcr/journal-2015.1/issues
  //private static final Pattern JOURNAL_BOOK_SERIES_DATASET_XMLNS = Pattern.compile("/schema/transport/[^/]+/journal-[^/]+/"); 

  // The url for a book chapter "main.xml" can be identified by its use of ISBN as the subdirectory
  // ISBN will be at least 10 in length, allowing digits and hyphen
  // xxxA.tar!/xxx/9780444593788/BODY/B9780444593788000013/main.xml 
  private static final Pattern BOOK_MAIN_URL = Pattern.compile(".*\\.tar!/[^/]+/[0-9-]{10,}/[^/]+/[^/]+/main\\.xml$");
  // whereas a journal or book-series would use issn (8 or 9 with hyphen) as the 2nd dir under the tar  

  //Use to identify the volume and optional issue and supplement information
  // from the full path of the main.xml - journal version only
  //     <base_url>/<year>/<tarnum>.tar!/<tarnum>/03781119/v554i2/S0378111914011998/main.xml
  // will have a "v" portion $1, may have "i" portion $2 and/or "s" portion $3
  // there could be a hyphen in the volume or issue portion
  static final Pattern ISSUE_INFO_PATTERN = 
      Pattern.compile(".*\\.tar!/[^/]+/[^/]+/v([^is/]+)(?:i([^s/]+))?(?:s([^/]+))?/[^/]+/main\\.xml$", Pattern.CASE_INSENSITIVE);

  private static SourceXmlSchemaHelper journalDatasetHelper = null;
  private static SourceXmlSchemaHelper journalMainHelper = null;
  private static SourceXmlSchemaHelper bookDatasetHelper = null;
  private static SourceXmlSchemaHelper bookMainHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ElsevierDTD5XmlSourceMetadataExtractor();
  }

  /* 
   * The iterator finds both "dataset.xml" and all the per-item "main.xml" files.
   * The dataset.xml file contains information for all the articles included in the tar delivery
   * The main.xml file is specific to one item and is the only place for the author, article.title
   *   as well as a backup place for doi and copyright data
   * There are four possible schemas
   *   dataset.xml for journals and book-series
   *   main.xml for journals and book-series
   *   dataset.xml for books
   *   main.xml for book chapters
   * Decide between dataset.xml choices based on the namespace in the document
   * Decide between main.xml choices based on the url to the file.  
   * Once they are created, just return them at each subsequent request
   */
  public static class ElsevierDTD5XmlSourceMetadataExtractor extends SourceXmlMetadataExtractor {

    /*
     * This setUpSchema shouldn't be called directly
     * but for safety, just use the CU to figure out which schema to use.
     * 
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
      String url = cu.getUrl();

      // Is this a "main.xml"
      if ((url != null) && url.endsWith("/main.xml")) {
        // We need to decide if this is a book's main.xml or a journal's main.xml
        if ((BOOK_MAIN_URL.matcher(url)).find()) {
          if(bookMainHelper == null) {
            bookMainHelper = new ElsevierBooksMainDTD5XmlSchemaHelper();
          }
          return bookMainHelper;
        } else {
          if(journalMainHelper == null) {
            journalMainHelper = new ElsevierJournalsMainDTD5XmlSchemaHelper();
          }
          return journalMainHelper;
        }
      }
      // It's a dataset.xml
      log.debug3("it's a dataset");
      String namespace = null; //default
      Node rootNode = xmlDoc.getFirstChild();
      NamedNodeMap attrMap = rootNode.getAttributes();
      if (attrMap != null) {
        Node nspaceNode = attrMap.getNamedItem("xmlns");
        if (nspaceNode != null) {
            namespace=nspaceNode.getNodeValue();
            log.debug3("dataset xmlns: " + namespace);
        }
      }
      // If we got a namespace from the doc, see if it was a book 
      if ( (namespace != null)  && ((BOOK_DATASET_XMLNS.matcher(namespace)).find()) ) {
        // this is a book dataset, so return this - creating if necessary
        if (bookDatasetHelper == null) {
          bookDatasetHelper = new ElsevierBooksDatasetXmlSchemaHelper();
        }
        return bookDatasetHelper;
      } else {
        // this is either defined as a journal or no namespace was set...
        // this is a journal - return this one, creating if necessary
        if (journalDatasetHelper == null) {
          journalDatasetHelper = new ElsevierJournalsDatasetXmlSchemaHelper();
        }
        return journalDatasetHelper;
      }
    }


    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {

      // if this is the dataset file, just emit - we'll check for pdf at the article level
      if ((helper == journalDatasetHelper) || (helper == bookDatasetHelper)){
        return null;
      }
      String md_url = cu.getUrl();
      String pdf_url = md_url.substring(0,md_url.length() - 4) + ".pdf"; 
      log.debug3("pdf file is " + pdf_url);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdf_url);
      return returnList;
    }

    /* 
     * We know we have a matching pdf if we get to this routine
     * There is no post-cook for dataset.xml processing
     * Fork depending on whether this is a book or a journal/book-series
     * 
     */
    @Override 
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      if ((schemaHelper == journalDatasetHelper) || (schemaHelper == bookDatasetHelper)){
        // if this is an AM from the dataset level file, we don't need any additional processing done.
        return;
      }
      boolean isJournal = (schemaHelper == journalMainHelper);
      log.debug3("in Elsevier postCookProcess - isJournal is " + isJournal);
      if (isJournal) {
        String md_url = cu.getUrl();
        /*
         * Now get the volume, issue and supplement status from the URL
         * md_url looks like this:
         *      03781119/v554i2/S0378111914011998/main.xml
         *   we want the second section which could have a v, i, and s component
         *   v# or v#-# 
         *   i# or i#-#
         *   s#orLtr
         *   will have at least v
         *   exs: /v113-115sC/ or /v58i2-3/ or /v117i6/ or /v39sC/ or /v100i8sS/   
         */

        Matcher vMat = ISSUE_INFO_PATTERN.matcher(md_url);
        log.debug3("checking for volume information from path");
        if (vMat.matches()) {
          String vol = vMat.group(1);
          String optIss = vMat.group(2);
          String optSup = vMat.group(3);
          log.debug3("found volume information: V" + vol + "I" + optIss + "S" + optSup);
          thisAM.put(MetadataField.FIELD_VOLUME, vol);
          if( (optIss != null) || (optSup != null)){
            StringBuilder val = new StringBuilder();
            if (optIss != null) {
              val.append(optIss);
            }
            if (optSup != null) {
              val.append(optSup);
            }
            // there is no field equivalent to the suppl used by Elsevier
            thisAM.put(MetadataField.FIELD_ISSUE, val.toString()); 
          }
        }
      }
    }
  }
}
