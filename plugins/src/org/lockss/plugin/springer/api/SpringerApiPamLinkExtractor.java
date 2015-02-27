package org.lockss.plugin.springer.api;

import static org.lockss.plugin.springer.api.SpringerApiCrawlSeed.logUrl;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.w3c.dom.*;
import org.xml.sax.*;

public class SpringerApiPamLinkExtractor implements LinkExtractor {

  protected static final String CDN_URL = "http://download.springer.com/";

  protected boolean bHasMore;
  
  public SpringerApiPamLinkExtractor() {
    this.bHasMore = true;
  }
  
  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {
    srcUrl = logUrl(srcUrl);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    InputSource inputSource = new InputSource(in);
    inputSource.setEncoding(encoding);
    Document doc = null;
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.parse(inputSource);
    }
    catch (ParserConfigurationException pce) {
      throw new PluginException("Error configuring parser for " + srcUrl, pce);
    }
    catch (SAXException se) {
      throw new PluginException("Error while parsing " + srcUrl, se);
    }
    try {
      Element response = doc.getDocumentElement();
      Element result = getChildByTagName(response, "result");
      int total = Integer.parseInt(getChildByTagName(result, "total").getTextContent());
      int start = Integer.parseInt(getChildByTagName(result, "start").getTextContent());
      int pageLength = Integer.parseInt(getChildByTagName(result, "pageLength").getTextContent());
      bHasMore = (start + pageLength <= total);
      NodeList dois = response.getElementsByTagName("prism:doi");
      for (int i = 0 ; i < dois.getLength() ; ++i) {
        processDoi(cb, dois.item(i).getTextContent());
      }
    }
    catch (NumberFormatException nfe) {
      throw new PluginException("Error while parsing " + srcUrl, nfe);
    }
  }
  
  public boolean hasMore() {
    return bHasMore;
  }
  
  public void processDoi(Callback cb, String doi) {
    String pdfUrl = CDN_URL + "content/pdf/" + doi + ".pdf";
    cb.foundLink(pdfUrl);
  }
  
  public static Element getChildByTagName(Element element, String name) {
    for (Node child = element.getFirstChild() ; child != null ; child = child.getNextSibling()) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        Element childElement = (Element)child;
        if (childElement.getTagName().equals(name)) {
          return childElement;
        }
      }
    }
    return null;
  }
  
  public static void main(String[] args) throws Exception {
    new SpringerApiPamLinkExtractor().extractUrls(null,
                                                  new FileInputStream("/tmp/e6/1432-0428_57_1"),
                                                  null,
                                                  "http://api.springer.com/meta/v1/pam?q=issn:1432-0428%20volume:57&api_key=e82c6645a99151c778a71e04d0ed1423&p=100&s=301",
                                                  new Callback() {
                                                    @Override
                                                    public void foundLink(String url) {
                                                      System.err.println(url);
                                                    }
                                                  });
  }

}
