/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

public class TestRisMetadataExtractor extends FileMetadataExtractorTestCase {
  
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
          risString = risString + (tagVal[0] + "  - " + tagVal[1] + "\r\n");
        } else {
          risString.concat(tagVal[1] + "\r\n");
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
  
  public void testGoodRis() throws Exception {
    ArticleMetadata am = extractFrom(createRisString());
    assertEquals("[Rothstein, Mark A., Wendell, Wilson K.]",
                 am.getList(MetadataField.FIELD_AUTHOR)+"");
    assertEquals("[2667]", 
                 am.getList(MetadataField.FIELD_START_PAGE).toString());
          assertEquals("[2668]", 
                       am.getList(MetadataField.FIELD_END_PAGE).toString());
    assertEquals("[0028-4793]", 
                 am.getList(MetadataField.FIELD_ISSN).toString());
          assertEquals("[352]", 
                       am.getList(MetadataField.FIELD_VOLUME).toString());
    assertEquals("[26]", 
                 am.getList(MetadataField.FIELD_ISSUE).toString());
    assertEquals("[Article Tile]", 
                 am.getList(MetadataField.FIELD_ARTICLE_TITLE).toString());
    assertEquals("[2005/06/30]", 
                 am.getList(MetadataField.FIELD_DATE).toString());
    assertEquals("[New England Journal of Medicine]",
                 am.getList(MetadataField.FIELD_PUBLICATION_TITLE).toString());
    assertEquals("[Massachusetts Medical Society]",
                 am.getList(MetadataField.FIELD_PUBLISHER).toString());
  }
  
  private class MyFileMetadataExtractorFactory implements FileMetadataExtractorFactory {
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String mimeType)
        throws PluginException {
      return new RisMetadataExtractor();
    }
  }
  
  public void testContinuationLinesAndWhitespace() throws Exception {
    ArticleMetadata am = extractFrom("\r\n" +
                                     "    \r\n" +
                                     "TY  - JOUR\r\n" +
                                     "C1  -    This is a \r\n" +
                                     "   value with\r\n" +
                                     "several  \r\n" +
                                     "\r\n" +
                                     "   \r\n" +
                                     "\t\tcontinuations   \r\n" +
                                     "C2  - No continuation\r\n" +
                                     "ER  - \r\n" +
                                     "\r\n" +
                                     "  ");
    assertEquals(4, am.rawSize());
    assertEquals("JOUR", am.getRaw("TY"));
    assertEquals("This is a value with several continuations", am.getRaw("C1"));
    assertEquals("No continuation", am.getRaw("C2"));
    assertEquals("", am.getRaw("ER"));
  }

}
