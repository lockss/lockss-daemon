/*
 * $Id: NatureHtmlMetadataExtractorFactory.java 40402 2015-03-10 22:37:41Z alexandraohlson $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.molvis.plugin;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class MolecularVisionMetadataExtractorFactory
  implements FileMetadataExtractorFactory {

  static Logger log = Logger.getLogger(MolecularVisionMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {

    return new MolecularVisionMetadataExtractor();
  }

  public static class MolecularVisionMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    private static MultiMap tagMap = new MultiValueMap();

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
    throws IOException {
      ArticleMetadata am = super.extract(target, cu);

      ArchivalUnit au = cu.getArchivalUnit();
      TdbAu tdbau = au.getTdbAu();

      String publisherName = null;
      String volume = null;
      String issn = null;
      String eissn = null;
      String title = null;


      if (tdbau != null) {
          publisherName =  tdbau.getPublisherName();

        title =  tdbau.getPublicationTitle();
        volume = tdbau.getVolume();
        issn = tdbau.getIssn();
        eissn = tdbau.getEissn();

        am.put(MetadataField.FIELD_ARTICLE_TITLE, title);
        am.put(MetadataField.FIELD_EISSN, eissn);
        am.put(MetadataField.FIELD_ISSN, issn);
        am.put(MetadataField.FIELD_VOLUME, volume);
      } else if (publisherName == null) {
        publisherName = "Molecular Vision";
      }

      am.put(MetadataField.FIELD_PUBLISHER, publisherName);

      am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);

      am.cook(tagMap);

      return am;
    }
  }
}
