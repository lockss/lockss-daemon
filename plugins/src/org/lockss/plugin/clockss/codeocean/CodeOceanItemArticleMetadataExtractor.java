/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.codeocean;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class CodeOceanItemArticleMetadataExtractor 
  implements ArticleMetadataExtractor{
	  private static final String CODE_OCEAN = "Code Ocean";
	  /// http://foo.com/sourcefiles/code-ocean/2018/test-publisher/333b30ee-8262-4210-9ec7-f6462048f893.zip
	  private static final Pattern  ZIP_PAT = Pattern.compile("/code-ocean-released/([^/]+)/([^/]+)/([^/]+)\\.zip", Pattern.CASE_INSENSITIVE);
  private COEmitter emit = null;
  private static final Logger log = 
      Logger.getLogger(CodeOceanItemArticleMetadataExtractor.class);
  
  

@Override
  public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
      throws IOException, PluginException {
    if (emit == null) {
      emit = new COEmitter(af, emitter);
    }
    
    CachedUrl metadataCu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      
    // base_url/2018/pubfoo/111blah222.zip
    // EITHER - we'll have a plugin with base_url and directory of "2018/pubfoo" per publisher
    // OR - we'll collect all 2018 stuff and then map each "pubfoo" to a specific publisher
    // not sure which yet - this is just a prototype/placeholder while we experiment
    if(metadataCu != null) {
    	    String zip_url = metadataCu.getUrl();
    		Matcher urlMat = ZIP_PAT.matcher(zip_url);
    		String year = null;
    		String publisher = null;
    		String file_id = null;
    		if (urlMat.find()) {
    			year = urlMat.group(1);
    			publisher = urlMat.group(2);
    			file_id = urlMat.group(3);
    		}
    	    
    	    ArticleMetadata md = new ArticleMetadata();
    	    md.put(MetadataField.FIELD_ACCESS_URL, metadataCu.getUrl());
    	    md.put(MetadataField.FIELD_PROVIDER, CODE_OCEAN);
    	    md.put(MetadataField.FIELD_PUBLISHER, publisher);
    	    md.put(MetadataField.FIELD_ARTICLE_TYPE, "file");
    	    md.put(MetadataField.FIELD_PROPRIETARY_IDENTIFIER, file_id);
    	    if (year != null) md.put(MetadataField.FIELD_DATE, year); // ingest year for now
    	    emit.emitMetadata(metadataCu, md);
    } 
  }
  
  
  class COEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;

    COEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
      parent.emitMetadata(af, am);
    }

    void setParentEmitter(Emitter parent) {
      this.parent = parent;
    }
  }
}