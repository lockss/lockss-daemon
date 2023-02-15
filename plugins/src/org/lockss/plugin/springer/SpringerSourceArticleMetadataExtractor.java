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

package org.lockss.plugin.springer;

import java.io.IOException;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class SpringerSourceArticleMetadataExtractor implements ArticleMetadataExtractor{

  private static Logger log = Logger.getLogger(SpringerSourceArticleMetadataExtractor.class);

  public SpringerSourceArticleMetadataExtractor() 
  {
    super();
  }

  /** For standard bibiographic metadata fields for which the extractor did
   * not produce a valid value, fill in a value from the TDB if available.
   * @param af the ArticleFiles on which extract() was called.
   * @param cu the CachedUrl selected by {@link #getCuToExtract(ArticleFiles)}.
   * @param am the ArticleMetadata being emitted.
   */
  protected void addTdbDefaults(ArticleFiles af, CachedUrl cu, ArticleMetadata am) {
    if (log.isDebug3()) log.debug3("adding("+af.getFullTextUrl());
    am.putIfBetter(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
    if (log.isDebug3()) log.debug3("am: ("+am + ")");
  }

  @Override
  public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
      throws IOException, PluginException {

    SpringerEmitter emit = new SpringerEmitter(af,emitter);

    CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
            
    FileMetadataExtractor me = null;

    if(cu != null)
    {
      try{
        me = cu.getFileMetadataExtractor(target);

        if(me != null) {
          me.extract(target, cu, emit);
        } else {
          ArticleMetadata am = new ArticleMetadata();
          //emit.emitMetadata(cu, am);
          emit.emitMetadata(cu, am);
        }

      } catch (RuntimeException e) {
        log.debug3("for af (" + af + ")", e);
        
        if (me != null)
          try{
            ArticleMetadata am = new ArticleMetadata();
            emit.emitMetadata(cu, am);
          } 
          catch (RuntimeException e2) {
            log.debug("retry with default metadata for af (" + af + ")", e2);
          }
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
  }

  class SpringerEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;

    SpringerEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
    addTdbDefaults(af, cu, am);
      parent.emitMetadata(af, am);
    }

    void setParentEmitter(Emitter parent) {
      this.parent = parent;
    }
  }
}