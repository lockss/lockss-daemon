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
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.wiley;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
      "/wiley-[^/]+/[0-9]{4}/[A-Z]/([A-Z]+)([0-9]+)\\.([0-9]+)");

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


  }
}

