/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.elsevier;

import org.lockss.util.Logger;

//
// A generic article iterator for CLOCKSS source plugins that are zipped and
// want to iterate on files within the zipped file that end in .xml at some 
// level below the root directory. 
//
public class ElsevierDeliveredSourceArticleIteratorFactory extends ElsevierDTD5XmlSourceArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(ElsevierDeliveredSourceArticleIteratorFactory.class);
  
  // delivered plugin adds in a 'directory' just below the year - don't worry about making sure it is exact   
  protected static final String TOP_DELIVERED_METADATA_PATTERN_TEMPLATE = 
      "\"(%s%d/[^/]+/[^/]+)[A-Z]+\\.tar!/([^/]+)/(dataset|.*/main)\\.xml$\",base_url,year,";


  protected String getTopPatternTemplate() {
    return TOP_DELIVERED_METADATA_PATTERN_TEMPLATE;
  }

}
