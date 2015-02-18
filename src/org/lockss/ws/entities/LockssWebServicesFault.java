/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Exception specific to the processing of web services.
 */
package org.lockss.ws.entities;

import javax.xml.ws.WebFault;

@WebFault(faultBean="org.lockss.ws.entities.LockssWebServicesFaultInfo")
@SuppressWarnings("serial")
public class LockssWebServicesFault extends Exception {
  LockssWebServicesFaultInfo faultInfo = new LockssWebServicesFaultInfo();

  public LockssWebServicesFault() {
    super();
  }

  public LockssWebServicesFault(LockssWebServicesFaultInfo faultInfo) {
    super();
    this.faultInfo = faultInfo;
  }

  public LockssWebServicesFault(String message) {
    super(message);
  }

  public LockssWebServicesFault(String message,
      LockssWebServicesFaultInfo faultInfo) {
    super(message);
    this.faultInfo = faultInfo;
  }

  public LockssWebServicesFault(Throwable cause) {
    super(cause);
  }

  public LockssWebServicesFault(Throwable cause,
      LockssWebServicesFaultInfo faultInfo) {
    super(cause);
    this.faultInfo = faultInfo;
  }

  public LockssWebServicesFault(String message, Throwable cause) {
    super(message, cause);
  }

  public LockssWebServicesFault(String message, Throwable cause,
      LockssWebServicesFaultInfo faultInfo) {
    super(message, cause);
    this.faultInfo = faultInfo;
  }

  public LockssWebServicesFaultInfo getFaultInfo() {
    return faultInfo;
  }
}
