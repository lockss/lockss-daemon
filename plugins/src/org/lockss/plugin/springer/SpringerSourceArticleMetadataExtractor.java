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