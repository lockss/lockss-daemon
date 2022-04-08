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

package org.lockss.plugin.wiley;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * Implements a FileMetadataExtractorFactory for Wiley source content
 * Files used to write this class constructed from Wiley FTP archive:
 *      <base_url>/<year>/A/ADMA23.16.zip 
 * or, as of 2018
 *      <base_url>/<directory>/ADMA23.16.zip 
 *           
 * Metadata found in all xmls (full-text and abstract).
 * 
 * Full-text xml:
 *      <base_url>/<year>/A/AAB102.1.zip!/j.1744-7348.1983.tb02660.x.wml.xml
 *      <base_url>/<year>/A/ADMA23.16.zip!/1810_ftp.wml.xml
 *      
 * Abstract xml:
 *      <base_url>/<year>/A/1803_hdp.wml.xml
 */
public class WileySourceXmlMetadataExtractorFactory
extends SourceXmlMetadataExtractorFactory {

  private static SourceXmlSchemaHelper WileyHelper = null;

  //eg: wiley-released/2013/A/AMET36.3.zip 
  //    where AMeT is the JID, 36 is the volume and 3 is the issue 
  // there are also numerical directories possible at the level of "A" but
  // they don't follow this pattern so can't be decoded
  static private final Pattern JOURNAL_PATTERN = Pattern.compile(
      "/wiley-[^/]+/[^/]+/(?:[A-Z]/)?([A-Z]+)([0-9]+)\\.([0-9]+)");
  static private final Pattern XML_SUFFIX = Pattern.compile(
      "/([^/]+?)(\\.wml(2)?)?\\.xml$");

  static Logger log = Logger.getLogger(WileySourceXmlMetadataExtractorFactory.class);


  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new WileySourceXmlMetadataExtractor();
  }

  public static class WileySourceXmlMetadataExtractor  extends SourceXmlMetadataExtractor {
    
    
    // Before emitting, see if we can fill in any missing values
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in WileySourceXmlMetadataExtractor postCookProcess");

      // hardwire publisher for board report (look at imprints later)
      thisAM.put(MetadataField.FIELD_PUBLISHER, "John Wiley & Sons, Inc.");

      // get journal id, volume and issue from xml url - relic of old plugin
      Matcher mat = JOURNAL_PATTERN.matcher(cu.getUrl());
      if (mat.find()) {
        thisAM.putIfBetter(MetadataField.FIELD_PROPRIETARY_IDENTIFIER, 
                       mat.group(1));
        thisAM.putIfBetter(MetadataField.FIELD_VOLUME, mat.group(2));
        thisAM.putIfBetter(MetadataField.FIELD_ISSUE, mat.group(3));
      }
    }


    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if (WileyHelper != null) {
        return WileyHelper;
      }
      WileyHelper = new WileyXmlSchemaHelper();
      return WileyHelper;
    }

    
    /*
     * Sometimes Wiley doesn't list the pdf file associated with the metadata.
     * See if the pdf exists with just the suffix changed to pdf. The possible
     * suffixes to remove are (.xml|.wml.xml|.wml2.xml)
     * TODO: the xml might be sufficient - if it's abstract only or full-text
     * xml 
     */
    
    
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      String md_url = cu.getUrl();
      String cuBase = FilenameUtils.getFullPath(md_url);
      
      List<String> returnList = new ArrayList<String>();
      String fn_key = helper.getFilenameXPathKey();  
      String filenameValue = oneAM.getRaw(fn_key);
      if (filenameValue != null) {
        log.debug3("add " + filenameValue + " to filenames to check");
        returnList.add(cuBase + filenameValue);
      }
      /* go for an alternate */
      Matcher mat = XML_SUFFIX.matcher(md_url);
      if (mat.find()) {
        filenameValue = mat.group(1);
        log.debug3("add " + filenameValue + " to filenames to check");
        returnList.add(cuBase + filenameValue + ".pdf");
      }
      return returnList;
    }
    
  }
}

