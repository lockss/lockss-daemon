/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.associationforcomputingmachinery;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AssociationForComputingMachineryArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("ACMArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";

  protected static final String PATTERN_TEMPLATE = "\"%s%d/(\\d+[^/]+\\d+)/([^/]+-\\d+)/.*\\.(xml|pdf|html|mov)$\",base_url,year";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
      MetadataTarget target)
      throws PluginException {
    log.debug3("An ACMArticleIterator was initialized");
    return new ACMArticleIterator(au, new SubTreeArticleIterator.Spec()
    .setTarget(target)
    .setRootTemplate(ROOT_TEMPLATE)
    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }

  protected static class ACMArticleIterator extends SubTreeArticleIterator {

    protected static Pattern pdfPattern = Pattern.compile("(.*/[\\d]+[^/]+[\\d]+/)([^/]+[\\d]+)(/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    protected static Pattern xmlPattern = Pattern.compile("(.*/[\\d]+[^/]+[\\d]+/)([^/]+[\\d]+)(/[^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);
    //html files have an extra directory layer:
    //http://clockss-ingest.lockss.org/sourcefiles/acm-dev/2011/4oct2011/NEW-MAG-ELERN-V2011I9-2025356/2025357/110906_i_bozarth.html
    protected static Pattern htmlPattern = Pattern.compile("(.*/[\\d]+[^/]+[\\d]+/)([^/]+[\\d]+)(/[^/]+)?(/[^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
    protected static Pattern movPattern = Pattern.compile("(.*/[\\d]+[^/]+[\\d]+/)([^/]+[\\d]+)(/[^/]+)\\.mov$", Pattern.CASE_INSENSITIVE);
    private enum acmFileType {PDF, XML, HTML, MOV};
    public static class acmArticleFiles extends ArticleFiles {
      public static final String ROLE_FULL_VIDEO_MOV = "FullVideoMov";
        acmArticleFiles (String url) { 
          super();
        }
    }
    
    protected ACMArticleIterator(ArchivalUnit au,
        SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }

    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      
      log.debug3("createArticleFiles("+cu+")");

      // there will be mostly .pdf files, then fewer .xml files, 
      // then fewer still html files and fewest .mov files
      Matcher mat = pdfPattern.matcher(url);
      if (mat.find()) {
        return processAcmFile(cu, mat, acmFileType.PDF);
      } else if ((mat = xmlPattern.matcher(url)).find()){
          return processAcmFile(cu, mat, acmFileType.XML);
      } else if ((mat = htmlPattern.matcher(url)).find()){
          return processAcmFile(cu, mat, acmFileType.HTML);
      } else if ((mat = movPattern.matcher(url)).find()){
          return processAcmFile(cu, mat, acmFileType.MOV);
      } 
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    protected ArticleFiles processAcmFile(CachedUrl cu, Matcher mat, acmFileType type) {
      ArticleFiles af = new ArticleFiles();
      switch (type) {
        case PDF: af.setFullTextCu(cu);
                  log.debug3("process PDF: "+cu);
                  if(spec.getTarget() != MetadataTarget.Article()){
                    af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
                    //guessMetadataFile(af, mat);
                  }
                  break;
        case XML: af.setFullTextCu(cu);
                  log.debug3("process XML: "+cu);
                  if(cu != null && cu.hasContent()) {
                    af.setRoleCu(ArticleFiles.ROLE_ISSUE_METADATA, cu);
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);        
                  }
                  break;
        case HTML: af.setFullTextCu(cu);
                   log.debug3("process HTML: "+cu);
                   if(cu != null && cu.hasContent()) {
                     af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, cu);
                   }
                  break;
        case MOV: af.setFullTextCu(cu);
                  log.debug3("process MOV: "+cu);
                  if(cu != null && cu.hasContent()) {
                    af.setRoleCu(acmArticleFiles.ROLE_FULL_VIDEO_MOV, cu);
                  }
                  break;
        default:  log.warning("Invalid Filetype: "+type+" for " + cu);
                  return null;
      }
      return af;
    }

    protected void guessMetadataFile(ArticleFiles af, Matcher mat){
      CachedUrl metadataCu = au.makeCachedUrl(mat.replaceFirst("$1$2/$2.xml"));
      if(metadataCu != null && metadataCu.hasContent()) {
        //af.setRoleCu(ArticleFiles.ROLE_ISSUE_METADATA, metadataCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, metadataCu);
        
      }
    }
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
  throws PluginException {
    // using the ACMArticleMetadataExtractor causes only the metadata from the
    // first .xml file be emitted.  It tries to limit what is emitted
    //return new AssociationForComputingMachineryArticleMetadataExtractor();  
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);

  }
}
