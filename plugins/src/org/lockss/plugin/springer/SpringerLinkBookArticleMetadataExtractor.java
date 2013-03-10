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

package org.lockss.plugin.springer;

import java.io.IOException;

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

public class SpringerLinkBookArticleMetadataExtractor implements ArticleMetadataExtractor{

//	private SpringerEmitter emit = null;
	private static Logger log = Logger.getLogger("SpringerLinkBookArticleMetadataExtractor");

	public SpringerLinkBookArticleMetadataExtractor()
	{
		super();
	}

	 protected void addAccessUrl(ArticleMetadata am, CachedUrl cu)
	 {
		    if (!am.hasValidValue(MetadataField.FIELD_ACCESS_URL))
		      am.put(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
	 }

	@Override
	public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
			throws IOException, PluginException {

	  /* create a new SpringerEmitter off the passed in emitter */
	  SpringerEmitter emit = new SpringerEmitter(af, emitter);

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

	class SpringerEmitter implements FileMetadataExtractor.Emitter {
	    private Emitter parent;
	    private ArticleFiles af;

	    SpringerEmitter(ArticleFiles af, Emitter parent) {
	      this.af = af;
	      this.parent = parent;
	    }

	    @Override
      public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
	      addAccessUrl(am, cu);
	      parent.emitMetadata(af, am);
	    }

	    void setParentEmitter(Emitter parent) {
	      this.parent = parent;
	    }
	  }
}