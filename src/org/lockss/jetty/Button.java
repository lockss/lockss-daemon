/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import org.lockss.util.Logger;
import org.mortbay.html.Tag;

import java.io.IOException;
import java.io.Writer;

/**
 * A button element for Jetty. This provides an element that is represented by
 * the HTML button element rather than an input element. This means that submit
 * buttons can have a separate label and value, supporting i18n.
 */
public class Button extends Tag {

  static Logger log = Logger.getLogger("Button");

  /** The text displayed on the button. This can be internationalised. */
  protected String label;

  /**
   * Create a button.
   * @param key    the name of the button
   * @param value  the value submitted by the button
   * @param type   the type of the button, such as submit, reset, cancel
   * @param label  the text displayed on the button
   */
  public Button(String key, String value, String type, String label) {
    super(key);
    this.label = label;
    // Add attributes to the map
    attribute("name", key);
    attribute("value", value);
    attribute("type", type);
  }

  public void write(Writer out) throws IOException {
    out.write( String.format(
        "<button %s>%s</button>",
        combineAttributes(), label)
    );
  }

  /**
   * Combine the attribute key/value pairs into a string which can be inserted
   * into the element's opening tag.
   * @return
   */
  protected String combineAttributes() {
    StringBuilder sb = new StringBuilder();
    for (Object k : attributeMap.keySet()) {
      // Jetty seems to quote string values before putting them in the
      // attributeMap, so don't quote them again!
      sb.append(" ").append(k).append("=").append(attributeMap.get(k));
    }
    return sb.toString();
  }

}

