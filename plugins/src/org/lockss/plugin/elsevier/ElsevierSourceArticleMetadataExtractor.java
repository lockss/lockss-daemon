/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.elsevier;

import java.io.IOException;
import java.util.HashSet;

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

public class ElsevierSourceArticleMetadataExtractor implements ArticleMetadataExtractor{

  private ElsevierEmitter emit = null;
  private static final Logger log = Logger.getLogger(ElsevierSourceArticleMetadataExtractor.class);

  protected void addAccessUrl(ArticleMetadata am, ArticleFiles af) 
  {
    if (!am.hasValidValue(MetadataField.FIELD_ACCESS_URL)) 
      am.put(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
  }

  @Override
  public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
      throws IOException, PluginException {
    if (emit == null)
      emit = new ElsevierEmitter(af,emitter);
    //This is redundant on the creation pass
    //when called from ListObjects a new emitter is passed in each time.
    emit.setParentEmitter(emitter);
    emit.setArticleFiles(af);
        	
    if (emit.hasEmitted(af.getFullTextUrl()))
      return;

    CachedUrl cu = af.getFullTextCu();
    FileMetadataExtractor me = null;

    if (cu != null) {
      try {
        me = cu.getFileMetadataExtractor(target);

        if (me != null) {
          me.extract(target, cu, emit);
        } else {
          emit.emitMetadata(cu, getDefaultArticleMetadata(af, cu));
        }

      } catch (RuntimeException e) {
        log.debug("for af (" + af + ")", e);

        if (me != null) 
          try {
            emit.emitMetadata(cu, getDefaultArticleMetadata(af, cu));
          } catch (RuntimeException e2) {
            log.debug("retry with default metadata for af (" + af + ")", e2);
          }
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
  }

  ArticleMetadata getDefaultArticleMetadata(ArticleFiles af, CachedUrl cu) {
    TitleConfig tc = cu.getArchivalUnit().getTitleConfig();
    TdbAu tdbau = (tc == null) ? null : tc.getTdbAu();
    String year = (tdbau == null) ? null : tdbau.getStartYear();
    String publisher = (tdbau == null) ? "Elsevier" : tdbau.getPublisherName();

    ArticleMetadata md = new ArticleMetadata();
    md.put(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
    if (year != null) md.put(MetadataField.FIELD_DATE, year);
    if (publisher != null) md.put(MetadataField.FIELD_PUBLISHER, publisher);
    return md;
  }

  class ElsevierEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;
    private HashSet<String> collectedArticles;

    ElsevierEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
      collectedArticles = new HashSet<String>();
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
      if (collectedArticles.contains(cu.getUrl())) { 
    	log.debug3("˙: " + cu.getUrl());
        return;
      }
      
      collectedArticles.add(cu.getUrl());
      addAccessUrl(am, af);
      if (log.isDebug3()) {
        log.debug3("emitMetadata():\n" + am.ppString(0));
      }
      parent.emitMetadata(af, am);
    }
    
    /*
     * ListObjects calls repeatedly with different values
     * allow the code to reset 
     */
    void setParentEmitter(Emitter parent) {
        this.parent = parent;
    }
    void setArticleFiles(ArticleFiles af) {
        this.af = af;
    }

    public boolean hasEmitted(String url)
    {
      return collectedArticles.contains(url);
    }
  }
}