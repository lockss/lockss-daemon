package org.lockss.plugin.clockss.euilaw;

import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

public class EuiLawXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(EuiLawXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper MathMLPublishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new JatsPublishingSourceXmlMetadataExtractor();
  }

  public static class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    /*
     * This setUpSchema shouldn't be called directly
     * but for safety, just use the CU to figure out which schema to use.
     *
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
      String url = cu.getUrl();
      if (MathMLPublishingHelper == null) {
        MathMLPublishingHelper = new EuiLawMathMlSchemaHelper();
      }
      return MathMLPublishingHelper;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {
      log.info("what");

    }

  }
}
