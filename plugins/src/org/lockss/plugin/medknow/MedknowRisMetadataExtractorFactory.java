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

package org.lockss.plugin.medknow;

import java.io.IOException;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 *  NOTE: I have found the following:
 *  JO is often used (incorrectly) but consistently as the abbreviated form of the journal title, use JF
 *  Y1 is the date
 *  manually setting the access_url is safest to ensure it's in the AU
TY  - JOUR
A1  - Dash, M
A1  - Padhi, S
A1  - Sahu, S
A1  - Mohanty, I
A1  - Panda, P
A1  - Parida, B
A1  - Sahoo, M
T1  - HIV counseling and testing in a tertiary care hospital in Ganjam district, Odisha, India
Y1  - 2013/4/1
JF  - Journal of Postgraduate Medicine
JO  - J Postgrad Med
SP  - 110
EP  - 114
VL  - 59
IS  - 2
UR  - http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=2;spage=110;epage=114;aulast=Dash
DO  - 10.4103/0022-3859.113831
N2  - <b>Background:</b> Human immunodeficiency virus (HIV) counseling and testing (HCT) conducted at integrated counseling and testing centers (ICTCs) is an entry point, cost-effective intervention in preventing transmission of HIV. <b>Objectives:</b> To study the prevalence of HIV among ICTC attendees, sociodemographic characteristics, and risk behaviors of HIV-seropositive clients. <b>Materials and Methods:</b> It was hospital record-based cross-sectional study of 26,518 registered ICTC clients at a tertiary care hospital in Ganjam district, Odisha, India over a 4-year period from January 2009 to September 2012. <b>Results:</b> A total of 1732 (7.5%) out of 22,897 who were tested for HIV were seropositive. Among HIV-seropositives, 1138 (65.7%) were males, while 594 (34.3%) were females. Majority (88.3%) of seropositives were between the age group of 15-49 years. Client-initiated HIV testing (12.1%) was more seropositive compared to provider-initiated (2.9%). Among discordant couples, majority (95.5%) were male partner/husband positive and female partner/wife negative. Positives were more amongst married, less educated, low socioeconomic status, and outmigrants (<i>P</i>&lt;0.0001). Risk factors included heterosexual promiscuous (89.3%), parent-to-child transmission 5.8%, unknown 3.1%, infected blood transfusion 0.8%, homosexual 0.5%, and infected needles (0.5%). <b>Conclusions:</b> There is need to encourage activities that promote HCT in all health facilities. This will increase the diagnosis of new HIV cases. The data generated in ICTC provide an important clue to understand the epidemiology in a particular geographic region and local planning for care and treatment of those infected with HIV and preventive strategies for those at risk especially married, young adults, and outmigrants to reduce new infections.
ER  - 
 *
 */
public class MedknowRisMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(MedknowRisMetadataExtractorFactory.class);
  
  private static final String MEDKNOW_PUBNAME = "Medknow Publications";

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    
   /*BASE RIS metadata extractor uses the following
    *"T1", MetadataField.FIELD_ARTICLE_TITLE
    *"AU", MetadataField.FIELD_AUTHOR - use A1
    *"JF", MetadataField.FIELD_PUBLICATION_TITLE
    *"DO", MetadataField.FIELD_DOI
    *"PB", MetadataField.FIELD_PUBLISHER - set manually
    *"VL", MetadataField.FIELD_VOLUME
    *"IS", MetadataField.FIELD_ISSUE
    *"SP", MetadataField.FIELD_START_PAGE
    *"EP", MetadataField.FIELD_END_PAGE
    *"DA", MetadataField.FIELD_DATE  - unset and use Y1
    */

    log.debug3("Inside Medknow Metadata extractor factory for RIS files");

    MedknowRisMetadataExtractor mris = new MedknowRisMetadataExtractor();

    mris.removeRisTag("DA");
    mris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    mris.addRisTag("Y1", MetadataField.FIELD_DATE);
    //if UR isn't in the AU then it will get changed later to FULL_TEXT_CU
    return mris;
  }

  public static class MedknowRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); //cooks at the end of this
      
      log.debug3("starting medknow portion of extract...");
      if (am.isEmpty()) {
        log.debug3("the AM from parent->extract was empty");
        return; /* do not emit */
      }

      /*
       * RIS data can be variable.  We don't have any way to add priority to
       * the cooking of data, so fallback to alternate values manually
       */
      if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
        if (am.getRaw("JO") != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("JO")); // might be unabbreviated version
        }
      } 
      
      /* type defaults to ARTICLE_TYPE_JOURNALARTICLE */
      /* The BaseArticleMetadataExtractor will add the tdb defaults and check the access_url */
      am.putIfBetter(MetadataField.FIELD_PUBLISHER,MEDKNOW_PUBNAME);
      emitter.emitMetadata(cu, am);
    }

  }

}
