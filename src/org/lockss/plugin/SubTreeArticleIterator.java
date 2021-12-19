/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.util.Constants.RegexpContext;
import org.lockss.daemon.*;
import org.lockss.extractor.*;


/**
 * Article iterator that finds articles by iterating through all the
 * CachedUrls of the AU, or through specific subtrees, visiting those that
 * match a MIME type and/or regular expression, or a subclass-specified
 * condition.  For each node visited, an ArticleFiles may (or, by a
 * subclass, may not) be generated.
 */
public class SubTreeArticleIterator implements Iterator<ArticleFiles> {
  
  static Logger log = Logger.getLogger(SubTreeArticleIterator.class);
  
  /** Specification of the CachedUrls the iterator should return.  Setters
   * are chained. */
  public static class Spec {
    private MetadataTarget target;
    private String mimeType;
    private List<String> roots;
    private List<String> rootTemplates;

    private PatSpec matchPatSpec;
    private PatSpec includePatSpec;
    private PatSpec excludePatSpec;

    /** If true, iterator will descend into archive files (zip, etc.) and
     * include their members rather than the archive file itself */
    private boolean isVisitArchiveMembers = false;


    /** Set the MIME type of the desired files.  If null or not set, MIME
     * type does not enter into  determination of which files to visit.
     * @param mimeType
     * @return this
     */
    public Spec setMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /** Return the desired MIME type */
    public String getMimeType() {
      return mimeType;
    }

    /** The MetadataTarget determines the type of articles desired.
     * Currently the format can be used to specify the MIME type.
     * @param target
     * @return this
     */
    public Spec setTarget(MetadataTarget target) {
      this.target = target;
      return this;
    }

    /** Return the target */
    public MetadataTarget getTarget() {
      return target;
    }

    /** Set the URL of the root of the subtree below which to iterate.
     * @param root subtree root
     * @return this
     */
    public Spec setRoot(String root) {
      return setRoots(ListUtil.list(root));
    }

    /** Set the URL(s) of the root(s) of the subtree(s) below which to
     * iterate to the result of expanding the printf template.
     * @param rootTemplate template (printf string and args) for subtree root
     * @return this
     */
    public Spec setRootTemplate(String rootTemplate) {
      return setRootTemplates(ListUtil.list(rootTemplate));
    }

    /** Set the URLs of the roots of the subtrees below which to iterate.
     * @param rootTemplates templates (printf string and args) for subtree roots
     * @return this
     */
    public Spec setRoots(List<String> roots) {
      if (rootTemplates != null) {
	throw new
	  IllegalArgumentException("Can't set both roots and rootTemplates");
      }
      this.roots = roots;
      return this;
    }

    /** Set the URL(s) of the root(s) of the subtree(s) below which to
     * iterate to the result of expanding the printf templates.
     * @param rootTemplates template (printf string and args) for subtree roots
     * @return this
     */
    public Spec setRootTemplates(List<String> rootTemplates) {
      if (roots != null) {
	throw new
	  IllegalArgumentException("Can't set both roots and rootTemplates");
      }
      this.rootTemplates = rootTemplates;
      return this;
    }

    /** Return the roots */
    public List<String> getRoots() {
      return roots;
    }

    /** Return the root templates */
    public List<String> getRootTemplates() {
      return rootTemplates;
    }

    PatSpec getMatchPatSpec() {
      if (matchPatSpec == null) {
	matchPatSpec = new PatSpec();
      }
      return matchPatSpec;
    }

    PatSpec getIncludePatSpec() {
      if (includePatSpec == null) {
	includePatSpec = new PatSpec();
      }
      return includePatSpec;
    }

    boolean hasIncludePat() {
      return includePatSpec != null;
    }

    PatSpec getExcludePatSpec() {
      if (excludePatSpec == null) {
	excludePatSpec = new PatSpec();
      }
      return excludePatSpec;
    }

    boolean hasExcludePat() {
      return excludePatSpec != null;
    }

