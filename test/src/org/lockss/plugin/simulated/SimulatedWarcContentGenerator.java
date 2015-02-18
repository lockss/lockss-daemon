/*
 * $Id$
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
import org.archive.io.warc.*;
import org.archive.uid.RecordIDGenerator;
import org.archive.uid.UUIDGenerator;
import org.archive.util.anvl.*;
import org.archive.util.ArchiveUtils;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;

/**
 * A convenience class which takes care of handling the content
 * tree itself for the case where the content is in a WARC file.
 *
 * @author  David S. H. Rosenthal
 * @author  Felix Ostrowski
 * @version 0.0
 */

public class SimulatedWarcContentGenerator extends SimulatedContentGenerator {
  private static Logger logger = Logger.getLogger("SimulatedWarcContentGenerator");
  String warcFilePrefix = "SimulatedCrawl";
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
  String[] stem = {
    "http://www.content.org/",
    "http://www.website.org/",
    "http://www.library.org/",
  };
  boolean compressWarc = true;

  public SimulatedWarcContentGenerator(String rootPath) {
    super(rootPath);
    logger.debug3("Created instance for " + rootPath);
  }

  public String generateContentTree() {
    String ret = super.generateContentTree();
    WARCWriter aw = makeWARCWriter();
    long startPosition = 0;


    //  There should now be a suitable hierarchy at contentRoot,
    //  except that there needs to be a link to the eventual WARC file(s).
    try {
      for (int i = 0; i < stem.length; i++) {
        boolean kill = ((i + 1) >= stem.length);
        aw.checkSize();
        startPosition = aw.getPosition();
        logger.debug2("About to pack content for " + stem[i] + " in WARC at " +
            contentRoot + " offset " + startPosition);
        packIntoWarcFile(new File(contentRoot), new URL(stem[i]), aw, 0, kill);
        logger.debug2("Packed content for " + stem[i] + " in WARC at " +
            contentRoot + " to " + aw.getPosition());
      }
      aw.close();
      linkToWarcFiles();
    } catch (IOException ex) {
      logger.error("pack() threw " + ex);
      return null;
    } finally {
    }
    printWarcFiles();
    return ret;
  }

  private void packIntoWarcFile(File f,
      URL url,
      WARCWriter aw,
      int lev,
      boolean kill) throws IOException {
    String fPath = f.getCanonicalPath();
    logger.debug3("packIntoWarcFile(" + fPath + ") lev " + lev + " kill " + kill);
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
        packIntoWarcFile(newFile, new URL(newUrl), aw, lev + 1, kill);
      }
      {
        String[] namesLeft = f.list();
        if (namesLeft.length == 0) {
          logger.debug3(fPath + " empty");
        } else for (int j = 0; j < namesLeft.length; j++) {
          logger.debug3(fPath + " contains " + namesLeft[j]);
        }
      }
      if (lev > 1 && kill) {
        logger.debug3("rmdir(" + fPath + ")");
        f.delete();
      }
    } else if (f.isFile()) {
      logger.debug3("File " + fPath + " lev " + lev);
      String extension = ".warc" + (compressWarc ? ".gz" : "");
      if (!fPath.endsWith(extension) &&
          !fPath.endsWith(extension + ".open") &&
          !(fPath.endsWith("index.html") && lev <= 1)) {
        String uri = url.toString();
        String contentType = mimeType[mimeType.length-1];
        String hostIP = "127.0.0.1";
        String timeStamp = ArchiveUtils.getLog14Date();
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
        ANVLRecord record = null;
        // Use the following to add additional data:
        // ANVLRecord record = new ANVLRecord();
        // record.addLabelValue("foo", "bar");
        // TODO: implement request records.
        aw.writeResourceRecord(uri, timeStamp, contentType, record, is, recordLength);
        logger.debug3("Wrote to " + aw.getPosition() + ": Deleting " + fPath);
        if (kill) {
          f.delete();
        }
      } else {
        logger.debug3("Ignoring " + fPath);
      }
    } else {
      String msg = fPath + " is neither file not dir";
      logger.error(msg);
      throw new IOException(msg);
    }
  }

  private WARCWriter makeWARCWriter() {
    WARCWriter ret = null;
    List dirs = new ArrayList();
    dirs.add(new File(contentRoot));
    List warcinfoData = new ArrayList();
    warcinfoData.add("");

    String template = "${prefix}-${timestamp17}-${serialno}";
    RecordIDGenerator generator = new UUIDGenerator();

    WARCWriterPoolSettingsData settings = new WARCWriterPoolSettingsData(
	warcFilePrefix, template, maxSize, compressWarc, dirs, warcinfoData,
	generator);

    ret = new WARCWriter(serialNo, settings);

    return ret;
  }

  private void linkToWarcFiles() {
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
        logger.error("linkToWarcFiles() threw " + ex);
      } else {
        logger.error("index.html missing");
      }
    } else {
      logger.error("Directory " + contentRoot + " missing");
    }
  }

  private void printWarcFiles() {
    File dir = new File(contentRoot);
    if (dir.isDirectory()) try {
      String[] fileNames = dir.list();

      for (int i = 0; i < fileNames.length; i++) {
        if (fileNames[i].endsWith(".warc.gz") ||
            fileNames[i].endsWith("warc")) {
          logger.debug3("File: " + fileNames[i]);

          File aFile = new File(dir, fileNames[i]);
          ArchiveReader aRead = ArchiveReaderFactory.get(aFile);
          // Just don't ask why the next line is necessary
          // TODO: ask why the next line is necessary.
          // ((ARCReader)aRead).setParseHttpHeaders(false);
          for (Iterator it = aRead.iterator(); it.hasNext(); ) {
            ArchiveRecord aRec = (ArchiveRecord)it.next();
            ArchiveRecordHeader aHead = aRec.getHeader();
            logger.debug3(aHead.toString());
          }
        }
      }
    } catch (IOException ex) {
      logger.error("linkToWarcFiles() threw " + ex);
    } else {
      logger.error("Directory " + contentRoot + " missing");
    }
  }
}
