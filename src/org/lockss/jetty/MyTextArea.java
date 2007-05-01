/*
 * $Id: MyTextArea.java,v 1.4 2007-05-01 23:35:13 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import org.mortbay.html.*;
import org.lockss.util.*;

/** Specialization of TextArea to work around a bug in Lynx.  A newline
 * inside the closing textarea tag causes Lynx to include the rest of the
 * html on the page in the textarea, so suppress the newline in this
 * case. */
public class MyTextArea extends TextArea {
  static Logger log = Logger.getLogger("IpAccessServlet");

  private String mytag;			// private in Block, dammit

  /** @param name The name of the TextArea within the form */
  public MyTextArea(String name) {
    super(name);
    mytag = "textarea";
  }

  /** @param name The name of the TextArea within the form
   * @param s The string in the text area */
  public MyTextArea(String name, String s) {
    super(name, s);
  }

  // Copy of Block.write(Writer), changed to omit the newline in the
  // closing tag.  Block.write(Writer) calls super.write(Writer), but we
  // can't do that here (because that would call Block.write(Writer)), so
  // the body of Block.write(Writer) is also copied here.
  public void write(Writer out) throws IOException {
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
  }

}

