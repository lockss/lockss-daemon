/*
 * $Id: PdfTools.java,v 1.19 2007-08-14 09:19:26 thib_gc Exp $
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
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.output.*;
import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.DocumentTransformUtil.*;
import org.lockss.filter.pdf.PageTransformUtil.IdentityPageTransform;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.pdfbox.util.PDFOperator;

/**
 * <p>Tools to inspect the contents of PDF documents.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfTools {

  protected static final String SUFFIX_SEPARATOR = "-";

  public static class RewriteStream extends PageStreamTransform {

    public static class AlwaysChanged extends SimpleOperatorProcessor {
      @Override
      public void process(PageStreamTransform pageStreamTransform, PDFOperator operator, List operands) throws IOException {
        pageStreamTransform.signalChange();
        super.process(pageStreamTransform, operator, operands);
      }

    }

    public RewriteStream() throws IOException {
      super(PageStreamTransform.rewriteProperties(PropUtil.fromArgs(PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class.getName()),
                                                  AlwaysChanged.class.getName()));
    }

  }

  public static class DisplayReplaceString extends ReplaceString {
    @Override
    public void process(PageStreamTransform pageStreamTransform, PDFOperator operator, List operands) throws IOException {
      for (Object obj : operands) {
        System.out.println(obj.toString());
      }
      System.out.println(operator.toString());
      super.process(pageStreamTransform, operator, operands);
    }
    @Override
    public boolean identify(String candidate) {
      return true;
    }
    @Override
    public String getReplacement(String match) {
      return "qux";
    }
  }

  public static class Display extends PdfOperatorProcessor {
    @Override
    public void process(PageStreamTransform pageStreamTransform,
                        PDFOperator operator,
                        List operands)
        throws IOException {
      for (Object obj : operands) {
        System.out.println(obj.toString());
      }
      System.out.println(operator.toString());
    }
  }

  public static class DisplayFormXObjectOperatorProcessor extends FormXObjectOperatorProcessor {
    @Override
    public void process(PageStreamTransform pageStreamTransform, PDFOperator operator, List operands) throws IOException {
      for (Object obj : operands) {
        System.out.println(obj.toString());
      }
      System.out.println(operator.toString());
      super.process(pageStreamTransform, operator, operands);
    }
  }

  public class DebugTransform extends IdentityPageTransform {

    @Override
    public boolean transform(PdfPage pdfPage) throws IOException {
      PageStreamTransform one = new PageStreamTransform(PdfUtil.INVOKE_NAMED_XOBJECT, DisplayFormXObjectOperatorProcessor.class,
                                                        PdfUtil.SHOW_TEXT, DisplayReplaceString.class);
      one.transform(pdfPage);
      output.println("###############################");
      PageStreamTransform two = new PageStreamTransform(PageStreamTransform.rewriteProperties(PropUtil.fromArgs(PdfUtil.INVOKE_NAMED_XOBJECT, DisplayFormXObjectOperatorProcessor.class.getName()),
                                                                                              Display.class.getName()));
      two.transform(pdfPage);
      return true;
    }

  }

  protected class DumpAnnotations extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      Iterator iter = pdfPage.getAnnotationIterator();
      for (int ann = 0 ; iter.hasNext() ; ++ann) {
        PDAnnotation annotation = (PDAnnotation)iter.next();
        dump("Contents", annotation.getContents());
        dump("Rectangle", annotation.getRectangle());
      }
      return true;
    }
  }

  protected class DumpBoxes extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      dump("Art box", pdfPage.getArtBox());
      dump("Bleed box", pdfPage.getBleedBox());
      dump("Crop box", pdfPage.getCropBox());
      dump("Media box", pdfPage.getMediaBox());
      dump("Trim box", pdfPage.getTrimBox());
      return super.transform(pdfPage);
    }
  }

  protected class DumpDictionary extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      dump(pdfPage.getDictionary());
      return super.transform(pdfPage);
    }
  }

  protected class DumpNumberedStream extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      Iterator tokens = pdfPage.getStreamTokenIterator();
      for (int tok = 0 ; tokens.hasNext() ; ++tok) {
        output.println(Integer.toString(tok) + "\t" + tokens.next().toString());
      }
      return super.transform(pdfPage);
    }
  }

  protected class DumpStream extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      for (Iterator tokens = pdfPage.getStreamTokenIterator() ; tokens.hasNext() ; ) {
        output.println("\t" + tokens.next().toString());
      }
      return super.transform(pdfPage);
    }
  }

  protected class DumpTrailer extends IdentityDocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      dump(pdfDocument.getTrailer());
      return super.transform(pdfDocument);
    }
  }

  protected class SplitPageTransform extends PageTransformWrapper {

    protected String suffix;

    public SplitPageTransform(PageTransform pageTransform,
                              String suffix) {
      super(pageTransform);
      this.suffix = suffix;
    }

    public boolean transform(PdfDocument pdfDocument) throws IOException {
      if (getVerbose()) {
        System.out.println(suffix);
      }
      beginOutput(suffix);
      boolean ret = true;
      for (Iterator iter = pdfDocument.getPageIterator() ; iter.hasNext() ; ) {
        ret = pageTransform.transform((PdfPage)iter.next());
        output.println();
        if (!ret) {
          break;
        }
      }
      if (getConsole()) {
        output.println();
      }
      endOutput();
      return ret;
    }

  }

  protected class DumpMetadata extends IdentityDocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      output.println("Number of pages: " + pdfDocument.getNumberOfPages());
      dump("Creation date", pdfDocument.getCreationDate().getTime());
      dump("Modification date", pdfDocument.getModificationDate().getTime());
      dump("Author", pdfDocument.getAuthor());
      dump("Creator", pdfDocument.getCreator());
      dump("Keywords", pdfDocument.getKeywords());
      dump("Producer", pdfDocument.getProducer());
      dump("Subject", pdfDocument.getSubject());
      dump("Title", pdfDocument.getTitle());
      dump("Metadata (as string):\n", pdfDocument.getMetadataAsString()); // newline; typically large
      return super.transform(pdfDocument);
    }
  }

  protected class SplitDocumentTransform extends DocumentTransformDecorator {

    protected String suffix;

    public SplitDocumentTransform(DocumentTransform documentTransform,
                                  String suffix) {
      super(documentTransform);
      this.suffix = suffix;
    }

    public boolean transform(PdfDocument pdfDocument) throws IOException {
      if (getVerbose()) {
        System.out.println(suffix);
      }
      beginOutput(suffix);
      boolean ret = documentTransform.transform(pdfDocument);
      if (getConsole()) {
        output.println();
      }
      endOutput();
      return ret;
    }

  }

  protected String baseName;

  protected CommandLine commandLine;

  protected PdfDocument document;

  protected PrintStream output;

  protected AggregateDocumentTransform transform;

  protected void beginOutput(String suffix) throws IOException {
    String fileName = getBaseName() + SUFFIX_SEPARATOR + suffix + ".txt";
    output = new PrintStream(fileName);
    if (getConsole()) {
      output = new PrintStream(new TeeOutputStream(output,
                                                   new FilterOutputStream(System.out) {
                                                       @Override public void close() { /* nothing */ }
                                                   }));
    }
  }

  protected void dump(COSDictionary dictionary) {
    for (Iterator keys = dictionary.keyList().iterator() ; keys.hasNext() ; ) {
      COSName key = (COSName)keys.next();
      dump(key.getName(), dictionary.getItem(key).toString());
    }
  }

  protected void dump(String name, Object obj) {
    if (obj != null) {
      dump(name, obj.toString());
    }
  }

  protected void dump(String name, String str) {
    if (str != null && !str.equals("")) {
      output.println(name + ": " + str);
    }
  }

  protected void endOutput() {
    output.close();
    output = null;
  }

  protected String getBaseName() {
    return baseName;
  }

  protected boolean getConsole() {
    return hasOption(CONSOLE_CHAR);
  }

  protected String getInputArgument() {
    return commandLine.getOptionValue(INPUT_CHAR);
  }

  protected boolean getMetadata() {
    return hasOption(METADATA_CHAR);
  }

  protected boolean getNumberedPageStreams() {
    return hasOption(NUMBERED_PAGE_STREAMS_CHAR);
  }

  protected boolean getOutput() {
    return commandLine.hasOption(OUTPUT_CHAR);
  }

  protected boolean getPageAnnotations() {
    return hasOption(PAGE_ANNOTATIONS_CHAR);
  }

  protected boolean getPageBoxes() {
    return hasOption(PAGE_BOXES_CHAR);
  }

  protected boolean getPageDictionaries() {
    return hasOption(PAGE_DICTIONARIES_CHAR);
  }

  protected boolean getPageStreams() {
    return hasOption(PAGE_STREAMS_CHAR);
  }

  protected boolean getRewrite() {
    return hasOption(REWRITE_CHAR);
  }

  protected boolean getTrailer() {
    return hasOption(TRAILER_CHAR);
  }

  protected boolean getTransform() {
    return hasOption(REWRITE_CHAR) || hasOption(APPLY_CHAR);
  }

  protected boolean getVerbose() {
    return hasOption(VERBOSE_CHAR);
  }

  protected boolean hasOption(char opt) {
    return commandLine.hasOption(opt);
  }

  protected void parseInput() throws IOException {
    InputStream inputStream = null;
    try {
      baseName = getInputArgument();
      inputStream = new FileInputStream(baseName);
      document = new PdfDocument(inputStream);
      if (baseName.endsWith(".pdf")) {
        baseName = baseName.substring(0, baseName.length() - 4);
      }
    }
    finally {
      IOUtil.safeClose(inputStream);
    }
  }

  protected void prepareTransform()
      throws IOException,
             ClassNotFoundException,
             IllegalAccessException,
             InstantiationException {
    transform = new AggregateDocumentTransform();

    // METADATA
    if (getMetadata()) {
      transform.add(new SplitDocumentTransform(new DumpMetadata(), METADATA_STR));
    }

    // TRAILER
    if (getTrailer()) {
      transform.add(new SplitDocumentTransform(new DumpTrailer(), TRAILER_STR));
    }

    // PAGE ANNOTATIONS
    if (getPageAnnotations()) {
      transform.add(new SplitPageTransform(new DumpAnnotations(), PAGE_ANNOTATIONS_STR));
    }

    // PAGE BOXES
    if (getPageBoxes()) {
      transform.add(new SplitPageTransform(new DumpBoxes(), PAGE_BOXES_STR));
    }

    // PAGE DICTIONARIES
    if (getPageDictionaries()) {
      transform.add(new SplitPageTransform(new DumpDictionary(), PAGE_DICTIONARIES_STR));
    }

    // PAGE STREAMS
    if (getPageStreams()) {
      transform.add(new SplitPageTransform(new DumpStream(), PAGE_STREAMS_STR));
    }

    // NUMBERED PAGE STREAMS
    if (getNumberedPageStreams()) {
      transform.add(new SplitPageTransform(new DumpNumberedStream(), NUMBERED_PAGE_STREAMS_STR));
    }

    if (getTransform()) {
      // REWRITE
      if (getRewrite()) {
        transform.add(new TransformEachPage(new RewriteStream()));
      }
      else if (getApply()) {
        Object tra = Class.forName(getApplyArg()).newInstance();
        if (tra instanceof DocumentTransform) {
          transform.add((DocumentTransform)tra);
        }
        else if (tra instanceof PageTransform) {
          transform.add(new TransformEachPage((PageTransform)tra));
        }
        else {
          throw new ClassCastException(getApplyArg()
                                       + " is not of type "
                                       + DocumentTransform.class.getName()
                                       + " or "
                                       + PageTransform.class.getName());
        }
      }
    }

  }

  protected String getApplyArg() {
    return commandLine.getOptionValue(APPLY_CHAR);
  }

  protected boolean getApply() {
    return hasOption(APPLY_CHAR);
  }

  protected synchronized void processCommandLine(CommandLine commandLine)
      throws IOException,
             MissingOptionException,
             ClassNotFoundException,
             IllegalAccessException,
             InstantiationException {
    this.commandLine = commandLine;
    validateCommandLine();
    parseInput();
    prepareTransform();
    OutputStream out = prepareOutput();
    PdfUtil.applyAndSave(new StrictDocumentTransform(transform), document, out);
    //new DocumentTransformUtil.StrictDocumentTransform(transform).transform(document); // FIXME
  }

  protected OutputStream prepareOutput() throws IOException {
    if (getTransform()) {
      if (getRewrite()) {
        return new FileOutputStream(getBaseName() + SUFFIX_SEPARATOR + REWRITE_STR + ".pdf");
      }
      else if (getApply()) {
        return new FileOutputStream(getBaseName() + SUFFIX_SEPARATOR + APPLY_STR + ".pdf");
      }
    }
    return new NullOutputStream();
  }

  protected void validateCommandLine() throws MissingOptionException {
    // OUT is required if and only if either APPLY or REWRITE are specified
    if (getTransform() && !getOutput()) {
      throw new MissingOptionException(  "Cannot specify "
                                       + APPLY_STR
                                       + " or "
                                       + REWRITE_STR
                                       + " without "
                                       + OUTPUT_STR);
    }
    if (!getTransform() && getOutput()) {
      throw new MissingOptionException(  "Cannot specify "
                                         + OUTPUT_STR
                                         + " without "
                                         + APPLY_STR
                                         + " or "
                                         + REWRITE_STR);
    }

  }

  protected static final String APPLY_ARG = "transformclass";

  protected static final char APPLY_CHAR = 'a';

  protected static final String APPLY_DESCR = "apply a transform";

  protected static final String APPLY_STR = "apply";

  protected static final char CONSOLE_CHAR = 'c';

  protected static final String CONSOLE_DESCR = "duplicate all output to the console";

  protected static final String CONSOLE_STR = "console";

  protected static final char HELP_CHAR = 'h';

  protected static final String HELP_DESCR = "display this usage message";

  protected static final String HELP_STR = "help";

  protected static final String INPUT_ARG = "inputfile";

  protected static final char INPUT_CHAR = 'i';

  protected static final String INPUT_DESCR = "specifies the input file";

  protected static final String INPUT_STR = "input";

  protected static final char METADATA_CHAR = 'm';

  protected static final String METADATA_DESCR = "dump the document metadata";

  protected static final String METADATA_STR = "metadata";

  protected static final char NUMBERED_PAGE_STREAMS_CHAR = 'N';

  protected static final String NUMBERED_PAGE_STREAMS_DESCR = "dump the token stream of each page with numbers";

  protected static final String NUMBERED_PAGE_STREAMS_STR = "numbered-page-streams";

  protected static final String OUTPUT_ARG = "outputfile";

  protected static final char OUTPUT_CHAR = 'o';

  protected static final String OUTPUT_DESCR = "specifies the output file";

  protected static final String OUTPUT_STR = "output";

  protected static final char PAGE_ANNOTATIONS_CHAR = 'A';

  protected static final String PAGE_ANNOTATIONS_DESCR = "dump the annotations of each page";

  protected static final String PAGE_ANNOTATIONS_STR = "page-annotations";

  protected static final char PAGE_BOXES_CHAR = 'B';

  protected static final String PAGE_BOXES_DESCR = "dump the boxes of each page";

  protected static final String PAGE_BOXES_STR = "page-boxes";

  protected static final char PAGE_DICTIONARIES_CHAR = 'D';

  protected static final String PAGE_DICTIONARIES_DESCR = "dump the dictionary of each page";

  protected static final String PAGE_DICTIONARIES_STR = "page-dictionaries";

  protected static final char PAGE_STREAMS_CHAR = 'S';

  protected static final String PAGE_STREAMS_DESCR = "dump the token stream of each page";

  protected static final String PAGE_STREAMS_STR = "page-streams";

  protected static final char REWRITE_CHAR = 'r';

  protected static final String REWRITE_DESCR = "rewrite the input file";

  protected static final String REWRITE_STR = "rewrite";

  protected static final char TRAILER_CHAR = 't';

  protected static final String TRAILER_DESCR = "dump the document trailer";

  protected static final String TRAILER_STR = "trailer";

  protected static final char VERBOSE_CHAR = 'v';

  protected static final String VERBOSE_DESCR = "produce verbose output";

  protected static final String VERBOSE_STR = "verbose";

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

  protected static Options prepareOptions() {
    Options options = new Options();

    OptionGroup leadingGroup = new OptionGroup();

    OptionBuilder.withDescription(HELP_DESCR);
    OptionBuilder.withLongOpt(HELP_STR);
    leadingGroup.addOption(OptionBuilder.create(HELP_CHAR));

    OptionBuilder.hasArg();
    OptionBuilder.withArgName(INPUT_ARG);
    OptionBuilder.withDescription(INPUT_DESCR);
    OptionBuilder.withLongOpt(INPUT_STR);
    leadingGroup.addOption(OptionBuilder.create(INPUT_CHAR));

    leadingGroup.setRequired(true);
    options.addOptionGroup(leadingGroup);

    OptionGroup transformGroup = new OptionGroup();

    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(APPLY_ARG);
    OptionBuilder.withDescription(APPLY_DESCR);
    OptionBuilder.withLongOpt(APPLY_STR);
    transformGroup.addOption(OptionBuilder.create(APPLY_CHAR));

    OptionBuilder.withDescription(REWRITE_DESCR);
    OptionBuilder.withLongOpt(REWRITE_STR);
    transformGroup.addOption(OptionBuilder.create(REWRITE_CHAR));

    options.addOptionGroup(transformGroup);

    OptionBuilder.withDescription(PAGE_ANNOTATIONS_DESCR);
    OptionBuilder.withLongOpt(PAGE_ANNOTATIONS_STR);
    options.addOption(OptionBuilder.create(PAGE_ANNOTATIONS_CHAR));

    OptionBuilder.withDescription(PAGE_BOXES_DESCR);
    OptionBuilder.withLongOpt(PAGE_BOXES_STR);
    options.addOption(OptionBuilder.create(PAGE_BOXES_CHAR));

    OptionBuilder.withDescription(CONSOLE_DESCR);
    OptionBuilder.withLongOpt(CONSOLE_STR);
    options.addOption(OptionBuilder.create(CONSOLE_CHAR));

    OptionBuilder.withDescription(PAGE_DICTIONARIES_DESCR);
    OptionBuilder.withLongOpt(PAGE_DICTIONARIES_STR);
    options.addOption(OptionBuilder.create(PAGE_DICTIONARIES_CHAR));

    OptionBuilder.withDescription(METADATA_DESCR);
    OptionBuilder.withLongOpt(METADATA_STR);
    options.addOption(OptionBuilder.create(METADATA_CHAR));

    OptionBuilder.withDescription(NUMBERED_PAGE_STREAMS_DESCR);
    OptionBuilder.withLongOpt(NUMBERED_PAGE_STREAMS_STR);
    options.addOption(OptionBuilder.create(NUMBERED_PAGE_STREAMS_CHAR));

    OptionBuilder.hasArg();
    OptionBuilder.withArgName(OUTPUT_ARG);
    OptionBuilder.withDescription(OUTPUT_DESCR);
    OptionBuilder.withLongOpt(OUTPUT_STR);
    options.addOption(OptionBuilder.create(OUTPUT_CHAR));

    OptionBuilder.withDescription(PAGE_STREAMS_DESCR);
    OptionBuilder.withLongOpt(PAGE_STREAMS_STR);
    options.addOption(OptionBuilder.create(PAGE_STREAMS_CHAR));

    OptionBuilder.withDescription(TRAILER_DESCR);
    OptionBuilder.withLongOpt(TRAILER_STR);
    options.addOption(OptionBuilder.create(TRAILER_CHAR));

    OptionBuilder.withDescription(VERBOSE_DESCR);
    OptionBuilder.withLongOpt(VERBOSE_STR);
    options.addOption(OptionBuilder.create(VERBOSE_CHAR));

    return options;
  }

}
