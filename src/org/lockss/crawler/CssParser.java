/*
 * $Id: CssParser.java,v 1.1 2006-12-09 01:30:59 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.io.*;

import org.lockss.plugin.ArchivalUnit;
import org.w3c.css.sac.*;
import org.w3c.flute.parser.Parser;

/**
 * <p>Extracts links (URIs) from CSS documents.</p>
 * <p>The current implementation is based on the W3C's Flute.</p>
 * @author Thib Guicherd-Callin
 * @see <a href="http://www.w3.org/Style/CSS/SAC/">SAC and Flute at the W3C</a>
 */
public class CssParser implements ContentParser {

  /**
   * <p>An implementation of {@link DocumentHandler} that extracts all
   * links (URIs) from a CSS document and passes them to a
   * {@link FoundUrlCallback} instance.</p>
   * @author Thib Guicherd-Callin
   */
  public static class LockssDocumentHandler implements DocumentHandler {

    protected FoundUrlCallback callback;
    
    public LockssDocumentHandler(FoundUrlCallback callback) {
      this.callback = callback;
    }

    /* Inherit doucmentation */
    public void importStyle(String uri,
                            SACMediaList media,
                            String defaultNamespaceURI)
        throws CSSException {
      callback.foundUrl(uri);
    }

    /* Inherit doucmentation */
    public void property(String name,
                         LexicalUnit value,
                         boolean important)
        throws CSSException {
      if (value.getLexicalUnitType() == LexicalUnit.SAC_URI) {
        callback.foundUrl(value.getStringValue());
      }
    }

    /*
     * All the following methods just ignore their event
     */
    
    public void comment(String text) throws CSSException {}
    public void endDocument(InputSource source) throws CSSException {}
    public void endFontFace() throws CSSException {}
    public void endMedia(SACMediaList media) throws CSSException {}
    public void endPage(String name, String pseudo_page) throws CSSException {}
    public void endSelector(SelectorList selectors) throws CSSException {}
    public void ignorableAtRule(String atRule) throws CSSException {}
    public void namespaceDeclaration(String prefix, String uri) throws CSSException {}
    public void startDocument(InputSource source) throws CSSException {}
    public void startFontFace() throws CSSException {}
    public void startMedia(SACMediaList media) throws CSSException {}
    public void startPage(String name, String pseudo_page) throws CSSException {}
    public void startSelector(SelectorList selectors) throws CSSException {}

  }
  
  /* Inherit documentation */
  public void parseForUrls(Reader reader, 
                           String srcUrl,
                           ArchivalUnit au,
                           FoundUrlCallback cb)
      throws IOException {
    Parser parser = new Parser();
    parser.setDocumentHandler(new LockssDocumentHandler(cb));
    parser.parseStyleSheet(new InputSource(reader));
  }
  
}
