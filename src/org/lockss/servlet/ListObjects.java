/*
 * $Id: ListObjects.java,v 1.15.2.1 2011-02-14 00:21:53 tlipkis Exp $
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

package org.lockss.servlet;

import javax.servlet.*;
import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;
import org.mortbay.html.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;

/** Output a plain list of the URLs in an AU
 */
public class ListObjects extends LockssServlet {
  static final Logger log = Logger.getLogger("ListObjects");

  private String auid;
  private ArchivalUnit au;

  private PluginManager pluginMgr;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    au = null;
    auid = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
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
    if (type.equalsIgnoreCase("urls")) {
      auid = getParameter("auid");
      au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	displayError("No such AU: " + auid);
	return;
      }
      listUrls();
    } else if (type.equalsIgnoreCase("dois")) {
      auid = getParameter("auid");
      au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	displayError("No such AU: " + auid);
	return;
      }
      listDOIs();
    } else if (type.equalsIgnoreCase("files")) {
      auid = getParameter("auid");
      au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	displayError("No such AU: " + auid);
	return;
      }
      listFiles();
    } else if (type.equalsIgnoreCase("articles")) {
      auid = getParameter("auid");
      au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	displayError("No such AU: " + auid);
	return;
      }
      listArticles();
    } else if (type.equalsIgnoreCase("aus")) {
      listAUs();
    } else if (type.equalsIgnoreCase("auids")) {
      listAuIds();
    } else {
      displayError("Unknown object type: " + type);
      return;
    }
  }

  void listUrls() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# URLs in " + au.getName());
    wrtr.println();
    for (Iterator iter = au.getAuCachedUrlSet().contentHashIterator();
	 iter.hasNext(); ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)iter.next();
      if (cusn.hasContent()) {
	wrtr.println(cusn.getUrl());
      }
    }
    wrtr.println("# end");
  }

  void listDOIs() throws IOException {
    final PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# DOIs in " + au.getName());
    wrtr.println();
    ArticleMetadataExtractor.Emitter emitter =
      new ArticleMetadataExtractor.Emitter() {
	public void emitMetadata(ArticleFiles af,
				 ArticleMetadata md) {
          if (md != null) {
            String doi = md.get(MetadataField.FIELD_DOI);
            if (doi != null) {
              wrtr.println(doi);
            }
          }          
	}
      };
    ArticleMetadataExtractor mdExtractor =
      au.getPlugin().getArticleMetadataExtractor(MetadataTarget.DOI, au);
    int logMissing = 3;
    for (Iterator<ArticleFiles> iter = au.getArticleIterator();
	 iter.hasNext(); ) {
      ArticleFiles af = iter.next();
      CachedUrl cu = af.getFullTextCu();
      if (cu == null) {
	// shouldn't happen, but if it does it likely will many times.
	if (logMissing-- > 0) {
	  log.error("ArticleIterator generated ArticleFiles with no full text CU: " + af);
	}
	continue;
      }
      try {
        if (cu.hasContent()) {
	  mdExtractor.extract(MetadataTarget.DOI, af, emitter);
        }
      } catch (IOException e) {
        log.warning("listDOIs() threw " + e);
      } catch (PluginException e) {
        log.warning("listDOIs() threw " + e);
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
    wrtr.println("# end");
  }

  void listFiles() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# Files in " + au.getName());
    wrtr.println("# URL\tContentType\tsize");
    wrtr.println();
    for (Iterator iter = au.getAuCachedUrlSet().contentHashIterator();
	 iter.hasNext(); ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)iter.next();
      CachedUrl cu = AuUtil.getCu(cusn);
      if (cu != null && cu.hasContent()) {
	String url = cu.getUrl();
	String contentType = cu.getContentType();
	long bytes = cu.getContentSize();
	if (contentType == null) {
	  contentType = "unknown";
	}
	wrtr.println(url + "\t" + contentType + "\t" + bytes);
      }
      AuUtil.safeRelease(cu);
    }
    wrtr.println("# end");
  }

  void listArticles() throws IOException {
    boolean isDoi = !StringUtil.isNullString(getParameter("doi"));

    final PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# Articles in " + au.getName());
    wrtr.println();

    ArticleMetadataExtractor mdExtractor =
      au.getPlugin().getArticleMetadataExtractor(MetadataTarget.Article, au);
    ArticleMetadataExtractor.Emitter emitter = null;
    final MutableObject lastEmittedAf = new MutableObject();
    if (mdExtractor != null) {
      emitter =
	new ArticleMetadataExtractor.Emitter() {
	  public void emitMetadata(ArticleFiles af,
				   ArticleMetadata md) {
	    String url = md.get(MetadataField.FIELD_ACCESS_URL);
	    String doi = null;
	    if (md != null) {
	      doi = md.get(MetadataField.FIELD_DOI);
	    }
	    if (doi != null) {
	      wrtr.println(url + "\t" + doi);
	    } else {
	      wrtr.println(url);
	    }
	    lastEmittedAf.setValue(af);
	  }
	};
    }

    int logMissing = 3;
    for (Iterator<ArticleFiles> iter = au.getArticleIterator();
	 iter.hasNext(); ) {
      ArticleFiles af = iter.next();
      CachedUrl cu = af.getFullTextCu();
      if (cu == null) {
	// shouldn't happen, but if it does it likely will many times.
	if (logMissing-- > 0) {
	  log.error("ArticleIterator generated ArticleFiles with no full text CU: " + af);
	}
	continue;
      }
      String doi = null;
      try {
	if (cu.hasContent()) {
	  String url = cu.getUrl();
	  if (isDoi && mdExtractor != null) {
	    // extract metadata iff DOIs were requested
	    mdExtractor.extract(MetadataTarget.Article, af, emitter);
	  }
	  if (mdExtractor == null || af != lastEmittedAf.getValue()) {
	    // if we didn't extract metadata, or the metadata extractor
	    // didn't emit any metadata records, print the article URL here
	    wrtr.println(url);
	  }
	}
      } catch (IOException e) {
	log.warning("listArticles() threw " + e);
      } catch (PluginException e) {
	log.warning("listArticles() threw " + e);
      } finally {
	AuUtil.safeRelease(cu);       
      }
    }
    wrtr.println("# end");
  }

  void listAUs() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# AUs on " + getMachineName());
    wrtr.println();
    boolean includeInternalAus = isDebugUser();
    for (ArchivalUnit au : pluginMgr.getAllAus()) {
      if (!includeInternalAus && pluginMgr.isInternalAu(au)) {
	continue;
      }
      wrtr.println(au.getName());
    }
    wrtr.println("# end");
  }

  void listAuIds() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");
    wrtr.println("# AUIDs on " + getMachineName());
    wrtr.println();
    boolean includeInternalAus = isDebugUser();
    for (ArchivalUnit au : pluginMgr.getAllAus()) {
      if (!includeInternalAus && pluginMgr.isInternalAu(au)) {
	continue;
      }
      wrtr.println(au.getAuId());
    }
    wrtr.println("# end");
  }

  // unfinished form to select object list

