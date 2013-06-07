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
              BufferedReader bReader = new BufferedReader(cu.openForReading());
	      String pdfSequence = getPdfSequenceFrom(bReader);
	      IOUtil.safeClose(bReader);
	      
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
		}
    	
    	return null;
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
	    return new CasaEditriceCluebSourceArticleMetadataExtractor(metadataMap);
  }
}
