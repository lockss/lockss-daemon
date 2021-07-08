

package org.lockss.plugin.illiesia;

import java.io.IOException;
import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;

import org.lockss.util.Logger;

public class IlliesiaHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  protected static Logger logger = Logger.getLogger(IlliesiaHtmlLinkExtractor.class);

  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
    throws IOException {
    IlliesiaCharsetUtil.InputStreamAndCharset InAndCs = IlliesiaCharsetUtil.getCharsetStream(in, encoding);
    InputStream newIs = InAndCs.getInStream();
    String expCs = InAndCs.getCharset();
    logger.debug3("Original encoding anticipated: " + encoding);
    logger.debug3("Encoding guessed from BOM: " + expCs);
    super.extractUrls(au, newIs, expCs, srcUrl, cb);
  }

  public static class Factory implements LinkExtractorFactory {
    @Override
    public LinkExtractor createLinkExtractor(String mimeType)
      throws PluginException {
      return new IlliesiaHtmlLinkExtractor();
    }
  }

}
