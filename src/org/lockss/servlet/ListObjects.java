/*
 * $Id$
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

package org.lockss.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import javax.servlet.*;

import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;
import org.lockss.filter.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.mortbay.html.*;

/** Plain output of various lists - AUs, URLs, metadata, etc.  Mostly for
 * debugging/testing; also able to handle unlimited length lists. */
public class ListObjects extends LockssServlet {
  
  private static final Logger log = Logger.getLogger(ListObjects.class);

  private String auid;
  private String url;
  
  private ArchivalUnit au;

  private PluginManager pluginMgr;
  private CrawlManager crawlMgr;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    au = null;
    auid = null;
    url = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
    crawlMgr = getLockssDaemon().getCrawlManager();
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }
    String type = getParameter("type");
    if (StringUtil.isNullString(type)) {
      displayError("\"type\" arg must be specified");
      return;
    }
    if (type.equalsIgnoreCase("aus")) {
      new AuNameList().execute();
    } else if (type.equalsIgnoreCase("auids")) {
      new AuidList().execute();
    } else {
      // all others need au
      auid = getParameter("auid");
      url = getParameter("url");
      au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	displayError("No such AU: " + auid);
	return;
      }
      if (type.equalsIgnoreCase("urls")) {
	new UrlList().execute();
      } else if (type.equalsIgnoreCase("urlsm")) {
	new UrlMemberList().execute();
      } else if (type.equalsIgnoreCase("files")) {
	new FileList().execute();
      } else if (type.equalsIgnoreCase("filesm")) {
	new FileMemberList().execute();
      } else if (type.equalsIgnoreCase("suburls")) {
	new SubstanceUrlList().execute();
      } else if (type.equalsIgnoreCase("suburlsdetail")) {
	new SubstanceUrlListWithExplanations().execute();
      } else if (type.equalsIgnoreCase("subfiles")) {
	new SubstanceFileList().execute();
      } else if (type.equalsIgnoreCase("articles")) {
	boolean isDoi = !StringUtil.isNullString(getParameter("doi"));
	if (isDoi) {
	  // XXX Backwards compatible - still needed?
	  new DoiList().setIncludeUrl(true).execute();
	} else {
	  new ArticleUrlList().execute();
	}
      } else if (type.equalsIgnoreCase("dois")) {
	new DoiList().execute();
      } else if (type.equalsIgnoreCase("metadata")) {
	new MetadataList().execute();
      } else if (type.equalsIgnoreCase("extracturls")) {
	new ExtractUrlsList().execute();
      } else {
	displayError("Unknown list type: " + type);
	return;
      }
    }
  }

  // Used by status table(s) to determine whether to display links to
  // Metadata and DOIs
  public static boolean hasArticleMetadata(ArchivalUnit au) {
    return null !=
      au.getPlugin().getArticleMetadataExtractor(MetadataTarget.Article(), au);

      // Shouldn't invoke factory, but this method isn't in interface
//     return null !=
//       au.getPlugin().getArticleMetadataExtractorFactory(MetadataTarget.Article);
  }

  // Used by status table(s) to determine whether to display link to articles
  public static boolean hasArticleList(ArchivalUnit au) {
    return null != au.getPlugin().getArticleIteratorFactory();
  }

  void displayError(String error) throws IOException {
    Page page = newPage();
    Composite comp = new Composite();
    comp.add("<center><font color=red size=+1>");
    comp.add(error);
    comp.add("</font></center><br>");
    page.add(comp);
    endPage(page);
  }

  /** Base for classes that print lists of objexts */
  abstract class BaseList {
    
    PrintWriter wrtr;
    int itemCnt = 0;
    
    boolean isError = false;

    /** Subs must print a header */
    abstract void printHeader();

    /** Subs must execute a body between begin() and end() */
    abstract void doBody() throws IOException;

    /** Subs must supply the name of the thing they're enumerating */
    abstract String unitName();

    /** Override if unit name isn't pluralized by adding "s" */
    String units(int n) {
      return StringUtil.numberOfUnits(n, unitName());
    }

    void begin() throws IOException {
      resp.setCharacterEncoding(Constants.ENCODING_UTF_8);
      wrtr = resp.getWriter();
      resp.setContentType("text/plain");
      printHeader();
      wrtr.println();
    }

    void finish() {;
      wrtr.println("# " + units(itemCnt));
      wrtr.println(isError ? "# end (errors)" : "# end");
      wrtr.flush();
    }

    final void execute() throws IOException {
      begin();
      doBody();
      finish();
    }
    
  }

  /** Lists of objects (URLs, files, etc.) which are based on AU's
   * repository nodes */
  abstract class BaseNodeList extends BaseList {
    
    /** Subs must process content CUs */
    abstract void processContentCu(CachedUrl cu);

    void doBody() {
      for (CuIterator iter = getIterator();
	   iter.hasNext(); ) {
	CachedUrl cu = iter.next();
	try {
	  processCu(cu);
	} finally {
	  AuUtil.safeRelease(cu);
	}
      }
    }

    CuIterator getIterator() {
      return au.getAuCachedUrlSet().getCuIterator();
    }

    protected void processCu(CachedUrl cu) {
      if (cu.hasContent()) {
	processContentCu(cu);
	itemCnt++;
      }
    }
    
  }

  /** List URLs in AU */
  class UrlList extends BaseNodeList {
    
    void printHeader() {
      wrtr.println("# URLs in " + au.getName());
    }
    
    String unitName() {
      return "URL";
    }

    void processContentCu(CachedUrl cu) {
      wrtr.println(cu.getUrl());
    }
  }

  /** List URLs in AU, including archive file members */
  class UrlMemberList extends UrlList {
    
    void printHeader() {
      wrtr.println("# URLs* in " + au.getName());
    }

    CuIterator getIterator() {
      return au.getAuCachedUrlSet().archiveMemberIterator();
    }
    
    String units(int n) {
      return StringUtil.numberOfUnits(n,
				      "URL (including archive members)",
				      "URLs (including archive members)");
    }
  }

  /** Base class for substance URLs/files */
  abstract class SubstanceList extends UrlList {

    protected SubstanceChecker subChecker;
    
    SubstanceList() {
      super();
      subChecker = new SubstanceChecker(au);
    }

    CuIterator getIterator() {
      return au.getAuCachedUrlSet().getCuIterator();
    }
  }

  /** List substance URLs in AU */
  class SubstanceUrlList extends SubstanceList {

    SubstanceUrlList() {
      super();
    }

    void printHeader() {
      wrtr.println("# Substance URLs* in " + au.getName());
    }

    String units(int n) {
      return StringUtil.numberOfUnits(n, "substance URL", "substance URLs");
    }

    protected void processCu(CachedUrl cu) {
      if (cu.hasContent()) {
 	String cuUrl = cu.getUrl();
	List<String> urls = subChecker.getUrlsToCheck(cu);
	for (String url : urls) {
	  if (subChecker.isSubstanceUrl(url)) {
	    wrtr.println(url);
	    itemCnt++;
	    break;
	  }
	}
      }
    }

    void processContentCu(CachedUrl cu) {
      throw new IllegalStateException("Shouldn't be called");
    }
  }

  /** List substance URLs in AU */
  class SubstanceUrlListWithExplanations extends SubstanceList {

    SubstanceUrlListWithExplanations() {
      super();
    }

    void printHeader() {
      wrtr.println("# Substance URLs (with redirect detail) in " +
		   au.getName());
      wrtr.println("# Substance checker mode is " + subChecker.getMode());
    }

    String units(int n) {
      return StringUtil.numberOfUnits(n, "substance URL", "substance URLs");
    }

    protected void processCu(CachedUrl cu) {
      if (cu.hasContent()) {
 	String cuUrl = cu.getUrl();
	List<String> urls = subChecker.getUrlsToCheck(cu);
	Map<String,Boolean> res =
	  (Map<String,Boolean>)new HashMap<String,Boolean>();
	boolean hasSubst = false;
	for (String url : urls) {
	  boolean urlSubst = subChecker.isSubstanceUrl(url);
	  res.put(url, urlSubst);
	  hasSubst = hasSubst || urlSubst;
	}
	if (hasSubst) {
	  itemCnt++;
	}
	wrtr.println((hasSubst ? "Yes" : "No ") + "  " + cuUrl);
	if (urls.size() != 1 || cuUrl != urls.get(0)) {
	  wrtr.println("  Redirect chain:");
	  for (String url : AuUtil.getRedirectChain(cu)) {
	    if (res.containsKey(url)) {
	      wrtr.println((hasSubst ? "  Yes" : "  No ") + "   " + url);
	    } else {
	      wrtr.println("        " + url);
	    }
	  }
	}
      }
    }

    void processContentCu(CachedUrl cu) {
      throw new IllegalStateException("Shouldn't be called");
    }
  }

  /** List URLs, content type and length. */
  class FileList extends BaseNodeList {
    
    void printHeader() {
      wrtr.println("# Files in " + au.getName());
      wrtr.println("# URL\tContentType\tsize");
    }
    
    String unitName() {
      return "file";
    }

    void processContentCu(CachedUrl cu) {
      String url = cu.getUrl();
      String contentType = cu.getContentType();
      long bytes = cu.getContentSize();
      if (contentType == null) {
	contentType = "unknown";
      }
      wrtr.println(url + "\t" + contentType + "\t" + bytes);
    }
  }

  /** List URLs, content type and length, including archive file members. */
  // MetaArchive experiment 2010ish.  Still in use?
  class FileMemberList extends FileList {
    
    void printHeader() {
      wrtr.println("# Files* in " + au.getName());
      wrtr.println("# URL\tContentType\tsize");
    }

    CuIterator getIterator() {
      return au.getAuCachedUrlSet().archiveMemberIterator();
    }
    
    String units(int n) {
      return StringUtil.numberOfUnits(n,
				      "file (including archive members)",
				      "files (including archive members)");
    }
  }

  /** List substance URLs in AU */
  class SubstanceFileList extends SubstanceList {

    SubstanceFileList() {
      super();
    }

    void printHeader() {
      wrtr.println("# Substance files* in " + au.getName());
    }

    String units(int n) {
      return StringUtil.numberOfUnits(n, "substance file", "substance files");
    }

    protected void processCu(CachedUrl cu) {
      if (cu.hasContent()) {
 	String cuUrl = cu.getUrl();
	List<String> urls = subChecker.getUrlsToCheck(cu);
	for (String url : urls) {
	  if (subChecker.isSubstanceUrl(url)) {
	    String contentType = cu.getContentType();
	    long bytes = cu.getContentSize();
	    if (contentType == null) {
	      contentType = "unknown";
	    }
	    wrtr.println(url + "\t" + contentType + "\t" + bytes);
	    itemCnt++;
	    break;
	  }
	}

      }
    }

    void processContentCu(CachedUrl cu) {
      throw new IllegalStateException("Shouldn't be called");
    }
  }

  /** Base for lists based on ArticleIterator */
  abstract class BaseArticleList extends BaseList {
    
    int errCnt = 0;
    
    int maxErrs = 3;
    
    MetadataTarget target = null;

    void setMetadataTarget(MetadataTarget target) {
      this.target = target;
    }
    
    /** Subs must process each article */
    abstract void processArticle(ArticleFiles af)
	throws IOException, PluginException;

    boolean isLogError() {
      isError = true;
      return errCnt++ <= maxErrs;
    }

    void doBody() throws IOException {
      Iterator<ArticleFiles> iter =
          (target == null) ? au.getArticleIterator() : au.getArticleIterator(target);
      while (iter.hasNext()) {
	ArticleFiles af = iter.next();
	if (af.isEmpty()) {
	  // Probable plugin error.  Shouldn't happen, but if it does it
	  // likely will many times.
	  if (isLogError()) {
	    log.error("ArticleIterator generated empty ArticleFiles");
	  }
	  continue;
	}
	try {
	  processArticle(af);
	} catch (Exception e) {
	  if (isLogError()) {
	    log.warning("listDOIs() threw", e);
	  }
	}
      }
    }
    
  }

  /** Base for lists requiring metadata extraction */
  abstract class BaseMetadataList extends BaseArticleList {
  
    ArticleMetadataExtractor mdExtractor;
    
    ArticleMetadataExtractor.Emitter emitter;

    @Override
    void begin() throws IOException {
      super.begin();
      mdExtractor = au.getPlugin().getArticleMetadataExtractor(target, au);
    }

    /** Subs must process each article and list of metadata */
    abstract void processArticle(ArticleFiles af, List<ArticleMetadata> amlst);

    @Override
    void doBody() throws IOException {
      if (mdExtractor == null) {
	// XXX error format
	wrtr.println("# Plugin " + au.getPlugin().getPluginName() +
		     " does not supply a metadata extractor.");
	isError = true;
	return;
      }
      super.doBody();
    }

    void processArticle(ArticleFiles af) throws IOException, PluginException {
      // Create a ListEmitter per article
      ListEmitter emitter = new ListEmitter();
      mdExtractor.extract(target, af, emitter);
      processArticle(af, emitter.getAmList());
      // If we finish one normally, start logging errors again.
      errCnt = 0;
    }
    
  }

  /** Metadata emitter that collects ArticleMetadata into a list. */
  static class ListEmitter implements ArticleMetadataExtractor.Emitter {
    
    List<ArticleMetadata> amlst = new ArrayList<ArticleMetadata>();

    public void emitMetadata(ArticleFiles af, ArticleMetadata md) {
      if (log.isDebug3()) log.debug3("emit("+af+", "+md+")");
      if (md != null) {
	amlst.add(md);
      };
    }

    public List<ArticleMetadata> getAmList() {
      return amlst;
    }
    
  }

  /** List DOIs of articles in AU */
  class DoiList extends BaseMetadataList {
    
    protected boolean isIncludeUrl = false;
    
    int logMissing = 3;

    DoiList() {
      super();
      setMetadataTarget(MetadataTarget.Doi());
    }

    void printHeader() {
      wrtr.println("# DOIs in " + au.getName());
    }

    String unitName() {
      return "DOI";
    }

    void processArticle(ArticleFiles af, List<ArticleMetadata> amlst) {
      if (amlst == null || amlst.isEmpty()) {
	if (isIncludeUrl) {
	  CachedUrl cu = af.getFullTextCu();
	  if (cu != null) {
	    wrtr.println(cu.getUrl());
	    itemCnt++;
	  } else {
	    // shouldn't happen, but if it does it likely will many times.
	    if (logMissing-- > 0) {
	      log.error("ArticleIterator generated ArticleFiles with no full text CU: " + af);
	    }
	  }
	}
      } else {
	for (ArticleMetadata md : amlst) {
	  if (md != null) {
	    String doi = md.get(MetadataField.FIELD_DOI);
	    if (doi != null) {
	      if (isIncludeUrl) {
		String url = md.get(MetadataField.FIELD_ACCESS_URL);
		wrtr.println(url + "\t" + doi);
	      } else {
		wrtr.println(doi);
	      }
	      itemCnt++;
	    }
	  }
	}
      }
    }

    DoiList setIncludeUrl(boolean val) {
      isIncludeUrl = val;
      return this;
    }
    
  }

  /** List URL of articles in AU */
  class ArticleUrlList extends BaseArticleList {
    
    int logMissing = 3;

    ArticleUrlList() {
      super();
      setMetadataTarget(MetadataTarget.Article());
    }

    void printHeader() {
      wrtr.println("# Articles in " + au.getName());
    }

    String unitName() {
      return "article";
    }

    void processArticle(ArticleFiles af) {
      CachedUrl cu = af.getFullTextCu();
      if (cu != null) {
	wrtr.println(cu.getUrl());
	itemCnt++;
      } else {
	// shouldn't happen, but if it does it likely will many times.
	if (logMissing-- > 0) {
	  log.error("ArticleIterator generated ArticleFiles with no full text CU: " + af);
	}
      }
    }
    
  }

  /** Dump all ArticleFiles and metadata of articles in AU */
  class MetadataList extends BaseMetadataList {
    
    MetadataList() {
      super();
      setMetadataTarget(MetadataTarget.Any());
    }
    
    void printHeader() {
      wrtr.println("# All metadata in " + au.getName());
    }

    String unitName() {
      return "metadata record";
    }

    void processArticle(ArticleFiles af, List<ArticleMetadata> amlst) {
// 	  wrtr.println(af);
      wrtr.print(af.ppString(0));
      
      for (ArticleMetadata md : amlst) {
	if (md != null) {
// 	  wrtr.println(md);
	  wrtr.print(md.ppString(0));
	  itemCnt++;
	}
      }      
      wrtr.println();
    }
    
  }

  /** Base for lists of AUs */
  abstract class BaseAuList extends BaseList {

    void doBody() throws IOException {
      boolean includeInternalAus = isDebugUser();
      for (ArchivalUnit au : pluginMgr.getAllAus()) {
	if (includeInternalAus || !pluginMgr.isInternalAu(au)) {
	  processAu(au);
	  itemCnt++;
	}
      }
    }

    /** Subs must process each AU */
    abstract void processAu(ArchivalUnit au);
    
  }

  /** List AU names */
  class AuNameList extends BaseAuList {
    
    void printHeader() {
      wrtr.println("# AUs on " + getMachineName());
    }

    String unitName() {
      return "AU";
    }

    void processAu(ArchivalUnit au) {
      wrtr.println(au.getName());
    }
    
  }

  /** List AUIDs */
  class AuidList extends BaseAuList {
    
    void printHeader() {
      wrtr.println("# AUIDs on " + getMachineName());
    }

    String unitName() {
      return "AU";
    }

    void processAu(ArchivalUnit au) {
      wrtr.println(au.getAuId());
    }
    
  }
  
  /** Display list of URLs that link extractor finds in file */
  class ExtractUrlsList extends BaseList {

    void doBody() throws IOException {
      if (url == null) {
	wrtr.println("URL must be specified");
	isError = true;
	return;
      }
      CachedUrl cu = au.makeCachedUrl(url);
      if (!cu.hasContent()) {
	wrtr.println("No content: " + url);
	isError = true;
	return;
      }
      LinkExtractor extractor = au.getLinkExtractor(cu.getContentType());
      if (extractor == null) {
	wrtr.println("No link extractor for content type: " +
		     cu.getContentType());
	isError = true;
	return;
      }

      InputStream in = null;
      try {
	CharsetUtil.InputStreamAndCharset isc =
	  CharsetUtil.getCharsetStream(cu);
	String charset = isc.getCharset();
	in = isc.getInStream();

	in = FilterUtil.getCrawlFilteredStream(au, in, charset,
					       cu.getContentType());
	MyLinkExtractorCallback cb = new MyLinkExtractorCallback(cu);
	extractor.extractUrls(au, in, charset, PluginUtil.getBaseUrl(cu), cb);

	for (String eurl : cb.getExtractedUrls()) {
	  String excl = null;
	  try {
	    if (!au.shouldBeCached(eurl)) {
	      excl = "Excluded";
	    } else {
	      if (crawlMgr != null &&
		  crawlMgr.isGloballyExcludedUrl(au, eurl)) {
		excl = "Globally excluded";
	      }
	    }
	    if (excl == null) {
	      wrtr.println(eurl);
	      itemCnt++;
	    } else {
	      wrtr.println(eurl + "\t" + excl);
	    }
	  } finally {
	    AuUtil.safeRelease(cu);
	  }
	}

      } catch (PluginException | IOException e) {
	String msg = "Plugin LinkExtractor error";
	log.error(msg, e);
	wrtr.println(msg + ": " + e.toString());
	isError = true;
      } finally {
	IOUtil.safeClose(in);
      }

    }

    void printHeader() {
      wrtr.println("# URLs extracted from " + url + " in " + au.getName());
    }

    String unitName() {
      return "Extracted URL";
    }

  }

  class MyLinkExtractorCallback implements LinkExtractor.Callback {
    TreeSet<String> foundUrls = new TreeSet<String>();
    ArchivalUnit au;
    CachedUrl cu;

    public MyLinkExtractorCallback(CachedUrl cu) {
      this.au = cu.getArchivalUnit();
      this.cu = cu;
    }

    public Collection<String> getExtractedUrls() {
      return foundUrls;
    }

    public void foundLink(String eurl) {
      if (!BaseCrawler.isSupportedUrlProtocol(url)) {
	return;
      }
      try {
	String normUrl = UrlUtil.normalizeUrl(eurl, au);
	if (normUrl.equals(eurl)) {
	  if (log.isDebug3()) log.debug3("Self reference to " + url);
	  return;
	}
	foundUrls.add(normUrl);
      } catch (MalformedURLException e) {
	//XXX what exactly does this log want to tell?
	log.warning("Normalizing", e);
      } catch (PluginBehaviorException e) {
	log.warning("Normalizing", e);
      }
    }
  }

}