    /** Set the regular expression the article URLs must match
     * @param regex compiled regular expression
     * @return this
     */
    public Spec setPattern(Pattern regex) {
      getMatchPatSpec().setPattern(regex);
      return this;
    }

    /** Set the regular expression the article URLs must match
     * @param regex regular expression
     * @return this
     */
    public Spec setPattern(String regex) {
      return setPattern(regex, 0);
    }

    /** Set the regular expression the article URLs must match
     * @param regex regular expression
     * @param flags compilation flags for regex
     * @return this
     */
    public Spec setPattern(String regex, int flags) {
      getMatchPatSpec().setPattern(regex, flags);
      return this;
    }

    /** Set the regular expression the article URLs must match to the
     * expanstion of the template
     * @param patternTemplate printf string and args
     * @return this
     */
    public Spec setPatternTemplate(String patternTemplate) {
      return setPatternTemplate(patternTemplate, 0);
    }

    /** Set the regular expression the article URLs must match to the
     * expanstion of the template
     * @param patternTemplate printf string and args
     * @param flags compilation flags for regex
     * @return this
     */
    public Spec setPatternTemplate(String patternTemplate, int flags) {
      getMatchPatSpec().setPatternTemplate(patternTemplate, flags);
      return this;
    }

    /** Return the pattern */
    public Pattern getPattern() {
      return getMatchPatSpec().getPattern();
    }

    /** Return the pattern template */
    public String getPatternTemplate() {
      return getMatchPatSpec().getPatternTemplate();
    }

    /** Return the pattern compilation flags */
    public int getPatternFlags() {
      return getMatchPatSpec().getPatternFlags();
    }

    /** Include only subtrees that match the pattern.  The pattern must be
     * anchored at the front
     * @param regex compiled regular expression
     * @return this
     */
    public Spec setIncludeSubTreePattern(Pattern regex) {
      getIncludePatSpec().setPattern(regex);
      return this;
    }

    /** Include only subtrees that match the pattern.  The pattern must be
     * anchored at the front
     * @param regex regular expression
     * @return this
     */
    public Spec setIncludeSubTreePattern(String regex) {
      return setIncludeSubTreePattern(regex, 0);
    }

    /** Include only subtrees that match the pattern.  The pattern must be
     * anchored at the front
     * @param regex regular expression
     * @param flags compilation flags for regex
     * @return this
     */
    public Spec setIncludeSubTreePattern(String regex, int flags) {
      getIncludePatSpec().setPattern(regex, flags);
      return this;
    }

    /** Include only subtrees that match the pattern.  The pattern must be
     * anchored at the front
     * @param patternTemplate printf string and args
     * @return this
     */
    public Spec setIncludeSubTreePatternTemplate(String patternTemplate) {
      return setIncludeSubTreePatternTemplate(patternTemplate, 0);
    }

    /** Include only subtrees that match the pattern.  The pattern must be
     * anchored at the front
     * @param patternTemplate printf string and args
     * @param flags compilation flags for regex
     * @return this
     */
    public Spec setIncludeSubTreePatternTemplate(String patternTemplate,
						 int flags) {
      getIncludePatSpec().setPatternTemplate(patternTemplate, flags);
      return this;
    }

    /** Return the include subtrees pattern */
    public Pattern getIncludeSubTreePattern() {
      return getIncludePatSpec().getPattern();
    }

    /** Return the include subtrees pattern template */
    public String getIncludeSubTreePatternTemplate() {
      return getIncludePatSpec().getPatternTemplate();
    }

    /** Return the include subtrees pattern compilation flags */
    public int getIncludeSubTreePatternFlags() {
      return getIncludePatSpec().getPatternFlags();
    }

    /** Exclude subtrees that match the pattern.
     * @param regex compiled regular expression
     * @return this
     */
    public Spec setExcludeSubTreePattern(Pattern regex) {
      getExcludePatSpec().setPattern(regex);
      return this;
    }