//   private void displayPage() throws IOException {
//     Page page = newPage();
//     layoutErrorBlock(page);
//     ServletUtil.layoutExplanationBlock(page, "Object Lists");
//     page.add(makeForm());
//     page.add("<br>");
//     layoutFooter(page);
//     ServletUtil.writePage(resp, page);
//   }

//   private Element makeForm() {
//     Composite comp = new Composite();
//     Form frm = new Form(srvURL(myServletDescr()));
//     frm.method("POST");

//     Input reload = new Input(Input.Submit, KEY_ACTION, ACTION_RELOAD_CONFIG);
//     setTabOrder(reload);
//     frm.add("<br><center>"+reload+"</center>");
//     Input backup = new Input(Input.Submit, KEY_ACTION, ACTION_MAIL_BACKUP);
//     setTabOrder(backup);
//     frm.add("<br><center>"+backup+"</center>");
//     Input thrw = new Input(Input.Submit, KEY_ACTION, ACTION_THROW_IOEXCEPTION);
//     Input thmsg = new Input(Input.Text, KEY_MSG);
//     setTabOrder(thrw);
//     setTabOrder(thmsg);
//     frm.add("<br><center>"+thrw+" " + thmsg + "</center>");
//     frm.add("<br><center>AU Actions: select AU</center>");
//     Composite ausel = ServletUtil.layoutSelectAu(this, KEY_AUID, auid);
//     frm.add("<br><center>"+ausel+"</center>");
//     setTabOrder(ausel);

//     Input v3Poll = new Input(Input.Submit, KEY_ACTION,
// 			     ( showForcePoll
// 			       ? ACTION_FORCE_START_V3_POLL
// 			       : ACTION_START_V3_POLL));
//     Input crawl = new Input(Input.Submit, KEY_ACTION,
// 			    ( showForceCrawl
// 			      ? ACTION_FORCE_START_CRAWL
// 			      : ACTION_START_CRAWL));
//     frm.add("<br><center>" + v3Poll + "</center>");
//     frm.add("<br><center>" + crawl + "</center>");
//     comp.add(frm);
//     return comp;
//   }

  void displayError(String error) throws IOException {
    Page page = newPage();
    Composite comp = new Composite();
    comp.add("<center><font color=red size=+1>");
    comp.add(error);
    comp.add("</font></center><br>");
    page.add(comp);
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }
}
