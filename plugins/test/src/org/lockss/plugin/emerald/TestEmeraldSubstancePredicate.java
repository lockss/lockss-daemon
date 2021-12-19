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

import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;

/**
 * 
 *
 */
public class TestEmeraldSubstancePredicate extends LockssTestCase {
  private EmeraldSubstancePredicateFactory sfact;
  private EmeraldSubstancePredicate subP;
  private MySimulatedArchivalUnit sau;
  private SimulatedContentGenerator scgen;
  private String tmpDirPath;
  private File tmpRoot;
  private File f1, f2, f3, f4;
  private String html1 = "EmeraldTest1.html";      // html file
  private String url1 = "http://www.EmeraldTest1.com/EmeraldTest1.html";
  private String pdf2 = "EmeraldTest2.pdf";        // pdf file
  private String url2 = "http://www.EmeraldTest2.com/EmeraldTest2.pdf";
  private String htmlPdf3 = "EmeraldTest3.pdf";     // html file type with pdf suffix
  private String url3 = "http://www.EmeraldTest3.com/EmeraldTest3.pdf";
  private String pdfHtml4 = "EmeraldTest4.html";    // pdf file type with html suffix
  private String url4 = "http://www.EmeraldTest4.com/EmeraldTest4.html";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.example.com/";
  private static final int DEFAULT_FILESIZE = 3000;
  protected static Logger logger = Logger.getLogger("SimulatedContentGenerator");
  private MockCachedUrl mcu1, mcu2, mcu3, mcu4;
 
