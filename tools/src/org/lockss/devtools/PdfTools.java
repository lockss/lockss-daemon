/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.devtools;

import java.io.*;
import java.util.List;

import org.apache.commons.cli.*;
import org.lockss.pdf.*;
import org.lockss.util.IOUtil;

/**
 * <p>Tools to inspect the contents of PDF documents.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfTools {

  public static final char HELP = 'h';
  
  public static final String HELP_LONG = "help";
  
  public static final char INPUT = 'i';
  
  public static final String INPUT_LONG = "input";
  
  public static final char REWRITE = 'r';
  
  public static final String REWRITE_LONG = "rewrite";
  
  public static final char TOKEN_STREAMS = 't';
  
  public static final String TOKEN_STREAMS_LONG = "token-streams";
  
  public static void main(String[] args) throws Exception {
    PdfTools pdfTools = new PdfTools();
    pdfTools.entryPoint(args);
  }

  protected CommandLine cline;
  
  protected PdfDocument pdfDocument;
  
  public void entryPoint(String[] args) throws Exception {
    try {
      Options options = initializeOptions();
      CommandLineParser parser = new GnuParser();
      cline = parser.parse(options, args);
      if (args.length == 0 || cline.hasOption(HELP)) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PdfTools", options);
      }
      else {
        dispatch();
      }
    }
    finally {
      PdfUtil.safeClose(pdfDocument);
    }
  }
  
  protected void dispatch() throws Exception {
    if (cline.hasOption(REWRITE)) {
      doRewrite();
    }
    if (cline.hasOption(TOKEN_STREAMS)) {
      doTokenStreams();
    }
  }
  
  protected void doRewrite() throws Exception {
    initializePdfDocument();
    PdfUtil.normalizeAllTokenStreams(pdfDocument);
    FileOutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(cline.getOptionValue(REWRITE));
      pdfDocument.save(outputStream);
    }
    finally {
      IOUtil.safeClose(outputStream);
    }
  }
  
  protected void doTokenStreams() throws Exception {
    initializePdfDocument();
    PrintStream ps = null;
    try {
      ps = new PrintStream(new FileOutputStream(cline.getOptionValue(TOKEN_STREAMS)));
      List<PdfPage> pages = pdfDocument.getPages();
      for (int pag = 0 ; pag < pages.size(); ++pag) {
        List<PdfTokenStream> streams = pages.get(pag).getAllTokenStreams();
        for (int str = 0 ; str < streams.size() ; ++str) {
          ps.format("Page %d - Stream %d\n", pag, str);
          List<PdfToken> tokens = streams.get(str).getTokens();
          for (int tok = 0 ; tok < tokens.size() ; ++tok) {
            ps.format("%d\t%s\n", tok, PdfUtil.prettyPrint(tokens.get(tok)));
          }
          ps.println();
        }
        ps.println();
      }
    }
    finally {
      IOUtil.safeClose(ps);
    }
  }
  
  protected Options initializeOptions() {
    Options options = new Options();
    
    OptionBuilder.withLongOpt(HELP_LONG);
    OptionBuilder.withDescription("Displays this help message");
    options.addOption(OptionBuilder.create(HELP));

    OptionBuilder.withLongOpt(INPUT_LONG);
    OptionBuilder.withDescription("Input PDF file");
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("infile");
    options.addOption(OptionBuilder.create(INPUT));

    OptionBuilder.withLongOpt(REWRITE_LONG);
    OptionBuilder.withDescription("Rewrites infile (normalizes token streams, etc.) to outfile");
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("outfile");
    options.addOption(OptionBuilder.create(REWRITE));

    OptionBuilder.withLongOpt(TOKEN_STREAMS_LONG);
    OptionBuilder.withDescription("Dumps all token streams to outfile");
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("outfile");
    options.addOption(OptionBuilder.create(TOKEN_STREAMS));

    return options;
  }
  
  protected void initializePdfDocument() throws Exception {
    if (pdfDocument != null) {
      return;
    }
    if (!cline.hasOption(INPUT)) {
      throw new MissingOptionException(String.format("--%s/-%c is required", INPUT_LONG, INPUT));
    }
    pdfDocument = DefaultPdfDocumentFactory.getInstance().parse(new FileInputStream(cline.getOptionValue(INPUT)));
  }
  
}
