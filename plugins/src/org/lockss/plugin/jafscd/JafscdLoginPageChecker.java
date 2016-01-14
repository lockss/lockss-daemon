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

package org.lockss.plugin.jafscd;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.lockss.daemon.*;
import org.lockss.util.*;

public class JafscdLoginPageChecker implements LoginPageChecker {

  /**
   * <p>A recognizable string found on login pages.</p>
   */
  //<base href="http://www.agdevjournal.com/volume-1-issue-2/150-testing-and-educating-on-urban-soil-lead-a-case-of-chicago-community-gardens.html" />
  protected static final String URL_VOLUME_1 = "volume-1-issue-2/150-testing-and-educating-on-urban-soil-lead-a-case-of-chicago-community-gardens.html";
  //href="/attachments/article/150/JAFSCD_Testing_and_Educating_on_Urban_Soil_Lead_Jan-2011.pdf"
  protected static final String PDF_VOLUME_1 = "150/JAFSCD_Testing_and_Educating_on_Urban_Soil_Lead_Jan-2011.pdf";
  
  //<base href="http://www.agdevjournal.com/volume-2-issue-1/222-preparing-future-food-system-planning-professionals-and-scholars.html" />
  protected static final String URL_VOLUME_2 = "volume-2-issue-1/222-preparing-future-food-system-planning-professionals-and-scholars.html";
  //href="/attachments/article/222/JAFSCD_Teaching_Food_System_Planning_Dec-2011.pdf"
  protected static final String PDF_VOLUME_2 = "222/JAFSCD_Teaching_Food_System_Planning_Dec-2011.pdf";
  
  //<base href="http://www.agdevjournal.com/volume-3-issue-1/284-sustainable-livelihoods-approaches-women-south-africa.html" />
  protected static final String URL_VOLUME_3 = "volume-3-issue-1/284-sustainable-livelihoods-approaches-women-south-africa.html";
  //href="/attachments/article/284/JAFSCD_Sustainable_Livelihoods_Women_South_Africa_August-2012.pdf"
  protected static final String PDF_VOLUME_3 = "284/JAFSCD_Sustainable_Livelihoods_Women_South_Africa_August-2012.pdf";
  
  //<base href="http://www.agdevjournal.com/volume-4-issue-1/407-alternative-framework-global-land-rush.html" />
  protected static final String URL_VOLUME_4 = "volume-4-issue-1/407-alternative-framework-global-land-rush.html";
  //href="/attachments/article/407/JAFSCD_Alternate_Framework_Landgrabbing_Jan-2014.pdf"
  protected static final String PDF_VOLUME_4 = "407/JAFSCD_Alternate_Framework_Landgrabbing_Jan-2014.pdf";
  
  //<base href="http://www.agdevjournal.com/volume-5-issue-1/483-exurban-farmers-perceptions.html" />
  protected static final String URL_VOLUME_5 = "volume-5-issue-1/483-exurban-farmers-perceptions.html";
  //href="/attachments/article/483/JAFSCD-Exurban-Famers-Perceptions-October-2014.pdf"
  protected static final String PDF_VOLUME_5 = "483/JAFSCD-Exurban-Famers-Perceptions-October-2014.pdf";
  
  private static final List<String> URLS = 
		  Collections.unmodifiableList(Arrays.asList(URL_VOLUME_1, URL_VOLUME_2, URL_VOLUME_3, URL_VOLUME_4, URL_VOLUME_5));
  
  private static final List<String> PDFS = 
		  Collections.unmodifiableList(Arrays.asList(PDF_VOLUME_1, PDF_VOLUME_2, PDF_VOLUME_3, PDF_VOLUME_4, PDF_VOLUME_5));

  @Override
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
             PluginException {
	    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
	    	BufferedReader bReader = new BufferedReader(reader);
	    	int volPage;
	    	//Go through line by line if we are at one of the pages we know pdf links for and 
	    	//they aren't there then we don't have access and we return true
			for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {										
				line = line.trim();										
				if(line.contains("<base")){
					//Is this one of our selected pages
					volPage = checkForUrl(line);
					//If we didn't find a url we were looking for then we're done
					if(volPage == -1) {
						return false;
					}
					//We found a url we are looking for now check for pdf
					return checkReaderForString(bReader, PDFS.get(volPage));
				}
			}
	    }
	    return false;
	  }
	  
	  public int checkForUrl(String line) {
		  for(int i = 0 ; i<URLS.size() ; i++) {
			  if(line.contains(URLS.get(i))){
				  return i;
			  }
		  }
		  return -1;
	  }
	  
	  public boolean checkReaderForString(BufferedReader bReader, String term) 
			  throws IOException {
		  for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {										
				line = line.trim();										
				if(line.contains(term)){
					//pdf found, so we do have access
					return false;
				}
		  }
		  return true;
	  }

}
