/*
 * $Id: BePressArticleIteratorFactory.java,v 1.9.12.1 2012-06-20 00:03:09 nchondros Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class BePressArticleIteratorFactory
  implements ArticleIteratorFactory,
	     ArticleMetadataExtractorFactory {
  
  /**
   * <p>An article iterator factory, for the Section plugin variant</p>
   * @author Thib Guicherd-Callin
   */
  public static class Section implements ArticleIteratorFactory {
    
    protected static final String ROOT_TEMPLATE = "\"%s%s/%s\", base_url, journal_abbr, journal_section";
    
    protected static final String PATTERN_TEMPLATE = "\"^%s%s/%s/((([^0-9]+/)?(vol)?%d/(iss)?[0-9]+/(art|editorial)?[0-9]+)|(vol%d/(?-i:[A-Z])[0-9]+))$\", base_url, journal_abbr, journal_section, volume, volume";

    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
        throws PluginException {
      return new BePressArticleIterator(au,
                                        new SubTreeArticleIterator.Spec()
                                            .setTarget(target)
                                            .setRootTemplate(ROOT_TEMPLATE)
                                            .setPatternTemplate(PATTERN_TEMPLATE),
                                        true);
    }
    
  }
  
  protected static Logger log = Logger.getLogger("BePressArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s%s\", base_url, journal_abbr";
  
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/((([^0-9]+/)?(vol)?%d/(iss)?[0-9]+/(art|editorial)?[0-9]+)|(vol%d/(?-i:[A-Z])[0-9]+))$\", base_url, journal_abbr, volume, volume";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new BePressArticleIterator(au,
                                      new SubTreeArticleIterator.Spec()
				          .setTarget(target)
				          .setRootTemplate(ROOT_TEMPLATE)
				          .setPatternTemplate(PATTERN_TEMPLATE),
				      false);
  }
  
  protected static class BePressArticleIterator extends SubTreeArticleIterator {
    
    protected Pattern pattern;
    
    public BePressArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec,
                                  boolean isSection) {
      super(au, spec);
      String volumeAsString = au.getConfiguration().get(ConfigParamDescr.VOLUME_NUMBER.getKey());
      String journalAbbr = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ABBR.getKey());
      if (isSection) {
        journalAbbr = journalAbbr + "/" + au.getConfiguration().get("journal_section");
      }
      this.pattern = Pattern.compile(String.format("/%s/((([^0-9]+/)?(vol)?%s/(iss)?[0-9]+/(art)?[0-9]+)|(vol%s/(?-i:[A-Z])[0-9]+))$", journalAbbr, volumeAsString, volumeAsString), Pattern.CASE_INSENSITIVE);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = pattern.matcher(url);
      if (mat.find()) {
        return processUrl(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processUrl(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      // XXX Full text PDF link embedded in page, cannot guess URL
      return af;
    }
    
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }
}
