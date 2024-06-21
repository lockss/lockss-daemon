package org.lockss.plugin.clockss.isciencenotes;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
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
            if (CrossRefHelper != null) {
                return CrossRefHelper;
            }
            CrossRefHelper = new CrossRefSchemaHelper();
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
            thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
            thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
        }

    }
}

