/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.escrs;

import org.lockss.util.Logger;

import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.extractor.ArticleMetadataExtractorFactory;

/*public class EscrsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory{

    protected static Logger log = Logger.getLogger(EscrsArticleIteratorFactory.class);

    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE = "\"%sj/%s/a/[^/]+/(abstract/)?\\?(format=pdf&)?lang=(en|pt|es)\", base_url, journal_id";

    private static final List<Pattern> PDF_PATTERNS = new ArrayList<Pattern>(); 
    private static final List<Pattern> ABSTRACT_PATTERNS = new ArrayList<Pattern>(); 

    private static final List<String> PDF_REPLACEMENTS = new ArrayList<String>();
    private static final List<String> ABSTRACTS_REPLACEMENTS = new ArrayList<String>();
    
}
*/