  public void setUp() throws Exception {
    super.setUp();
    
    sfact = new EmeraldSubstancePredicateFactory();
    String tmpdir = setUpDiskSpace();
    // create the directory, if it doesn't exist
    sau = new MySimulatedArchivalUnit();
    // simulate that crawl rules only allow .pdfs 
    List subPat = ListUtil.list(".*\\.(pdf)$");
    sau.setSubstanceUrlPatterns(compileRegexps(subPat));   
    
    subP = sfact.makeSubstancePredicate(sau);
    Configuration sconfig= simAuConfig(tmpdir);
    scgen = sau.getContentGenerator(sconfig, tmpdir);
    tmpDirPath = scgen.getContentRoot();

    tmpRoot = new File(tmpDirPath);
    if (!tmpRoot.exists()){
      tmpRoot.mkdirs();
    }
    makeFilesAndCus();     
  }   

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();

    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "6");
    conf.put("fileTypes",
             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
                   | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  /* makeFilesAndCus()
   *  setting up the files and mockCachedUrls for testing
   *  url1/f1/mcu1 - an html file with html mime type
   *  url2/f2/mcu2 - a pdf file with matching application/pdf mime type
   *  url3/f3/mcu3 - file with pdf suffix with html mime type
   *  url3/f3/mcu3 - file with html suffix with applicaiton/pdf mime type
   *    
   */
  private void makeFilesAndCus() {
    try {
      boolean success = false;     
      if (!success) {
        // html file type, html suffix
        f1 = createHtmlFile(tmpRoot, 1, 0, 0, html1);
        mcu1 = new MockCachedUrl(url1, f1.getAbsolutePath(), false);
        mcu1.setExists(true);
        mcu1.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
        // pdf file type, pdf suffix
        f2 = createPdfFile(tmpRoot, 1, 0, 0, pdf2);
        mcu2 = new MockCachedUrl(url2, f2.getAbsolutePath(), false);
        mcu2.setExists(true);
        mcu2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
        // html file type, pdf suffix
        f3 = createHtmlFile(tmpRoot, 1, 0, 0, htmlPdf3);
        mcu3 = new MockCachedUrl(url3, f3.getAbsolutePath(), false);
        mcu3.setExists(true);
        mcu3.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
        // pdf file type, html suffix
        f4 = createPdfFile(tmpRoot, 1, 0, 0, pdfHtml4);
        mcu4 = new MockCachedUrl(url4, f4.getAbsolutePath(), false);
        mcu4.setExists(true);
        mcu4.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
        success = true;
      }
    } catch (Exception e) { System.err.println(e); }
    
  }
  private void cleanupFiles() {
    f1.delete();
    f2.delete();
    f3.delete();
    f4.delete();
    if (tmpRoot.isDirectory()){
      tmpRoot.delete();
    }
  }
  private File createHtmlFile(File parentDir, int fileNum, int depth,
      int branchNum, String filename) {
    File file = null;
    try {
      file = new File(parentDir, filename);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      logger.debug3("Creating HTML file at " + file.getAbsolutePath());
      String file_content = scgen.getHtmlFileContent(filename, fileNum, depth,
                       branchNum, false);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
     
    } catch (Exception e) { System.err.println(e); }
    return file;
  }

  private File createPdfFile(File parentDir, int fileNum, int depth,
      int branchNum, String fileName) {
    File file = null;
    try {

      logger.debug2("Create PDF at " + parentDir.getAbsolutePath() + "/" +
          fileName);
      file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      logger.debug3("Creating PDF file at " + file.getAbsolutePath());
      String file_content = "";

      File testPdf = new File("./plugins/test/src/org/lockss/plugin/minerva/TestMetadata.pdf");
      file_content = testPdf.toString();
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { 
      System.err.println(e); 
    }
    return file;
  }


  public void testEmeraldSubstanceChecker() {
    /* isSubstanceUrl returns true if url matches pattern
     * AND url's mime/type matches the substance type looked for -
     * for Emerald, that type is only application/pdf
     */
    // html file matches type, but not type Emerald considers substance
    // returns false
    assertFalse(subP.isSubstanceUrl(url1));
    // pdf file is substantial, should return true
    assertTrue(subP.isSubstanceUrl(url2));
    // the following should return false because the content does not match suffix
    assertFalse(subP.isSubstanceUrl(url3));
    assertFalse(subP.isSubstanceUrl(url4));
    
    cleanupFiles();
  }
  
  List<Pattern> compileRegexps(List<String> regexps)
  throws MalformedPatternException {
    return RegexpUtil.compileRegexps(regexps);
  }

  /*
   * Created MySimulatedArchivalUnit so I could control the behavior
   * of the MockCachedUrls created by the AU.
   * extended from SimulatedArchivalUnit, to get the SimulatedContentGenerator
   * but needed to add the make/set of the SubstanceUrl patterns from
   * MockArchivalUnit
   */
  public class MySimulatedArchivalUnit extends SimulatedArchivalUnit {
    List<Pattern> substanceUrlPatterns;
    List<Pattern> nonSubstanceUrlPatterns;
    SubstancePredicate substancePred;
    
    public MockCachedUrl makeCachedUrl(String url) {

      if (url.endsWith(html1)) {
        return mcu1;
      } else if (url.endsWith(pdf2)){
        return mcu2;
      } else if (url.endsWith(htmlPdf3)){
        return mcu3;
      } else if (url.endsWith(pdfHtml4)){
        return mcu4;
     }
     return null;
    }
    
    public List<Pattern> makeNonSubstanceUrlPatterns() {
      return nonSubstanceUrlPatterns;
    }

    public List<Pattern> makeSubstanceUrlPatterns() {
      return substanceUrlPatterns;
    }

    public SubstancePredicate makeSubstancePredicate() {
      return substancePred;
    }

    public void setNonSubstanceUrlPatterns(List<Pattern> pats) {
      nonSubstanceUrlPatterns = pats;
    }
    public void setSubstanceUrlPatterns(List<Pattern> pats) {
      substanceUrlPatterns = pats;
    }
    public void setNonSubstancePatterns(List<Pattern> pats) {
      nonSubstanceUrlPatterns = pats;
    }
    public void setSubstancePredicate(SubstancePredicate pred) {
      substancePred = pred;
    }
  }
}

