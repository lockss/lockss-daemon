package org.lockss.plugin.silverchair.geoscienceworld;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.scielo.SciEloHtmlLinkExtractor;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoscienceWorldLinkExtractorFactory implements LinkExtractorFactory {
    
    /*
    Around May/2023, GSW webpage html source contains some invalid <a> tag, need to filter out

        <a href="javascript:;" class="js-add-to-citation-download-manager"
           data-resource-id="590534"
           data-resource-type-id="3">Add to Citation Manager</a>

           <a class="showAbstractLink js-show-abstract at-Show-Abstract-Link"
           data-aTagid="590733"
           data-is-lay-abstract="False"
           data-abstract-type="abstract"
           href="javascript:;"
           aria-controls="abstract-590733"
           aria-expanded="false">
Abstract            <i class="abstract-toggle-icon js-abstract-toggle-icon icon-general_arrow-down"
               data-icon-class="icon-general_arrow"></i>
        </a>
    */

    private static final Logger log = Logger.getLogger(GeoscienceWorldLinkExtractorFactory.class);


    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new GeoscienceWorldLinkExtractor();
    }
}