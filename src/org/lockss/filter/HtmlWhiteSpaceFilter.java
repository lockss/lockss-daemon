/*
 * $Id: HtmlWhiteSpaceFilter.java,v 1.1 2013-09-06 19:02:07 alexandraohlson Exp $
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

package org.lockss.filter;
import java.io.*;

/** A Reader that cleans up whitespace specifically for HTML
 * It does the following:
 *     replaces &nbsp with " "
 *     uses the standard WhiteSpaceFilter to reduce multiple spaces down to one
 *     removes singleton spaces between tags <boo> </boo>
 *     removes single spaces that are just before or just after tag " <" and "> " 
 * 
 */
public class HtmlWhiteSpaceFilter extends WhiteSpaceFilter {
  
// hmmm...doing the white space stuff first... perhaps need to switch who I inherit from...
  public HtmlWhiteSpaceFilter(Reader reader) {
    super(reader);

    // Do some replace on strings
    String[][] findAndReplace = new String[][] {
        // use of &nbsp; or " " inconsistent over time
        {"&nbsp;", " "}, 
        // remove space between tags
        {"> <", "><"},
        // also catch the case where there is a single space
        // before or after a tag symbol (even if not next to another tag)
        {"> ", ">"},
        {" <", "<"},
        
    };
    this.reader = StringFilter.makeNestedFilter(reader, findAndReplace, false); 
  }

}
