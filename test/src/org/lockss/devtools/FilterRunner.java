/*
 * $Id: FilterRunner.java,v 1.1 2003-07-18 23:25:26 troberts Exp $
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
import org.lockss.util.*;
import org.lockss.test.*;

/**
 */
public class FilterRunner {


  public static Reader getFilteredReader(String fileName) throws IOException {
    List tagList =
      ListUtil.list(
		    //   		    new HtmlTagFilter.TagPair("<!--", "-->"),
     		    new HtmlTagFilter.TagPair("<script", "</script>", true),
		    new HtmlTagFilter.TagPair("<", ">")
 		    );
    
    return HtmlTagFilter.makeNestedFilter(new FileReader(fileName), tagList);
  }

//   public static Reader getFilteredReader(String fileName) throws IOException {
//     InputStream filteredInputStream =
//       new LcapFilteredFileInputStream(new FileInputStream(fileName));
//     return new InputStreamReader(filteredInputStream);
//   }

  public static void printUsageAndExit() {
    System.out.println("usage");
    System.exit(0);
  }

//   private static void filterFile(File source, File dest) throws IOException {
//     Reader reader = getFilteredReader(source.getAbsolutePath());
//     Writer writer = new FileWriter(dest);
//     StreamUtil.copy(reader, writer);
//     reader.close();
//     writer.close();
//   }

  private static void filterFile(File source, File dest) throws IOException {
    InputStream is =
      new ReaderInputStream(getFilteredReader(source.getAbsolutePath()));
    OutputStream os = new FileOutputStream(dest);
    StreamUtil.copy(is, os);
    is.close();
    os.close();
  }

  private static File makeDestFile(File file, File sourceDir, File destDir)
      throws IOException {
    String relativePath = FileUtil.getPathUnderRoot(file, sourceDir);
    File returnFile = new File(destDir, relativePath);
    returnFile.getParentFile().mkdirs();
    returnFile.createNewFile();
    return returnFile;
  }

  private static void filterFiles(File sourceDir, File destDir)
      throws IOException {
    List fileList = FileUtil.enumerateFiles(sourceDir);
    for (Iterator it = fileList.iterator(); it.hasNext();) {
      File curFile = (File) it.next();
      filterFile(curFile, makeDestFile(curFile, sourceDir, destDir));
    }
  }

  public static void main(String args[]) {
    if (args.length == 2) {
      try{
	File source = new File(args[0]);
	File dest = new File(args[1]);
	filterFiles(source, dest);
      } catch (IOException e) {
	e.printStackTrace();
      }
    } else {
      printUsageAndExit();
    }
  }
}
