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

package org.lockss.plugin.emerald;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class Emerald2020MetadataExtractorFactory implements FileMetadataExtractorFactory {

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new Emerald2020MetadataExtractor();
  }

  /*
     // From journal
     <meta name="dc.Title" content="Point cloud measurements-uncertainty calculation on spatial-feature based registration">
      <meta name="citation_journal_title" content="Sensor Review">
      <meta name="dc.Subject" content="Algorithmic model,Measurement uncertainty,Registered point cloud,Spatial feature">
      <meta name="dc.Description" content="Measurement uncertainty calculation is an important and complicated problem in digitised components inspection. In such inspections, a coordinate measuring machine (CMM) and laser scanner are usually used to get the surface point clouds of the component in different postures. Then, the point clouds are registered to construct fully connected point clouds of the component’s surfaces. However, in most cases, the measurement uncertainty is difficult to estimate after the scanned point cloud has been registered. This paper aims to propose a simplified method for calculating the uncertainty of point cloud measurements based on spatial feature registration.,In the proposed method, algorithmic models are used to calculate the point cloud measurement uncertainty based on noncontact measurements of the planes, lines and points of the component and spatial feature registration.,The measurement uncertainty based on spatial feature registration is related to the mutual position of registration features and the number of sensor commutation in the scanning process, but not to the spatial distribution of the measured feature. The results of experiments conducted verify the efficacy of the proposed method.,The proposed method provides an efficient algorithm for calculating the measurement uncertainty of registration point clouds based on part features, and therefore has important theoretical and practical significance in digitised components inspection.">
      <meta name="dc.Date" scheme="WTN8601" content="2019-01-21T00:00:00Z">
      <meta name="dc.Type" content="article">
      <meta name="dc.Format" content="text/html">
      <meta name="dc.Identifier" scheme="doi" content="10.1108/SR-02-2018-0043">
      <meta name="dc.Identifier" scheme="issn" content="0260-2288">
      <meta name="dc.Identifier" scheme="original-pdf" content="SR-02-2018-0043.pdf">
      <meta name="dc.Language" content="en">
      <meta name="dc.Coverage" content="world">
      <meta name="keywords" content="Algorithmic model,Measurement uncertainty,Registered point cloud,Spatial feature">
      <meta name="dc.Publisher" content="Emerald Publishing Limited">
      <meta name="DCTERMS.bibliographicCitation" scheme="KEV.ctx" content="&amp;ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Ajournal&amp;rft.spage=129&amp;rft.epage=136&amp;rft.volume=39&amp;rft.issue=1">
      <meta name="dc.Contributor" content="Lijun Ding">
      <meta name="dc.Contributor" content="Shuguang Dai">
      <meta name="dc.Contributor" content="Pingan Mu">
      <!--dublincore end-->
      <meta name="description" content="Measurement uncertainty calculation is an important and complicated problem in digitised components inspection. In such inspections, a coordinate measuring machine (CMM) and laser scanner are usually used to get the surface point clouds of the component in different postures. Then, the point clouds are registered to construct fully connected point clouds of the component’s surfaces. However, in most cases, the measurement uncertainty is difficult to estimate after the scanned point cloud has been registered. This paper aims to propose a simplified method for calculating the uncertainty of point cloud measurements based on spatial feature registration.,In the proposed method, algorithmic models are used to calculate the point cloud measurement uncertainty based on noncontact measurements of the planes, lines and points of the component and spatial feature registration.,The measurement uncertainty based on spatial feature registration is related to the mutual position of registration features and the number of sensor commutation in the scanning process, but not to the spatial distribution of the measured feature. The results of experiments conducted verify the efficacy of the proposed method.,The proposed method provides an efficient algorithm for calculating the measurement uncertainty of registration point clouds based on part features, and therefore has important theoretical and practical significance in digitised components inspection.">

      // From book
      <meta name="dc.Title" content="“Internet of Things” Firms and New Patterns of Internationalization">
      <meta name="dc.Subject" content="Internet of Things,Born global,International business,International entrepreneurship and firms internationalization process">
      <meta name="dc.Description" content="The main aim of this research is to provide initial evidences on the internationalization process of the Internet of Things (IoT) firms, seeking to understand which international model could better capture their behavior in the exploration of new business opportunities. A web-based questionnaire has been developed and sent to a sample of IoT managers in order to understand how these firms set up new business in a global landscape. Findings show that the well-known Uppsala model seems to be exceeded in explaining the internationalization process of the IoT firms. These firms seem to be born-global firms in nature with a gradual approach to inter­nationalize. In particular, IoT firms seek to get a leading position within the domestic market before exploring foreign markets. Finally, the IoT managers confirmed these first evidences, highlighting that IoT firms are born global in nature with a gradual approach in exploring and exploiting new business opportunities abroad.">
      <meta name="dc.Date" scheme="WTN8601" content="2017-12-14T00:00:00Z">
      <meta name="dc.Type" content="book-part">
      <meta name="dc.Format" content="text/html">
      <meta name="dc.Identifier" scheme="doi" content="10.1108/978-1-78714-501-620171009">
      <meta name="dc.Identifier" scheme="isbn" content="978-1-78714-502-3">
      <meta name="dc.Identifier" scheme="original-pdf" content="978-1-78714-501-620171009.pdf">
      <meta name="dc.Language" content="en">
      <meta name="dc.Coverage" content="world">
      <meta name="keywords" content="Internet of Things,Born global,International business,International entrepreneurship and firms internationalization process">
      <meta name="dc.Publisher" content="Emerald Publishing Limited">
      <meta name="DCTERMS.bibliographicCitation" scheme="KEV.ctx" content="&amp;ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Abook&amp;rft.spage=123&amp;rft.epage=141&amp;rft.volume=">
      <meta name="citation_inbook_title" content="Global Opportunities for Entrepreneurial Growth: Coopetition and Knowledge Dynamics within and across Firms">
      <meta name = "DC.Relation.IsPartOf" content = "https://www.emerald.com/insight/publication/doi/10.1108/9781787145016">
      <meta name = "DC.Relation.IsPartOf" content = "https://www.emerald.com/insight/publication/acronym/ASEEE">
      <meta name="dc.Contributor" content="Valerio Veglio">
      <!--dublincore end-->
      <meta name="description" content="The main aim of this research is to provide initial evidences on the internationalization process of the Internet of Things (IoT) firms, seeking to understand which international model could better capture their behavior in the exploration of new business opportunities. A web-based questionnaire has been developed and sent to a sample of IoT managers in order to understand how these firms set up new business in a global landscape. Findings show that the well-known Uppsala model seems to be exceeded in explaining the internationalization process of the IoT firms. These firms seem to be born-global firms in nature with a gradual approach to inter­nationalize. In particular, IoT firms seek to get a leading position within the domestic market before exploring foreign markets. Finally, the IoT managers confirmed these first evidences, highlighting that IoT firms are born global in nature with a gradual approach in exploring and exploiting new business opportunities abroad.">

   */

  public static class Emerald2020MetadataExtractor
          extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("DC.Date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.Contributor", MetadataField.DC_FIELD_CONTRIBUTOR);
      tagMap.put("dc.Subject", MetadataField.DC_FIELD_SUBJECT);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
            throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }
  }
}
