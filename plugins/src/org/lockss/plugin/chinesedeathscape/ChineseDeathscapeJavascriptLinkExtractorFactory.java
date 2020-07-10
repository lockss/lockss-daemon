package org.lockss.plugin.chinesedeathscape;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;

public class ChineseDeathscapeJavascriptLinkExtractorFactory implements LinkExtractorFactory {

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new ChineseDeathscapeJavascriptLinkExtractor();
    }
}
