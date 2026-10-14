/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;

/*
 * JavaScript link extractor for Atypon (Literatum UX3) webpack bundles.
 *
 * WHY: UX3 sites load most of their JS as webpack "lazy chunks". The chunk
 * URLs never appear in the HTML - the webpack runtime inside main.bundle-*.js
 * builds them at run time from two lookup tables (chunk id -> name, chunk id -> hash):
 *
 *   webpack 4 (seen on journals.asm.org, 10/2026):
 *     __webpack_require__.p+"js/"+({1:"vendors~foo",6:"alertPopup",...}[a]||a)
 *                         +"-"+{0:"cecc6f...",1:"45a8c6...",...}[a]+".js"
 *   webpack 5:
 *     r.u=e=>"js/"+({12:"toc",...}[e]||e)+"."+{5:"aaaa",12:"bbbb"}[e]+".js"
 *   (and the same shapes for extracted CSS chunks, ending in ".css")
 *
 * Without this extractor an AU gets main.bundle but none of the ~60 chunks,
 * and replay breaks quietly as soon as a page tries to lazy-load one
 * (figure viewer, tabs, TOC widgets...).
 *
 * This extractor rebuilds every chunk URL from those tables. The chunk URLs are
 * relative to the webpack publicPath; this is normally the directory that
 * holds "js/", e.g. https://journals.asm.org/products/asmj/releasedAssets/
 *
 * It also emits the UX3 i18n file, which main.bundle fetches as
 *   "".concat(window.__webpack_public_path__,"/locales/").concat(lang,".json")
 * Note that publicPath already ends with "/", so the browser asks for
 * ".../releasedAssets//locales/en.json" (double slash). We emit exactly that URL,
 * because it's what replay will request.
 *
 * Files that contain no chunk tables (most JS) produce no links, so it is
 * safe to register this for every JavaScript mime type.
 * Crawl rules are unchanged: BaseAtypon already permits
 *   ^https?://host/[^?]*\.(...|js|...)  and  ^https?://host/(...|products|...)
 */
public class BaseAtyponJavaScriptLinkExtractorFactory implements LinkExtractorFactory {

  private static final Logger log =
      Logger.getLogger(BaseAtyponJavaScriptLinkExtractorFactory.class);

  /* Don't try to regex through enormous files (vendor bundles are ~1-2MB) */
  protected static final int MAX_CHARS = 20 * 1024 * 1024;

  private static final String IDX = "\\s*\\[\\s*[\\w$]+\\s*\\]\\s*";
  private static final String OBJ = "(\\{[^{}]*\\})";

  /*
   * "prefix" + ({names}[id] || id) + "sep" + {hashes}[id] + "suffix"
   *  group 1 prefix, 2 names map, 3 separator, 4 hash map, 5 suffix
   */
  protected static final Pattern PATTERN_NAMED_CHUNKS = Pattern.compile(
      "\"([^\"]*)\"\\s*\\+\\s*\\(\\s*" + OBJ + IDX + "\\|\\|\\s*[\\w$]+\\s*\\)\\s*"
      + "\\+\\s*\"([^\"]*)\"\\s*\\+\\s*" + OBJ + IDX
      + "\\+\\s*\"([^\"]*\\.(?:js|css))\"");

  /*
   * "prefix" + id + "sep" + {hashes}[id] + "suffix"   (no names table)
   *  group 1 prefix, 2 id variable, 3 separator, 4 hash map, 5 suffix
   */
  protected static final Pattern PATTERN_UNNAMED_CHUNKS = Pattern.compile(
      "\"([^\"]*)\"\\s*\\+\\s*([\\w$]+)\\s*\\+\\s*\"([^\"]*)\"\\s*\\+\\s*" + OBJ
      + "\\s*\\[\\s*\\2\\s*\\]\\s*\\+\\s*\"([^\"]*\\.(?:js|css))\"");

  /* one key:"value" pair in a table; key may be a bare id or quoted */
  protected static final Pattern PATTERN_MAP_ENTRY = Pattern.compile(
      "(?:\"([^\"]+)\"|'([^']+)'|([\\w$]+))\\s*:\\s*\"([^\"]*)\"");

  /* ux3 i18n loader */
  protected static final Pattern PATTERN_LOCALES = Pattern.compile(
      "__webpack_public_path__\\s*,\\s*\"(/?locales/)\"");

