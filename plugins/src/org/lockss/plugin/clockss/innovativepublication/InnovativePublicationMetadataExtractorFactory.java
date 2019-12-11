package org.lockss.plugin.clockss.innovativepublication;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
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

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new InnovativePublicationMetadataExtractor();
    }

    public class InnovativePublicationMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }
        
        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {

            if (InnovativeJatsXmlHelper == null) {
                InnovativeJatsXmlHelper = new JatsPublishingSchemaHelper();
            }
            return InnovativeJatsXmlHelper;
        }

        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            String pdfPath = "";
            String url_string = cu.getUrl();
            List<String> returnList = new ArrayList<String>();
            //XML and PDF are located inside the same directory in most cases
            //http://content5.lockss.org/sourcefiles/innovativepublication-released/2019/IJCA/2019/volume%206/issue%204/IJCA%206-4-478-480.pdf
            //http://content5.lockss.org/sourcefiles/innovativepublication-released/2019/IJCA/2019/volume%206/issue%204/IJCA%206-4-481-487.xml
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

