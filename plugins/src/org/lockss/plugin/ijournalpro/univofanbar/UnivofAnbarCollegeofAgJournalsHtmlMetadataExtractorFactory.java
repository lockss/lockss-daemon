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

package org.lockss.plugin.ijournalpro.univofanbar;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class UnivofAnbarCollegeofAgJournalsHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(UnivofAnbarCollegeofAgJournalsHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new KareHtmlMetadataExtractor();
  }

  public static class KareHtmlMetadataExtractor
          implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();

    /*
      <meta name="citation_title" content="Comparative Analysis of the Excitation Functions of 238U as Breeder Fuel Using OPTMAN Code." />
      <meta name="citation_author" content="Anthony,, M. I." />
      <meta name="citation_author_institution" content="Department of Physics, Nigerian Defense Academy, Kaduna, Kaduna State, Nigeria." />
      <meta name="citation_author" content="Ige, O. O." />
      <meta name="citation_author_institution" content="Department of Physics, Nigerian Defense Academy, Kaduna, Kaduna State, Nigeria." />
      <meta name="citation_author" content="Rilwan, U. " />
      <meta name="citation_author_institution" content="Department of Physics, Nigerian Army University, PMB 1500, Biu, Borno State, Nigeria." />
      <meta name="citation_author" content="Jonah,, S. A." />
      <meta name="citation_author_institution" content="Centre for Energy Research and Training, (CERT), Zaria, Kaduna State, Nigeria" />
      <meta name="citation_author" content="Aliyu, M. A." />
      <meta name="citation_author_institution" content="Department of Mathematics, Nasarawa State University, Keffi, Nasarawa State, Nigeria" />
      <meta name="citation_author" content="El-Taher, Atef " />
      <meta name="citation_author_institution" content="Physics Department, Faculty of Science, Al Azhar University, Assiut , Egypt." />
      <meta name="citation_abstract" content="The comparative analysis of the excitation function of Uranium-238 was carried out using a Coupled-Channeled Optical Model code, OPTMAN. The high demand for nuclear reactor fuels has necessitated this research. As one of the major naturally occurring radioisotope of Uranium with lots of fuel prospect, Uranium-238 occur in large quantities (99%). Two steps process away from Uranium-238 on neutron capture can produce fissile materials to be used as reactor fuel. Though, Uranium-238 is not by itself a fissile material. But, it is a breeder reactor fuel. Computations were done for both the Potential Expanded by Derivatives (PED) which account for the Rigid-Rotor Model (RRM) that treat nuclei as rigid vibrating sphere as well as account for nuclear volume conservation and Rotational Model Potentials (RMP) which account for the Soft-Rotator Model (SRM) that treat nuclei as soft rotating spherical deformed shapes. Each of the calculated data was compared with the retrieved data from Evaluated Nuclear Dada File (ENDF) which was found to be in good agreement. The threshold energies in all cases were found to be &amp;le;4 MeV for both PED (Potential Expanded by Derivatives) and RMP (Rotational Model Potentials). It is observed that results from RMP much better agreed with the retrieved data than one obtained from PED.&amp;nbsp;" />
      <meta name="citation_id" content="180759" />
      <meta name="citation_publication_date" content="2023/09/30" />
      <meta name="citation_date" content="2023-09-30" />
      <meta name="citation_journal_title" content="Kirkuk Journal of Science" />
      <meta name="citation_issn" content="3005-4788" />
      <meta name="citation_volume" content="18" />
      <meta name="citation_issue" content="3" />
      <meta name="citation_firstpage" content="1" />
      <meta name="citation_lastpage" content="6" />
      <meta name="citation_publisher" content="University of Kirkuk" />
      <meta name="citation_doi" content="10.32894/kujss.2023.141462.1106" />
     */
    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      ArchivalUnit au = cu.getArchivalUnit();

      String tdbVolume = null;
      String citationVolume = null;

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();

      if (tdbau != null) {
        tdbVolume = tdbau.getVolume();
      }

      citationVolume = am.get(MetadataField.FIELD_VOLUME);

      log.debug3("Check metadata citationVolume = " + citationVolume + ", tdbVolume = " + tdbVolume);

      if (tdbVolume == null || citationVolume == null || !tdbVolume.equals(citationVolume)) {
        log.debug3("Emit metadata falied maching volume, citationVolume = " + citationVolume + ", tdbVolume = " + tdbVolume);
        return;
      }
      emitter.emitMetadata(cu, am);
    }
  }
}