  /* what a chunk name / hash is allowed to look like - guards against false matches */
  protected static final Pattern PATTERN_SAFE_NAME = Pattern.compile("^[\\w.~@-]+$");
  protected static final Pattern PATTERN_SAFE_PREFIX = Pattern.compile("^[\\w.~/-]*$");

  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    return new BaseAtyponJavaScriptLinkExtractor();
  }

  public class BaseAtyponJavaScriptLinkExtractor implements LinkExtractor {

    @Override
    public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
                            String srcUrl, LinkExtractor.Callback cb)
        throws IOException, PluginException {
      if (in == null || srcUrl == null) {
        return;
      }
      String js = readAll(in, encoding);
      String publicPath = getPublicPath(srcUrl);
      Set<String> found = new LinkedHashSet<String>();

      Matcher m = PATTERN_NAMED_CHUNKS.matcher(js);
      while (m.find()) {
        addChunks(found, publicPath, m.group(1), parseMap(m.group(2)),
                  m.group(3), parseMap(m.group(4)), m.group(5));
      }
      m = PATTERN_UNNAMED_CHUNKS.matcher(js);
      while (m.find()) {
        addChunks(found, publicPath, m.group(1), null,
                  m.group(3), parseMap(m.group(4)), m.group(5));
      }
      m = PATTERN_LOCALES.matcher(js);
      if (m.find()) {
        String sep = m.group(1).startsWith("/") ? "" : "/";
        for (String lang : getLocales(au)) {
          // concat(publicPath, "/locales/") -> keep the double slash on purpose
          found.add(publicPath + sep + m.group(1) + lang + ".json");
        }
      }

      for (String url : found) {
        String newUrl = (au != null) ? AuUtil.normalizeHttpHttpsFromBaseUrl(au, url) : url;
        log.debug3("webpack asset: " + newUrl + " from " + srcUrl);
        cb.foundLink(newUrl);
      }
      if (!found.isEmpty()) {
        log.debug2(srcUrl + ": emitted " + found.size() + " webpack asset URLs");
      }
    }
  }

  private void addChunks(Set<String> found, String publicPath, String prefix,
                         Map<String, String> names, String sep,
                         Map<String, String> hashes, String suffix) {
    if (hashes.isEmpty() || !PATTERN_SAFE_PREFIX.matcher(prefix).matches()
        || !(sep.isEmpty() || PATTERN_SAFE_NAME.matcher(sep).matches())) {
      return;
    }
    for (Map.Entry<String, String> e : hashes.entrySet()) {
      String id = e.getKey();
      String name = (names != null && names.containsKey(id)) ? names.get(id) : id;
      String hash = e.getValue();
      if (!PATTERN_SAFE_NAME.matcher(name).matches()
          || !PATTERN_SAFE_NAME.matcher(hash).matches()) {
        continue;
      }
      found.add(publicPath + prefix + name + sep + hash + suffix);
    }
  }

  protected static Map<String, String> parseMap(String obj) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    Matcher m = PATTERN_MAP_ENTRY.matcher(obj);
    while (m.find()) {
      String key = m.group(1) != null ? m.group(1)
                 : m.group(2) != null ? m.group(2) : m.group(3);
      map.put(key, m.group(4));
    }
    return map;
  }

  /*
   * Webpack publicPath is not in the bundle (UX3 sets window.__webpack_public_path__
   * inline in the HTML). Infer it from where the bundle lives: UX3 always puts
   * bundles in <publicPath>js/, so take everything up to "/js/".
   * Otherwise, assume chunks are relative to the bundle's own directory.
   * Children with a different layout can override.
   */
  protected String getPublicPath(String srcUrl) {
    String noQuery = srcUrl.replaceFirst("[?#].*$", "");
    int idx = noQuery.lastIndexOf("/js/");
    if (idx >= 0) {
      return noQuery.substring(0, idx + 1);
    }
    return noQuery.substring(0, noQuery.lastIndexOf('/') + 1);
  }

  /* UX3 picks the locale from the page language; all known Atypon AUs are "en" */
  protected String[] getLocales(ArchivalUnit au) {
    return new String[] { "en" };
  }

  private static String readAll(InputStream in, String encoding) throws IOException {
    Reader r = new InputStreamReader(in, encoding != null ? encoding : "UTF-8");
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[65536];
    int n;
    while ((n = r.read(buf)) > 0 && sb.length() < MAX_CHARS) {
      sb.append(buf, 0, n);
    }
    return sb.toString();
  }
}
