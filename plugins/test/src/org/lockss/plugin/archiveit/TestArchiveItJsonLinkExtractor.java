package org.lockss.plugin.archiveit;

import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Constants;

import java.util.*;

public class TestArchiveItJsonLinkExtractor  extends LockssTestCase {

  public void testArchiveItJsonLinkExtractor() throws Exception {
    ArchiveItApiJsonLinkExtractor le = new ArchiveItApiJsonLinkExtractor();
    final List<Map.Entry<String, String>> extracted = new ArrayList<>();
    le.extractUrls(null,
        getClass().getResourceAsStream("archiveIt_page1.json"),
        Constants.ENCODING_UTF_8,
        "http://warcs.archive-it.org/wasapi/v1/webdata?collection=FAKE",
        extracted::add
    );

    //
    List<Map.Entry<String, String>>  expected = new ArrayList<>();
    expected.add(
        new AbstractMap.SimpleEntry<>("https://warcs.archive-it.org/webdatafile/fake_file_20210704003834155.warc.gz", "2021-07-04T00:38:34.155000Z"));

    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702093423221.warc.gz","2021-07-02T09:34:23.221000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703190053003.warc.gz","2021-07-03T19:00:53.003000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703072016999.warc.gz","2021-07-03T07:20:16.999000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702183254614.warc.gz","2021-07-02T18:32:54.614000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703214536323.warc.gz","2021-07-03T21:45:36.323000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703231355655.warc.gz","2021-07-03T23:13:55.655000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702015149232.warc.gz","2021-07-02T01:51:49.232000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703225709173.warc.gz","2021-07-03T22:57:09.173000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703135150274.warc.gz","2021-07-03T13:51:50.274000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702193801221.warc.gz","2021-07-02T19:38:01.221000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210702202352828.warc.gz","2021-07-02T20:23:52.828000Z"));
    expected.add(
        new AbstractMap.SimpleEntry<>(
      "https://warcs.archive-it.org/webdatafile/fake_file_20210703180958699.warc.gz","2021-07-03T18:09:58.699000Z"));

    assertEquals(expected, extracted);
  }

}
