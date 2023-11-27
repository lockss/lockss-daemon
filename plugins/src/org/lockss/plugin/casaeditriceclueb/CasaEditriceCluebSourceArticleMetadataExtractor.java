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

import java.io.IOException;
import java.util.HashMap;

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

public class CasaEditriceCluebSourceArticleMetadataExtractor implements ArticleMetadataExtractor{

	private CluebEmitter emit = null;
	private static Logger log = Logger.getLogger("CluebSourceArticleMetadataExtractor");
	private static HashMap<String, String> metadata;
	
	public CasaEditriceCluebSourceArticleMetadataExtractor(HashMap<String, String> metadataMap) 
	{
		super();
		metadata = metadataMap;
	}
	
	 protected void addAccessUrl(ArticleMetadata am, CachedUrl cu) 
	 {
		    if (!am.hasValidValue(MetadataField.FIELD_ACCESS_URL)) 
		      am.put(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
	 }
	
	@Override
	public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
			throws IOException, PluginException {
		if(emit == null)
			emit = new CluebEmitter(af,emitter);
		
		CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
		FileMetadataExtractor me = null;
	    
		if(cu != null)
		{
			try{
				me = cu.getFileMetadataExtractor(target);
				
				if(me != null)
					me.extract(target, cu, emit);
				else
					emit.emitMetadata(cu, getDefaultArticleMetadata(cu));

			} catch (RuntimeException e) {
				log.debug("for af (" + af + ")", e);
				
				if(me != null)
					try{
						emit.emitMetadata(cu, getDefaultArticleMetadata(cu));
					}
					catch (RuntimeException e2) {
						log.debug("retry with default metadata for af (" + af + ")", e2);
					}
			} finally {
				AuUtil.safeRelease(cu);
			}
		}
	  }
	
	ArticleMetadata getDefaultArticleMetadata(CachedUrl cu) {
	    TitleConfig tc = cu.getArchivalUnit().getTitleConfig();
	    TdbAu tdbau = (tc == null) ? null : tc.getTdbAu();
	    String year = (tdbau == null) ? null : tdbau.getStartYear();
	    String journalTitle = (tdbau == null) ? null : tdbau.getJournalTitle();

	    ArticleMetadata md = new ArticleMetadata();
	    md.put(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
	    if (year != null) md.put(MetadataField.FIELD_DATE, year);
	    if (journalTitle != null) md.put(MetadataField.FIELD_JOURNAL_TITLE,
	                                       journalTitle);
	    return md;
	  }
	
	class CluebEmitter implements FileMetadataExtractor.Emitter {
	    private Emitter parent;
	    private ArticleFiles af;

	    CluebEmitter(ArticleFiles af, Emitter parent) {
	      this.af = af;
	      this.parent = parent;
	    }

	    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
	      addAccessUrl(am, cu);
	      parent.emitMetadata(af, am);
	    }
	    
	    String getMetadata(CachedUrl cu) {
	    	return metadata.get(cu.getUrl());
	    }

	    void setParentEmitter(Emitter parent) {
	      this.parent = parent;
	    }
	  }
}