package org.lockss.extractor;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;

/**
 * Factory for creating new link extractors for HtmlParserLinkExtractor
 *
 * @author nvibhor, mlanken, fkautz
 */
public class HtmlParserLinkExtractorFactory implements LinkExtractorFactory {

  /**
   * @param mimeType ignored
   * @return new HtmlParserLinkExtractor
   * @see org.lockss.extractor.LinkExtractorFactory#createLinkExtractor(java.lang.String)
   */
  @Override
  public LinkExtractor createLinkExtractor(String mimeType)
      throws PluginException {
    return new HtmlParserLinkExtractor();
  }

}