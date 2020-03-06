package org.lockss.plugin.pub2web.ms;

import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
PDF link used to be a href link
<a href="/deliver/fulltext/jmmcr/3/2/jmmcr000086.pdf?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000086&amp;mimeType=pdf&amp;isFastTrackArticle=" title="" rel="external" class="externallink pdf
list-group-item list-group-item-info" ><div class="fa fa-file-pdf-o full-text-icon"></div>PDF
<div class="fulltextsize ">
539.07
Kb
</div></a>


PDF link embedded into form action since 3/2020

<form action="/deliver/fulltext/micro/165/3/254_micro000750.pdf?itemId=%2Fcontent%2Fjournal%2Fmicro%2F10.1099%2Fmic.0.000750&mimeType=pdf&containerItemId=content/journal/micro"
target="/content/journal/micro/10.1099/mic.0.000750-pdf"
data-title="Download"
data-itemId="http://instance.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.000750"
class="ft-download-content__form ft-download-content__form--pdf js-ft-download-form " >
<input type="hidden" name="pending" value="false" >
<i class="fa fa-file-pdf-o
access-options-icon" aria-hidden="true"></i>
<span class="hidden-xxs">PDF</span>
</form>
 */

public class MsHtmlFormExtractor extends HtmlFormExtractor {

    private static Logger log = Logger.getLogger(MsHtmlFormExtractor.class);

    public MsHtmlFormExtractor(ArchivalUnit au, LinkExtractor.Callback cb, String encoding,
                                  Map<String, FormFieldRestrictions> restrictions,
                                  Map<String, FieldIterator> generators) {
        super(au, cb, encoding, restrictions, generators);
    }

    @Override
    public FormElementLinkExtractor newTagsLinkExtractor() {
        return new MsFormElementLinkExtractor();
    }

    public static class MsFormElementLinkExtractor extends FormElementLinkExtractor {

        private static final String ACTION_ATTR = "action";
        
        private static Logger log = Logger.getLogger(MsFormElementLinkExtractor.class);

        /*
         * Extending
         */
        public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {

            String srcUrl = node.baseUri();

            log.debug3("Fei: MsHtmlFormExtractor custom tagBegin for" + srcUrl);

            if (node.hasAttr(ACTION_ATTR)) {
                if ("action".equalsIgnoreCase((node.attr(ACTION_ATTR)))) {
                    String pdfLink = node.attr(ACTION_ATTR);

                    String base = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
                    String newUrl = base + pdfLink;
                    log.debug3("Fei: MsHtmlFormExtractor PDF link: " + newUrl);
                    cb.foundLink(newUrl);
                }
            }
            log.debug3("now calling the super tagBegin");
            super.tagBegin(node, au, cb);
        }
    }
}



