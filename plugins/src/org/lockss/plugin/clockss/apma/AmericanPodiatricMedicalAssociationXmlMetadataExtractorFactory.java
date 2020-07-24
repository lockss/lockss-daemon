package org.lockss.plugin.clockss.apma;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
import org.lockss.util.LineRewritingReader;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AmericanPodiatricMedicalAssociationXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

    static Logger log = Logger.getLogger(AmericanPodiatricMedicalAssociationXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper JatsPublishingHelper = null;

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
            if (JatsPublishingHelper == null) {
                JatsPublishingHelper = new JatsPublishingSchemaHelper();
            }
            return JatsPublishingHelper;
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

        /**
         * <p>Some IOP XML files contains HTML4 entities, that trip the SAX parser.
         * Work around them with Apache Commons Lang3.</p>
         */
        @Override
        protected XPathXmlMetadataParser createXpathXmlMetadataParser() {
            return new XPathXmlMetadataParser(getDoXmlFiltering()) {
                @Override
                protected InputStream getInputStreamFromCU(CachedUrl cu) {
                    if (isDoXmlFiltering()) {
                        return new XmlFilteringInputStream(new ReaderInputStream(new LineRewritingReader(new InputStreamReader(cu.getUnfilteredInputStream())) {
                            @Override
                            public String rewriteLine(String line) {
                                return StringEscapeUtils.unescapeHtml4(line);
                            }
                        }));
                    }
                    else {
                        return cu.getUnfilteredInputStream();
                    }
                }
            };
        }

        /**
         * @see #createXpathXmlMetadataParser()
         */
        @Override
        public boolean getDoXmlFiltering() {
            return true;
        }

    }
}