    /** Exclude subtrees that match the pattern.
     * @param regex regular expression
     * @return this
     */
    public Spec setExcludeSubTreePattern(String regex) {
      return setExcludeSubTreePattern(regex, 0);
    }

    /** Exclude subtrees that match the pattern.
     * @param regex regular expression
     * @param flags compilation flags for regex
     * @return this
     */
    public Spec setExcludeSubTreePattern(String regex, int flags) {
      getExcludePatSpec().setPattern(regex, flags);
      return this;
    }

    /** Exclude subtrees that match the pattern.
     * @param patternTemplate printf string and args
     * @return this
     */
    public Spec setExcludeSubTreePatternTemplate(String patternTemplate) {
      return setExcludeSubTreePatternTemplate(patternTemplate, 0);
    }

    /** Exclude subtrees that match the pattern.
     * @param patternTemplate printf string and args
     * @param flags compilation flags for regex
     * @return this
     */
    public Spec setExcludeSubTreePatternTemplate(String patternTemplate,
						 int flags) {
      getExcludePatSpec().setPatternTemplate(patternTemplate, flags);
      return this;
    }

    /** Return the exclude subtrees pattern */
    public Pattern getExcludeSubTreePattern() {
      return getExcludePatSpec().getPattern();
    }

    /** Return the exclude subtrees pattern template */
    public String getExcludeSubTreePatternTemplate() {
      return getExcludePatSpec().getPatternTemplate();
    }

    /** Return the exclude subtrees pattern compilation flags */
    public int getExcludeSubTreePatternFlags() {
      return getExcludePatSpec().getPatternFlags();
    }

    /** If true, will descend into archive files */
    public Spec setVisitArchiveMembers(boolean val) {
      isVisitArchiveMembers = val;
      return this;
    }

    /** Return true if should descend into archive files */
    public boolean isVisitArchiveMembers() {
      return isVisitArchiveMembers;
    }

    public String toString() {
      return "[SAISpec: " +
        "mime: " + mimeType +
        ", roots: " + (roots != null ? roots : rootTemplates) +
        ", mat: " + matchPatSpec +
        ", incl: " + includePatSpec +
        ", excl: " + excludePatSpec + "]";
    }
  }

  /** Encapsulates the various ways to specifiy a pattern (as a compiled
   * Pattern, a printf template, or a template and compilation flags */
  static class PatSpec {
    private Pattern pat;
    private String patTempl;
    private int patFlags = 0;		// Pattern compilation flags

    void setPattern(Pattern regex) {
      pat = regex;
    }

    void setPattern(String regex, int flags) {
      if (patTempl != null) {
	throw new IllegalArgumentException("Can't set both pattern and patternTemplate");
      }
      patFlags = flags;
      pat = Pattern.compile(regex, flags);
    }

    void setPatternTemplate(String patternTemplate, int flags) {
      if (pat != null) {
	throw new IllegalArgumentException("Can't set both pattern and patternTemplate");
      }
      this.patTempl = patternTemplate;
      this.patFlags = flags;
    }

    Pattern getPattern() {
      return pat;
    }

    public String getPatternTemplate() {
      return patTempl;
    }

    public int getPatternFlags() {
      return patFlags;
    }

    public String toString() {
      return "[PatSpec: " +
        (getPattern() != null ? getPattern().pattern() : getPatternTemplate()) +
        "]";
    }
  }


  /** The spec that URLs must match */
  protected Spec spec;
  /** The mimeType that files must match */
  protected String mimeType;
  /** The AU being iterated over */
  protected ArchivalUnit au;
  /** Pattern that URLs must match */
  protected Pattern pat = null;
  /** Pattern for subtrees to recurse into, or null */
  protected Pattern includeSubTreePat = null;
  /** Pattern for subtrees not to recurse into, or null */
  protected Pattern excludeSubTreePat = null;
  /** Underlying CachedUrl iterator */
  protected CuIterator cuIter = null;
  /** Root CachecUrlSets */
  Collection<CachedUrlSet> roots;
  /** Iterator over subtree roots */
  protected Iterator<CachedUrlSet> rootIter = null;

