package org.lockss.plugin.clockss.actadermatovenereologica;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This xml is using use DTD for NLM databases
 * "https://dtd.nlm.nih.gov/ncbi/pubmed/in/PubMed.dtd”
 */

public class ActaDermatoVenereologicaNLMXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(ActaDermatoVenereologicaNLMXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper ActaNLMXmlHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new ActaDermatoVenereologicaNLMXMLMetadataExtractor();
    }

    public class ActaDermatoVenereologicaNLMXMLMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            if (ActaNLMXmlHelper == null) {
                ActaNLMXmlHelper= new ActaDermatoVenereologicaNLMXmlHelper();
                log.debug3("Setup GActaDermatoVenereologicaNLMXmlHelper Metadata Extractor");
            }
            return ActaNLMXmlHelper;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            if (thisAM.getRaw(ActaDermatoVenereologicaNLMXmlHelper.PAGINATION) != null) {
                String pages = thisAM.getRaw(ActaDermatoVenereologicaNLMXmlHelper.PAGINATION);

                // 53-57
                String page_pattern_string = "(\\d+)(\\s*(-)?\\s*(\\d+))?";
                Pattern page_pattern = Pattern.compile("^\\s*" + page_pattern_string, Pattern.CASE_INSENSITIVE);
                Matcher matcher = page_pattern.matcher(pages);

                String start_page = "0";
                String end_page = "0";

                if (matcher.find()) {
                    start_page = matcher.group(1);
                    end_page = matcher.group(4);
                }

                thisAM.put(MetadataField.FIELD_START_PAGE, start_page);
                thisAM.put(MetadataField.FIELD_END_PAGE, end_page);
            }

        }
    }
}
