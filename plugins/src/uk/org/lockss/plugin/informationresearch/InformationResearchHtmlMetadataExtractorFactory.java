/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package uk.org.lockss.plugin.informationresearch;

import java.io.*;
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringEscapeUtils;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class InformationResearchHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(InformationResearchHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new InformationResearchHtmlMetadataExtractor();
  }
  
  public static class InformationResearchHtmlMetadataExtractor 
    implements FileMetadataExtractor {

      /*
      <meta name="dcterms.title" content="Trust or do not trust: evaluation strategies used by online health information consumers in South East Asia" />
      <meta name="citation_author" content="Inthiran, Anushia" />
      <meta name="dcterms.subject" content="Health information searching is a popular activity conducted on the Internet. However, the focus of past research studies has been on health consumers from the western world. Thus, there is a gap of information pertaining to online health information searching behaviour from the South East Asian region. In this study, trust evaluation strategies used by South East Asian health consumers are described." />
      <meta name="description" content="Health information searching is a popular activity conducted on the Internet. However, the focus of past research studies has been on health consumers from the western world. Thus, there is a gap of information pertaining to online health information searching behaviour from the South East Asian region. In this study, trust evaluation strategies used by South East Asian health consumers are described. A grounded theory approach was used. A total of 80 participants were interviewed.  Interviews were analysed using qualitative analysis methods. Open coding and thematic analysis methods were employed.  Results indicate most participants evaluate information for trustworthiness. The most popular technique used is evaluating the quality of the source. In addition, South East Asian health consumers place high trust value on information based on personal experiences.  This research study extends current understanding of trustworthiness evaluations and points to the need for education and training mechanisms to be in place." />
      <meta name="keywords" content="health information, information searching, South East Asia, trust, grounded theory," />
      <meta name="dcterms.publisher" content="University of Borås" />
      <meta name="dcterms.type" content="text" />
      <meta name="dcterms.identifier" content="ISSN-1368-1613" />
      <meta name="dcterms.identifier" content="http://InformationR.net/ir/26-1/paper886.html" />
      <meta name="dcterms.IsPartOf" content="http://InformationR.net/ir/26-1/infres261.html" />
      <meta name="dcterms.format" content="text/html" />
      <meta name="dc.language" content="en" />
      <meta name="dcterms.rights" content="http://creativecommons.org/licenses/by-nd-nc/1.0/" />
      <meta  name="dcterms.issued" content="2020-09-15" />
   </head>
       */

    // Map Taylor & Francis DublinCore HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dcterms.issued", MetadataField.FIELD_DATE);
      tagMap.put("dcterms.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("dcterms.publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("dc.date.available:", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.description:", MetadataField.DC_FIELD_DESCRIPTION);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      if (am.getRaw("dcterms.identifier")!= null ){
        if (am.getRaw("dcterms.identifier").contains("ISSN-")) {
          am.put(MetadataField.FIELD_ISSN, am.getRaw("dcterms.identifier").replace("ISSN-", ""));
        } else if (am.getRaw("dcterms.identifier").contains("DOI:")) {
          am.put(MetadataField.FIELD_DOI, am.getRaw("dcterms.identifier").replace("DOI: ", ""));

        }
      }

      emitter.emitMetadata(cu, am);
    }
  }
}