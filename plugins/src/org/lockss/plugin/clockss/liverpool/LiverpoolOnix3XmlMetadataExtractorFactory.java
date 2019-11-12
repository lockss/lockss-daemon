package org.lockss.plugin.clockss.liverpool;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class LiverpoolOnix3XmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(LiverpoolOnix3XmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper Onix3Helper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new LiverpoolOnix3XmlMetadataExtractor();
    }

    public class LiverpoolOnix3XmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            // Once you have it, just keep returning the same one. It won't change.

            log.debug3("Fei: LiverpoolOnix3XmlMetadataExtractor");

            if (Onix3Helper == null) {
                Onix3Helper = new Onix3BooksSchemaHelper();
            }
            return Onix3Helper;
        }


        /* In this case, use the RecordReference + .pdf for the matching file */
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {


            String filenameValue = oneAM.getRaw(Onix3BooksSchemaHelper.ONIX_RR);
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());
            String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + filenameValue + ".pdf");
            List<String> returnList = new ArrayList<String>();
            returnList.add(fullPathFile);
            return returnList;
        }

    }
}

