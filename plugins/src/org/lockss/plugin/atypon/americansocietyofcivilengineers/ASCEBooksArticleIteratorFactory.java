/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import org.lockss.plugin.atypon.BaseAtyponArticleIteratorFactory;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ASCEBooksArticleIteratorFactory extends BaseAtyponArticleIteratorFactory{

    protected static Logger log = Logger.getLogger(ASCEBooksArticleIteratorFactory.class);
    
    @Override
    protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
        return new SubTreeArticleIteratorBuilder(au) {
            @Override
            protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
              return new BuildableSubTreeArticleIterator(au, spec) {
                @Override
                protected ArticleFiles createArticleFiles(CachedUrl cu) {
                  log.debug3("the original cu is " + cu.getUrl());
                  log.debug3("the normalized cu is " + au.siteNormalizeUrl(cu.getUrl()));
                  if (!cu.getUrl().equals(au.siteNormalizeUrl(cu.getUrl()))) {
                    log.debug3("The normalized cu is different so no article files will be emitted.");
                    return null;
                  }
                  return super.createArticleFiles(cu);
                }
              };
            }
          };
    }
}
