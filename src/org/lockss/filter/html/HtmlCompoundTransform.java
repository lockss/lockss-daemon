/*
 * $Id$
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

package org.lockss.filter.html;

import java.io.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.lexer.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/**
 * An HtmlTransform that applies a series of HtmlTransforms.  Each transform
 * requires a separate pass through the parse tree, so if you have a set of
 * transforms that perform a similar action, <i>eg</i>, exclude nodes
 * matching a pattern, it's more efficient to apply one transform with a
 * complex condition:<pre>
 *  NodeFilter[] filters = new NodeFilter[] {
 *    // Filter out &lt;div id="footer"&gt;...&lt;/div&gt;
 *    HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
 *    // Filter out &lt;div class="...advert..."&gt;...&lt;/div&gt;
 *    HtmlNodeFilters.tagWithAttributeRegex("div", "class", ".*advert.*"),
 *  }
 *  return HtmlNodeFilterTransform.exclude(new OrFilter(filters));</pre>
 * than to apply a series of separate transforms:<pre>
 *  HtmlTransform[] transforms = new HtmlTransform[] {
 *    // Filter out &lt;div id="footer"&gt;...&lt;/div&gt;
 *    HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div", "id","footer")),
 *    // Filter out &lt;div class="...advert..."&gt;...&lt;/div&gt;
 *    HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("div", "class", ".*advert.*")),
 *  }
 *  return new HtmlCompoundTransform(transforms));</pre>
 */
public class HtmlCompoundTransform implements HtmlTransform {
  private static Logger log = Logger.getLogger("HtmlCompoundTransform");

  private HtmlTransform[] transforms;

  public HtmlCompoundTransform(HtmlTransform[] transforms) {
    this.transforms = transforms;
  }

  public HtmlCompoundTransform(HtmlTransform t1) {
    this(new HtmlTransform[] {t1});
  }

  public HtmlCompoundTransform(HtmlTransform t1,
                               HtmlTransform t2) {
    this(new HtmlTransform[] {t1, t2});
  }

  public HtmlCompoundTransform(HtmlTransform t1,
                               HtmlTransform t2,
			       HtmlTransform t3) {
    this(new HtmlTransform[] {t1, t2, t3});
  }

  public NodeList transform(NodeList nodeList) throws IOException {
    for (int ix = 0; ix < transforms.length; ix++) {
      nodeList = transforms[ix].transform(nodeList);
    }
    return nodeList;
  }
}
