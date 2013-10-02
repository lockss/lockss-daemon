/*
 * $Id: PortlandPressArticleIteratorFactory.java,v 1.4 2013-10-02 19:15:14 etenbrink Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.portlandpress;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class PortlandPressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
      Logger.getLogger("PortlandPressArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE =
      "\"%s%s/%s/\", base_url, journal_id, volume_name";  
  
  // pick up the abstract as the logical definition of one article
  // - lives one level higher than pdf & fulltext
  // pick up <lettersnums>.htm, but not <lettersnums>add.htm
  protected static final String PATTERN_TEMPLATE =
      "\"^%s%s/%s/(?![^/]+add[.]htm)%.3s[^/]+[.]htm\", " +
      "base_url,journal_id, volume_name, journal_id";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    /*
     * Article lives at three locations:  
     * <baseurl>/<jid>/<volnum>/                : Abstract
     *           <lettersnums>.htm
     *           <lettersnums>add.htm - supplementary stuff
     *           <lettersnums>add.mov  - ex - quicktime movie
     *           <lettersnums>add.pdf - ex - online data
     * <base-url>/<jid>/<volnum>/startpagenum/  : PDF & legacy html
     *           <lettersnums>.htm
     *           <lettersnums>.pdf  (note that pdf filename does not start with jid)
     * <base-url>/<jid>/ev/<volnum>/<stpage>    : Enhanced full text version
     *           <lettersnums_ev.htm>
     * notes: startpagenum can have letters in it
     * lettersnums seems to be concatenated <jid{max 3 chars}><volnum><startpagenum>
     *    except for pdf which is <volnum><startpagenum>
     */
    
    // various aspects of an article
    // http://www.clinsci.org/cs/120/cs1200013add.htm
    // http://essays.biochemistry.org/bsessays/048/bse0480001.htm
    // http://essays.biochemistry.org/bsessays/048/0001/0480001.pdf
    // http://essays.biochemistry.org/bsessays/048/0001/bse0480001.htm
    // http://essays.biochemistry.org/bsessays/ev/048/0045/bse0480045_ev.htm
    // http://essays.biochemistry.org/bsessays/ev/048/0045/bse0480045_evf01.htm
    // http://essays.biochemistry.org/bsessays/ev/048/0045/bse0480045_evrefs.htm
    // http://essays.biochemistry.org/bsessays/ev/048/0045/bse0480045_evtab01.htm
    // http://essays.biochemistry.org/bsessays/ev/048/0045/bse0480045_evtext01.htm
    // http://essays.biochemistry.org/bsessays/ev/048/0045/bse0480045_evtitle.htm
    
    // Identify groups in the pattern "/(<jid>)/(<volnum>)/(<articlenum>).htm
    // articlenum is <jid><volnum><pageno> and we need pageno to find the content files
    final Pattern ABSTRACT_PATTERN = Pattern.compile(
        "/([^/]{1,3})([^/]*)/([^/]+)/\\1\\3([^/]+)[.]htm$", Pattern.CASE_INSENSITIVE);
    
    final Pattern HTML_PATTERN = Pattern.compile(
        "/([^/]{1,3})([^/]*)/([^/]+)/([^/]+)/\\1\\3\\4[.]htm$", Pattern.CASE_INSENSITIVE);
    
    final Pattern PDF_PATTERN = Pattern.compile(
        "/([^/]{1,3})([^/]*)/([^/]+)/([^/]+)/\\3\\4[.]pdf$", Pattern.CASE_INSENSITIVE);
    
    // how to change from one form (aspect) of article to another
    final String ABSTRACT_REPLACEMENT = "/$1$2/$3/$1$3$4.htm";
    final String HTML_REPLACEMENT = "/$1$2/$3/$4/$1$3$4.htm";
    final String PDF_REPLACEMENT = "/$1$2/$3/$4/$3$4.pdf";
    final String ADD_REPLACEMENT = "/$1$2/$3/$1$3$4add.htm";
    final String EV_REPLACEMENT = "/$1$2/ev/$3/$4/$1$3$4_ev.htm";
    
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means it is considered a FULL_TEXT_CU
    // until this fulltext concept is deprecated
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    builder.addAspect(
        EV_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up *add.htm to be a suppl aspect
    builder.addAspect(ADD_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML, 
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
