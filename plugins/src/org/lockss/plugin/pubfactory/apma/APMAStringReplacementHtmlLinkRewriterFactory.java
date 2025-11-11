/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.pubfactory.apma;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APMAStringReplacementHtmlLinkRewriterFactory implements LinkRewriterFactory {

  private static final Logger log = Logger.getLogger(APMAStringReplacementHtmlLinkRewriterFactory.class);





  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xform)
          throws PluginException, IOException {

    NodeFilterHtmlLinkRewriterFactory lrf = new NodeFilterHtmlLinkRewriterFactory();

    String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String journal_id = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
    String volume = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());

    String article_url = "/view/journals/" + journal_id + "/" + volume;

    log.debug3("url = " + url + ", journal_id = "+ journal_id + ", volume = " + volume + ", aritcle_url = " + article_url);

    // Check the URL condition
    if (url.contains(article_url) && url.endsWith(".xml")) {

      //replace view/journals/apms/100/6/1000445.xml" with "/downloadpdf/view/journals/apms/100/6/1000445.pdf"
      String pdf_replacement_url = url
              .replace(base_url, "")
              .replace("view/journals/", "downloadpdf/view/journals/")
              .replace(".xml",".pdf");

      log.debug3("Replacement journal_id = "+ journal_id + ", volume = " + volume + ", aritcle_url = " + article_url +", pdf_replacement_url = " + pdf_replacement_url);


      // Convert the InputStream to a byte array once
      byte[] inputBytes = toByteArray(in);

      // Convert the byte array to a string
      String inputContent = new String(inputBytes, encoding);

      // Log expanded context around 'typography-body'
      int index = inputContent.indexOf("typography-body");
      if (index != -1) {
        log.debug3("Expanded Context around 'typography-body': " +
                inputContent.substring(Math.max(0, index - 500), Math.min(inputContent.length(), index + 500)));
      } else {
        log.debug3("The 'typography-body' class was not found in the content.");
      }

      // Updated regex
      String spanRegex = "<span\\s+class=\"typography-body\\s*\">\\s*PDF\\s*</span>";
      String spanReplacement = "<a href=\"/" + pdf_replacement_url + "\">PDF</a>";

      // Log all matches
      Pattern pattern = Pattern.compile(spanRegex);
      Matcher matcher = pattern.matcher(inputContent);
      boolean foundSpan = false;
      while (matcher.find()) {
        foundSpan = true;
        log.debug3("Found span match: " + matcher.group() + ", spanReplacement  = " + spanReplacement);
      }

      if (!foundSpan) {
        log.debug3("No span matches found in the content.");
      }

      // Perform replacement
      String updatedContent = inputContent.replaceAll(spanRegex, spanReplacement);

      // Convert the updated content back to an InputStream
      InputStream updatedInputStream = new ByteArrayInputStream(updatedContent.getBytes(encoding));


      lrf.addPreXform(new NodeFilter() {
        @Override
        public boolean accept(Node node) {

          return false;
        }
      });

      return lrf.createLinkRewriter(mimeType, au, updatedInputStream, encoding, url, xform);
    }

    return lrf.createLinkRewriter(mimeType, au, in, encoding, url, xform);
  }

  // Helper method to convert InputStream to byte array
  private byte[] toByteArray(InputStream in) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      byte[] data = new byte[1024];
      int nRead;
      while ((nRead = in.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return buffer.toByteArray();
    }
  }

  // Helper method to write InputStream to a file
  private void writeInputStreamToFile(InputStream in, String filePath) throws IOException {
    // Create a FileOutputStream to write to the specified file
    try (FileOutputStream fos = new FileOutputStream(filePath);
         BufferedInputStream bis = new BufferedInputStream(in)) {
      byte[] buffer = new byte[1024]; // Buffer for reading data
      int bytesRead;
      while ((bytesRead = bis.read(buffer)) != -1) {
        fos.write(buffer, 0, bytesRead);
      }

      log.debug3("===writeInputStreamToFile===" + filePath);
    }
  }
}
