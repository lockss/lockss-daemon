/*
 * $Id: TangramSourceXmlMetadataExtractorFactory.java,v 1.2 2014-09-30 18:12:41 aishizaki Exp $
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.tangram;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
//import org.lockss.extractor.FileMetadataExtractor.Emitter;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
//import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.xml.sax.SAXException;


public class TangramSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(TangramSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper tangramHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new TangramSourceXmlMetadataExtractor();
  }

  public static class TangramSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    // this version shouldn't get called. It will ultimately get removed
    // in favor of the version that takes a CachedUrl
    @Override
    protected SourceXmlSchemaHelper setUpSchema() {
      return null; // cause a plugin exception to get thrown
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if (tangramHelper != null) {
        return tangramHelper;
      }
      tangramHelper = new TangramSourceXmlSchemaHelper();
      return tangramHelper;
    }
     
    /**
     *  overriding super's extract to use a Tangram-specific XPathXmlMetadataParser
     *  that will set the default encoding to UTF-8, rather than the current ISO-8859
     *  This is a temporary fix until the daemon can better handle setting the encoding
     *  from the metadata
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      try {

        SourceXmlSchemaHelper schemaHelper;
        // 1. figure out which XmlMetadataExtractorHelper class to use to get
        // the schema specific information
        if ((schemaHelper = setUpSchema(cu)) == null) {
          log.debug("Unable to set up XML schema. Cannot extract from XML");
          throw new PluginException("XML schema not set up for " + cu.getUrl());
        }     

        // 2. Gather all the metadata in to a list of AM records
        // XPathXmlMetadataParser is not thread safe, must be called each time
        List<ArticleMetadata> amList = 
            new TangramSourceXPathXmlMetadataParser(schemaHelper.getGlobalMetaMap(), 
                schemaHelper.getArticleNode(), 
                schemaHelper.getArticleMetaMap(),
                getDoXmlFiltering()).extractMetadata(target, cu);


 
        //3. Optional consolidation of duplicate records within one XML file
        // a child plugin can leave the default (no deduplication) or 
        // AMCollection pointing to just a subset of the full
        // AM list
        // 3. Consolidate identical records based on DeDuplicationXPathKey
        // consolidating as specified by the consolidateRecords() method
        
        Collection<ArticleMetadata> AMCollection = getConsolidatedAMList(schemaHelper,
            amList);

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
        handleSAXException(cu, emitter, ex);
      } catch (IOException ex) {
        handleIOException(cu, emitter, ex);
      }


    }

  }
}
