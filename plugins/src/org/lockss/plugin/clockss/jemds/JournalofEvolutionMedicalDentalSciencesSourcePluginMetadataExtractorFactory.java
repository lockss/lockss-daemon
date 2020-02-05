package org.lockss.plugin.clockss.jemds;

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class JournalofEvolutionMedicalDentalSciencesSourcePluginMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(JournalofEvolutionMedicalDentalSciencesSourcePluginMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper JournalofEvolutionMedicalDentalSciencesJatsXmlHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new JournalofEvolutionMedicalDentalSciencesSourcePluginMetadataExtractor();
    }

    public class JournalofEvolutionMedicalDentalSciencesSourcePluginMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {

            if (JournalofEvolutionMedicalDentalSciencesJatsXmlHelper == null) {
                JournalofEvolutionMedicalDentalSciencesJatsXmlHelper = new JatsPublishingSchemaHelper();
            }
            return JournalofEvolutionMedicalDentalSciencesJatsXmlHelper;
        }

        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            String pdfPath = "";
            String url_string = cu.getUrl();
            List<String> returnList = new ArrayList<String>();
            //XML and PDF are located inside the same directory in most cases
            //http://content5.lockss.org/sourcefiles/jemds-released/2019/20191213/1-soumya%20kanduri--jan-7-3/1-soumya%20kanduri--jan-7-3.pdf
            //http://content5.lockss.org/sourcefiles/jemds-released/2019/20191213/1-soumya%20kanduri--jan-7-3/1-soumya%20kanduri--jan-7-3.xml
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

