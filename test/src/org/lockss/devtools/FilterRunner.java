/*
 * $Id: FilterRunner.java,v 1.3 2003-09-09 20:23:07 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.crawler.*;
import org.lockss.filter.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 */
public class FilterRunner {

  private static boolean useNullStream = false;

  public static Reader getFilteredReader(String fileName) throws IOException {
    List tagList =
      ListUtil.list(
		    new HtmlTagFilter.TagPair("<script", "</script>", true),
		    new HtmlTagFilter.TagPair("<table", "</table>", true),
		    new HtmlTagFilter.TagPair("This article has been cited by",
					      " other articles:", true),
		    new HtmlTagFilter.TagPair("[Medline", "]", true),
 		    new HtmlTagFilter.TagPair("<", ">")
 		    );
    
    return HtmlTagFilter.makeNestedFilter(new FileReader(fileName), tagList);
  }

  public static void printUsageAndExit() {
    System.out.println("usage");
    System.exit(0);
  }

  private static OutputStream getOutputStream(File dest)
      throws FileNotFoundException {
    if (useNullStream) {
      return new NullOutputStream();
    }
    return new FileOutputStream(dest);
  }

  /**
   * Just copy the file over
   */
  private static long filterFile0(File source, File dest) throws IOException {
    InputStream is = new FileInputStream(source.getAbsolutePath());
//     OutputStream os = new FileOutputStream(dest);
//     OutputStream os = new NullOutputStream();
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  /**
   * Use HtmlTagFilter (wrapped in a ReaderInputStream)
   */
  private static long filterFile1(File source, File dest) throws IOException {
    InputStream is =
      new ReaderInputStream(getFilteredReader(source.getAbsolutePath()));
//     OutputStream os = new FileOutputStream(dest);
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  /**
   * Use FileInputStream (wrapped ina InputStreamReader and ReaderInputStream)
   * To try to vaguely match what the HtmlTagFilter has to do (convert to a 
   * reader and back)
   */
  private static long filterFile2(File source, File dest) throws IOException {
    InputStream filteredInputStream =
      new LcapFilteredFileInputStream(new FileInputStream(source.getAbsolutePath()));
    InputStream is =
      new ReaderInputStream(new InputStreamReader(filteredInputStream));
//     OutputStream os = new FileOutputStream(dest);
//     OutputStream os = new NullOutputStream();
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  /**
   * Uses an HtmlTagFilter, but doesn't convert it to an InputStream
   */
  private static long filterFile3(File source, File dest) throws IOException {
    Reader reader = getFilteredReader(source.getAbsolutePath());
//     Writer writer = new FileWriter(dest);
    Writer writer = new NullWriter();
    long bytes = StreamUtil.copy(reader, writer);
    reader.close();
    writer.close();
    return bytes;
  }

  /**
   * Use an LcapFilteredFileInputStream
   */
  private static long filterFile4(File source, File dest) throws IOException {
    InputStream is =
      new LcapFilteredFileInputStream(new FileInputStream(source.getAbsolutePath()));
//     OutputStream os = new FileOutputStream(dest);
//     OutputStream os = new NullOutputStream();
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  /**
   * Use a nested LcapFilteredFileInputStreams
   */
  private static long filterFile5(File source, File dest) throws IOException {
    InputStream is =
      new LcapFilteredFileInputStream(new LcapFilteredFileInputStream(new FileInputStream(source.getAbsolutePath())));
//     OutputStream os = new FileOutputStream(dest);
//     OutputStream os = new NullOutputStream();
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  /**
   * Use a nested LcapFilteredFileInputStreams
   */
  private static long filterFile6(File source, File dest) throws IOException {
    InputStream is1 =
      new ReaderInputStream(getFilteredReader(source.getAbsolutePath()));
    InputStream is = new WhiteSpaceFilter(is1);
//     OutputStream os = new FileOutputStream(dest);
//     OutputStream os = new NullOutputStream();
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  /**
   * Use a nested LcapFilteredFileInputStreams
   */
  private static long filterFile7(File source, File dest) throws IOException {
    InputStream is = new WhiteSpaceFilter(new FileInputStream(source.getAbsolutePath()));
    OutputStream os = getOutputStream(dest);
    long bytes = StreamUtil.copy(is, os);
    is.close();
    os.close();
    return bytes;
  }

  private static long filterFile(File source, File dest, int method)
      throws IOException{
    switch (method) {
    case 0:
      return filterFile0(source, dest);
    case 1:
      return filterFile1(source, dest);
    case 2:
      return filterFile2(source, dest);
    case 3:
      return filterFile3(source, dest);
    case 4:
      return filterFile4(source, dest);
    case 5:
      return filterFile5(source, dest);
    case 6:
      return filterFile6(source, dest);
    case 7:
      return filterFile7(source, dest);
    }
    return -1;
  }

  private static File makeDestFile(File file, File sourceDir, File destDir)
      throws IOException {
    String relativePath = FileUtil.getPathUnderRoot(file, sourceDir);
    File returnFile = new File(destDir, relativePath);
    returnFile.getParentFile().mkdirs();
    returnFile.createNewFile();
    return returnFile;
  }

  private static void filterFiles(File sourceDir, File destDir, int method)
      throws IOException {
    long startTime = TimeBase.nowMs();
    List fileList = FileUtil.enumerateFiles(sourceDir);
    int totalBytes = 0;
    for (Iterator it = fileList.iterator(); it.hasNext();) {
      File curFile = (File) it.next();
      totalBytes +=
	filterFile(curFile, makeDestFile(curFile, sourceDir, destDir), method);
    }
    long endTime = TimeBase.nowMs();
    long totalMS = endTime - startTime;
    System.out.println("Method: "+method);
    System.out.println(totalBytes +" processed in "+totalMS+" milliseconds");
    System.out.println("Rate: "+(totalBytes/totalMS)+ " bytes/ms");
  }

  public static void main(String args[]) {
    if (args.length == 3) {
      try{
	File source = new File(args[0]);
	File dest = new File(args[1]);
	int method = Integer.parseInt(args[2]);
	filterFiles(source, dest, method);
      } catch (IOException e) {
	e.printStackTrace();
      }
    } else {
      printUsageAndExit();
    }
  }
}
