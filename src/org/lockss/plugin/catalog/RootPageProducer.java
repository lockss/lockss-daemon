package org.lockss.plugin.catalog;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.lockss.app.LockssDaemon;
import org.lockss.daemon.LockssWatchdog;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.Plugin;
import org.lockss.repository.LockssRepository;
import org.lockss.repository.RepositoryNode;
import org.lockss.util.CIProperties;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class RootPageProducer {
  /** The Id of the Catalog AU */
  public final static String CATALOG_AU_ID = "CatalogAU";
  /** This will be finalized once the protocol is decided */
  public final static String AU_FQDN_MASK = "http://lockssbox/catalogAU/%s";
  /** The text (caption) for the root page link to each AU */
  public final static String AU_LINK_TEXT_MASK = "AU %s";
  /** Content type for generated root catalog page */
  // "application/xhtml+xml" maybe?
  public final static String AU_CATALOG_PAGE_CONTENT_TYPE = "text/html";
  /** Name of the root page */
  public final static String CATALOG_AU_ROOT_PAGE = "index.html";
  /** Title of the root page */
  public final static String CATALOG_ROOT_PAGE_TITLE = "LOCKSS Catalog Root Page";

  /**
   * Given the root daemon object, enumerate all AUs it handles and generate the
   * root page of the CatalogAU while also updating its contents with URLs
   * pointing to each cataloged AU
   */
  public static void produce(LockssDaemon daemon, ArchivalUnit catalogAU,
      LockssWatchdog wdog) throws Exception {
    // begin generating the root page, complete it on the fly as we enumerate
    // AUs

    Plugin plugin = catalogAU.getPlugin();
    String rootUrl = String.format(AU_FQDN_MASK, CATALOG_AU_ROOT_PAGE);
    LockssRepository catalogRepository = daemon.getLockssRepository(catalogAU);
    RepositoryNode rootNode = catalogRepository.createNewNode(rootUrl);
    rootNode.makeNewVersion();
    OutputStream rootOS = rootNode.getNewOutputStream();

    OutputFormat of = new OutputFormat("XML", "UTF-8", true);
    of.setDoctype("-//W3C//DTD XHTML 1.0 Strict//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
    XMLSerializer serializer = new XMLSerializer(rootOS, of);
    ContentHandler hd = serializer.asContentHandler();
    hd.startDocument();
    AttributesImpl attrs = new AttributesImpl();
    attrs.addAttribute("", "", "xmlns", "CDATA", "http://www.w3.org/1999/xhtml");
    attrs.addAttribute("", "", "xml:lang", "CDATA", "en");
    attrs.addAttribute("", "", "lang", "CDATA", "en");
    hd.startElement("", "", "html", attrs);
    hd.startElement("", "", "head", null);
    hd.startElement("", "", "title", null);
    hd.characters(CATALOG_ROOT_PAGE_TITLE.toCharArray(), 0,
        CATALOG_ROOT_PAGE_TITLE.length());
    hd.endElement("", "", "title");
    attrs.clear();
    attrs.addAttribute("", "", "http-equiv", "CDATA", "Content-Type");
    attrs.addAttribute("", "", "content", "CDATA", AU_CATALOG_PAGE_CONTENT_TYPE);
    attrs.addAttribute("", "", "charset", "CDATA", "UTF-8");
    hd.startElement("", "", "meta", attrs);
    hd.endElement("", "", "meta");
    hd.endElement("", "", "head");

    UrlToChecksumMapper mapper = new UrlToChecksumMapperDirect();
    hd.startElement("", "", "body", null);
    for (ArchivalUnit au : daemon.getPluginManager().getAllAus()) {
      if (!au.getAuId().equals(CATALOG_AU_ID)) {
        String url = String.format(AU_FQDN_MASK, au.getAuId());
        RepositoryNode leafNode = catalogRepository.createNewNode(url);
        leafNode.makeNewVersion();
        OutputStream leafOS = leafNode.getNewOutputStream();
        mapper.generateXMLMap(au, new OutputStreamWriter(leafOS));
        leafOS.close();
        CIProperties headers = new CIProperties();
        headers.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
        headers.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
        leafNode.setNewProperties(headers);
        leafNode.sealNewVersion();

        // add content to root page of CatalogAU
        attrs.clear();
        attrs.addAttribute("", "", "href", "CDATA", url);
        hd.startElement("", "", "a", attrs);
        String linkText = String.format(AU_LINK_TEXT_MASK, au.getAuId());
        hd.characters(linkText.toCharArray(), 0, linkText.length());
        hd.endElement("", "", "a");
        hd.startElement("", "", "br", null);
        hd.endElement("", "", "br");

        // System.err.println(String.format("Au id=%s name=%s", au.getAuId(),
        // au.getName()));
      }
    }
    hd.endElement("", "", "body");
    hd.endElement("", "", "html");
    hd.endDocument();
    rootOS.close();
    CIProperties headers = new CIProperties();
    headers.setProperty(CachedUrl.PROPERTY_NODE_URL, rootUrl);
    headers.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE,
        AU_CATALOG_PAGE_CONTENT_TYPE);
    rootNode.setNewProperties(headers);
    rootNode.sealNewVersion();

    // System.err.println(String.format("Reported %d entries", num));
  }
}
