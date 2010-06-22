/*
 * $Id: SubTreeArticleIterator.java,v 1.7 2010-06-22 09:00:18 tlipkis Exp $
 */

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

package org.lockss.plugin;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;
import org.lockss.extractor.*;


/*
 * Article iterator that finds articles by iterating through all the
 * CachedUrls of the AU, or through specific subtrees, visiting those that
 * match a MIME type and/or regular expression, or a subclass-specified
 * condition.  Fir each node visited, and ArticleFiles may (or, by a
 * sobclass, may not) be generated.
 */
public class SubTreeArticleIterator implements Iterator<ArticleFiles> {
  
  static Logger log = Logger.getLogger("SubTreeArticleIterator");
  
  public static final String DEFAULT_MIME_TYPE = null;

  /** Specification of the CachedUrls the iterator should return.  Setters
   * are chained. */
  public static class Spec {
    private MetadataTarget target;
    private String mimeType;
    private List<String> roots;
    private List<String> rootTemplates;
    private Pattern pat;
    private String patTempl;
    private int patFlags = 0;		// Pattern compilation flags


    /** Set the MIME type of the desired files.  If null or not set, MIME
     * type is ignored. */
    public Spec setMimeType(String val) {
      mimeType = val;
      return this;
    }

    /** The MetadataTarget determines the type of articles desired */
    public Spec setTarget(MetadataTarget val) {
      target = val;
      return this;
    }

    /** Set the URL of root of the subtree below which to iterate. */
    public Spec setRoot(String root) {
      return setRoots(ListUtil.list(root));
    }

    /** Set the URL(s) of the root(s) to the result of expanding the printf
     * template. */
    public Spec setRootTemplate(String rootTemplate) {
      return setRootTemplates(ListUtil.list(rootTemplate));
    }

    /** Set the URLs of multiple roots of the subtree below which to
     * iterate. */
    public Spec setRoots(List<String> roots) {
      if (rootTemplates != null) {
	throw new
	  IllegalArgumentException("Can't set both roots and rootTemplates");
      }
      this.roots = roots;
      return this;
    }

    /** Set the URLs of the roots to the result of expanding the printf
     * templates. */
    public Spec setRootTemplates(List<String> rootTemplates) {
      if (roots != null) {
	throw new
	  IllegalArgumentException("Can't set both roots and rootTemplates");
      }
      this.rootTemplates = rootTemplates;
      return this;
    }

    /** Set the regular expression the article URLs must match */
    public Spec setPattern(Pattern regex) {
      pat = regex;
      return this;
    }

    /** Set the regular expression the article URLs must match */
    public Spec setPattern(String regex) {
      return setPattern(regex, 0);
    }

    /** Set the regular expression the article URLs must match */
    public Spec setPattern(String regex, int flags) {
      if (patTempl != null) {
	throw new IllegalArgumentException("Can't set both pattern and patternTemplate");
      }
      pat = Pattern.compile(regex, flags);
      return this;
    }

    public Spec setPatternTemplate(String patternTemplate) {
      return setPatternTemplate(patternTemplate, 0);
    }

    public Spec setPatternTemplate(String patternTemplate, int flags) {
      if (pat != null) {
	throw new IllegalArgumentException("Can't set both pattern and patternTemplate");
      }
      this.patTempl = patternTemplate;
      this.patFlags = flags;
      return this;
    }
  }

  Spec spec;
  String mimeType;
  ArchivalUnit au;
  Iterator it = null;
  Pattern pat = null;
  private Iterator cusIter = null;
  private Iterator<CachedUrlSet> rootIter = null;

  
  // if null, we have to look for nextElement
  private ArticleFiles nextElement = null;


  public SubTreeArticleIterator(ArchivalUnit au, Spec spec) {
    this.au = au;
    this.spec = spec;
    mimeType = getMimeType();
    Collection<CachedUrlSet> roots = makeRoots();
    this.pat = makePattern();
    rootIter = roots.iterator();
    log.debug("Create: AU: " + au.getName() + ", Mime: " + this.mimeType
	      + ", roots: " + roots + ", pat: " + pat);
  }

  // XXX fix when work out how target is used
  private String getMimeType() {
    String tmpMime = spec.target != null ? spec.target.getFormat() : null;
    if (tmpMime == null) {
      tmpMime = spec.mimeType;
    }
    if (tmpMime == null) {
      tmpMime = au.getPlugin().getDefaultArticleMimeType();
    }
    if (tmpMime == null) {
      tmpMime = DEFAULT_MIME_TYPE;
    }
    return tmpMime;
  }

