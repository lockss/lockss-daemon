package org.lockss.plugin.ubiquitypress.upn;

import java.io.IOException;
import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/*
*public class UbiquityPartnerNetworkXmlLinkExtractorFactory
*    extends XmlLinkExtractorFactory {
*
*    private static Logger logger =
*            Logger.getLogger("UbiquityPartnerNetworkXmlLinkExtractorFactory");
*
*    XmlLinkExtractor UbiquityPartnerNetworkXmlLinkExtractor =
*            new XmlLinkExtractor;
*
*
*
*}
*/

public class UbiquityPartnerNetworkXmlLinkExtractorFactory
        implements LinkExtractorFactory { /* tried extends XmlLinkExtractorFactory but no luck. */
    protected static final Logger log = Logger.getLogger("UbiquityPartnerNetworkXmlLinkExtractorFactory");

    @Override
    public LinkExtractor createLinkExtractor(String mimeType)
            throws PluginException {
        return new UbiquityPartnerNetworkXmlLinkExtractor();
    }
    /* static class is stylistic but is used because it doesnt need the implicit pointer to the parent class */
    protected static class UbiquityPartnerNetworkXmlLinkExtractor
            extends XmlLinkExtractor {

        @Override
        public void extractUrls(ArchivalUnit au,
                                InputStream in,
                                String encoding,
                                String srcUrl,
                                Callback cb)
                throws IOException, PluginException {
            String badUrl = new String("https://www.gewina-studium.nl/articles/10.18352/studium.10198/galley/10893/download/");
            if (srcUrl.equals(badUrl)) {
                log.debug3("NOT extracting from "+srcUrl);
                return;
            } /* This works for some reason... */
            super.extractUrls(au,
                        in,
                        encoding,
                        srcUrl,
                        cb);
        }

    }

}