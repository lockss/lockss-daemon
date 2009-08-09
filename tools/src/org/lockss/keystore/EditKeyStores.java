/*
 * $Id: EditKeyStores.java,v 1.3 2009-08-09 07:39:47 tlipkis Exp $
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
import sun.security.x509.X500Signer;
import sun.security.pkcs.PKCS10;
import sun.security.provider.IdentityDatabase;
import sun.security.provider.SystemSigner;

import org.lockss.util.KeyStoreUtil;

/**
 * A tool to build key stores for V3 over SSL support in CLOCKSS
 */

public class EditKeyStores {

  private static void usage() {
    System.out.println("Usage: [-i inputDir] [-o outputDir] host1 host2 ...");
    System.exit(0);
  }

  public static void main(String[] args) {
    String inDir = "/tmp/input";
    String outDir = "/tmp/output";
    List hostlist = new ArrayList();

    /*
     * Parse args
     */
    if (args.length == 0) {
      usage();
    }

    try {
      for (int ix = 0; ix < args.length; ix++) {
	if (args[ix].startsWith("-")) {
	  if ("-i".equals(args[ix])) {
	    inDir = args[++ix];
	    OUTdebug("Input directory " + inDir);
	    continue;
	  }
	  if ("-o".equals(args[ix])) {
	    outDir = args[++ix];
	    OUTdebug("Output directory " + outDir);
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
    try {
      KeyStoreUtil.createPLNKeyStores(new File(inDir), new File(outDir),
				      hostlist);
    } catch (NoSuchAlgorithmException ex) {
      OUTerror("createPLNKeyStores threw " + ex);
      return;
    } catch (NoSuchProviderException ex) {
      OUTerror("createPLNKeyStores threw: " + ex);
      return;
    }
  }


  private static void OUTdebug(String s) {
    if (false)
      System.err.println("debug:" + s);
  }
  private static void OUTerror(String s) {
    if (true)
      System.err.println("error: " + s);
  }
}
