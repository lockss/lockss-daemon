/*
 * Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University
 * All rights reserved.  (standard LOCKSS BSD license text - copy from sibling files)
 */

package org.lockss.plugin.atypon;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.RegexpCssLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.Logger;

/*
 * CSS link rewriter that protects data: URIs from the default rewriter.
 *
 * WHY (found 10/2026 on journals.asm.org build-*.css):
 *   The stylesheet embeds a 44,798-char font as
 *     @font-face{font-family:Montserrat Subset;
 *                src:url("data:application/font-woff;charset=utf-8;base64,d09G...==") format("woff");...}
 *   The default CSS rewriter treats that quoted data: value as a relative link
 *   (same bug that produced the crawl-rule exclusion for
 *   ".../css/%22data:application/font-woff"), and the rewritten text breaks
 *   the CSS syntax. Firefox then drops everything after it:
 *     live build.css   = 7,072 rules
 *     served build.css =   910 rules   (908 rules precede the data: URI)
 *   i.e. ~87% of the site's CSS (layout, menus, icons) is lost on replay.
 *
 * HOW: replace every url(data:...) / url("data:...") / url('data:...') with a
 * short placeholder, run the normal rewriter on the rest, then put the original
 * data: values back. Non-data URLs are rewritten exactly as before.
 */
public class BaseAtyponCssLinkRewriterFactory implements LinkRewriterFactory {

  private static final Logger log = Logger.getLogger(BaseAtyponCssLinkRewriterFactory.class);

  // url( [ws] [quote] data: ... [same quote] [ws] )   -- DOTALL, non-greedy to the matching close
  protected static final Pattern DATA_URL = Pattern.compile(
      "url\\(\\s*(?:\"data:[^\"]*\"|'data:[^']*'|data:[^)\"'\\s]*)\\s*\\)",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  // must not look like a URL or contain CSS-special chars, so the rewriter leaves it alone
  private static final String TOKEN = "LOCKSSDATAURI";

  private final LinkRewriterFactory delegate;

  public BaseAtyponCssLinkRewriterFactory() {
    this(new RegexpCssLinkRewriterFactory());
  }

  protected BaseAtyponCssLinkRewriterFactory(LinkRewriterFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public InputStream createLinkRewriter(String mimeType, ArchivalUnit au, InputStream in,
                                        String encoding, String url,
                                        ServletUtil.LinkTransform xform)
      throws PluginException, IOException {
    Charset cs = Charset.forName(encoding != null ? encoding : "UTF-8");
    String css = readAll(in, cs);

    // 1. pull data: URIs out
    List<String> saved = new ArrayList<String>();
    StringBuffer sb = new StringBuffer(css.length());
    Matcher m = DATA_URL.matcher(css);
    while (m.find()) {
      m.appendReplacement(sb, Matcher.quoteReplacement("url(" + TOKEN + saved.size() + ")"));
      saved.add(m.group());
    }
    m.appendTail(sb);
    if (saved.isEmpty()) {
      return delegate.createLinkRewriter(mimeType, au,
          new ByteArrayInputStream(css.getBytes(cs)), cs.name(), url, xform);
    }
    log.debug2(url + ": protecting " + saved.size() + " data: URI(s) from CSS rewriting");

    // 2. rewrite the rest normally
    InputStream rewritten = delegate.createLinkRewriter(mimeType, au,
        new ByteArrayInputStream(sb.toString().getBytes(cs)), cs.name(), url, xform);
    String out = readAll(rewritten, cs);

    // 3. restore. The delegate may have turned url(TOKENn) into url('<prefix>TOKENn')
    //    so match the whole url(...) that contains the token.
    for (int i = saved.size() - 1; i >= 0; i--) {   // reverse: TOKEN1 vs TOKEN10
      Pattern p = Pattern.compile("url\\([^)]*" + TOKEN + i + "(?![0-9])[^)]*\\)");
      Matcher r = p.matcher(out);
      if (r.find()) {
        out = r.replaceFirst(Matcher.quoteReplacement(saved.get(i)));
      } else {
        log.warning(url + ": could not restore data: URI #" + i);
      }
    }
    return new ByteArrayInputStream(out.getBytes(cs));
  }

  private static String readAll(InputStream in, Charset cs) throws IOException {
    Reader r = new InputStreamReader(in, cs);
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[65536];
    int n;
    while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
    r.close();
    return sb.toString();
  }
}
