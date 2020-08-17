package org.lockss.plugin.blackquotidian;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;

public class BlackQuotidianJavascriptLinkExtractorFactory implements LinkExtractorFactory {

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new BlackQuotidianJavascriptLinkExtractor();
    }
}
