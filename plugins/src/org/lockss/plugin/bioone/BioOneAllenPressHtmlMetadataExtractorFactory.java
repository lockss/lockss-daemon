/*
 * $Id$
 */

/*

 Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bioone;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.apache.oro.text.regex.*;


/**
 * Now modified to use the archived content instead of the new Atypon version. 
 * Articles used as HTML source examples (from beta7.lockss.org) were taken from these TOCs:
 * http://www.bioone.org/perlserv/?request=get-toc&issn=0097-4463&volume=74&issue=4
 * http://www.bioone.org/perlserv/?request=get-toc&issn=0002-8444&volume=91&issue=1&ct=1
 * The individual articles have URLs like:
 * http://www.bioone.org/perlserv/?request=get-document&doi=10.1640 %2F 0002-8444 %28 2001 %29 091[0001 %3A TGOAGI]2.0.CO%3B2
 * http://www.bioone.org/perlserv/?request=get-document&doi=10.2992 %2F 0097-4463 %28 2005 %29 74[217 %3A NSAROT]2.0.CO%3B2
 * (spaces inserted to illustrate some of the metadata encoded therein)
 *
 * @author Neil Mayo
 */
public class BioOneAllenPressHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BioOneHtmlMetadataExtractorFactory");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
  throws PluginException {
    return new BioOneAllenPressHtmlMetadataExtractor();
  }
  
  public static class BioOneAllenPressHtmlMetadataExtractor
    implements FileMetadataExtractor {
      
    // Flags indicating where to find the appropriate text in a line of HTML
      
    // Journal title: <h2 style="margin-bottom: 0px">journalTitle</h2>
    private final String jTitleBeginFlag = "<h2 style=\"margin-bottom: 0px\">";
    private final String jTitleEndFlag = "</h2>";
    
    // Article title: <h1> articleTitle </h1>
    private final String aTitleBeginFlag = "<h1>";
    private final String aTitleEndFlag = "</h1>";

    // Authors: <p class="authors">List of authors with superscript</p>
    private final String authorsBeginFlag = "<p class=\"authors\">";
    private final String authorsEndFlag = "</p>";

    // DOI: <p class=\"info\">DOI: 10.NNNN/........</p>
    private final String doiBeginFlag = "<p class=\"info\">DOI:";
    private final String doiEndFlag = "</p>";

    // Start/End pages: <p style="margin-top: 0px">Article: pp. 217&#8211;224 |
    private final String ppBeginFlag = "Article: pp.\\s*";
    private final String ppEndFlag = "\\s*|";

    // Article issue link holds a lot of useful info as URL params:
    // <p><a href="http://www.bioone.org/perlserv/?request=get-toc&#38;issn=0002-8444&#38;volume=91&#38;issue=1">Volume 91, Issue 1 (January 2001)</a></p>
    
    
    // Precompiled patterns
    
    // Match ", " or " and " or ", and " in an author list - any number of spaces at start or end
    private static Pattern authorSplitPattern = RegexpUtil.uncheckedCompile("(\\s*,?\\s+and\\s*|\\s*,\\s+)", Perl5Compiler.READ_ONLY_MASK);
    private final static String AUTHOR_SEPARATOR = "\t";
    private static Substitution authorSeparatorSubstitution = new StringSubstitution(AUTHOR_SEPARATOR);

    // Match the URL and link text of a link to the issue TOC: 
    // <p><a href="http://www.bioone.org/perlserv/?request=get-toc&#38;issn=0002-8444&#38;volume=91&#38;issue=1">Volume 91, Issue 1 (January 2001)</a></p>
    private static Pattern issueLinkPattern = 
        RegexpUtil.uncheckedCompile("issn=(\\d{4}-\\d{4})&#38;volume=(\\d*)&#38;issue=(\\d*)\">Volume (\\d*), Issue (\\d*) \\(([^)]*)\\)", 
                Perl5Compiler.READ_ONLY_MASK);
    
    // Match the format of the DOI in order to extract metadata hidden in it:
    // 10.1640/0002-8444(2001)091[0001:TGOAGI]2.0.CO;2
    // ISSN 0002-8444, volume 91, start page 1 
    // This is intended for exact matching, not containment matching.
    private static Pattern doiPattern = RegexpUtil.uncheckedCompile("10\\.\\d{4}/(\\d{4}-\\d{4})\\((\\d{4})\\)(\\d*)\\[(\\d*):[^\\s]*", 
            Perl5Compiler.READ_ONLY_MASK);
    
    // Match pages: 217&#8211;224
    private static Pattern ppPattern = RegexpUtil.uncheckedCompile("(\\d*)&#8211;(\\d*)");
    
    /**
     * Extract metadata from the cached URL
     * @param cu The cached URL to extract the metadata from
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
      if (cu == null) {
          throw new IllegalArgumentException("extract(null)");
      }
                  
      ArticleMetadata metadata = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);  
      
      
      // Note: Volume and Start Page can alternatively be extracted from URL before reading the content

      // Get the content
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
          
        // go through the cached URL content line by line
        for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
          line = line.trim();

          // Get DOI from info paragraph tag (occurs on same line with other tags)
          if (line.contains(doiBeginFlag)) {
            String doi = StringUtil.getTextBetween(line, doiBeginFlag, doiEndFlag).trim();
            // Process the DOI to extract other available metadata; also checks the format 
            if (processDOI(doi, metadata)) {
              // Trim space before adding the DOI
              metadata.put(MetadataField.FIELD_DOI, doi.trim());
            }
          }
          
          // Get start and end pages: <p style="margin-top: 0px">Article: pp. 217&#8211;224 |
          if (line.contains(ppBeginFlag)) {
            String pp = StringUtil.getTextBetween(line, ppBeginFlag, ppEndFlag).trim();
            Perl5Matcher matcher = RegexpUtil.getMatcher();
            if (matcher.contains(pp, ppPattern)) {
              MatchResult matches = matcher.getMatch();
              int sp = Integer.parseInt(matches.group(1));
              int ep = Integer.parseInt(matches.group(2));
              metadata.put(MetadataField.FIELD_START_PAGE, ""+sp);
              //metadata.putEndPage(""+ep);
            }
          }
            
          // Get ISSN, volume, issue, date of issue from issue link (text or URL)
          // <p><a href="http://www.bioone.org/perlserv/?request=get-toc&#38;issn=0002-8444&#38;volume=91&#38;issue=1">Volume 91, Issue 1 (January 2001)</a></p>
          if (StringUtil.startsWithIgnoreCase(line, "<h1>Full Text View</h1>")) {
            // Issue link is on the next line
            // OR TRY StringUtil.readLineWithContinuation(BufferedReader rdr)
            String myLine = bReader.readLine();
            if (myLine!=null) {
              // Match the whole line (URL and link text)
              Perl5Matcher matcher = RegexpUtil.getMatcher();
              if (matcher.contains(myLine, issueLinkPattern)) {
                MatchResult matches = matcher.getMatch();
                    String issn = matches.group(1);
                // We have 2 matches each for volume and issue; alternatively 
                // use back refs in the match pattern to ensure the volume/issue
                // instances match up
                int volume = Integer.parseInt(matches.group(2));
                int issue = Integer.parseInt(matches.group(3));
                int volume2 = Integer.parseInt(matches.group(4));
                int issue2 = Integer.parseInt(matches.group(5));
                String date = matches.group(6);
                
                metadata.put(MetadataField.FIELD_ISSN, issn);
                metadata.put(MetadataField.FIELD_VOLUME, ""+volume);
                metadata.put(MetadataField.FIELD_ISSUE, ""+issue);
                metadata.put(MetadataField.FIELD_DATE, date);
              }
              
              // The preceding process assumes the name=value pairs always come 
              // in the same order; alternatively retrieve the URL params:
              // myLine = StringUtil.getTextBetween(myLine, issueLinkBeginFlag, issueLinkEndFlag).trim();
              // and then split() them up into name/value pairs
            }   
          }
            
          // Get journal title from h2 tag (only use of h2)
          if (StringUtil.startsWithIgnoreCase(line, jTitleBeginFlag)) {
            metadata.put(MetadataField.FIELD_JOURNAL_TITLE, 
                StringUtil.getTextBetween(line, jTitleBeginFlag, jTitleEndFlag).trim());
          }
            
          // Get the article title from h1 tag (but not the Full Text View heading)
          if (StringUtil.startsWithIgnoreCase(line, "<h1>") && !StringUtil.startsWithIgnoreCase(line, "<h1>Full Text")){
            // Append more lines until h1 closing tag (should all be on the next line)
            String section = line;
            while (!section.contains("</h1>")) {
              line = bReader.readLine();
              if (line==null) break;
              else section += " "+line.trim();
            }
            section = StringUtil.getTextBetween(section, aTitleBeginFlag, aTitleEndFlag).trim();
            section = HtmlUtil.stripHtmlTags(section);
            metadata.put(MetadataField.FIELD_ARTICLE_TITLE, section.trim());
          }                   
            
          // Get the authors from a single line starting with an "authors" paragraph tag
          if (StringUtil.startsWithIgnoreCase(line, authorsBeginFlag)) {
            String myLine = StringUtil.getTextBetween(line, authorsBeginFlag, authorsEndFlag).trim();
            // Also strip out any sup tags along with their content
            myLine = HtmlUtil.stripHtmlTagsWithTheirContent(myLine, "sup");
            myLine = HtmlUtil.stripHtmlTags(myLine);
            // Make semicolon-separated list of authors by replacing authorSplitPattern with authorSeparatorSubstitution
            Perl5Matcher matcher = RegexpUtil.getMatcher();
            myLine = Util.substitute(matcher, authorSplitPattern, authorSeparatorSubstitution, myLine, Util.SUBSTITUTE_ALL);
            for (String author : myLine.split(AUTHOR_SEPARATOR)) {    
              metadata.put(MetadataField.FIELD_AUTHOR, author);
            }
          }
  
        }
                          
      } finally {
         IOUtil.safeClose(bReader);
      }
      emitter.emitMetadata(cu, metadata);
    }

    /**
     * Extracts metadata which is available bound up in the DOI format, and returns
     * a boolean if it does not match the expected format.  
     * 
     * @return true if the DOI mtched the expected format, false otherwise
     */
    public boolean processDOI(String doi, ArticleMetadata metadata) {
      // Get ISSN/Volume/Page from DOI, which contains them in this sort of format: 
      // 10.1640/0002-8444(2001)091[0001:TGOAGI]2.0.CO;2
      // volume 91, start page 1
      // Note we have already retrieved ISSN, volume and startPage - the following can be used instead
      // to check the previous metadata.
      Perl5Matcher matcher = RegexpUtil.getMatcher();
      if (matcher.matches(doi, doiPattern)) {
        MatchResult matches = matcher.getMatch();
        String issn = matches.group(1);
        String year = matches.group(2);
        // The volume and start page are zero-padded so must be converted (or that could be incorporated in the pattern) 
        int volume = Integer.parseInt(matches.group(3));
        int startPage = Integer.parseInt(matches.group(4));
        // Add the metadata (the year is not used as there is a more precise date of issue available)
        metadata.put(MetadataField.FIELD_ISSN, issn);
        metadata.put(MetadataField.FIELD_VOLUME, ""+volume);
        metadata.put(MetadataField.FIELD_START_PAGE, ""+startPage);
        return true;
      }
      return false;
    }
  }

}
