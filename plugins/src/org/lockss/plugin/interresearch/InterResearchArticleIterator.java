package org.lockss.plugin.interresearch;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class InterResearchArticleIterator
    implements ArticleIteratorFactory,
    ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(InterResearchArticleIterator.class);

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"%sabstracts/%s/v.*\", base_url, journal_id";

  // https://www.int-res.com/abstracts/dao/v143/p205-226/
  // https://www.int-res.com/abstracts/dao/v143/p205-226/
  protected static final Pattern ABS_PATTERN = Pattern.compile("abstracts/[^/]+/v(\\d+)/p(\\d+)-(\\d+)");
  protected static String ABS_REPL;

  protected static String MAIN_PDF_REPL;
  protected static String MAIN_PDF_REPL0;
  protected static String MAIN_PDF_REPL00;
  protected static String FEATURE_PDF_REPL;
  protected static String FEATURE_PDF_REPL0;
  protected static String FEATURE_PDF_REPL00;
  protected static String OA_PDF_REPL;
  protected static String OA_PDF_REPL0;
  protected static String OA_PDF_REPL00;

  protected static String MAIN_XML_REPL;
  protected static String MAIN_XML_REPL0;
  protected static String MAIN_XML_REPL00;
  protected static String FEATURE_XML_REPL;
  protected static String FEATURE_XML_REPL0;
  protected static String FEATURE_XML_REPL00;
  protected static String OA_XML_REPL;
  protected static String OA_XML_REPL0;
  protected static String OA_XML_REPL00;

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    String fileName = String.format("%s$1p$2", au.getConfiguration().get("journal_id").charAt(0));
    String fileName0 = String.format("%s$1p0$2", au.getConfiguration().get("journal_id").charAt(0));
    String fileName00 = String.format("%s$1p00$2", au.getConfiguration().get("journal_id").charAt(0));

    String mainPath = String.format("articles/%s%s/", au.getConfiguration().get("journal_id"), au.getConfiguration().get("year"));
    String featPath = "articles/feature/";
    String openPath = String.format("articles/%s_oa/", au.getConfiguration().get("journal_id"));

    ABS_REPL = String.format("abstracts/%s/v$1/p$2-$3", au.getConfiguration().get("journal_id"));

           // https://www.int-res.com/articles/dao2021/143/d143p159.pdf
    MAIN_PDF_REPL = String.format("%s$1/%s.pdf", mainPath, fileName);
    MAIN_PDF_REPL0 = String.format("%s$1/%s.pdf", mainPath, fileName0);
    MAIN_PDF_REPL00 = String.format("%s$1/%s.pdf", mainPath, fileName00);
           // https://www.int-res.com/articles/feature/d143p205.pdf
    FEATURE_PDF_REPL = String.format( "%s%s.pdf", featPath, fileName);
    FEATURE_PDF_REPL0 = String.format( "%s%s.pdf", featPath, fileName0);
    FEATURE_PDF_REPL00 = String.format( "%s%s.pdf", featPath, fileName00);
           // https://www.int-res.com/articles/dao_oa/d143p169.pdf
    OA_PDF_REPL = String.format("%s%s.pdf", openPath, fileName);
    OA_PDF_REPL0 = String.format("%s%s.pdf", openPath, fileName0);
    OA_PDF_REPL00 = String.format("%s%s.pdf", openPath, fileName00);


    MAIN_XML_REPL = String.format("%s$1/%s.xml", mainPath, fileName);
    MAIN_XML_REPL0 = String.format("%s$1/%s.xml", mainPath, fileName0);
    MAIN_XML_REPL00 = String.format("%s$1/%s.xml", mainPath, fileName00);
    // https://www.int-res.com/articles/feature/d143p205.XML
    FEATURE_XML_REPL = String.format( "%s%s.xml", featPath, fileName);
    FEATURE_XML_REPL0 = String.format( "%s%s.xml", featPath, fileName0);
    FEATURE_XML_REPL00 = String.format( "%s%s.xml", featPath, fileName00);
    // https://www.int-res.com/articles/dao_oa/d143p169.xml
    OA_XML_REPL = String.format("%s%s.xml", openPath, fileName);
    OA_XML_REPL0 = String.format("%s%s.xml", openPath, fileName0);
    OA_XML_REPL00 = String.format("%s%s.xml", openPath, fileName00);


    builder.setSpec(
      target,
      ROOT_TEMPLATE,
      PATTERN_TEMPLATE,
      Pattern.CASE_INSENSITIVE
    );

    builder.addAspect(
      ABS_PATTERN,
      ABS_REPL,
      ArticleFiles.ROLE_ABSTRACT,
      ArticleFiles.ROLE_ARTICLE_METADATA
    );

    builder.addAspect(
      Arrays.asList(
          MAIN_PDF_REPL,
          MAIN_PDF_REPL0,
          MAIN_PDF_REPL00,
          FEATURE_PDF_REPL,
          FEATURE_PDF_REPL0,
          FEATURE_PDF_REPL00,
          OA_PDF_REPL,
          OA_PDF_REPL0,
          OA_PDF_REPL00
      ),
      ArticleFiles.ROLE_FULL_TEXT_PDF
    );

    builder.addAspect(
      Arrays.asList(
          MAIN_XML_REPL,
          MAIN_XML_REPL0,
          MAIN_XML_REPL00,
          FEATURE_XML_REPL,
          FEATURE_XML_REPL0,
          FEATURE_XML_REPL00,
          OA_XML_REPL,
          OA_XML_REPL0,
          OA_XML_REPL00
      ),
      ArticleFiles.ROLE_FULL_TEXT_XML
    );

    builder.setFullTextFromRoles(
      ArticleFiles.ROLE_FULL_TEXT_PDF,
      ArticleFiles.ROLE_ABSTRACT
    );

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
