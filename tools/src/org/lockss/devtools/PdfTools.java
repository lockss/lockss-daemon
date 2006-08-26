/*
 * $Id: PdfTools.java,v 1.4 2006-08-26 19:18:38 thib_gc Exp $
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

import org.lockss.filter.pdf.*;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.pdmodel.common.PDRectangle;

/**
 * <p>Tools to inspect the contents of PDF documents.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfTools {

  public static class DumpBoxes implements PdfPageTransform {
    public void transform(PdfDocument pdfDocument, PDPage pdfPage) throws IOException {
      System.out.println("[begin boxes]");
      display(pdfPage.getArtBox(), "Art box");
      display(pdfPage.getBleedBox(), "Bleed box");
      display(pdfPage.getCropBox(), "Crop box");
      display(pdfPage.getMediaBox(), "Media box");
      display(pdfPage.getTrimBox(), "Trim box");
      System.out.println("[end boxes]");
    }
    protected static void display(PDRectangle rectangle, String name) {
      if (rectangle != null) {
        System.out.println(name + ": " + rectangle.toString());
      }
    }
  }

  public static class DumpPageDictionary implements PdfPageTransform {
    public void transform(PdfDocument pdfDocument, PDPage pdfPage) throws IOException {
      System.out.println("[begin page dictionary]");
      dump(pdfPage.getCOSDictionary());
      System.out.println("[end page dictionary]");
    }
  }

//  public static class DumpAnnotations implements PdfPageTransform {
//
//    public void transform(PdfDocument pdfDocument,
//                          PDPage pdfPage)
//        throws IOException {
//      System.out.println("[begin annotations]");
//      Iterator iter = pdfPage.getAnnotations().iterator();
//      for (int ann = 0 ; iter.hasNext() ; ++ann) {
//        PDAnnotation annotation = (PDAnnotation)iter.next();
//        System.out.println("[begin annotation [" + ann + "]]");
//        System.out.println("Contents: " + annotation.getContents());
//        System.out.println("Rectangle: " + annotation.getRectangle().toString());
//        System.out.println("[end annotation]");
//      }
//      System.out.println("[end annotations]");
//    }
//
//  }

  public static class DumpPageStream implements PdfPageTransform {
    public void transform(PdfDocument pdfDocument, PDPage pdfPage) throws IOException {
      System.out.println("[begin page stream]");
      Iterator tokens = pdfPage.getContents().getStream().getStreamTokens().iterator();
      for (int tok = 0 ; tokens.hasNext() ; ++tok) {
        System.out.println(Integer.toString(tok) + "\t" + tokens.next().toString());
      }
      System.out.println("[end page stream]");
    }
  }

  public static class DumpTrailer implements PdfTransform {
    public void transform(PdfDocument pdfDocument) throws IOException {
      System.out.println("[begin trailer]");
      dump(pdfDocument.getTrailer());
      System.out.println("[end trailer]");
    }
  }

  protected static final String USAGE =
    "Usage:\n" +
    "\n" +
    "-h -help --help     Displays this message\n" +
    "-usage --usage      Displays this message\n" +
    "\n" +
//    "-dumpannotations Dumps the annotations on each page\n" +
    "-dumppagedictionary Dumps the dictionary of each page\n" +
    "-dumppageboxes      Dumps the bounding boxes of each page\n" +
    "-dumppagestream     Dumps a numbered list of tokens of each page\n" +
    "-dumptrailer        Dumps the trailer dictionary\n" +
    "-null               Also write PDFBox-saved version in file-out.pdf\n";

  public static void main(String[] args) {
    if (   args.length == 0
        || (  args.length == 1
            && (   args[0].equals("-h")
                || args[0].equals("-help")
                || args[0].equals("--help")
                || args[0].equals("-usage")
                || args[0].equals("--usage")))) {
      System.out.println(USAGE);
      return;
    }

    CompoundPdfTransform pdfTransform = new CompoundPdfTransform();
    CompoundPdfPageTransform pdfPageTransform = new CompoundPdfPageTransform();
    boolean ignoreResult = true;

    for (int arg = 0 ; arg < args.length ; ++arg) {
      if (false) {
        // Copy/paste hack
      }
//      else if (args[arg].equals("-dumpannotations")) {
//        pdfPageTransform.add(new DumpAnnotations());
//      }
      else if (args[arg].equals("-dumppageboxes")) {
        pdfPageTransform.add(new DumpBoxes());
      }
      else if (args[arg].equals("-dumppagedictionary")) {
        pdfPageTransform.add(new DumpPageDictionary());
      }
      else if (args[arg].equals("-dumppagestream")) {
        pdfPageTransform.add(new DumpPageStream());
      }
      else if (args[arg].equals("-dumptrailer")) {
        pdfTransform.add(new DumpTrailer());
      }
      else if (args[arg].equals("-null")) {
        ignoreResult = false;
      }
    }

    pdfTransform.add(new TransformEachPage(pdfPageTransform));

    try {
      JFileChooser chooser = new JFileChooser();
      if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
        return;
      }
      InputStream inputStream = new FileInputStream(chooser.getSelectedFile());
      OutputStream outputStream = null;
      if (ignoreResult) {
        outputStream = new OutputStream() {
          public void write(int b) { }
        };
      }
      else {
        outputStream = new FileOutputStream(chooser.getSelectedFile().toString().replaceAll(".pdf", "-out.pdf"));
      }
      PdfUtil.applyPdfTransform(pdfTransform, inputStream, outputStream);
      inputStream.close();
      outputStream.close();
    }
    catch (Exception exc) {
      exc.printStackTrace();
    }
  }

  protected static void dump(COSDictionary dictionary) {
    for (Iterator keys = dictionary.keyList().iterator() ; keys.hasNext() ; ) {
      COSName key = (COSName)keys.next();
      System.out.println(key.getName() + "\t" + dictionary.getItem(key).toString());
    }
  }

}
