package org.lockss.plugin.clockss;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.util.Logger;

public class SourceBibtexMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SourceBibtexMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {

    log.debug3("Inside Source Metadata extractor factory for BibTeX files");

    return new BibtexMetadataExtractor();
  }
}
