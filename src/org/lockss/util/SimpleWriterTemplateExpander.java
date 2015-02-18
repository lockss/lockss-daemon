/*
 * $Id$
 */

/*

Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Simple engine to assist in substituting values for tokens in a template.
 */
public class SimpleWriterTemplateExpander {

  // Matches alphanumeric tokens enclosed in '@'
  static Pattern ONE_ITER_PAT = Pattern.compile("@(\\w+)@");

  private String template;
  private Writer out;
  private Matcher mat;
  private int prevEnd;

  /** Create a SimpleWriterTemplateExpander to process the template,
   * writing the result to the writer.
   * @param template the template
   * @param the Writer to which to write the result
   */
  public SimpleWriterTemplateExpander(String template, Writer out) {
    if (template == null) {
      throw new IllegalArgumentException("Template must not be null");
    }
    if (out == null) {
      throw new IllegalArgumentException("Writer must not be null");
    }
    this.template = template;
    this.out = out;
  }

  /** Return the next token in the template, after writing all characters
   * since the previous token (or beginning of string if this is the first
   * call) to the writer.  Returns null when there are no more tokens, at
   * which point the entire template has been written.
   * @returns the next token, or null if no more tokens
   */
  public String nextToken() throws IOException {
    if (out == null) {
      throw new IllegalStateException("No more tokens to process");
    }
    if (mat == null) {
      mat = ONE_ITER_PAT.matcher(template);
      prevEnd = 0;
    }
    if (mat.find()) {
      if (prevEnd != mat.start()) {
	write(prevEnd, mat.start());
      }
      prevEnd = mat.end();
      return mat.group(1);
    } else {
      write(prevEnd, -1);
      out = null;
      mat = null;
      return null;
    }
  }

  private void write(int start, int end) throws IOException {
    out.write(template, start,
	      end == -1 ? template.length() - start : end - start);
  }
}