  private Pattern makePattern() {
    if (spec.pat != null) {
      return spec.pat;
    }
    if (spec.patTempl != null) {
      String re = convertVariableRegexpString(spec.patTempl).getRegexp();
      return Pattern.compile(re, spec.patFlags);
    }
    return null;
  }

  private Collection<CachedUrlSet> makeRoots() {
    Collection<String> roots = makeRootUrls();
    log.debug("rootUrls: " + roots);
    if (roots == null || roots.isEmpty()) {
      return ListUtil.list(au.getAuCachedUrlSet());
    }
    Collection<CachedUrlSet> res = new ArrayList<CachedUrlSet>();
    for (String root : roots) {
      res.add(au.makeCachedUrlSet(new RangeCachedUrlSetSpec(root)));
    }
    return res;
  }

  private Collection<String> makeRootUrls() {
    if (spec.roots != null) {
      return spec.roots;
    }
    if (spec.rootTemplates == null) {
      return null;
    }
    Collection<String> res = new ArrayList<String>();
    for (String template : spec.rootTemplates) {
      List<String> lst = convertUrlList(template);
      if (lst == null) {
	log.warning("Null converted string from " + template);
	continue;
      }
      res.addAll(lst);
    }
    return res;
  }

  List<String> convertUrlList(String printfString) {
    log.debug("convert("+printfString+"): "+ au);
    log.debug("params: " + au.getProperties());
    return new PrintfConverter.UrlListConverter(au).getUrlList(printfString);
  }

  PrintfConverter.MatchPattern
    convertVariableRegexpString(String printfString) {
    log.debug("reconvert("+printfString+"): "+ au);
    return new PrintfConverter.RegexpConverter(au).getMatchPattern(printfString);
  }

  private ArticleFiles findNextElement() {
    if (nextElement != null) {
      return nextElement;
    }
    while (true) {
      CachedUrl cu = null;
      try {
	if (cusIter == null || !cusIter.hasNext()) {
	  if (!rootIter.hasNext()) {
	    return null;
	  } else {
	    CachedUrlSet root = rootIter.next();
	    cusIter = root.contentHashIterator();
	    continue;
	  }
	} else {
	  CachedUrlSetNode node = (CachedUrlSetNode)cusIter.next();
	  if (node instanceof CachedUrl) {
	    cu = (CachedUrl)node;
	    if (isArticleCu(cu)) {
	      nextElement = createArticleFiles(cu);
	      if (nextElement == null) {
		continue;
	      }
	      return nextElement;
	    }
	  }
	}
      } catch (Exception ex) {
	// No action intended - iterator should ignore this cu.
	if (cu == null) {
	  log.error("Error", ex);
	} else {
	  log.error("Error processing " + cu.getUrl(), ex);
	}
      } finally {
	AuUtil.safeRelease(cu);
      }
    }
  }

  /** Default implementation creates an ArticleFiles with the full text CU
   * set to the visited CU.  Override to create a more complex
   * ArticleFiles */
  protected ArticleFiles createArticleFiles(CachedUrl cu) {
    ArticleFiles res = new ArticleFiles();
    res.setFullTextCu(cu);
    return res;
  }


  /** Return true if the CachedUrl is of the desired MIME type and its URL
   * matches the regular expression.  Override for other article
   * criteria */
  protected boolean isArticleCu(CachedUrl cu) {
    log.debug3("isArticleCu(" + cu.getUrl() + ")");

    if (!cu.hasContent()) {
      log.debug3("No content for: " + cu.getUrl());
      return false;
    }
    // Match pattern first; it's cheaper than getContentType()
    if (pat != null) {
      Matcher match = pat.matcher(cu.getUrl());
      if (!match.find()) {
	log.debug3("No match for " + pat + ": " + cu.getUrl());
	return false;
      }
    }
    if (mimeType != null) {
      String cuMime =
	HeaderUtil.getMimeTypeFromContentType(cu.getContentType());
      if (!mimeType.equalsIgnoreCase(cuMime)) {
	log.debug3("Mime mismatch (" + mimeType + "): " + cu.getUrl()
		   + "(" + cu.getContentType() + ")");
	return false;
      }
    }
    return true;
  }

  public boolean hasNext() {
    return findNextElement() != null;
  }

  public ArticleFiles next() {
    ArticleFiles element = findNextElement();
    nextElement = null;

    if (element != null) {
      return element;
    }
    throw new NoSuchElementException();
  }
  
  public void remove() {
    throw new UnsupportedOperationException("Not implemented");
  }
  
}