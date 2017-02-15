package org.lockss.crawler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;

import com.lyncode.xoai.model.xoai.XOAIMetadata;
import com.lyncode.xoai.serviceprovider.parsers.MetadataParser;
import com.lyncode.xoai.services.api.MetadataSearch;

public class FuncXoai extends LockssTestCase {
    public void testParseMetadata() throws Exception {
        InputStream input = getResourceAsStream("xoai.xml");

        XOAIMetadata metadata = new MetadataParser().parse(input);
        MetadataSearch<String> searcher = metadata.searcher();
        assertThat(metadata.getElements().size(), equalTo(1));
        assertThat(searcher.findOne("dc.creator"), equalTo("Sousa, Jesus Maria Angelica Fernandes"));
        assertThat(searcher.findOne("dc.date.submitted"), equalTo("1995"));
        assertThat(searcher.findAll("dc.subject").size(), equalTo(5));
    }

}