/*
 * $Id$
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

package org.lockss.keystore;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import sun.security.x509.X500Name;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.X509Key;
import sun.security.pkcs.PKCS10;

import org.lockss.util.*;

/**
 * A tool to build key stores for V3 over SSL support in CLOCKSS
 */

public class EditKeyStores {
  protected static Logger log = Logger.getLogger("EditKeyStores");
  
  private static SecureRandom testOnlySecureRandom = null;

  static void setTestOnlySecureRandom(SecureRandom rng) {
    testOnlySecureRandom = rng;
  }

  private static void usage() {
    System.out.println("Usage:");
    System.out.println("   EditKeyStores [-i inDir] [-o outDir] host1 host2 ...");
    System.out.println("      or");
    System.out.println("   EditKeyStores -s pub-keystore [-p pub-password] [-o outDir] host1 host2 ...");
    System.out.println("");
    System.out.println("Creates, in outDir, a private key for each host.  In the first variant");
    System.out.println("each keystore contains the host's private key and public certificates");
    System.out.println("for all the other hosts.  In the second variant the public certificates");
    System.out.println("are written into a shared, public keystore and each hosts keystore contains");
    System.out.println("only its own private key and public cert.");

    System.exit(0);
  }

  public static void main(String[] args) {
    String inDir = "/tmp/input";
    String outDir = "/tmp/output";
    File pubFile = null;
    String pubPass = null;
    List hostlist = new ArrayList();
    boolean tflag = false;

    /*
     * Parse args
     */
    if (args.length == 0) {
      usage();
    }

    try {
      for (int ix = 0; ix < args.length; ix++) {
	if (args[ix].startsWith("-")) {
	  if ("-t".equals(args[ix])) {
	    // testing - allows use of an rng that doesn't require kernel
	    // randomness, and prevent error exit
	    tflag = true;
	    continue;
	  }
	  if ("-i".equals(args[ix])) {
	    inDir = args[++ix];
	    log.debug("Input directory " + inDir);
	    continue;
	  }
	  if ("-o".equals(args[ix])) {
	    outDir = args[++ix];
	    log.debug("Output directory " + outDir);
	    continue;
	  }
	  if ("-s".equals(args[ix])) {
	    pubFile = new File(args[++ix]);
	    log.debug("Public keystore " + pubFile);
	    continue;
	  }
	  if ("-p".equals(args[ix])) {
	    pubPass = args[++ix];
	    continue;
	  }
	  usage();
	} else {
	  hostlist.add(args[ix]);
	}
      }
    } catch (Exception e) {
      usage();
    }
    if (hostlist.isEmpty()) {
      usage();
    }
    File outDirFile = new File(outDir);
    if (!outDirFile.isDirectory()) {
      outDirFile.mkdirs();
    }
    try {
      SecureRandom rng = tflag ? testOnlySecureRandom : getSecureRandom();
      if (pubFile != null) {
	if (StringUtil.isNullString(pubPass)) {
	  log.info("No public keystore password supplied, using \"password\"");
	  pubPass = "password";
	}
	KeyStoreUtil.createSharedPLNKeyStores(outDirFile, hostlist,
					      pubFile, pubPass, rng);
	log.info("Keystores generated in " + outDirFile
		 + ", public keystore in " + pubFile);
      } else {
	KeyStoreUtil.createPLNKeyStores(new File(inDir), outDirFile,
					hostlist, rng);
	log.info("Keystores generated in " + outDirFile);
      }
    } catch (Exception e) {
      log.error("Failed, keystores not generated: " + e.toString());
      if (!tflag) {
	System.exit(1);
      }
    }
  }

  static SecureRandom getSecureRandom() {
    try {
      return SecureRandom.getInstance("SHA1PRNG", "SUN");
    } catch (Exception ex) {
      log.error("Couldn't get SecureRandom: " + ex);
      return null;
    }
  }
}
