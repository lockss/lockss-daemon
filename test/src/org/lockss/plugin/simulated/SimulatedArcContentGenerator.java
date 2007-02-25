/*
 * $Id: SimulatedArcContentGenerator.java,v 1.1 2007-02-25 23:06:38 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.text.*;
import org.archive.io.*;
import org.archive.io.arc.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;

/**
 * A convenience class which takes care of handling the content
 * tree itself for the case where the content is in an ARC file.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class SimulatedArcContentGenerator extends SimulatedContentGenerator {
  private static Logger logger = Logger.getLogger("SimulatedArcContentGenerator");
  String arcFilePrefix = "SimulatedCrawl";
  AtomicInteger serialNo = new AtomicInteger(0);
  int maxSize = 100000000;
  String[] suffix = {
    ".txt",
    ".html",
    ".pdf",
    ".jpg",
    ".bin",
  };
  String[] mimeType = {
    "text/plain",
    "text/html",
    "application/pdf",
    "image/jpg",
    "application/octet-stream",
  };
  String stem = "http://www.example.com/";
  boolean compressArc = true;

  public SimulatedArcContentGenerator(String rootPath) {
    super(rootPath);
    logger.debug3("Created instance for " + rootPath);
  }

  public String generateContentTree() {
    String ret = super.generateContentTree();
    ARCWriter aw = makeARCWriter();
    long startPosition = 0;
	

    //  There should now be a suitable hierarchy at contentRoot,
    //  except that there needs to be a link to the eventual ARC file(s).
    try {
      aw.checkSize();
      startPosition = aw.getPosition();
      logger.debug2("About to pack content in ARC at " + contentRoot +
		    " offset " + startPosition);
      packIntoArcFile(new File(contentRoot), new URL(stem), aw, 0);
      logger.debug2("Packed content in ARC at " + contentRoot + " to " +
		    aw.getPosition());
      aw.close();
      linkToArcFiles();
    } catch (IOException ex) {
      logger.error("pack() threw " + ex);
      return null;
    }
    printArcFiles(startPosition);
    return ret;
  }

  private void packIntoArcFile(File f, URL url, ARCWriter aw, int lev) throws IOException {
    String fPath = f.getCanonicalPath();
    logger.debug3("packIntoArcFile(" + fPath + ") lev " + lev);
    if (f.isDirectory()) {
      // Iterate through the directory
      File[] names = f.listFiles();
      for (int i = 0; i < names.length; i++) {
	String newPath = fPath + File.separator + names[i].getName();
	String newUrl = url.toString();
	if (!newUrl.endsWith(File.separator)) {
	  newUrl += File.separator;
	}
	newUrl += names[i].getName();
	File newFile = new File(newPath);
	packIntoArcFile(newFile, new URL(newUrl), aw, lev + 1);
      }
      {
	String[] namesLeft = f.list();
	if (namesLeft.length == 0) {
	  logger.debug3(fPath + " empty");
	} else for (int j = 0; j < namesLeft.length; j++) {
	  logger.debug3(fPath + " contains " + namesLeft[j]);
	}
      }
      if (lev > 1) {
	logger.debug3("rmdir(" + fPath + ")");
	f.delete();
      }
    } else if (f.isFile()) {
      logger.debug3("File " + fPath + " lev " + lev);
      if (!fPath.endsWith(".arc.gz") &&
	  !fPath.endsWith(".arc.gz.open") &&
	  !fPath.endsWith(".arc") &&
	  !fPath.endsWith(".arc.open") &&
	  !(fPath.endsWith("index.html") && lev <= 1)) {
	String uri = url.toString();
	String contentType = mimeType[mimeType.length-1];
	String hostIP = "127.0.0.1";
	long timeStamp = System.currentTimeMillis();
	int recordLength = (int) f.length();
	InputStream is = new FileInputStream(f);

	for (int i = 0; i < suffix.length; i++) {
	  String name = f.getName();
	  if (name.endsWith(suffix[i])) {
	    contentType = mimeType[i];
	    break;
	  }
	}

	logger.debug3("Packing " + fPath + " type " + contentType);
	aw.write(uri, contentType, hostIP, timeStamp, recordLength, is);
	logger.debug3("Wrote to " + aw.getPosition() + ": Deleting " + fPath);
	f.delete();
      } else {
	logger.debug3("Ignoring " + fPath);
      }
    } else {
      String msg = fPath + " is neither file not dir";
      logger.error(msg);
      throw new IOException(msg);
    }
  }

  private ARCWriter makeARCWriter() {
    ARCWriter ret = null;
    List dirs = new ArrayList();
    dirs.add(new File(contentRoot));
    ret = new ARCWriter(serialNo, dirs, arcFilePrefix, compressArc, maxSize);
    return ret;
  }

  private void linkToArcFiles() {
    File dir = new File(contentRoot);
    if (dir.isDirectory()) {
      File index = new File(dir, INDEX_NAME);
      if (index.exists() && index.isFile()) try {
	FileOutputStream fos = new FileOutputStream(index);
	PrintWriter pw = new PrintWriter(fos);
	logger.debug3("Re-creating index file at " + index.getAbsolutePath());
	String file_content =
	  getIndexContent(dir, INDEX_NAME, LockssPermission.LOCKSS_PERMISSION_STRING);
	pw.print(file_content);
	pw.flush();
	pw.close();
	fos.close();
      } catch (IOException ex) {
	logger.error("linkToArcFiles() threw " + ex);
      } else {
	logger.error("index.html missing");
      }
    } else {
      logger.error("Directory " + contentRoot + " missing");
    }
  }

  private void printArcFiles(long startPosition) {
    File dir = new File(contentRoot);
    if (dir.isDirectory()) try {
      String[] fileNames = dir.list();

      for (int i = 0; i < fileNames.length; i++) {
	if (fileNames[i].endsWith(".arc.gz") ||
	    fileNames[i].endsWith("arc")) {
	  logger.debug3(fileNames[i] + " headers offset" + startPosition);
		    
	  File aFile = new File(dir, fileNames[i]);
	  ArchiveReader aRead = ArchiveReaderFactory.get(aFile);
	  // Just don't ask why the next line is necessary
	  ((ARCReader)aRead).setParseHttpHeaders(false);
	  for (Iterator it = aRead.iterator(); it.hasNext(); ) {
	    ArchiveRecord aRec = (ArchiveRecord)it.next();
	    ArchiveRecordHeader aHead = aRec.getHeader();
	    logger.debug3(aHead.toString());
	  }
	}
      }
    } catch (IOException ex) {
      logger.error("linkToArcFiles() threw " + ex);
    } else {
      logger.error("Directory " + contentRoot + " missing");
    }
  }
}
