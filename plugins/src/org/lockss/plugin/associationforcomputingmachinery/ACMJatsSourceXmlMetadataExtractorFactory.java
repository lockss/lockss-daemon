package org.lockss.plugin.associationforcomputingmachinery;

/*
 * $Id$
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

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class ACMJatsSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    static Logger log = Logger.getLogger(ACMJatsSourceXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper JatsPublishingHelper = null;
    private static SourceXmlSchemaHelper BitsPublishingHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new JatsPublishingSourceXmlMetadataExtractor();
    }

    public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

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
            // acm  conferences is using BITS format
            if ((url != null) && ((url.indexOf("conferences") > -1) || url.indexOf("books") > -1)) {
                log.debug3("Setup Bits schema helper for url " + url);
                if (BitsPublishingHelper == null) {
                    BitsPublishingHelper = new ACMBitsPublishingSchemaHelper();
                }
                return BitsPublishingHelper;
            } else {
                log.debug3("Setup Jats schema helper for url " + url);
                // acm other material is using JATS format
                if (JatsPublishingHelper == null) {
                    JatsPublishingHelper = new JatsPublishingSchemaHelper();
                }
                return JatsPublishingHelper;
            }
        }

        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            String pdfPath = "";
            String url_string = cu.getUrl();
            List<String> returnList = new ArrayList<String>();
            //XML and PDF are located inside the same directory in most cases
            //Occasionally there is not PDF file, but only xml file, which matches their website setup, like the following:
            //http://content5.lockss.org/sourcefiles/acmjats-released/2019_4/acmotherconferences_2839462-0718100456.zip
            //The also submit xml for issue, but not article, like the following:
            //http://content5.lockss.org/sourcefiles/acmjats-released/2019_4/TACCESSv12i2-0716230343.zip, which has no pdf
            if (url_string.indexOf(".xml") > -1) {
                pdfPath = url_string.replace(".xml", ".pdf");
                ArchivalUnit B_au = cu.getArchivalUnit();
                CachedUrl fileCu;
                fileCu = B_au.makeCachedUrl(pdfPath);
                log.debug3("Check for existence of " + pdfPath);
                if(fileCu != null && (fileCu.hasContent())) {
                    log.debug3("pdfPath is " + pdfPath);
                    returnList.add(pdfPath);
                } else {
                    log.debug3("no matching PDF found, use xml file instead " + pdfPath);
                    returnList.add(url_string);
                }
            }
            return returnList;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            //If we didn't get a valid date value, use the copyright year if it's there
            if (thisAM.get(MetadataField.FIELD_DATE) == null) {
                if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
                    thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
                } else {// last chance
                    thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
                }
            }
        }

    }
}
