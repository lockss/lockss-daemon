package org.lockss.plugin.interresearch;

import org.lockss.config.TdbAu;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class InterResearchArticleIterator
    implements ArticleIteratorFactory,
    ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(InterResearchArticleIterator.class);

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"%sabstracts/%s/v.*\", base_url, journal_id";

  // https://www.int-res.com/abstracts/dao/v143/p205-226
  //          there is sometimes an issue directory, the page count increments seamlessly as the issue number changes.
  // https://www.int-res.com/abstracts/cr/v78/n1/p83-101/
  // https://www.int-res.com/abstracts/cr/v78/n2/p103-116
  protected static final Pattern ABS_PATTERN = Pattern.compile("abstracts/[^/]+/v(\\d+)(/n\\d+)?/p(\\d+)-(\\d+)$");
  protected static String ABS_REPL;

  // https://www.int-res.com/articles/feature/d143p205.pdf
  // https://www.int-res.com/articles/dao_oa/d143p169.pdf
  // https://www.int-res.com/articles/cr_oa/c078p237.pdf
  // https://www.int-res.com/articles/ab2021/30/b030p001.pdf
  // https://www.int-res.com/articles/dao2021/143/d143p159.pdf
  // https://www.int-res.com/articles/cr2019/78/c078p103.pdf
  // PDF pattern can't be used to make an Abstract replacement because of the end_page in the abstract that doesn't
  // exist in the PDF url.

  // wow, now the pattern is totally non guessable
  // https://www.int-res.com/abstracts/aei/v12/p1-10
  // https://www.int-res.com/articles/aei2020/12/q012p001.pdf -- note the "q"

  // https://www.int-res.com/articles/xml/aei/12/q012p001.xml

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    String jid = au.getConfiguration().get("journal_id");
    // make abstract replacement now that we have the jid.
    ABS_REPL = String.format("abstracts/%s/v$1$2/p$3-$4", jid);
    // there are three different paths that the pdfs and xmls are in
    List<String> paths = Arrays.asList(
        "feature/",
        String.format("%s_oa/", jid),
        // the volume is appended to the <vol><year>/ construction
        String.format("%s%s/$1/", jid, au.getConfiguration().get("year"))
    );

    // the pdf and xml replacements are big lists. as the volume and page numbers are both zero padded to 3 characters.
    List<String> zero_padding = Arrays.asList("00", "0", "");
    List<String> PDF_REPLACEMENTS = new java.util.ArrayList<>();
    List<String> XML_REPLACEMENTS = new java.util.ArrayList<>();
    // iterate over the various zero paddings, and url paths for the pdfs and xmls.
    // also add each letter journal id, this happens when the first letter is taken already, and sometimes its another
    // letter entirely, see @InterResearchArticleMetadataExtractor
    for (String page_padding : zero_padding) {
      for (String vol_padding : zero_padding) {
        for (char letter : jid.toCharArray()) {
          // xml files are always the same path,
          XML_REPLACEMENTS.add(String.format("articles/xml/%s/$1/%s%s$1p%s$3.xml", jid, letter, vol_padding, page_padding));
          for (String path : paths) {
            PDF_REPLACEMENTS.add(String.format("articles/%s%s%s$1p%s$3.pdf", path, letter, vol_padding, page_padding));
          }
        }
      }
    }

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
      PDF_REPLACEMENTS,
      ArticleFiles.ROLE_FULL_TEXT_PDF
    );

    builder.addAspect(
      XML_REPLACEMENTS,
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
    return new InterResearchArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
