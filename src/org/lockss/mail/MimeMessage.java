/*
 * $Id$
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
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.lockss.util.*;

/**
 * Wrapper for JavaMail's MimeMessage that works with {@link MailService}.
 * Currently allows attaching text or file parts with no nesting.
 */
public class MimeMessage implements MailMessage {
  protected static Logger log = Logger.getLogger("MimeMessage");

  javax.mail.internet.MimeMessage msg;
  MimeMultipart parts = new MimeMultipart();
  private List tmpFiles;

  /** Create a MimeMessage */
  public MimeMessage() {
    super();
    Session session = Session.getDefaultInstance(new Properties());
    msg = new javax.mail.internet.MimeMessage(session);
  }

  /** Add a header value */
  public MailMessage addHeader(String name, String val) {
    try {
      msg.addHeader(name, val);
    } catch (MessagingException e) {
      throw new RuntimeException("Couldn't add header: " + name + ": " + val,
				 e);
    }
    return this;
  }

  /** Return value of header, comma separated if more than one.  Mostly for
   * testing */
  public String getHeader(String name) {
    try {
      return msg.getHeader(name, ", ");
    } catch (MessagingException e) {
      throw new RuntimeException("Couldn't get header", e);
    }
  }

  /** Add a text part to the message
   */
  public MimeMessage addTextPart(String text) {
    try {
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setContent(text, "text/plain");
      parts.addBodyPart(textPart);
    } catch (MessagingException e) {
      throw new RuntimeException("Couldn't add text part", e);
    }
    return this;
  }

  /** Add a file attachment
   */
  public MimeMessage addFile(File file, String name)
      throws FileNotFoundException {
    try {
      if (!file.exists() || !file.canRead()) {
	throw new FileNotFoundException(file.getAbsolutePath()
					+ " does not exist or is not readable.");
      }
      MimeBodyPart filePart = new MimeBodyPart();
      FileDataSource fileData = new FileDataSource(file);
      DataHandler fileDataHandler = new DataHandler(fileData);

      //     filePart.addHeader("Content-Transfer-Encoding","base64");
      filePart.setDataHandler(fileDataHandler);
      filePart.setFileName(name);
      parts.addBodyPart(filePart);
    } catch (MessagingException e) {
      throw new RuntimeException("Couldn't add file: " + file, e);
    }
    return this;
  }

  /** Add a file attachment; the file will be deleted after the message is
   * sent
   */
  public MimeMessage addTmpFile(File file, String name)
      throws FileNotFoundException {
    addFile(file, name);
    if (tmpFiles == null) {
      tmpFiles = new ArrayList();
    }
    tmpFiles.add(file);
    return this;
  }

  private void finish() {
    try {
      msg.setContent(parts);
    } catch (MessagingException e) {
      throw new RuntimeException("Completing", e);
    }
  }

  /** Write the message content (RFC822 data) to the stream. */
  public void writeData(OutputStream ostrm) throws IOException {
    try {
      if (msg.getLineCount() <= 0) {
	finish();
      }
      msg.writeTo(ostrm);
    } catch (MessagingException e) {
      throw new RuntimeException("writeData", e);
    } catch (IOException e) {
      throw new RuntimeException("writeData", e);
    }
  }


  /** Return an array of all the parts; mostly for testing */
  public MimeBodyPart[] getParts() {
    try {
      int n = parts.getCount();
      MimeBodyPart[] res = new MimeBodyPart[n];
      for (int ix = 0; ix < n; ix++) {
	res[ix] = (MimeBodyPart)parts.getBodyPart(ix);
      }
      return res;
    } catch (MessagingException e) {
      throw new RuntimeException("Couldn't get parts", e);
    }
  }

  /** delete temporary files */
  public void delete(boolean sentOk) {
    if (tmpFiles != null && !tmpFiles.isEmpty()) {
      for (Iterator iter = tmpFiles.iterator(); iter.hasNext(); ) {
	File file = (File)iter.next();
	try {
	  file.delete();
	} catch (Exception e) {
	  log.warning("Error deleting tmp file: " + file, e);
	}
      }
    }

  }
}
