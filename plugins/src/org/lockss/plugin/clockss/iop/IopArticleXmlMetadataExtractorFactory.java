/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.clockss.iop;

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.*;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;


/*
 * If the xml is at 0022-3727/48/35/355104/d_48_35_355104.xml
 * then the pdf is at 0022-3727/48/35/355104/d_48_35_355104.pdf
 * and the author manuscript is at 0022-3727/48/35/355104/d_48_35_355104am.pdf
 * 
 * We are now seeing some delivered (on hard-drive) content where the ".xml" version
 * is missing but there is a 0022-3727/48/35/355104/d_48_35_355104.article
 * so adapting to fail over to this when necessary
 * 
 */

public class IopArticleXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(IopArticleXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper JatsPublishingHelper = null;
  private static SourceXmlSchemaHelper ArticlePublishingHelper = null;
	private static SourceXmlSchemaHelper  CustomizedArticlePublishingHelper = null;
  private static final Map<String,String> IOPIssnTitleMap = new HashMap<String,String>();
  static {
	     IOPIssnTitleMap.put("0004-6256","The Astronomical Journal");
	     IOPIssnTitleMap.put("0004-6280","Publications of the Astronomical Society of the Pacific");
	     IOPIssnTitleMap.put("0004-637X","The Astrophysical Journal");
	     IOPIssnTitleMap.put("0021-4922","Japanese Journal of Applied Physics");
	     IOPIssnTitleMap.put("0022-3727","Journal of Physics D: Applied Physics");
	     IOPIssnTitleMap.put("0026-1394","Metrologia ");
	     IOPIssnTitleMap.put("0029-5515","Nuclear Fusion ");
	     IOPIssnTitleMap.put("0031-8949","Physica Scripta ");
	     IOPIssnTitleMap.put("0031-9120","Physics Education");
	     IOPIssnTitleMap.put("0031-9155","Physics in Medicine & Biology ");
	     IOPIssnTitleMap.put("0034-4885","Reports on Progress in Physics");
	     IOPIssnTitleMap.put("0036-021X","Russian Chemical Reviews");
	     IOPIssnTitleMap.put("0036-0279","Russian Mathematical Surveys ");
	     IOPIssnTitleMap.put("0067-0049","The Astrophysical Journal Supplement Series");
	     IOPIssnTitleMap.put("0143-0807","European Journal of Physics ");
	     IOPIssnTitleMap.put("0169-5983","Fluid Dynamics Research ");
	     IOPIssnTitleMap.put("0253-6102","Communications in Theoretical Physics ");
	     IOPIssnTitleMap.put("0256-307X","Chinese Physics Letters ");
	     IOPIssnTitleMap.put("0264-9381","Classical and Quantum Gravity");
	     IOPIssnTitleMap.put("0266-5611","Inverse Problems");
	     IOPIssnTitleMap.put("0268-1242","Semiconductor Science and Technology");
	     IOPIssnTitleMap.put("0295-5075","Europhysics Letters ");
	     IOPIssnTitleMap.put("0741-3335","Plasma Physics and Controlled Fusion");
	     IOPIssnTitleMap.put("0951-7715","Nonlinearity ");
	     IOPIssnTitleMap.put("0952-4746","Journal of Radiological Protection ");
	     IOPIssnTitleMap.put("0953-2048","Superconductor Science and Technology");
	     IOPIssnTitleMap.put("0953-4075","Journal of Physics B: Atomic, Molecular and Optical Physics");
	     IOPIssnTitleMap.put("0953-8984","Journal of Physics: Condensed Matter");
	     IOPIssnTitleMap.put("0954-3899","Journal of Physics G: Nuclear and Particle Physics");
	     IOPIssnTitleMap.put("0957-0233","Measurement Science and Technology");
	     IOPIssnTitleMap.put("0957-4484","Nanotechnology");
	     IOPIssnTitleMap.put("0960-1317","Journal of Micromechanics and Microengineering");
	     IOPIssnTitleMap.put("0963-0252","Plasma Sources Science and Technology");
	     IOPIssnTitleMap.put("0964-1726","Smart Materials and Structures");
	     IOPIssnTitleMap.put("0965-0393","Modelling and Simulation in Materials Science and Engineering");
	     IOPIssnTitleMap.put("0967-3334","Physiological Measurement ");
	     IOPIssnTitleMap.put("1009-0630","Plasma Science and Technology ");
	     IOPIssnTitleMap.put("1054-660X","Laser Physics ");
	     IOPIssnTitleMap.put("1063-7818","Quantum Electronics");
	     IOPIssnTitleMap.put("1063-7869","Physics-Uspekhi");
	     IOPIssnTitleMap.put("1064-5616","Sbornik: Mathematics");
	     IOPIssnTitleMap.put("1064-5632","Izvestiya: Mathematics");
	     IOPIssnTitleMap.put("1478-3975","Physical Biology");
	     IOPIssnTitleMap.put("1612-2011","Laser Physics Letters ");
	     IOPIssnTitleMap.put("1674-1056","Chinese Physics B ");
	     IOPIssnTitleMap.put("1674-1137","Chinese Physics C ");
	     IOPIssnTitleMap.put("1674-4527","Research in Astronomy and Astrophysics ");
	     IOPIssnTitleMap.put("1674-4926","Journal of Semiconductors ");
	     IOPIssnTitleMap.put("1741-2560","Journal of Neural Engineering");
	     IOPIssnTitleMap.put("1742-2132","Journal of Geophysics and Engineering ");
	     IOPIssnTitleMap.put("1742-6588","Journal of Physics: Conference Series");
	     IOPIssnTitleMap.put("1748-3182","Bioinspiration & Biomimetics");
	     IOPIssnTitleMap.put("1748-6041","Biomedical Materials");
	     IOPIssnTitleMap.put("1751-8113","Journal of Physics A: Mathematical and Theoretical");
	     IOPIssnTitleMap.put("1752-7155","Journal of Breath Research");
	     IOPIssnTitleMap.put("1755-1307","IOP Conference Series: Earth and Environmental Science");
	     IOPIssnTitleMap.put("1757-8981","IOP Conference Series: Materials Science and Engineering");
	     IOPIssnTitleMap.put("1758-5082","Biofabrication");
	     IOPIssnTitleMap.put("1882-0778","Applied Physics Express");
	     IOPIssnTitleMap.put("2040-8978","Journal of Optics ");
	     IOPIssnTitleMap.put("2041-8205","The Astrophysical Journal Letters");
	     IOPIssnTitleMap.put("2043-6262","Advances in Natural Sciences: Nanoscience and Nanotechnology");
	     IOPIssnTitleMap.put("0953-8585","Physics World");
	     //EISSN from here down; no print ISSN for title
	     IOPIssnTitleMap.put("2053-1583","2D Materials");
	     IOPIssnTitleMap.put("2057-1976","Biomedical Physics & Engineering Express");
	     IOPIssnTitleMap.put("2057-1739","Convergent Science Physical Oncology");
	     IOPIssnTitleMap.put("1748-9326","Environmental Research Letters");
	     IOPIssnTitleMap.put("2516-1075","Electronic Structure");
	     IOPIssnTitleMap.put("2058-8585","Flexible and Printed Electronics");
	     IOPIssnTitleMap.put("1475-7516","Journal of Cosmology and Astroparticle Physics ");
	     IOPIssnTitleMap.put("1748-0221","Journal of Instrumentation ");
	     IOPIssnTitleMap.put("2399-6528","Journal of Physics Communications");
	     IOPIssnTitleMap.put("2515-7655","Journal of Physics: Energy");
	     IOPIssnTitleMap.put("2515-7639","Journal of Physics: Materials");
	     IOPIssnTitleMap.put("2515-7647","Journal of Physics: Photonics");
	     IOPIssnTitleMap.put("1742-5468","Journal of Statistical Mechanics: Theory and Experiment ");
	     IOPIssnTitleMap.put("2050-6120","Methods and Applications in Fluorescence");
	     IOPIssnTitleMap.put("2399-7532","Multifunctional Materials");
	     IOPIssnTitleMap.put("2053-1591","Materials Research Express");
	     IOPIssnTitleMap.put("2399-1984","Nano Futures");
	     IOPIssnTitleMap.put("1367-2630","New Journal of Physics ");
	     IOPIssnTitleMap.put("2516-1067","Plasma Research Express");
	     IOPIssnTitleMap.put("2058-9565","Quantum Science & Technology");
	     IOPIssnTitleMap.put("2515-5172","AAS Research Notes");
	     IOPIssnTitleMap.put("2051-672X","Surface Topography: Metrology and Properties");
	     IOPIssnTitleMap.put("2053-1613","Translational Materials Research");
	     IOPIssnTitleMap.put("2515-7620","Environmental Research Communications");
  }  


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new JatsPublishingSourceXmlMetadataExtractor();
  }

  public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

	  @Override
	  protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
		  // Once you have it, just keep returning the same one. It won't change.
		  // They have three different kinds of xml:
		  // 1. customer xml, which is handled by if(cu.getUrl().endsWith("article"))
		  // 2. Jats format, which is handled by the second if-else
		  // 3. Brutal assumption here, the third case has "cm5" in filename
		  // http://clockss-ingest.lockss.org/sourcefiles/iop-released/2020_2/12-05-2020/0953-8984.tar.gz!/0953-8984/17/37/012/cm5_37_012.xml
		  // http://clockss-ingest.lockss.org/sourcefiles/iop-released/2020_2/12-05-2020/0953-8984.tar.gz!/0953-8984/17/37/012/cm5_37_012.xml
		  if(cu.getUrl().endsWith("article")) {
		  		log.debug3("Xml format: Article schema : "  + cu.getUrl());
		  		if (ArticlePublishingHelper == null) {
					ArticlePublishingHelper = new IopArticleXmlSchemaHelper();
				}
			  return ArticlePublishingHelper;

		  } else if (cu.getUrl().contains("/cm5_")) {
			  log.debug3("Xml format: Customer schema : "  + cu.getUrl());
			  if (CustomizedArticlePublishingHelper == null) {
				  CustomizedArticlePublishingHelper = new IopCustomizedArticleXmlSchemaHelper();
			  }
			  return CustomizedArticlePublishingHelper;

		  }  else {
				  log.debug3("Xml format: JATS schema : " + cu.getUrl());
				  if (JatsPublishingHelper == null) {
					  JatsPublishingHelper = new JatsPublishingSchemaHelper();
				  }
			  return JatsPublishingHelper;
		  }
	  }


    /* 
     * filename is the same as the xml, just change the suffix 
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .pdf 
      // instead of .xml
      String url_string = cu.getUrl();
      int suffix_index = url_string.lastIndexOf(".");
      String pdfName;
      if (suffix_index > 0) {
          pdfName = url_string.substring(0,suffix_index+1) + "pdf";
      } else {
          pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      }
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
    		CachedUrl cu, ArticleMetadata thisAM) {

    	log.debug3("in IOP postCookProcess");
    	if(schemaHelper == JatsPublishingHelper ) {
    		//If we didn't get a valid date value, use the copyright year if it's there
    		if (thisAM.get(MetadataField.FIELD_DATE) == null) {
    			if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate) != null) {
    				thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
    			} else {
    				// well then try the print date
    				thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
    			}
    		}
    		if ((thisAM.get(MetadataField.FIELD_ISSN) == null) && (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_pissn) != null)){
    			//try the pissn version if raw value not null
    			thisAM.put(MetadataField.FIELD_ISSN, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_pissn));
    		}
    	} else {
    		if (thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
    			// try to set a publication.title from the IOP issn-title map
    			String mapTitle = IOPIssnTitleMap.get(thisAM.get(MetadataField.FIELD_ISSN)); 
    			if (mapTitle == null) {
    				mapTitle = IOPIssnTitleMap.get(thisAM.get(MetadataField.FIELD_EISSN));
    			}
    			if (mapTitle != null) {
    				thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, mapTitle);
    			}
    		}
    	}
    }

	  @Override
	  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
			  throws IOException, PluginException {
		  try {
			  SourceXmlSchemaHelper schemaHelper;
			  // 1. figure out which XmlMetadataExtractorHelper class to use to get
			  // the schema specific information
			  if ((schemaHelper = setUpSchema(cu)) == null) {
				  log.debug("Unable to set up XML schema. Cannot extract from XML");
				  throw new PluginException("XML schema not set up for " + cu.getUrl());
			  }

			  List<ArticleMetadata> amList =
					  new IopXPathXmlMetadataParser(schemaHelper.getGlobalMetaMap(),
							  schemaHelper.getArticleNode(),
							  schemaHelper.getArticleMetaMap()).extractMetadataFromCu(target, cu);

			  Collection<ArticleMetadata> AMCollection = modifyAMList(schemaHelper, cu,
					  amList);

			  // 4. check, cook, and emit every item in resulting AM collection (list)
			  for ( ArticleMetadata oneAM : AMCollection) {
				  if (preEmitCheck(schemaHelper, cu, oneAM)) {
					  oneAM.cook(schemaHelper.getCookMap());
					  postCookProcess(schemaHelper, cu, oneAM); // hook for optional processing
					  emitter.emitMetadata(cu,oneAM);
				  }
			  }

		  } catch (XPathExpressionException e) {
			  log.debug3("Xpath expression exception:",e);
		  } catch (SAXException e) {
			  handleSAXException(cu, emitter, e);
		  } catch (IOException ex) {
			  handleIOException(cu, emitter, ex);

		  }
	  }
    
  }
}
