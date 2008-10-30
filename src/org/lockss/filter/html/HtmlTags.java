/*
 * $Id: HtmlTags.java,v 1.4 2008-10-30 08:29:30 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.htmlparser.tags.*;

/**
 * Collection of additional simple HtmlParser tags.
 * @see HtmlFilterInputStream#makeParser()
 */
public class HtmlTags {

  /**
   * An IFRAME tag.  Registered with PrototypicalNodeFactory to cause iframe
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class Iframe extends CompositeTag {

    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"IFRAME"};

    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }

  }

  /**
   * A NOSCRIPT tag.  Registered with PrototypicalNodeFactory to cause iframe
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class Noscript extends CompositeTag {

    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"NOSCRIPT"};

    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }

  }

}
