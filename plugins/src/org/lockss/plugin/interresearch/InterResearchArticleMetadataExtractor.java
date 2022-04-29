package org.lockss.plugin.interresearch;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;


public class InterResearchArticleMetadataExtractor extends BaseArticleMetadataExtractor {
  protected static Logger log = Logger.getLogger(InterResearchArticleMetadataExtractor.class);

  public InterResearchArticleMetadataExtractor(String roleArticleMetadata) {
    super(roleArticleMetadata);
  }

  protected void assignRoleFromMd(ArticleFiles af,
                                  CachedUrl cu,
                                  ArticleMetadata am,
                                  String role,
                                  String metaTag) {
    String md_url = am.getRaw(metaTag);
    if (md_url == null) {
      return;
    }
    // check if the pdf_url exists in the crawl, if not, return.
    CachedUrl testCu = cu.getArchivalUnit().makeCachedUrl(md_url);
    if ((testCu == null) || (!testCu.hasContent())) {
      return;
    }
    // if the md_url is already the roleurl, there is nothing to do
    String ft_url = af.getRoleUrl(role);
    if (md_url == ft_url) {
      return;
    }
    af.setRoleCu(role, testCu);
    // finally, if the url is a pdf, set it to the fulltextcu
    if (role == ArticleFiles.ROLE_FULL_TEXT_PDF) {
      af.setFullTextCu(testCu);
    }
    return;
  }

  class InterResearchEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;


    InterResearchEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {

      if (isAddTdbDefaults()) {
        addTdbDefaults(af, cu, am);
      }
      if (isCheckAccessUrl()) {
        checkAccessUrl(af, cu, am);
      }
      assignRoleFromMd(af, cu, am, ArticleFiles.ROLE_FULL_TEXT_PDF, "citation_pdf_url");
      assignRoleFromMd(af, cu, am, ArticleFiles.ROLE_FULL_TEXT_XML, "citation_xml_url");
      parent.emitMetadata(af, am);
    }

  }

  public void extract(MetadataTarget target, ArticleFiles af,
                      ArticleMetadataExtractor.Emitter emitter)
      throws IOException, PluginException {

    InterResearchEmitter myEmitter = new InterResearchEmitter(af, emitter);
    CachedUrl cu = getCuToExtract(af);
    if (log.isDebug3()) log.debug3("extract(" + af + "), cu: " + cu);
    if (cu != null) {
      try {
        FileMetadataExtractor me = cu.getFileMetadataExtractor(target);
        if (me != null) {
          me.extract(target, cu, myEmitter);
          return;
        }
      } catch (IOException ex) {
        log.warning("Error in FileMetadataExtractor", ex);
      } finally {
        AuUtil.safeRelease(cu);
      }
    } else {
      // get full-text CU if cuRole CU not present
      cu = af.getFullTextCu();
      if (log.isDebug3()) {
        log.debug3("Missing CU for role " + cuRole
            + ". Using fullTextCU " + af.getFullTextUrl());
      }
    }
    // Here if cuRole wasn't present or extractor threw IOException
    if (cu != null) {
      try {
        if (log.isDebug3()) {
          log.debug3("Storing tdb info for  " + cu.getUrl());
        }
        ArticleMetadata am = new ArticleMetadata();
        myEmitter.emitMetadata(cu, am);
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
  }

}
