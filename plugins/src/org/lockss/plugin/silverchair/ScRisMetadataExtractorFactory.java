/*
 * $Id$
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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.silverchair;

import java.io.IOException;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

/*
 * Example from http://jama.jamanetwork.com/article.aspx?articleid=1487502:
TY  - JOUR
T1  - Reporting of noninferiority and equivalence randomized trials: Extension of the consort 2010 statement
AU  - Piaggio G
AU  - Elbourne DR
AU  - Pocock SJ
AU  - Evans SW
AU  - Altman DG
AU  - CONSORT Group f
Y1  - 2012/12/26
N1  - 10.1001/jama.2012.87802
JO  - JAMA
SP  - 2594
EP  - 2604
VL  - 308
IS  - 24
N2  - The CONSORT (Consolidated Standards of Reporting Trials) Statement, which includes a checklist and a flow diagram, is a guideline developed to help authors improve the reporting of the findings from randomized controlled trials. It was updated most recently in 2010. Its primary focus is on individually randomized trials with 2 parallel groups that assess the possible superiority of one treatment compared with another. The CONSORT Statement has been extended to other trial designs such as cluster randomization, and recommendations for noninferiority and equivalence trials were made in 2006. In this article, we present an updated extension of the CONSORT checklist for reporting noninferiority and equivalence trials, based on the 2010 version of the CONSORT Statement and the 2008 CONSORT Statement for the reporting of abstracts, and provide illustrative examples and explanations for those items that differ from the main 2010 CONSORT checklist. The intent is to improve reporting of noninferiority and equivalence trials, enabling readers to assess the reliability of their results and conclusions.
SN  - 0098-7484
M3  - doi: 10.1001/jama.2012.87802
UR  - http://dx.doi.org/10.1001/jama.2012.87802
ER  - 
 *
 * Example from http://journal.publications.chestnet.org/article.aspx?articleID=1216502:
TY  - JOUR
T1  - The costs of critical care telemedicine programs: A systematic review and analysis
AU  - Kumar, Gaurav
AU  - Falk, Derik M.
AU  - Bonello, Robert S.
AU  - Kahn, Jeremy M.
AU  - Perencevich, Eli
AU  - Cram, Peter
Y1  - 2013/01/01
N1  - 10.1378/chest.11-3031
JO  - CHEST Journal
SP  - 19
EP  - 29
VL  - 143
IS  - 1
N2  - Background: 
                                        Implementation of telemedicine programs in ICUs (tele-ICUs) may improve patient outcomes, but the costs of these programs are unknown. We performed a systematic literature review to summarize existing data on the costs of tele-ICUs and collected detailed data on the costs of implementing a tele-ICU in a network of Veterans Health Administration (VHA) hospitals.Methods: 
                                        We conducted a systematic review of studies published between January 1, 1990, and July 1, 2011, reporting costs of tele-ICUs. Studies were summarized, and key cost data were abstracted. We then obtained the costs of implementing a tele-ICU in a network of seven VHA hospitals and report these costs in light of the existing literature.Results: 
                                        Our systematic review identified eight studies reporting tele-ICU costs. These studies suggested combined implementation and first year of operation costs for a tele-ICU of $50,000 to $100,000 per monitored ICU-bed. Changes in patient care costs after tele-ICU implementation ranged from a $3,000 reduction to a $5,600 increase in hospital cost per patient. VHA data suggested a cost for implementation and first year of operation of $70,000 to $87,000 per ICU-bed, depending on the depreciation methods applied.Conclusions: 
                                        The cost of tele-ICU implementation is substantial, and the impact of these programs on hospital costs or profits is unclear. Until additional data become available, clinicians and administrators should carefully weigh the clinical and economic aspects of tele-ICUs when considering investing in this technology.
SN  - 0012-3692
M3  - doi: 10.1378/chest.11-3031
UR  - http://dx.doi.org/10.1378/chest.11-3031
ER  - 
 *
 * Example from http://annals.org/article.aspx?articleID=1700659:
TY  - JOUR
T1  - Bringing the End in Sight: Consensus Regarding HIV Screening Strategies
AU  - Das, Moupali
AU  - Volberding, Paul
Y1  - 2013/07/02
N1  - 10.7326/0003-4819-159-1-201307020-00643
JO  - Annals of Internal Medicine
SP  - 63
EP  - 64
VL  - 159
IS  - 1
N2  - 
SN  - 0003-4819
M3  - doi: 10.7326/0003-4819-159-1-201307020-00643
UR  - http://dx.doi.org/10.7326/0003-4819-159-1-201307020-00643
ER  - 
 */
public class ScRisMetadataExtractorFactory implements FileMetadataExtractorFactory {

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new RisMetadataExtractor() {
      @Override
      public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
          throws IOException, PluginException {
        ArticleMetadata am = extract(target, cu);
        am.putRaw("extractor.type", "RIS");
        /*
         * FIXME
         * putIfBetter only replaces the value if the one stored is invalid
         * which may not be what was intended here, maybe use am.replace(...)
         */
        am.putIfBetter(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("JO"));
        am.putIfBetter(MetadataField.FIELD_DATE, am.getRaw("Y1"));
        am.putIfBetter(MetadataField.FIELD_DOI, am.getRaw("N1"));
        emitter.emitMetadata(cu, am);
      }
    }; 
  }
    
}
