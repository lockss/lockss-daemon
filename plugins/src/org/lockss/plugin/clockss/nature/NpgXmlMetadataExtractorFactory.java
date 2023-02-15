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

package org.lockss.plugin.clockss.nature;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class NpgXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(NpgXmlMetadataExtractorFactory.class);
    
  private static SourceXmlSchemaHelper NPGHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new NpgXmlMetadataExtractor();
  }

  public class NpgXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (NPGHelper != null) {
        return NPGHelper;
      }
      NPGHelper = new NpgXmlSchemaHelper();
      return NPGHelper;
    }

    
    /*
     * Some articles are abstract only and the XML itself provides the entirety 
     * of the content. So look for a PDF but if that's not there, just use 
     * the XML as proof of existence - so all records will emit
     * 
     * In 2016 all the zip files had the foo.xml at the top level of the archive.
     * If there was a PDF, it also was at the top level of the archive with the same basename.
     * 
     * With the 2017 delivery the file layout became less consistent.
     *   bonekey12345.xml (the pdf will be a sibling file of the same basename)
     *   xml/bonekey12345.xml (none of these have pdfs)
     *   xml_temp/bonekey12345.xml (pdf file will live at pdf_temp/bonekey12345.pdf)
     */
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      String url_string = cu.getUrl();
      String pdfFileName = url_string.substring(0,url_string.length() - 3) + "pdf";
      /* if there was a subdirectory within the archive, replace xml with pdf */
      String pdfUrl = pdfFileName.replace("/xml","/pdf");
      ArrayList<String> returnList = new ArrayList<String>();
      returnList.add(pdfUrl); /* the pdf file in its likely location */
      // This extractor is used both by source plugin and by resulting triggered content harvest
      // If this isn't the zip (source plugin) then there will be an abstract html
      // that would be preferred as access_url
      if (!(url_string.contains("zip!"))) {
        String absFileName = url_string.substring(0,url_string.length() - 3) + "html";
        // if there was a subdirectory before the filename, eg "/xml_temp/foo.xml" --> /foo.xml
        absFileName = absFileName.replace("/xml_temp/","/");
        //or
        absFileName = absFileName.replace("/xml/","/");
        returnList.add(absFileName);
      }
      returnList.add(url_string); /* the xml file itself */
      return returnList;
    }    
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
    
      if (thisAM.get(MetadataField.FIELD_PUBLISHER) == null) {
        thisAM.put(MetadataField.FIELD_PUBLISHER,"Nature Publishing Group");
      }
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        String copydate = thisAM.getRaw(NpgXmlSchemaHelper.NPG_copyyear);
        if (copydate != null) {
          thisAM.put(MetadataField.FIELD_DATE,copydate);
        }
      }
    }

  }
}
