package org.lockss.plugin.michigan;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.util.Constants;

public class IiifJsonLinkExtractorFactory implements LinkExtractorFactory {
  
  public static final String MIME_TYPE_JSON_LD = "application/ld+json";
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    switch (mimeType) {
      case Constants.MIME_TYPE_JSON:
      case MIME_TYPE_JSON_LD:
        return new IiifJsonLinkExtractor();
      default:
        return null;
    }
  }
  
}