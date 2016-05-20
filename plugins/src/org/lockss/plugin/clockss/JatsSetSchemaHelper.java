/*


 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.*;

/**
 *  A helper class that defines a schema for XML multiple JATS items
 *  The XML provides an "article-set" within which each "article" node
 *  adheres to the JATS schema
 *  
 *  @author alexohlson
 */
public class JatsSetSchemaHelper
extends JatsPublishingSchemaHelper {
  private static final Logger log = Logger.getLogger(JatsSetSchemaHelper.class);


  /*
   * The only difference between JATS and our usage is that we have multiple
   * <article> tag sets enclosed in a single "<article-set>" pair.
   * At some point we made add global data, but not for now.
   */


  /* In our case, we have multiple articles in one file */
  static private final String JATSset_articleNode = "/article-set/article";

  /* 3. At some point we might set up a global map for "article-set" level data */
  
  /**
   * return JATS article map to identify xpaths of interest
   */
  @Override
  public String getArticleNode() {
    return JATSset_articleNode;
  }
}
