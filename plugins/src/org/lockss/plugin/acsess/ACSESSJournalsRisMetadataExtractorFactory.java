/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.acsess;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.util.Logger;

/*
 * ?? SN is empty - what to do ??
 * Example from https://dl.sciencesocieties.org/publications/aj/articles/106/4/1470:
TY  - JOUR
T2  - Agronomy Journal
TI  - Alfalfa Pasture Bloat Can Be Eliminated by Intermixing with Newly-Developed Sainfoin Population
AU  - Sottie, E. T.
AU  - Acharya, S. N.
AU  - McAllister, T.
AU  - Thomas, J.
AU  - Wang, Y.
AU  - Iwaasa, A.
PB  - The American Society of Agronomy, Inc.
PY  - 2014
CY  - Madison, WI
SN  - 
DO  - 10.2134/agronj13.0378
UR  - http://dx.doi.org/10.2134/agronj13.0378
LA  - English
SP  - 1470
EP  - 1478
AB  - Pasture bloat is a major deterrent to the grazing of alfalfa (Medicago sativa L.) pastures by ruminants, despite the high growth rates that are obtainable. Sainfoin (Onobrychis viciifolia Scop.), a condensed tannin-containing legume, is known to reduce alfalfa pasture bloat in mixed stands. Experiments were conducted in 2010 to 2012 at Lethbridge, AB, using two sainfoin/cultivar AC Blue J alfalfa mixed pastures originally seeded as 50:50 mixes in 2008 and rotationally grazed by steers (Bos taurus). New sainfoin population cultivar LRC-3519 specifically selected for improved performance under a multi-cut system, persisted better (29% of total herbage dry matter [DM]) compared to cultivar Nova (5%) after two cycles of rotational grazing in each year. Bloat incidence and severity in steers were reduced (P < 0.001) by 98% in LRC-3519 mixed stands compared to Nova mixtures when Angus steers grazed sainfoin/alfalfa mixed pastures under conditions for maximizing bloat occurrence. In a separate crop preference study during 2011–2012, eight steers were randomly paired and assigned to four alfalfa and sainfoin strip pastures to determine DM utilization and time spent grazing the two forages. Steers spent more time (55%) in alfalfa strips compared to sainfoin and grazed more (P < 0.05) alfalfa (2048 kg DM) than sainfoin (1164 kg DM). In spite of higher preference for alfalfa, a high proportion of new sainfoin in mixed alfalfa stands reduced risk of bloat substantially in cattle grazing mixed alfalfa/sainfoin pasture.
VL  - 106
M1  - 4
ER  - 
 *
 * Example from https://dl.sciencesocieties.org/publications/cns/articles/47/3/4
TY  - JOUR
T2  - Crops and Soils
TI  - Protecting bee health through integrated pest, crop, and landscape management
AU  - Ehmke, Tanner
PB  - American Society of Agronomy
PY  - 2014
CY  - Madison, WI
SN  - 
DO  - 10.2134/cs2014-47-3-1
UR  - http://dx.doi.org/10.2134/cs2014-47-3-1
LA  - English
SP  - 4
EP  - 11
AB  - The unprecedented rate of annual bee die-offs in recent years has made worldwide news. While the exact cause of these losses remains elusive, research suggests that modern agricultural production practices play an outsized role as a changing agricultural landscape puts pressure on both wild and domesticated bee species. What are some of the ways that farmers and CCAs can promote bee health?
VL  - 47
M1  - 3
ER  - 
 *
 * Example from https://dl.sciencesocieties.org/publications/jeq/articles/43/5/1546
TY  - JOUR
T2  - Journal of Environmental Quality
TI  - Detection of Pathogens, Indicators, and Antibiotic Resistance Genes after Land Application of Poultry Litter
AU  - Cook, K. L.
AU  - Netthisinghe, A. M. P.
AU  - Gilfillen, R. A.
PB  - The American Society of Agronomy, Crop Science Society of America, and Soil Science Society of America, Inc.
PY  - 2014
CY  - Madison, WI
SN  - 
DO  - 10.2134/jeq2013.10.0432
UR  - http://dx.doi.org/10.2134/jeq2013.10.0432
LA  - English
SP  - 1546
EP  - 1558
AB  - Poultry litter (PL) is a by-product of broiler production. Most PL is land applied. Land-applied PL is a valuable nutrient source for crop production but can also be a route of environmental contamination with manure-borne bacteria. The objective of this study was to characterize the fate of pathogens, fecal indicator bacteria (FIB), and bacteria containing antibiotic resistance genes (ARGs) after application of PL to soils under conventional till or no-till management. This 2-yr study was conducted in accordance with normal agricultural practices, and microbial populations were quantified using a combination of culture and quantitative, real-time polymerase chain reaction analysis. Initial concentrations of Campylobacter jejuni in PL were 5.4 ± 3.2 × 106 cells g-1 PL; Salmonella sp. was not detected in the PL but was enriched periodically from PL-amended soils. Escherichia coli was detected in PL (1.5 ± 1.3 × 102 culturable or 1.5 ± 0.3 × 107 genes g-1) but was rarely detected in field soils, whereas enterococci (1.5 ± 0.5 × 108 cells g-1 PL) were detected throughout the study. These results suggest that enterococci may be better FIB for field-applied PL. Concentrations of ARGs for sulfonamide, streptomycin, and tetracycline resistance increased up to 3.0 orders of magnitude after PL application and remained above background for up to 148 d. These data provide new knowledge about important microbial FIB, pathogens, and ARGs associated with PL application under realistic field-based conditions.
VL  - 43
M1  - 5
ER  - 
 */

  public class ACSESSJournalsRisMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    
    static Logger log = 
        Logger.getLogger(ACSESSJournalsRisMetadataExtractorFactory.class);
    
    public FileMetadataExtractor createFileMetadataExtractor(
        MetadataTarget target, String contentType) throws PluginException {
    
      log.debug3("Inside ACSESS RIS metadata extractor factory");
      RisMetadataExtractor aRisMeta = new RisMetadataExtractor();

      aRisMeta.addRisTag("T2", MetadataField.FIELD_PUBLICATION_TITLE);
      aRisMeta.addRisTag("TI", MetadataField.FIELD_ARTICLE_TITLE);
      aRisMeta.addRisTag("AB", MetadataField.FIELD_ABSTRACT);
      aRisMeta.addRisTag("PY", MetadataField.FIELD_DATE);
      aRisMeta.addRisTag("LA", MetadataField.FIELD_LANGUAGE);
      aRisMeta.addRisTag("M1", MetadataField.FIELD_ISSUE);

    return aRisMeta;
  }
    
}
