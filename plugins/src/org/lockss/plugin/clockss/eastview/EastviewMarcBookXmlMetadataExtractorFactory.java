package org.lockss.plugin.clockss.eastview;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EastviewMarcBookXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(EastviewMarcBookXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper xmlHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new EastviewMarcXmlMetadataExtractor();
    }

    public class EastviewMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // They upload two xml source files, and we are only interested in article related xml for metadata
            // psicoterapia_scienze_umane_issues_20191014.xml
            // psicoterapia_scienze_umane_articles_20191014.xml
            xmlHelper = (SourceXmlSchemaHelper) new EastviewMarcXmlMetadataExtractor();
            
            return xmlHelper;
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
                thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(".", ""));
            }
            
        }
    }
}