  // if null, we have to look for nextElement
  private ArticleFiles nextElement = null;

  // if any call to visitArticleCu() emits more than one ArticleFiles they
  // are accumulated in this list and moved into nextElement one at a time.
  // This simplifies the hasNext() and next() logic and avoids the overhead
  // of the intermediate list in the common case of zero or one
  // ArticleFiles per CU
  private LinkedList<ArticleFiles> nextElements;

  public SubTreeArticleIterator(ArchivalUnit au, Spec spec) {
    final String DEBUG_HEADER = "SubTreeArticleIterator(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "au = " + au);
      log.debug2(DEBUG_HEADER + "spec = " + spec);
    }
    this.au = au;
    this.spec = spec;

    mimeType = getMimeType();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mimeType = " + mimeType);
    if (spec.hasIncludePat()) {
      this.includeSubTreePat = makePattern(spec.getIncludePatSpec());
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "includeSubTreePat = " + includeSubTreePat);
    }
    if (spec.hasExcludePat()) {
      this.excludeSubTreePat = makePattern(spec.getExcludePatSpec());
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "excludeSubTreePat = " + excludeSubTreePat);
    }
    roots = makeRoots();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "roots = " + roots);
    this.pat = makePattern(spec.getMatchPatSpec());
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pat = " + pat);
    rootIter = roots.iterator();
    log.debug2("Create: AU: " + au.getName() + ", Mime: " + this.mimeType
	       + ", roots: " + roots + ", pat: " + pat);
  }

  // XXX fix when work out how target is used
  protected String getMimeType() {
    String tmpMime =
      spec.getTarget() != null ? spec.getTarget().getFormat() : null;
    if (tmpMime == null) {
      tmpMime = spec.getMimeType();
    }
    if (tmpMime == null) {
      tmpMime = au.getPlugin().getDefaultArticleMimeType();
    }
    return tmpMime;
  }

  protected Pattern makePattern(PatSpec pspec) {
    if (pspec.getPattern() != null) {
      return pspec.getPattern();
    }
    if (pspec.getPatternTemplate() != null) {
      String re =
	convertVariableUrlRegexpString(pspec.getPatternTemplate()).getRegexp();
      return Pattern.compile(re, pspec.getPatternFlags());
    }
    return null;
  }

  protected Collection<CachedUrlSet> makeRoots() {
    Collection<String> roots = makeRootUrls();
    log.debug2("rootUrls: " + roots);
    if (roots == null || roots.isEmpty()) {
      return ListUtil.list(makeCachedUrlSet(makeCuss(AuCachedUrlSetSpec.URL)));
    }
    Collection<CachedUrlSet> res = new ArrayList<CachedUrlSet>();
    for (String root : roots) {
      res.add(makeCachedUrlSet(makeCuss(root)));
    }
    return res;
  }

  CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    CachedUrlSet cus = au.makeCachedUrlSet(cuss);
    MetadataTarget target = spec.getTarget();
    if (target != null) {
      long date = target.getIncludeFilesChangedAfter();
      if (date > 0) {
	cus.setExcludeFilesUnchangedAfter(date);
      }
    }
    return cus;
  }

  CachedUrlSetSpec makeCuss(String root) {
    if (includeSubTreePat != null) {
      return PrunedCachedUrlSetSpec.includeMatchingSubTrees(root,
							    includeSubTreePat);
    }
    if (excludeSubTreePat != null) {
      return PrunedCachedUrlSetSpec.excludeMatchingSubTrees(root,
							    excludeSubTreePat);
    }
    return new RangeCachedUrlSetSpec(root);
  }

  protected Collection<String> makeRootUrls() {
    if (spec.getRoots() != null) {
      return spec.getRoots();
    }
    if (spec.getRootTemplates() == null) {
      return null;
    }
    Collection<String> res = new ArrayList<String>();
    for (String template : spec.getRootTemplates()) {
      List<String> lst = convertUrlList(template);
      if (lst == null) {
	log.warning("Null converted string from " + template);
	continue;
      }
      res.addAll(lst);
    }
    return res;
  }

  protected List<String> convertUrlList(String printfString) {
    return PrintfConverter.newUrlListConverter(au).getUrlList(printfString);
  }

  protected PrintfConverter.MatchPattern
    convertVariableUrlRegexpString(String printfString) {
    return PrintfConverter.newRegexpConverter(au, RegexpContext.Url).getMatchPattern(printfString);
  }

  private ArticleFiles findNextElement() {
    final String DEBUG_HEADER = "findNextElement(): ";
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "nextElement 1 = " + nextElement);
    if (nextElement != null) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "return 1 " + nextElement);
      return nextElement;
    }
    while (true) {
      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "nextElements = " + nextElements);
	if (nextElements != null) {
	  log.debug3(DEBUG_HEADER
	      + "nextElements.isEmpty() = " + nextElements.isEmpty());
	}
      }
      if (nextElements != null && !nextElements.isEmpty()) {
	nextElement = nextElements.remove();
	if (log.isDebug2())
	  log.debug2(DEBUG_HEADER + "return 2 " + nextElement);
	return nextElement;
      } else {
	CachedUrl cu = null;
	try {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cuIter 1 = " + cuIter);
	  if (cuIter == null || !cuIter.hasNext()) {
	    if (rootIter == null || !rootIter.hasNext()) {
	      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "return 3 null");
	      return null;
	    } else {
	      CachedUrlSet root = rootIter.next();
	      if (log.isDebug3()) {
		log.debug3(DEBUG_HEADER + "root = " + root);
		log.debug3(DEBUG_HEADER + "spec.isVisitArchiveMembers = "
		    + spec.isVisitArchiveMembers);
	      }
	      cuIter = spec.isVisitArchiveMembers
		? root.archiveMemberIterator() : root.getCuIterator();
		if (log.isDebug3())
		  log.debug3(DEBUG_HEADER + "cuIter 2 = " + cuIter);
	      continue;
	    }
	  } else {
	    cu = cuIter.next();
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);
	    boolean isCuArticle = isArticleCu(cu);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "isCuArticle = " + isCuArticle);
	    if (isCuArticle) {
	      // isArticleCu() might have caused file to open
	      cu.release();
	      visitArticleCu(cu);
	      if (log.isDebug3())
		log.debug3(DEBUG_HEADER + "nextElement 4 = " + nextElement);
	      if (nextElement == null) {
		continue;
	      }
	      if (log.isDebug2())
		log.debug2(DEBUG_HEADER + "return 4 " + nextElement);
	      return nextElement;
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
  }

  /** Emit an ArticleFiles from the iterator.  Should be called by
   * visitArticleCu() once for each ArticleFiles it wants to generate from
   * the CU */
  protected final void emitArticleFiles(ArticleFiles af) {
    if (log.isDebug3()) log.debug3("Emit: " + af);
    if (nextElement == null) {
      nextElement = af;
    } else {
      if (nextElements == null) {
	nextElements = new LinkedList();
      }
      nextElements.add(af);
    }
  }

  /** Invoked on each CachedUrl for which isArticleCu returns true, should
   * call emitArticleFiles() for each ArticleFiles to be generated for
   * the CachedUrl. Default implementation calls createArticleFiles() and
   * emits the single ArticleFiles it returns, if any.  Primarily for
   * compatibility with old subclasses prior to the introduction of this
   * method. */
  protected void visitArticleCu(CachedUrl cu) {
    final String DEBUG_HEADER = "visitArticleCu(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Visit: " + cu);
    // This next call is typically implemented by the content plugin for this
    // archival unit.
    ArticleFiles res = createArticleFiles(cu);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "res = " + res);
    if (res != null) {
      emitArticleFiles(res);
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
  
  /** Not implemented */
  public void remove() {
    throw new UnsupportedOperationException("Not implemented");
  }
}
