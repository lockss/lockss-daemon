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

public class AssociationForComputingMachineryArticleMetadataExtractor implements ArticleMetadataExtractor{

  private static Logger log = Logger.getLogger("AssociationForComputingMachineryArticleMetadataExtractor");

  protected String cuRole = null;
  protected boolean isAddTdbDefaults = true;

  /** Create an ArticleMetadataExtractor that applies a
   * FileMetadataExtractor to the {@link ArticleFiles}'s full text CU.
   */
  public AssociationForComputingMachineryArticleMetadataExtractor() {
  }

  /** Create an ArticleMetadataExtractor that applies a
   * FileMetadataExtractor to the CachedUrl associated with a named role.
   * @param cuRole the role from which to extract file metadata
   */
  public AssociationForComputingMachineryArticleMetadataExtractor(String cuRole) {
    this.cuRole = cuRole;

  }

  /** Determines whether the emitter will use the TDB to supply values for
   * ArticleMetadata fields that have no valid value.  This is true by
   * default.
   * @param val
   * @returns this, for chaining
   */
  public AssociationForComputingMachineryArticleMetadataExtractor setAddTdbDefaults(boolean val) {
    isAddTdbDefaults = val;
    return this;
  }

  /** Returns full text CU 
   *
   */
  protected CachedUrl getCuToExtract(ArticleFiles af) {
    //return cuRole != null ? af.getRoleCu(cuRole) : af.getFullTextCu();
    return af.getFullTextCu();
  }

  /** Return true if TDB defaults should be added to emitted ArticleMetadata.
   */
  protected boolean isAddTdbDefaults() {
    return isAddTdbDefaults;
  }

  /** For standard bibiographic metadata fields for which the extractor did
   * not produce a valid value, fill in a value from the TDB if available.
   * @param af the ArticleFiles on which extract() was called.
   * @param cu the CachedUrl selected by {@link #getCuToExtract(ArticleFiles)}.
   * @param am the ArticleMetadata being emitted.
   */
  protected void addTdbDefaults(ArticleFiles af,
                                CachedUrl cu, ArticleMetadata am) {
    if (log.isDebug3()) log.debug3("addTdbDefaults("+af+", "+cu+", "+am+")");
    if (!cu.getArchivalUnit().isBulkContent()) {
      // Fill in missing values rom TDB if TDB entries reflect bibliographic
      // information for the content. This is not the case for bulk data
      TitleConfig tc = cu.getArchivalUnit().getTitleConfig();
      if (log.isDebug3()) log.debug3("tc; "+tc);
      TdbAu tdbau = (tc == null) ? null : tc.getTdbAu();
      if (log.isDebug3()) log.debug3("tdbau; "+tdbau);
      if (tdbau != null) {
        if (log.isDebug3()) log.debug3("Adding data from " + tdbau + " to " + am);
        am.putIfBetter(MetadataField.FIELD_ISBN, tdbau.getPrintIsbn());
        am.putIfBetter(MetadataField.FIELD_EISBN, tdbau.getEisbn());
        am.putIfBetter(MetadataField.FIELD_ISSN, tdbau.getPrintIssn());
        am.putIfBetter(MetadataField.FIELD_EISSN, tdbau.getEissn());
        am.putIfBetter(MetadataField.FIELD_DATE, tdbau.getStartYear());
        am.putIfBetter(MetadataField.FIELD_VOLUME, tdbau.getStartVolume());
        am.putIfBetter(MetadataField.FIELD_ISSUE, tdbau.getStartIssue());
        am.putIfBetter(MetadataField.FIELD_JOURNAL_TITLE,tdbau.getJournalTitle());
        am.putIfBetter(MetadataField.FIELD_PUBLISHER,tdbau.getPublisherName());
      }
    }
    if (log.isDebug3()) log.debug3("adding("+af.getFullTextCu());
    am.putIfBetter(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
    if (log.isDebug3()) log.debug3("am: ("+am);
  }

  class MyEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;
   

    MyEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
      parent.emitMetadata(af, am);
    }

  }

  public void extract(MetadataTarget target, ArticleFiles af,
                      ArticleMetadataExtractor.Emitter emitter)
      throws IOException, PluginException {

    MyEmitter myEmitter = new MyEmitter(af, emitter);
    CachedUrl cu = getCuToExtract(af);
    // pass cu that's the ArticleFile
    // let the ACMXmlMetadataExtractor get the xml from the cu
    // so we can emit metadata per ArticleFile instead of all at once
    if (log.isDebug3()) log.debug3("extract(" + af + "), cu: " + cu);
    if (cu != null) {
      try {
        FileMetadataExtractor me = cu.getFileMetadataExtractor(target);
        if (me != null) {
          me.extract(target, cu, myEmitter);
          AuUtil.safeRelease(cu);
          return;
        }
      } catch (IOException ex) {
        log.warning("Error in FileMetadataExtractor", ex);
      }
      // generate default metadata in case of null filemetadataextractor or
      // IOException.

      ArticleMetadata am = new ArticleMetadata();
      myEmitter.emitMetadata(cu, am);
      AuUtil.safeRelease(cu);
    }
  }
}