/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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
