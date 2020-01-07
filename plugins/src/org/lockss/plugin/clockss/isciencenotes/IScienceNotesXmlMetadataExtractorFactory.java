package org.lockss.plugin.clockss.isciencenotes;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.CrossRefQuerySchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class IScienceNotesXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(IScienceNotesXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper CrossRefHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new IscienceNotesXmlMetadataExtractor();
    }

    public class IscienceNotesXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // Once you have it, just keep returning the same one. It won't change.
            if (CrossRefHelper != null) {
                return CrossRefHelper;
            }
            CrossRefHelper = new CrossRefQuerySchemaHelper();
            return CrossRefHelper;
        }


        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            // XML: 2016-1-iSciNote.xml
            // PDF: 2016-1-iSciNote.pdf
            String url_string = cu.getUrl();
            String pdfName = url_string.substring(0,url_string.length() - 4) + ".pdf";
            log.debug3("pdfName is " + pdfName);
            List<String> returnList = new ArrayList<String>();
            returnList.add(pdfName);
            return returnList;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {
            log.debug("in postcook");
            // In the AOFoundation metadata, the registrant is incorrectly set to WEB-FORM
            String pname = thisAM.get(MetadataField.FIELD_PUBLISHER);
            // they cannot seem to avoid spelling errors in the publication name. I'm going to manually set it
            // after doing a basic check.  The variants seen so far are:
            // European Cells and Material,European Cells and Materials,European Cells and Matherials, European Cells a¨nd Materials
            // European cells amd Material,European cells amd Materials, Europen Cells and Materials,etc
            String jname = thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE);
        }

    }
}

