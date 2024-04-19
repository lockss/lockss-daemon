package org.lockss.plugin.europeanmathematicalsociety.api;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;

public class EuropeanMathematicalSocietyJournalsJsonLinkExtractorFactory implements LinkExtractorFactory {

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new EuropeanMathematicalSocietyJournalsJsonLinkExtractor();
    }
}
