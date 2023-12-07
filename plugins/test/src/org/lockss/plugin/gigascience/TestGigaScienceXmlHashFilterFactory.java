/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.gigascience;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Constants;

public class TestGigaScienceXmlHashFilterFactory extends LockssTestCase{
    public void testFilter() throws Exception{
        FilterFactory fact = new GigaScienceXmlHashFilterFactory();
        String page = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ 
                     "<gigadb_entry>"+
                            "<dataset id='2082' doi='100661'>"+
                            "</dataset>"+
                            "<samples>"+
                                "<sample_attributes>"+
                                    "<attribute>"+
                                        "<key>geographic location (latitude)</key>"+
                                        "<value>21.54</value>"+
                                        "<unit id=''/>"+
                                    "</attribute>"+
                                    "<attribute>"+
                                        "<key>Description</key>"+
                                        "<value>DNA extracted from buds of two-year-old gynoecious Jatropha curcas for HiC sequencing, biological replicates 1</value>"+
                                        "<unit id=''/>"+
                                    "</attribute>"+
                                    "<attribute>"+
                                        "<key>Analyte type</key>"+
                                        "<value>DNA</value>"+
                                        "<unit id=''/>"+
                                    "</attribute>"+
                                "</sample_attributes>"+
                             "</samples>"+
                            "<experiments> </experiments>"+
                            "<files>"+
                                "<file id='419285' download_count='3'>"+
                                    "<name>The-species-list-of-phase-I_Supp-tab3.csv</name>"+
                                    "<type id='7'>Other</type>"+
                                    "<format id='37'>CSV</format>"+
                                "</file>"+
                                "<file id='419284' download_count='3'>"+
                                    "<name>public-fish-assembly-info_Supp-Tab1.tsv</name>"+
                                    "<type id='131'>Tabular data</type>"+
                                    "<format id='43'>TSV</format>"+
                                "</file>"+
                                "<file id='419283' download_count='8'>"+
                                    "<name>Phylogenetic_tree_of_8_species.nwk</name>"+
                                    "<type id='130'>Phylogenetic tree</type>"+
                                    "<format id='48'>Newick</format>"+
                                "</file>"+
                            "</files>"+
                        "</gigadb_entry>";
        String pageNoFiles =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                        "<gigadb_entry>"+
                            "<dataset id='2082' doi='100661'>"+
                            "</dataset>"+
                            "<samples> </samples>"+
                            "<experiments> </experiments>"+
                        "</gigadb_entry>";

        String result = processPage(page, fact);
        String resultNoFiles = processPage(pageNoFiles, fact);
        
        String correctPage =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                        "<gigadb_entry>"+
                            "<dataset doi=\"100661\" id=\"2082\"/>"+
                            "<samples>"+
                                "<sample_attributes>"+
                                    "<attribute>"+
                                        "<key>Analyte type</key>"+
                                        "<value>DNA</value>"+
                                        "<unit id=\"\"/>"+
                                    "</attribute>"+
                                    "<attribute>"+
                                        "<key>Description</key>"+
                                        "<value>DNA extracted from buds of two-year-old gynoecious Jatropha curcas for HiC sequencing, biological replicates 1</value>"+
                                        "<unit id=\"\"/>"+
                                    "</attribute>"+
                                    "<attribute>"+
                                        "<key>geographic location (latitude)</key>"+
                                        "<value>21.54</value>"+
                                        "<unit id=\"\"/>"+
                                    "</attribute>"+
                                "</sample_attributes>"+
                            "</samples>"+
                            "<experiments> </experiments>"+
                            "<files>"+
                                "<file id=\"419283\">"+
                                    "<name>Phylogenetic_tree_of_8_species.nwk</name>"+
                                    "<type id=\"130\">Phylogenetic tree</type>"+
                                    "<format id=\"48\">Newick</format>"+
                                "</file>"+
                                "<file id=\"419284\">"+
                                    "<name>public-fish-assembly-info_Supp-Tab1.tsv</name>"+
                                    "<type id=\"131\">Tabular data</type>"+
                                    "<format id=\"43\">TSV</format>"+
                                "</file>"+
                                "<file id=\"419285\">"+
                                    "<name>The-species-list-of-phase-I_Supp-tab3.csv</name>"+
                                    "<type id=\"7\">Other</type>"+
                                    "<format id=\"37\">CSV</format>"+
                                "</file>"+
                            "</files>"+
                        "</gigadb_entry>";
        String correctPageNoFiles = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                        "<gigadb_entry>"+
                            "<dataset doi=\"100661\" id=\"2082\"/>"+
                            "<samples> </samples>"+
                            "<experiments> </experiments>"+
                        "</gigadb_entry>";
                
        assertTrue(result.equals(correctPage));
        assertTrue(resultNoFiles.equals(correctPageNoFiles));
    }

    public String processPage(String page, FilterFactory fact) throws Exception{
        InputStream in = IOUtils.toInputStream(page,Constants.DEFAULT_ENCODING);
        InputStream out = fact.createFilteredInputStream(null, in, Constants.DEFAULT_ENCODING);
        String result = IOUtils.toString(out, Constants.DEFAULT_ENCODING);
        return result;
    }
}
