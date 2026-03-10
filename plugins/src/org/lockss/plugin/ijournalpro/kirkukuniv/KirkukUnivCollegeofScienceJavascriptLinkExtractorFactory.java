package org.lockss.plugin.ijournalpro.kirkukuniv;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.scalar.ScalarJavascriptLinkExtractor;

public class KirkukUnivCollegeofScienceJavascriptLinkExtractorFactory implements LinkExtractorFactory {

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new KirkukUnivCollegeofScienceJavascriptLinkExtractor();
    }
}
