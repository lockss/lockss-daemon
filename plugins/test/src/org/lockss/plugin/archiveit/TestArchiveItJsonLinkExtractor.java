package org.lockss.plugin.archiveit;

import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestArchiveItJsonLinkExtractor  extends LockssTestCase {

  public void testArchiveItJsonLinkExtractor() throws Exception {
    ArchiveItApiJsonLinkExtractor le = new ArchiveItApiJsonLinkExtractor();
    final List<String> extracted = new ArrayList<String>();
    le.extractUrls(null,
        getClass().getResourceAsStream("archiveIt_page1.json"),
        Constants.ENCODING_UTF_8,
        "http://warcs.archive-it.org/wasapi/v1/webdata?collection=FAKE",
        new LinkExtractor.Callback() {
          @Override
          public void foundLink(String url) {
            extracted.add(url);
          }
        });
    //
    List<String> expected = Arrays.asList(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210704003834155.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702093423221.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703190053003.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703072016999.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702183254614.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703214536323.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703231355655.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702015149232.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703225709173.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703135150274.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702193801221.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702202352828.warc.gz",
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703180958699.warc.gz"
    );
    assertEquals(expected, extracted);
  }

}
