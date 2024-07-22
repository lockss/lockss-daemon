/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.lbnl;

import org.apache.commons.lang3.StringUtils;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.XPathUtil;
import org.lockss.util.urlconn.CacheException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NamesforLifeLocLinkExtractor implements LinkExtractor {

    protected static final XPathExpression DOIDataSets;

    private static final Logger log = Logger.getLogger(NamesforLifeLocLinkExtractor.class);

    /*
     <sitemapindex xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
        <sitemap>
        <loc>
        https://www.namesforlife.com/sitemap-taxon-information-objects.xml
        </loc>
        <lastmod>2022-08-18</lastmod>
        </sitemap>
        <sitemap>
        <loc>
        https://www.namesforlife.com/sitemap-exemplar-information-objects.xml
        </loc>
        <lastmod>2022-08-18</lastmod>
        </sitemap>
        <sitemap>
        <loc>
        https://www.namesforlife.com/sitemap-name-information-objects.xml
        </loc>
        <lastmod>2022-08-18</lastmod>
        </sitemap>
        <sitemap>
        <loc>https://www.namesforlife.com/sitemap.xml</loc>
        </sitemap>
       </sitemapindex>
     */
    static {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            DOIDataSets = xpath.compile("//loc/text()");
        }
        catch (XPathExpressionException xpee) {
            throw new ExceptionInInitializerError(xpee);
        }
    }

    @Override
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
            throws IOException {
        String loggerUrl = loggerUrl(srcUrl);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputSource inputSource = new InputSource(new InputStreamReader(in, encoding));
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(inputSource);
        }
        catch (ParserConfigurationException pce) {
            throw new CacheException.UnknownExceptionException("Error configuring parser for " + loggerUrl, pce);
        }
        catch (SAXException se) {
            throw new CacheException.UnknownExceptionException("Error while parsing " + loggerUrl, se);
        }

        NodeList locs = null;
        try {

            locs = XPathUtil.evaluateNodeSet(DOIDataSets, doc);
            int loc_count = locs.getLength();
            log.debug3("Total DOI count : " + loc_count);
        }
        catch (XPathExpressionException xpee) {
            throw new CacheException.UnknownExceptionException("Error while parsing results for " + loggerUrl, xpee);
        }

        if (locs.getLength() == 0) {
            throw new CacheException.UnknownExceptionException("Internal error parsing results for " + loggerUrl);
        }

        for (int i = 0 ; i < locs.getLength() ; ++i) {
            log.debug3("Each DOI value " + locs.item(i).getNodeValue());
            processDoi(cb, locs.item(i).getNodeValue());
        }
    }

    public void processDoi(Callback cb, String loc) {
        if (loc != null && !StringUtils.isEmpty(loc)) {
            cb.foundLink(loc);
        }
    }

    public static final String loggerUrl(String srcUrl) {
        return srcUrl;
    }
}
