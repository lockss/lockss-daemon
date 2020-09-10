/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.emerald;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.spandidos.SpandidosHtmlMetadataExtractorFactory;
import org.lockss.util.Logger;

import java.io.IOException;

public class Emerald2020MetadataExtractorFactory implements FileMetadataExtractorFactory {

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new SpandidosHtmlMetadataExtractorFactory.SpandidosHtmlMetadataExtractor();
  }

  /*
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
   */

  public static class SpandidosHtmlMetadataExtractor
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
