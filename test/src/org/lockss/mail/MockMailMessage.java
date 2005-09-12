/*
 * $Id: MockMailMessage.java,v 1.2 2005-09-12 04:36:56 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.mail;

import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class MockMailMessage implements MailMessage {
  private String text;
  private boolean isDeleted = false;
  private boolean wasSentOk;

  MockMailMessage(String text) {
    this.text = text;
  }

  public MailMessage addHeader(String name, String val) {
    throw new UnsupportedOperationException();
  }

  public void writeData(OutputStream ostrm) throws IOException {
    Writer wrtr = new OutputStreamWriter(ostrm, Constants.DEFAULT_ENCODING);
    wrtr.write(text);
    wrtr.flush();
  }

  public void delete(boolean sentOk) {
    isDeleted = true;
    wasSentOk = sentOk;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public boolean wasSentOk() {
    return wasSentOk;
  }
}

