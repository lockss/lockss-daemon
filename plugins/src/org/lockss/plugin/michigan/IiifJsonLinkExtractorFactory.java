package org.lockss.plugin.michigan;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;

public class IiifJsonLinkExtractorFactory implements LinkExtractorFactory {
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    return new IiifJsonLinkExtractor();
  }
  
}
