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

package org.lockss.plugin.clockss.onixbooks;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.clockss.SourceXmlArticleIteratorFactory;
import org.lockss.plugin.wrapper.PluginCodeWrapper;
import org.lockss.util.Logger;

import java.util.Iterator;

public class Onix3BooksXmlArticleIteratorFactory extends SourceXmlArticleIteratorFactory {
  protected static Logger log = Logger.getLogger(Onix3BooksXmlArticleIteratorFactory.class);

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    // get this AUs metadata extractor, which is wrapped, in case the casting fails, catch
    try {
      PluginCodeWrapper pcw = (PluginCodeWrapper) au.getFileMetadataExtractor(target, "text/xml");
      Onix3LongSourceXmlMetadataExtractorFactory.Onix3LongSourceXmlMetadataExtractor ome =
          (Onix3LongSourceXmlMetadataExtractorFactory.Onix3LongSourceXmlMetadataExtractor) pcw.getWrappedObj();
      // reset the record set for this auid
      ome.resetRecordsSet(au.getAuId());
      log.debug3("Resetting allRecords set in order to deduplicate the disparate metadata sources.");
    } catch (ClassCastException exception) {
      log.debug3("Failed to cast Onix3LongSourceXmlMetadataExtractor, won't be able to reset the records set.");
    }
    return super.createArticleIterator(au, target);
  }
}
