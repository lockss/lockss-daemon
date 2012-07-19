/*
 * $Id: Button.java,v 1.1 2012-07-19 11:54:42 easyonthemayo Exp $
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
import org.mortbay.html.Element;
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

  private String mytag;			// private in Block, dammit

  /** The name of the button. */
  protected String key;
  /** The value submitted by the button. */
  protected String value;
  /** The type of the button, such as submit, reset, cancel. */
  protected String type;
  /** The text displayed on the button. */
  protected String label;

  /**
   * Create a button.
   * @param key
   * @param value  the value submitted by the button
   * @param type   the type of the button, such as submit, reset, cancel
   * @param label  the text displayed on the button
   */
  public Button(String key, String value, String type, String label) {
    super(key);
    mytag = "button";
    this.key = key;
    this.value = value;
    this.type = type;
    this.label = label;
  }

  public void write(Writer out) throws IOException {
    out.write(
        String.format(
            "<button type=\"%s\" name=\"%s\" value=\"%s\">%s</button>",
            type, key, value, label)
    );
  }

  // Copy of Block.write(Writer), changed to omit the newline in the
  // closing tag.  Block.write(Writer) calls super.write(Writer), but we
  // can't do that here (because that would call Block.write(Writer)), so
  // the body of Block.write(Writer) is also copied here.
  /*public void write(Writer out) throws IOException {
    out.write('<'+mytag+attributes()+'>');
    // this loop is Composite.write(Writer)
    for (int i=0; i <elements.size() ; i++) {
      Object element = elements.get(i);

      if (element instanceof Element)
	((Element)element).write(out);
      else if (element==null)
	out.write("null");
      else
	out.write(element.toString());
    }
    out.write("</"+mytag+">");
  }*/

}

