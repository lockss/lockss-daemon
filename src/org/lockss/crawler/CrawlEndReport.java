/*
 * $Id: CrawlEndReport.java,v 1.2 2009-02-05 05:08:47 tlipkis Exp $
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

package org.lockss.crawler;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.security.*;
import org.apache.commons.collections.map.LRUMap;
import org.lockss.util.*;
import org.lockss.mail.*;
import org.lockss.config.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.hasher.*;

/**
 * Utility class to generate and mail a report 
 */
public class CrawlEndReport {
  static final Logger log = Logger.getLogger("CrawlEndReport");

  IdentityManager idMgr;
  LockssDaemon daemon;
  ArchivalUnit au;
  String hashAlg;

  private DateFormat headerDf =
    new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss zzz");

  public CrawlEndReport (LockssDaemon daemon, ArchivalUnit au) {
    this.au = au;
    this.daemon = daemon;
    idMgr = daemon.getIdentityManager();
  }

  public void setHashAlgorithm(String alg) {
    hashAlg = alg;
  }

  private String crawlReportHeader(String machineName, String date) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Crawl completion report from " + machineName + ", " +
	      date + "\n");
    sb.append("Date = " + date + "\n");
    sb.append("Host = " + machineName + "\n");
    PeerIdentity pid =
      idMgr.getLocalPeerIdentity(org.lockss.poller.Poll.V3_PROTOCOL);
    sb.append("PeerId = "
	      + pid.getIdString()
	      + "\n");
    sb.append("AU = " + au.getName() + "\n");
    sb.append("AUID = " + au.getAuId() + "\n");
    sb.append("Hash = " + hashAlg + "\n");
    return sb.toString();
  }

  private File buildAuHashFile(String textPart)
      throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(hashAlg);
    SimpleHasher hasher = new SimpleHasher(digest);
    File blockFile = FileUtil.createTempFile("auhash", ".tmp");
    hasher.doV3Hash(au.getAuCachedUrlSet(), blockFile, textPart);
    return blockFile;
  }

  public void sendCrawlEndReport(ArchivalUnit au, String to) {
    String date = headerDf.format(new Date());
    String machineName = PlatformUtil.getLocalHostname();
    if (StringUtil.isNullString(machineName)) {
      machineName = "Unknown";
    }
    String textPart = crawlReportHeader(machineName, date);
    MailService mailSvc = daemon.getMailService();
    File rfile = null;
    MimeMessage msg = new MimeMessage();
    String from =
      CurrentConfig.getParam(ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL);
    msg.addHeader("From", from);
    msg.addHeader("To", to);
    msg.addHeader("Date", date);
    msg.addHeader("Subject",
		  "Crawl report for LOCKSS box " + machineName);
    try {
      try {
	rfile = buildAuHashFile(textPart);
	msg.addTextPart(textPart);
	msg.addTmpFile(rfile, "CrawlReport");
      } catch (NoSuchAlgorithmException e) {
	log.error("Couldn't generate crawl end report", e);
	msg.addTextPart(textPart + "\n\n"
			+ "Error: Configured hash algorithm is not supported: "
			+ hashAlg +
			"\nNo report generated.");
      } catch (IOException e) {
	log.error("Error generating crawl end report", e);
	msg.addTextPart("Error generating crawl end report: " + e);
      }
      mailSvc.sendMail(from, to, msg);
    } finally {
    }
  }
}

