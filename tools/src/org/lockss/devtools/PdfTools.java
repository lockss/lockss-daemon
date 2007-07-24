/*
 * $Id: PdfTools.java,v 1.18 2007-07-24 00:16:42 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Iterator;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.*;
import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.DocumentTransformUtil.*;
import org.lockss.filter.pdf.PageTransformUtil.IdentityPageTransform;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

/**
 * <p>Tools to inspect the contents of PDF documents.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfTools {

  private static final String HELP_DESCR = "display this usage message";

  protected static final char HELP_CHAR = 'h';

  protected static final String HELP_STR = "help";

  public static class DumpAnnotations extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      Iterator iter = pdfPage.getAnnotationIterator();
      for (int ann = 0 ; iter.hasNext() ; ++ann) {
        PDAnnotation annotation = (PDAnnotation)iter.next();
        dump("Contents", annotation.getContents());
        dump("Rectangle", annotation.getRectangle());
        console.println();
      }
      return true;
    }
  }

  public static class DumpBoxes extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      PdfTools.dump("Art box", pdfPage.getArtBox());
      PdfTools.dump("Bleed box", pdfPage.getBleedBox());
      PdfTools.dump("Crop box", pdfPage.getCropBox());
      PdfTools.dump("Media box", pdfPage.getMediaBox());
      PdfTools.dump("Trim box", pdfPage.getTrimBox());
      return super.transform(pdfPage);
    }
  }

  public static class DumpMetadata extends IdentityDocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      console.println("Number of pages: " + pdfDocument.getNumberOfPages());
      dump("Creation date", pdfDocument.getCreationDate().getTime());
      dump("Modification date", pdfDocument.getModificationDate().getTime());
      dump("Author", pdfDocument.getAuthor());
      dump("Creator", pdfDocument.getCreator());
      dump("Keywords", pdfDocument.getKeywords());
      dump("Producer", pdfDocument.getProducer());
      dump("Subject", pdfDocument.getSubject());
      dump("Title", pdfDocument.getTitle());
      console.println("Metadata (as string):"); // newline; typically large
      console.println(pdfDocument.getMetadataAsString());
      return super.transform(pdfDocument);
    }
  }

  public static class DumpNumberedPageStream extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      Iterator tokens = pdfPage.getStreamTokenIterator();
      for (int tok = 0 ; tokens.hasNext() ; ++tok) {
        console.println(Integer.toString(tok) + "\t" + tokens.next().toString());
      }
      return super.transform(pdfPage);
    }
  }

  public static class DumpPageDictionary extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      dump(pdfPage.getDictionary());
      return super.transform(pdfPage);
    }
  }

  public static class DumpPageStream extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      for (Iterator tokens = pdfPage.getStreamTokenIterator() ; tokens.hasNext() ; ) {
        console.println("\t" + tokens.next().toString());
      }
      return super.transform(pdfPage);
    }
  }

  public static class DumpTrailer extends IdentityDocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      dump(pdfDocument.getTrailer());
      return super.transform(pdfDocument);
    }
  }

  public static class ReiteratePageStream implements PageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      PDStream resultStream = pdfPage.getPdfDocument().makePdStream();
      OutputStream outputStream = resultStream.createOutputStream();
      ContentStreamWriter tokenWriter = new ContentStreamWriter(outputStream);
      tokenWriter.writeTokens(pdfPage.getStreamTokens());
      pdfPage.setContents(resultStream);
      return true;
    }
  }

  public static class SplitDocumentTransform extends DocumentTransformDecorator {

    protected String suffix;

    public SplitDocumentTransform(String suffix,
                                  DocumentTransform documentTransform) {
      super(documentTransform);
      this.suffix = suffix;
    }

    public boolean transform(PdfDocument pdfDocument) throws IOException {
      if (!splitOutput(suffix)) {
        return false;
      }
      boolean ret = documentTransform.transform(pdfDocument);
      console.println();
      if (!joinOutput(suffix)) {
        return false;
      }
      return ret;
    }

  }

  public static class SplitPageTransform extends PageTransformWrapper {

    protected String suffix;

    public SplitPageTransform(String suffix,
                              PageTransform pageTransform) {
      super(pageTransform);
      this.suffix = suffix;
    }

    public boolean transform(PdfDocument pdfDocument) throws IOException {
      if (!splitOutput(suffix)) {
        return false;
      }
      boolean ret = true;
      for (Iterator iter = pdfDocument.getPageIterator() ; iter.hasNext() ; ) {
        ret = pageTransform.transform((PdfPage)iter.next());
        console.println();
        if (!ret) {
          break;
        }
      }
      if (!joinOutput(suffix)) {
        return false;
      }
      return ret;
    }

  }

  protected static final String ANNOTATIONS = "-annotations";

  protected static final String APPLY = "-apply";

  protected static OutputDocumentTransform applyTransform;

  protected static boolean argAnnotations;

  protected static boolean argApply;

  protected static String argApplyValue;

  protected static boolean argBoxes;

  protected static boolean argIn;

  protected static String argInBase;

  protected static String argInValue;

  protected static boolean argLog;

  protected static String argLogValue;

  protected static boolean argMetadata;

  protected static boolean argNumberedPageStream;

  protected static boolean argOut;

  protected static String argOutValue;

  protected static boolean argPageStream;

  protected static boolean argQuiet;

  protected static boolean argRewrite;

  protected static boolean argTrailer;

  protected static final String BOXES = "-boxes";

  protected static PrintStream console;

  protected static boolean doTransform;

  protected static String doTransformString;

  protected static final String HELP = "-help";

  protected static final String HELP_SHORT = "-h";

  protected static final String IN = "-in";

  protected static final String LOG = "-log";

  protected static final String METADATA = "-metadata";

  protected static final String NUMBEREDPAGESTREAM = "-numberedpagestream";

  protected static final String OUT = "-out";

  protected static final String PAGESTREAM = "-pagestream";

  protected static InputStream pdfInputStream;

  protected static OutputStream pdfOutputStream;

  protected static final String QUIET = "-quiet";

  protected static PrintStream rememberConsole;

  protected static FileOutputStream rememberFileOutputStream;

  protected static final String REWRITE = "-rewrite";

  protected static final String TRAILER = "-trailer";

  protected static final String USAGE = "-usage";

  protected static final String USAGE_MESSAGE =
    "\tant run-tool -Dclass=" + PdfTools.class.getName() + "\n" +
    "\t\t-Dargs=\"-in in.pdf [options/commands...]\"\n" +
    "Help\n" +
    " -h -help -usage     Displays this message\n" +
    "Commands\n" +
    " -annotations        Dumps the annotations for each page to a file\n" +
    " -boxes              Dumps the bounding boxes for each page to a file\n" +
    " -metadata           Dumps the document metadata to a file\n" +
    " -numberedpagestream Dumps a numbered list of tokens for each page to a file\n" +
    " -pagestream         Dumps a list of tokens for each page to a file\n" +
    " -trailer            Dumps the document trailer dictionary to a file\n" +
    "Transforms\n" +
    " -apply my.package.MyTransform Applies the given document or page transform\n" +
    "                               to the input (output file: -out out.pdf)\n" +
    " -rewrite                      Rewrites the input, usually in a cleaner and\n" +
    "                               more verbose way (output file: -out out.pdf)\n" +
    "Output\n" +
    " -quiet              Suppresses command output to the console\n" +
    " -log log.txt        Copies command output to the given file\n" +
    "Unimplemented\n" +
    " -pagedictionary\n";

//  public static void main(String[] args) throws IOException {
//    try {
//      if (!(   parseHelp(args)
//    		&& parseArgs(args)
//    		&& validateArgs()
//    		&& setUpInputOutput()
//    		&& applyTransforms())) {
//        System.exit(1);
//      }
//    }
//    finally {
//      tearDownInputOutput();
//    }
//  }

  protected static boolean applyTransforms() throws IOException {
    AggregateDocumentTransform documentTransform = new AggregateDocumentTransform();

    if (argAnnotations) {
      documentTransform.add(new SplitPageTransform(ANNOTATIONS, new DumpAnnotations()));
    }
    if (argBoxes) {
      documentTransform.add(new SplitPageTransform(BOXES, new DumpBoxes()));
    }
    if (argMetadata) {
      documentTransform.add(new SplitDocumentTransform(METADATA, new DumpMetadata()));
    }
    if (argNumberedPageStream) {
      documentTransform.add(new SplitPageTransform(NUMBEREDPAGESTREAM, new DumpNumberedPageStream()));
    }
    if (argPageStream) {
      documentTransform.add(new SplitPageTransform(PAGESTREAM, new DumpPageStream()));
    }
    if (argTrailer) {
      documentTransform.add(new SplitDocumentTransform(TRAILER, new DumpTrailer()));
    }

    PdfDocument pdfDocument = null;
    try {
      pdfDocument = new PdfDocument(pdfInputStream);
      if (!documentTransform.transform(pdfDocument)) {
        System.err.println("Transform unsuccessful (first phase)");
        return false;
      }
      if (doTransform) {
        if (!applyTransform.transform(pdfDocument, pdfOutputStream)) {
          System.err.println("Transform unsuccessful (second phase)");
          return false;
        }
      }
      return true;
    }
    catch (IOException ioe) {
      System.err.println("Transform failed");
      ioe.printStackTrace(System.err);
      return false;
    }
    finally {
      PdfDocument.close(pdfDocument);
    }
  }

  protected static void dump(COSDictionary dictionary) {
    for (Iterator keys = dictionary.keyList().iterator() ; keys.hasNext() ; ) {
      COSName key = (COSName)keys.next();
      console.println(key.getName() + "\t" + dictionary.getItem(key).toString());
    }
  }

  protected static void dump(String name, Object obj) {
    if (obj != null) {
      console.println(name + ": " + obj.toString());
    }
  }

  protected static void dump(String name, String str) {
    if (str != null && !str.equals("")) {
      console.println(name + ": " + str);
    }
  }

  protected static boolean joinOutput(String suffix) {
    String fileName = argInBase + suffix + ".txt";
    try {
      console = rememberConsole;
      rememberConsole = null;
      rememberFileOutputStream.close();
      rememberFileOutputStream = null;
      return true;
    }
    catch (IOException ioe) {
      System.err.println("Error: could not close an output stream to " + fileName);
      ioe.printStackTrace(System.err);
      return false;
    }
  }

  protected static boolean parseArgs(String[] args) {
    for (int arg = 0 ; arg < args.length ; ++arg) {
      if (false) {
        // Copy/paste hack
      }
      else if (args[arg].equals(ANNOTATIONS)) {
        argAnnotations = true;
      }
      else if (args[arg].equals(APPLY)) {
        argApply = true;
        argApplyValue = args[++arg];
      }
      else if (args[arg].equals(BOXES)) {
        argBoxes = true;
      }
      else if (args[arg].equals(IN)) {
        argIn = true;
        argInValue = args[++arg];
      }
      else if (args[arg].equals(LOG)) {
        argLog = true;
        argLogValue = args[++arg];
      }
      else if (args[arg].equals(METADATA)) {
        argMetadata = true;
      }
      else if (args[arg].equals(NUMBEREDPAGESTREAM)) {
        argNumberedPageStream = true;
      }
      else if (args[arg].equals(OUT)) {
        argOut = true;
        argOutValue = args[++arg];
      }
      else if (args[arg].equals(PAGESTREAM)) {
        argPageStream = true;
      }
      else if (args[arg].equals(QUIET)) {
        argQuiet = true;
      }
      else if (args[arg].equals(REWRITE)) {
        argRewrite = true;
      }
      else if (args[arg].equals(TRAILER)) {
        argTrailer = true;
      }
      else {
        System.err.println("Unknown option: " + args[arg]);
        return false;
      }
    }
    return true;
  }

  protected static boolean parseHelp(String[] args) {
    if (   args.length == 0
        || (  args.length == 1
            && (   args[0].equals(HELP_SHORT)
                || args[0].equals(HELP)
                || args[0].equals(USAGE)))) {
      System.err.println(USAGE_MESSAGE);
      return false;
    }
    return true;
  }

  protected static boolean setUpInputOutput() {
    if (argInValue.endsWith(".pdf")) {
      argInBase = argInValue.substring(0, argInValue.length() - ".pdf".length());
    }
    else {
      argInBase = argInValue;
    }

    // Set up PDF input
    try {
      pdfInputStream = new FileInputStream(argInValue);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println("Error: input file not found");
      fnfe.printStackTrace(System.err);
      return false;
    }

    // Set up PDF output
    if (argOut) {
      try {
        if (argOutValue.startsWith("-")) {                // this functionality
          argOutValue = argInBase + argOutValue + ".pdf"; // is currently
        }                                                 // undocumented
        pdfOutputStream = new FileOutputStream(argOutValue);
      }
      catch (FileNotFoundException fnfe) {
        System.err.println("Error: could not set up output stream");
        fnfe.printStackTrace(System.err);
        return false;
      }
    }
    else {
      pdfOutputStream = new NullOutputStream();
    }

    // Set up console output
    // ...first, the actual console output
    if (argQuiet) {
      console = new PrintStream(new NullOutputStream());
    }
    else {
      console = System.out;
    }
    // ...then, the indirect output
    if (argLog) {
      try {
        FileOutputStream logOutputStream = new FileOutputStream(argLogValue);
        TeeOutputStream teeOutputStream = new TeeOutputStream(console, logOutputStream);
        console = new PrintStream(teeOutputStream);
      }
      catch (FileNotFoundException fnfe) {
        System.err.println("Error: could not set up log output stream");
        fnfe.printStackTrace(System.err);
        return false;
      }
    }

    // Done
    return true;
  }

  protected static boolean splitOutput(String suffix) {
    String fileName = argInBase + suffix + ".txt";
    try {
      rememberFileOutputStream = new FileOutputStream(fileName);
      TeeOutputStream teeOutputStream = new TeeOutputStream(console, rememberFileOutputStream);
      rememberConsole = console;
      console = new PrintStream(teeOutputStream);
      return true;
    }
    catch (FileNotFoundException fnfe) {
      System.err.println("Error: could not create an output stream for " + fileName);
      fnfe.printStackTrace(System.err);
      return false;
    }
  }

  protected static void tearDownInputOutput() {
    IOUtils.closeQuietly(pdfInputStream);
    IOUtils.closeQuietly(pdfOutputStream);
    IOUtils.closeQuietly(console);
  }

  protected static boolean validateArgs() {
    // Cannot have APPLY and REWRITE at the same time
    if (argApply && argRewrite) {
      System.err.println("Error: cannot specify " + APPLY + " and " + REWRITE + " at the same time.");
      return false;
    }

    // Remember that a transform is requested
    if (argApply || argRewrite) {
      doTransform = true;
      doTransformString = argApply ? APPLY : REWRITE;
    }

    // IN must be specified
    if (!argIn) {
      if (doTransform) {
        System.err.println("Error: cannot specify " + doTransformString + " without " + IN);
      }
      else {
        System.err.println("Error: must specify " + IN);
      }
      return false;
    }

    // OUT must be specified if and only if a transform is requested
    if (!argOut && doTransform) {
      System.err.println("Error: cannot specify " + doTransformString + " without " + OUT);
      return false;
    }
    if (argOut && !doTransform) {
      System.err.println("Error: cannot specify " + OUT + " without " + APPLY + " or " + REWRITE);
      return false;
    }

    if (doTransform) {
      if (argApply) {
        try {
          Class transformClass = Class.forName(argApplyValue);
          Object transform = transformClass.newInstance();
          if (transform instanceof OutputDocumentTransform) {
            applyTransform = (OutputDocumentTransform)transform;
          }
          else if (transform instanceof DocumentTransform) {
            applyTransform = new SimpleOutputDocumentTransform((DocumentTransform)transform);
          }
          else if (transform instanceof PageTransform) {
            applyTransform = new SimpleOutputDocumentTransform(new TransformEachPage((PageTransform)transform));
          }
          else {
            throw new ClassCastException(transform.getClass().getName());
          }
        }
        catch (ClassNotFoundException cnfe) {
          System.err.println("Error: could not load " + argApplyValue);
          cnfe.printStackTrace(System.err);
          return false;
        }
        catch (InstantiationException ie) {
          System.err.println("Error: could not instantiate " + argApplyValue);
          ie.printStackTrace(System.err);
          return false;
        }
        catch (IllegalAccessException iae) {
          System.err.println("Error: could not access " + argApplyValue);
          iae.printStackTrace(System.err);
          return false;
        }
        catch (ClassCastException cce) {
          System.err.println("Error: " + argApplyValue + " is not a transform type");
          cce.printStackTrace(System.err);
          return false;
        }
      }
      else /* argRewrite */ {
        applyTransform = new SimpleOutputDocumentTransform(new TransformEachPage(new ReiteratePageStream()));
      }
    }

    // Done
    return true;
  }

  protected static Options prepareOptions() {
    Options options = new Options();
    
    OptionGroup leadingGroup = new OptionGroup();
    
    OptionBuilder.withDescription(HELP_DESCR);
    OptionBuilder.withLongOpt(HELP_STR);
    leadingGroup.addOption(OptionBuilder.create(HELP_CHAR));
    
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("inputfile");
    OptionBuilder.withDescription("specifies the input file");
    OptionBuilder.withLongOpt("input");
    leadingGroup.addOption(OptionBuilder.create('i'));
    
    leadingGroup.setRequired(true);
    options.addOptionGroup(leadingGroup);
    
    OptionGroup transformGroup = new OptionGroup();
    
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("transformclass");
    OptionBuilder.withDescription("apply a transform");
    OptionBuilder.withLongOpt("apply");
    transformGroup.addOption(OptionBuilder.create('a'));
    
    OptionBuilder.withDescription("rewrite the input file");
    OptionBuilder.withLongOpt("rewrite");
    transformGroup.addOption(OptionBuilder.create('r'));
    
    options.addOptionGroup(transformGroup);
    
    OptionBuilder.withDescription("dump the annotations of each page");
    OptionBuilder.withLongOpt("page-annotations");
    options.addOption(OptionBuilder.create('A'));
    
    OptionBuilder.withDescription("dump the boxes of each page");
    OptionBuilder.withLongOpt("page-boxes");
    options.addOption(OptionBuilder.create('B'));
    
    OptionBuilder.withDescription("duplicate all output to the console");
    OptionBuilder.withLongOpt("console");
    options.addOption(OptionBuilder.create('c'));
    
    OptionBuilder.withDescription("dump the dictionary of each page");
    OptionBuilder.withLongOpt("page-dictionaries");
    options.addOption(OptionBuilder.create('D'));
    
    OptionBuilder.withDescription("dump the document metadata");
    OptionBuilder.withLongOpt("metadata");
    options.addOption(OptionBuilder.create('m'));
    
    OptionBuilder.withDescription("dump the token stream of each page with numbers");
    OptionBuilder.withLongOpt("numbered-page-streams");
    options.addOption(OptionBuilder.create('N'));
    
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("outputfile");
    OptionBuilder.withDescription("specifies the output file");
    OptionBuilder.withLongOpt("output");
    options.addOption(OptionBuilder.create('o'));
    
    OptionBuilder.withDescription("dump the token stream of each page");
    OptionBuilder.withLongOpt("page-streams");
    options.addOption(OptionBuilder.create('S'));
    
    OptionBuilder.withDescription("dump the document trailer");
    OptionBuilder.withLongOpt("trailer");
    options.addOption(OptionBuilder.create('t'));
    
    OptionBuilder.withDescription("produce verbose output");
    OptionBuilder.withLongOpt("verbose");
    options.addOption(OptionBuilder.create('v'));
    
    return options;
  }
  
  public static void main(String[] args) {
    try {
      // Parse the command line
      Options options = prepareOptions();
      CommandLineParser parser = new GnuParser();
      CommandLine commandLine = parser.parse(options, args);

      // Display the usage message
      if (commandLine.hasOption(HELP_CHAR)) {
        new HelpFormatter().printHelp("java " + PdfTools.class.getName(), options, true);
        return;
      }
      
      PdfTools pdfTools = new PdfTools();
      pdfTools.processCommandLine(commandLine);
    }
    catch (Exception exc) {
      exc.printStackTrace();
      System.exit(1);
    }
    
  }
  
  protected CommandLine commandLine;
  
  protected synchronized void processCommandLine(CommandLine commandLine) {
    this.commandLine = commandLine;
    validateCommandLine();
  }
  
  protected void validateCommandLine() {
    
  }
  
}
