/*
 * $Id: PdfTools.java,v 1.12 2006-09-13 22:04:09 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import javax.swing.JFileChooser;

import org.apache.commons.io.output.NullOutputStream;
import org.lockss.filter.pdf.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.common.*;
import org.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

/**
 * <p>Tools to inspect the contents of PDF documents.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfTools {

  public static class DumpAnnotations extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      System.out.println("[begin annotations]");
      Iterator iter = pdfPage.getAnnotationIterator();
      for (int ann = 0 ; iter.hasNext() ; ++ann) {
        PDAnnotation annotation = (PDAnnotation)iter.next();
        System.out.println("[begin annotation [" + ann + "]]");
        dump("Contents", annotation.getContents());
        dump("Rectangle", annotation.getRectangle());
        System.out.println("[end annotation [" + ann + "]]");
      }
      System.out.println("[end annotations]");
      return true;
    }
  }

  public static class DumpBoxes extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      System.out.println("[begin boxes]");
      PdfTools.dump("Art box", pdfPage.getArtBox());
      PdfTools.dump("Bleed box", pdfPage.getBleedBox());
      PdfTools.dump("Crop box", pdfPage.getCropBox());
      PdfTools.dump("Media box", pdfPage.getMediaBox());
      PdfTools.dump("Trim box", pdfPage.getTrimBox());
      System.out.println("[end boxes]");
      return super.transform(pdfPage);
    }
  }

  public static class DumpMetadata extends IdentityDocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      System.out.println("[begin metadata]");
      System.out.println("Number of pages: " + pdfDocument.getNumberOfPages());
      dump("Creation date", pdfDocument.getCreationDate().getTime());
      dump("Modification date", pdfDocument.getModificationDate().getTime());
      dump("Author", pdfDocument.getAuthor());
      dump("Creator", pdfDocument.getCreator());
      dump("Keywords", pdfDocument.getKeywords());
      dump("Producer", pdfDocument.getProducer());
      dump("Subject", pdfDocument.getSubject());
      dump("Title", pdfDocument.getTitle());
      System.out.println("Metadata (as string):"); // newline; typically large
      System.out.println(pdfDocument.getMetadataAsString());
      System.out.println("[end metadata]");
      return super.transform(pdfDocument);
    }
  }

  public static class DumpNumberedPageStream extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      System.out.println("[begin numbered page stream]");
      Iterator tokens = pdfPage.getStreamTokenIterator();
      for (int tok = 0 ; tokens.hasNext() ; ++tok) {
        System.out.println(Integer.toString(tok) + "\t" + tokens.next().toString());
      }
      System.out.println("[end numbered page stream]");
      return super.transform(pdfPage);
    }
  }

  public static class DumpPageDictionary extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      System.out.println("[begin page dictionary]");
      dump(pdfPage.getDictionary());
      System.out.println("[end page dictionary]");
      return super.transform(pdfPage);
    }
  }

  public static class DumpPageStream extends IdentityPageTransform {
    public boolean transform(PdfPage pdfPage) throws IOException {
      System.out.println("[begin page stream]");
      for (Iterator tokens = pdfPage.getStreamTokenIterator() ; tokens.hasNext() ; ) {
        System.out.println("\t" + tokens.next().toString());
      }
      System.out.println("[end page stream]");
      return super.transform(pdfPage);
    }
  }

  public static class DumpTrailer extends IdentityDocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      System.out.println("[begin trailer]");
      dump(pdfDocument.getTrailer());
      System.out.println("[end trailer]");
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

  protected static final String USAGE =
    "\tUsage:\n" +
    "Help\n" +
    " -h -help --help     Displays this message\n" +
    " -usage --usage      Displays this message\n" +
    "Diff-friendly\n" +
    " -annotations        Dumps the annotations of each page\n" +
    " -metadata           Dumps the metadata\n" +
    " -pageboxes          Dumps the bounding boxes of each page\n" +
    " -pagestream         Dumps a list of tokens on each page\n" +
    "Not diff-friendly\n" +
    " -numberedpagestream Dumps a numbered list of tokens on each page\n" +
    " -pagedictionary     Dumps the dictionary of each page\n" +
    " -trailer            Dumps the trailer dictionary\n" +
    "Other\n" +
    " -rewrite            Also write PDFBox-saved version in file-out.pdf\n";

  public static void main(String[] args) {
    if (   args.length == 0
        || (  args.length == 1
            && (   args[0].equals("-h")
                || args[0].equals("-help")
                || args[0].equals("--help")
                || args[0].equals("-usage")
                || args[0].equals("--usage")))) {
      System.out.println(USAGE);
      return; // exit
    }

    AggregateDocumentTransform documentTransform = new AggregateDocumentTransform(PdfUtil.AND_ALL);
    AggregatePageTransform pageTransform = new AggregatePageTransform(PdfUtil.AND_ALL);
    boolean ignoreResult = true;

    for (int arg = 0 ; arg < args.length ; ++arg) {
      if (false) {
        // Copy/paste hack
      }
      else if (args[arg].equals("-annotations")) {
        pageTransform.add(new DumpAnnotations());
      }
      else if (args[arg].equals("-metadata")) {
        documentTransform.add(new DumpMetadata());
      }
      else if (args[arg].equals("-numberedpagestream")) {
        pageTransform.add(new DumpNumberedPageStream());
      }
      else if (args[arg].equals("-pageboxes")) {
        pageTransform.add(new DumpBoxes());
      }
      else if (args[arg].equals("-pagedictionary")) {
        pageTransform.add(new DumpPageDictionary());
      }
      else if (args[arg].equals("-pagestream")) {
        pageTransform.add(new DumpPageStream());
      }
      else if (args[arg].equals("-rewrite")) {
        ignoreResult = false;
      }
      else if (args[arg].equals("-trailer")) {
        documentTransform.add(new DumpTrailer());
      }
      else {
        System.err.println("Unknown option: " + args[arg]);
      }
    }

    documentTransform.add(new TransformEachPage(pageTransform));
    if (!ignoreResult) {
      documentTransform.add(new TransformEachPage(new ReiteratePageStream()));
    }

    try {
      JFileChooser chooser = new JFileChooser();
      if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
        return;
      }
      InputStream inputStream = new FileInputStream(chooser.getSelectedFile());
      OutputStream outputStream = null;
      if (ignoreResult) {
        outputStream = new NullOutputStream();
      }
      else {
        outputStream = new FileOutputStream(chooser.getSelectedFile().toString().replaceAll(".pdf", "-out.pdf"));
      }

      PdfUtil.applyPdfTransform(documentTransform, inputStream, outputStream);
      inputStream.close();
      outputStream.close();
    }
    catch (Exception exc) {
      exc.printStackTrace();
    }
    finally {
      System.exit(0); // force AWT thread to exit
    }
  }

  protected static void dump(COSDictionary dictionary) {
    for (Iterator keys = dictionary.keyList().iterator() ; keys.hasNext() ; ) {
      COSName key = (COSName)keys.next();
      System.out.println(key.getName() + "\t" + dictionary.getItem(key).toString());
    }
  }

  protected static void dump(String name, Object obj) {
    if (obj != null) {
      System.out.println(name + ": " + obj.toString());
    }
  }

  protected static void dump(String name, String str) {
    if (str != null && !str.equals("")) {
      System.out.println(name + ": " + str);
    }
  }

}
