package org.lockss.plugin.pub2web.ms;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.jstor.JstorHtmlFormExtractor;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsHtmlFormExtractorFactory implements LinkExtractorFactory {
    
    public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {

        // must turnon form processng which is off by default
        return new MsHtmlLinkExtractor(false,true,null,null);
    }

    public static class MsHtmlLinkExtractor extends JsoupHtmlLinkExtractor {

        private static Logger log = Logger.getLogger(MsHtmlLinkExtractor.class);


        public MsHtmlLinkExtractor(boolean enableStats, boolean processForms,
                                      Map<String,
                                              HtmlFormExtractor
                                                      .FormFieldRestrictions> restrictors,
                                      Map<String, HtmlFormExtractor.FieldIterator>
                                              generators) {
            super(enableStats, processForms, restrictors, generators);
        }


        @Override
        protected HtmlFormExtractor getFormExtractor(final ArchivalUnit au,
                                                     final String encoding,
                                                     final Callback cb) {
            log.debug3("Creating new MsHtmlFormExtractor");
            return new MsHtmlFormExtractor(au, cb, encoding,   getFormRestrictors(), getFormGenerators());
        }
    }
}
