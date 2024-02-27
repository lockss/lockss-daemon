package org.lockss.plugin.silverchair;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.blackquotidianrdf.BlackQuotidianJavascriptLinkExtractor;

public class SilverchairVideoJsonLinkExtractorFactory implements LinkExtractorFactory {

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new SilverchairVideoJsonLinkExtractor();
    }
}
