package org.lockss.plugin.silverchair.americanjournalofoccupationaltherapy;

import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class AmericanJournalofOccupationalTherapySilverchairHtmlLinkExtractorFactory implements LinkExtractorFactory {

    private static final Logger log = Logger.getLogger(AmericanJournalofOccupationalTherapySilverchairHtmlLinkExtractorFactory.class);

    private static final String ANCHOR_TAG = "a";
    //https://ajot.aota.org/article.aspx?articleid=2360691
   /*
   <a id="pdfLink" class="al-link pdf article-pdfLink pdfaccess" data-article-id="2360691" data-article-url="/aota/content_public/journal/ajot/934184/6904060010p1.pdf" data-ajax-url="/Content/CheckPdfAccess">
            <i class="icon-file-pdf"></i><span>PDF</span>
        </a>

        https://aota.silverchair-cdn.com/aota/content_public/journal/ajot/931886/6901170010p1_ds002.pdf?Expires=1581626087&Signature=GS-acNp5BVyC2d8WRBqpLd3rQuar4OfPTmaGFe2pfctcKYmlCwtaDiSvjlxH-5WbjH7xULsuWmReGVVc5Wh6PD06tfJASmQDw6ofgHPGS~1NkThGTEa3MocxE0DF8u7vafK-OESnFqjW9gYbetYbyDhpVuhPPOoTbpenmyWPzIOuVwvitRLGbZaWeq-Y~BJPUfQB~SzekxY~7b0rdfEbEtaxXeCSgaqD1rnD44RdobHqGoNsBedR2eEQ5PN8HMZmeSxA5h-fM86dEgSPkfouyzpqOTtMtBlTKtf0RpseQ10OGDxGJuBoLU8eQ4-yMLzC1QP9b5NTbTngtMRpbrNP1g__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA
https://aota.silverchair-cdn.com/aota/content_public/journal/ajot/931886/6901170010p1_ds003.pdf?Expires=1581626087&Signature=4wJbgBhQ5GYdS3M7Hy34iTEWAWdu6zzxfW7LOwdC-f55jVAJqheE7SZCFhRhUeDnkNGSiAexNoHnyjKmP~zugFPquaTJBWy7X1kqQEQjtXbZw-wcwTNxunMSjaWAdAUgDZ758Rm4HAC5OdtK5ug3AoDf5crSgzvp8ifK5jFoXo9OQcXwekE~bUZk-EDHEuRiSjQL1gNDEUsyNn0iF7F1xunTrUFTiLlYcWKvjnmhnmJ-rNCMKOlq1wTXgOvIUN8VQttVLRNibSSOJ2G1ldFO3QsRRhWh6plb4iN3YIsd92fDHtTef6j2rP0w7PNTxmEG1llP~5uFHFSJNe2E452BwQ__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA
    */
    private static final String START_URL_STR = "article.aspx?articleid=";

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, false, null, null);
        registerExtractors(extractor);
        return extractor;
    }

    /*
     *  For when it is insufficient to simply use a different link tag or script
     *  tag link extractor class, a child plugin can override this and register
     *  additional or alternate extractors
     */
    protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {

        log.debug3("Fei: register new Extractor");

        /*
        extractor.registerTagExtractor(ANCHOR_TAG,
                new AmericanJournalofOccupationalTherapySilverchairHtmlLinkExtractor(new String[]{
                        "href",
                        "download",
                        "data-article-url"
                }));

         */
        extractor.registerTagExtractor(ANCHOR_TAG,
                new JsoupHtmlLinkExtractor.SimpleTagLinkExtractor(new String[]{
                        "href",
                        "download",
                        "data-article-url"
                }));
    }

    public static class AmericanJournalofOccupationalTherapySilverchairHtmlLinkExtractor extends JsoupHtmlLinkExtractor.SimpleTagLinkExtractor {

        public AmericanJournalofOccupationalTherapySilverchairHtmlLinkExtractor(final String[] attrs) {
            super(attrs);
        }

        public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
            String srcUrl = node.baseUri();

            log.debug3("Fei: srcUrl = " + srcUrl);

            //https://ajot.aota.org/issuebrowsebyyear.aspx?year=2015
            if (srcUrl.indexOf("article.aspx?articleid") > -1) {
                log.debug3("Fei: recursive loop srcUrl = " + srcUrl);
                JsoupHtmlLinkExtractor.checkLink(node, cb, "a");
            } else {
                log.debug3("Fei: none recursive loop srcUrl = " + srcUrl);
                JsoupHtmlLinkExtractor.checkLink(node, cb, "href");
            }

            if (srcUrl.indexOf(START_URL_STR) > -1) {
                log.debug3("Fei: included srcUrl = " + srcUrl);
                if (node.hasAttr("data-article-url")) {
                    String url = node.attr("data-article-url");
                    if (!StringUtil.isNullString(url)) {
                        log.debug3("Fei: PDF url = " + url);
                        cb.foundLink(AuUtil.normalizeHttpHttpsFromBaseUrl(au, url));
                    }
                }
            }
        }
    }
}
