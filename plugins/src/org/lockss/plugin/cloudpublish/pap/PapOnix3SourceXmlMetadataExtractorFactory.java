package org.lockss.plugin.cloudpublish.pap;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.onixbooks.Onix3LongSourceXmlMetadataExtractorFactory;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class PapOnix3SourceXmlMetadataExtractorFactory extends Onix3LongSourceXmlMetadataExtractorFactory {

  private static Logger log = Logger.getLogger(PapOnix3SourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper PapOnix3Helper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new PapOnix3SourceXmlMetadataExtractor();
  }
  public class PapOnix3SourceXmlMetadataExtractor extends Onix3LongSourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (PapOnix3Helper == null) {
        PapOnix3Helper = new PapOnixSchemaHelper();
      }
      return PapOnix3Helper;
    }

    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                            ArticleMetadata oneAM) {

      String fileName = oneAM.getRaw(helper.getFilenameXPathKey());
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      List<String> returnList = new ArrayList<>();
      returnList.add(cuBase + fileName + ".pdf");
      return returnList;
    }
  }

}
