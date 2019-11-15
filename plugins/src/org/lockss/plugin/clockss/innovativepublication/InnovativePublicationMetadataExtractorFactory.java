package org.lockss.plugin.clockss.innovativepublication;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
import org.lockss.plugin.clockss.casalini.CasaliniLibriMarcXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.casalini.CasaliniMarcXmlSchemaHelper;
import org.lockss.plugin.clockss.warc.WarcJatsPublishingSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InnovativePublicationMetadataExtractorFactory  extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(InnovativePublicationMetadataExtractorFactory .class);

    private static SourceXmlSchemaHelper InnovativeJatsXmlHelper = null;
    private static SourceXmlSchemaHelper InnovativeCrossRefXmlHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new InnovativePublicationMetadataExtractor();
    }

    public class InnovativePublicationMetadataExtractor extends SourceXmlMetadataExtractor {

        /*
         * This setUpSchema shouldn't be called directly
         * WARCs can use both JATSset and ONIX3 and we need to look in the
         * doc to determine which we're handling
         *
         */
        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }


        // WARCs now support both JATSset and ONIX3 for books
        // look in the doc to determine which we're handling in this case
        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {

            Node rootNode = doc.getFirstChild();
            log.debug3("root: " + rootNode.getNodeName());
            if ("ONIXMessage".equals(rootNode.getNodeName())) {
                log.debug3("ONIX xml");
                if (InnovativeJatsXmlHelper == null) {
                    InnovativeJatsXmlHelper = new JatsPublishingSchemaHelper();
                }
                return InnovativeJatsXmlHelper;
            } else {
                log.debug3("JATSset xml");
                if (InnovativeCrossRefXmlHelper == null) {
                    InnovativeCrossRefXmlHelper = new CrossRefSchemaHelper();
                }
                return InnovativeCrossRefXmlHelper;
            }
        }

        // It is not clear which one can be used as "volume" of the PDF file, we use the above "4"
        // we also assume it is single digit number between 1-9
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper,
                                                                CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            String yearNum = "";
            String pdfFilePath = "";
            String volumeNum = "1";
            ArrayList<String> returnList = new ArrayList<String>();
            Boolean volumeNumFound = false;

            if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR) != null) {
                yearNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR).replace(".", "");
            } else {
                log.debug3("yearNum is empty");
            }

            String fileNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());

            if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME) != null) {
                String volumeString = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME);
                int lastComma = volumeString.lastIndexOf(",");
                if (lastComma > -1) {
                    volumeNum = volumeString.substring((lastComma - 1), lastComma);
                    volumeNumFound = true;
                    pdfFilePath = cuBase + yearNum + "_" + volumeNum + "_" + fileNum + ".pdf";
                    returnList.add(pdfFilePath);
                    log.debug3("Found volume number, building PDF file with  filename - " + fileNum + ", volume - " + volumeNum + ", year - " + yearNum);
                }
            }
            if (!volumeNumFound) {
                ArrayList<Integer> volumes = new ArrayList<>();
                volumes.add(1);
                volumes.add(2);
                volumes.add(3);
                volumes.add(4);

                for (int volume : volumes) {
                    pdfFilePath = cuBase + yearNum + "_" + volume + "_" + fileNum + ".pdf";
                    returnList.add(pdfFilePath);
                    log.debug3("Could not find volume number from xml, building PDF file with filename - " + fileNum + ", with possible guessed volume - " + volumeNum + ", year - " + yearNum);
                }
            }

            return returnList;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_start_page) != null) {
                String pages = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_start_page);
                // It might in different formats
                // P. [1-20] [20]
                // 370-370 p.
                String page_pattern = "[^\\d]*?(\\d+)\\s*?\\-\\s*?(\\d+)[^\\d]*?";

                Pattern pattern = Pattern.compile(page_pattern, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(pages);

                String start_page = "0";
                String end_page = "0";

                while (matcher.find()) {
                    start_page = matcher.group(1);
                    end_page = matcher.group(2);
                }

                thisAM.put(MetadataField.FIELD_START_PAGE, start_page);
                thisAM.put(MetadataField.FIELD_END_PAGE, end_page);
            }

            if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_author) != null) {
                String author = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_author);
                thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(".", ""));
            }

            if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_doi) != null) {
                thisAM.put(MetadataField.FIELD_DOI, thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_doi));
            }
        }
    }
}

