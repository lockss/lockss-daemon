/*

  Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.RisMetadataExtractor;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestRisMetadataExtractor
  extends FileMetadataExtractorTestCase {
  static Logger log = Logger.getLogger("TestRisMetadataExtractor");
  public static String[][] baseRis = {{"TY","JOUR"},
	  								  {"T1","Article Tile"},
	  								  {"AU","Rothstein, Mark A."},
	  								  {"AU","Wendell, Wilson K."},
	  								  {"DA","2005/06/30"},
	  								  {"DO","10.1056/NEJMp058021"},
	  								  {"JF","New England Journal of Medicine"},
	  								  {"SP","2667"},
	  								  {"EP","2668"},
	  								  {"VL","352"},
	  								  {"IS","26"},
	  								  {"PB","Massachusetts Medical Society"},
	  								  {"SN","0028-4793"}};
  
  protected ArrayList<String[]> createBaseRisList(){
	  ArrayList<String[]> baseRisList = new ArrayList<String[]>();
	  for(String[] s: baseRis) {
		  baseRisList.add(s);
	  }
	  return baseRisList;
  }
  protected String createRisString(){
	  return createRisString(createBaseRisList());
  }
  protected String createRisString(ArrayList<String[]> tagValList) {
	  String risString = "";
	  for(String[] tagVal: tagValList){
		  if(tagVal.length == 2){
			  if(!tagVal[0].contentEquals("")){
				  risString = risString + (tagVal[0] + " - " + tagVal[1] + "\n");
			  } else {
				  risString.concat(tagVal[1] + "\n");
			  }
		  } 
	  }
	  return risString;
  }
  
  public FileMetadataExtractorFactory getFactory() {
	  return new MyFileMetadataExtractorFactory();
  }
  
  protected String getMimeType() {
	  return Constants.MIME_TYPE_RIS;
  }
  
  
  public void testNoTypeRis() throws Exception {
	  ArrayList<String[]> risList = createBaseRisList();
	  risList.remove(0);
	  ArticleMetadata am = extractFrom(createRisString(risList));
	  String exp = "[md:";
	  assertEquals(exp, am.toString());
  }
  
  public void testGoodRis() throws Exception {
	  ArticleMetadata am = extractFrom(createRisString());
	  String exp = "[md: [author: [Rothstein, Mark A., Wendell, Wilson K.]] [startpage: [2667]] [issn: [0028-4793]] [issue: [26]] [endpage: [2668]] [journal.title: [New England Journal of Medicine]] [volume: [352]] [article.title: [Article Tile]] [date: [2005/06/30]] [doi: [10.1056/NEJMp058021]] [publisher: [Massachusetts Medical Society]]";
	  assertEquals(exp, am.toString());
  }
  
  private class MyFileMetadataExtractorFactory
	  implements FileMetadataExtractorFactory {
	  MyFileMetadataExtractorFactory() {
	  }
	  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
	                               String mimeType)
	      throws PluginException {
	    return new RisMetadataExtractor();
	  }
  }

}
