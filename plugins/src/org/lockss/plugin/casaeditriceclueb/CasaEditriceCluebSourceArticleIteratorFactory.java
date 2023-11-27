/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.casaeditriceclueb;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

public class CasaEditriceCluebSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("CasaEditriceCluebSourceArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
  														
  protected static final String PATTERN_TEMPLATE = "\"%s%d/CLUEB_chapters\\.zip!/[^/]+/\\d+\\.\\d+_\\d+\\.xml$\",base_url,year";
  
  protected static HashMap<String,String> metadataMap = new HashMap<String,String>();
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
	  log.debug3("A CluebSourceArticleIterator was initialized");
    return new CluebArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setRootTemplate(ROOT_TEMPLATE)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class CluebArticleIterator extends SubTreeArticleIterator {
	 
    protected static Pattern xmlPattern = Pattern.compile("(.*/)([^/]+/)([^/]+)(/[^/]+\\.xml)$", Pattern.CASE_INSENSITIVE);
    
    protected CluebArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      spec.setVisitArchiveMembers(true);
      metadataMap = new HashMap<String,String>();
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = xmlPattern.matcher(url);
      if (mat.find()) {
        return processFullText(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      
      if(cu.hasContent()) {
	      String pdfSequence = getPdfSequenceFrom(new BufferedReader(cu.openForReading()));
	      
	      CachedUrl fullTextCu = au.makeCachedUrl(mat.replaceFirst("$1$3"+pdfSequence+".pdf"));
	      if(fullTextCu != null && fullTextCu.hasContent()) {
	    	  af.setFullTextCu(fullTextCu);
	    	  af.setRole(ArticleFiles.ROLE_FULL_TEXT_PDF, fullTextCu);
	    	  metadataMap.put(fullTextCu.getUrl(), cu.getUrl());
	      }
      } else {
    	  af.setFullTextCu(cu);
      }
      
      if(af.getFullTextCu() == null)
    	  return null;
    
      return af;
    }
    
    private String getPdfSequenceFrom(BufferedReader r) {
      String line;

      try {
        for(line = r.readLine(); line != null; line = r.readLine()) {
          if(line.contains("<SequenceNumber>")) {
            return String.format("_%04d", Integer.parseInt(line.replaceAll("\\<[^>]*>","")));
          }
        }
      } catch (IOException e) {
        log.error("Metadata file could not be read: ",e);
      } finally {
        IOUtil.safeClose(r);
      }

      return null;
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new CasaEditriceCluebSourceArticleMetadataExtractor(metadataMap);
  }
}
