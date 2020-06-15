package org.lockss.plugin.clockss.eastview;/*
 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

public class EastviewBookXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(EastviewBookXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper EastviewBookXmlHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new EastviewBookXmlMarcXmlMetadataExtractor();
    }

    public class EastviewBookXmlMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // There is only 1 xml: https://clockss-test.lockss.org/sourcefiles/eastview-released/2020/metadata.xml
            if  (EastviewBookXmlHelper == null) {
                EastviewBookXmlHelper = new EastviewMarcXmlSchemaHelper();
                log.debug3("Setup EastviewMarcXmlSchemaHelper");
            }
            return EastviewBookXmlHelper;
        }

        /*
          <datafield ind1="0" ind2=" " tag="773">
            <subfield code="t">Psicoterapia e scienze umane. Fascicolo 4, 2000.</subfield> // we are trying to get "4"
            <subfield code="d">Milano : Franco Angeli, 2000.</subfield>
            <subfield code="w">()2194804</subfield>
          </datafield>
       */
        // It is not clear which one can be used as "volume" of the PDF file, we use the above "4"
        // we also assume it is single digit number between 1-9
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper,
                                                                CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            ArrayList<String> returnList = new ArrayList<String>();


            String fileNum = oneAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pdf);
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());

            String  pdfFilePath = cuBase +  fileNum + ".pdf";
            returnList.add(pdfFilePath);

            return returnList;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_author) != null) {
                String author = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_author);
                thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(":", ""));
            }

            if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date) != null) {
                String pub_date = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date);
                thisAM.put(MetadataField.FIELD_DATE, pub_date.replace(".", ""));
            }

            if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_publisher) != null) {
                String publisher = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_publisher);
                thisAM.put(MetadataField.FIELD_PUBLISHER, publisher.replace(":", ""));
            }

            if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_title) != null) {
                String title = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_title);
                thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, title.replace(":", ""));
            }
        }
    }
}
