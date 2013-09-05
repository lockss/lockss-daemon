package org.lockss.extractor;

import org.lockss.daemon.PluginException;
import org.lockss.util.HeaderUtil;

/**
 * Created with IntelliJ IDEA. User: claire Date: 30/08/2013 Time: 10:55 To
 * change this template use File | Settings | File Templates.
 */
public class JsoupTagExtractorFactory implements FileMetadataExtractorFactory {
  /**
   * Create a MetadataExtractor
   * @param target the purpose for which metadata is being extracted
   * @param contentType the content type type from which to extract URLs
   */
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if ("text/html".equalsIgnoreCase(mimeType)) {
      return new JsoupHtmlMetaTagExtractor();
    }

    if("text/xml".equalsIgnoreCase(mimeType) ||
        "application/xml".equalsIgnoreCase(mimeType))
    {
      return new JsoupXmlTagExtractor();
    }
    return null;
  }
}
