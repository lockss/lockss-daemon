/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.util;

import java.io.*;
import java.net.*;

/**
 * Simple SMTP client.
 */
public class SmtpMailer {
  private static String smtpHost = null;
  protected static Logger logger = Logger.getLogger("SmtpMailer");

  private Socket socket;
  private PrintWriter wrtr;
  private BufferedReader rdr;
  private boolean useTestStreams = false;

  SmtpMailer() {
    // for testing only
  }

  /**
   * Contacts the specified smtp server, conses up header and sends message
   * @param to address
   * @param from address
   * @param subject line
   * @param message content
   * @param smtpHost mail server
   * @param smtpPort mail port
   * @param localHostName to redirect bounces
   * @return if successful
   * @throws IOException
   */
  boolean doSMTP(String to, String from, String subject, String message,
                 String smtpHost, int smtpPort, String localHostName)
      throws IOException {
    if (smtpHost == null) {
      return err("No smtp host specified; can't send email");
    }
    if (to == null) {
      return err("No email address specified; can't send email");
    }

    if (localHostName == null) {
      return err("No local host specified; can't send email");
    }

    SmtpMailer.smtpHost = smtpHost;

    if (!useTestStreams) {
      try {
        socket = new Socket(InetAddress.getByName(smtpHost), smtpPort);
      } catch (UnknownHostException e) {
        return err("Couldn't find host " + smtpHost + ":" + smtpPort + ": " + e);
      } catch (IOException e) {
        return err("Couldn't connect to host " + smtpHost + ":" +
                   smtpPort + ": " + e);
      }

      try {
        wrtr = new PrintWriter(socket.getOutputStream());
        rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      } catch (IOException e) {
        return err("Unable to obtain input or output stream from socket: " + e);
      }
    }
    if (!checkReply("220")) return false;

    send("HELO " + localHostName);
    if (!checkReply("250")) return false;

    send("MAIL FROM: <" + from + ">");
    if (!checkReply("250")) return false;

    send("RCPT TO: <" + to + ">");
    if (!checkReply("250")) return false;

    send("DATA");
    if (!checkReply("354")) return false;

    send("From: " + from);
    send("To: " + to);
    send("Subject: " + subject);
    send("X-Mailer: Smtp Mailer");
    send(""); //end headers
    send(message);
    send(".");

    if (!checkReply("250")) return false;

    send("QUIT");

    if (!useTestStreams) {
      wrtr.close();
      socket.close();
    }
    return true;
  }

  /**
   * Log error, close socket if necessary, return false
   * @param msg the message
   * @return false always
   */
  private boolean err(String msg) {
    logger.error(msg);
    if (socket != null) {
      try {
	socket.close();
      } catch (IOException e) {
	// no action
      }
      socket = null;
    }
    return false;
  }

  /**
   * Read SMTP reply and compare response code.
   * If unexpected, log error and exit
   * @param goodCode the reply code
   * @return if successful
   */
  boolean checkReply(String goodCode) {
    String reply;
    try {
      reply = rdr.readLine();
    } catch (IOException e) {
      return err("IOException reading from smtp server " + smtpHost + ": " + e);
    }
    if (reply.startsWith(goodCode)) {
      return true;
    } else {
      return err("Unexpected reply from " + smtpHost + ": " + reply);
    }
  }

  /**
   * Helper function to add network EOL to each string and
   * flush the printwriter
   * @param msg the message
   */
  private void send(String msg) {
    wrtr.print(msg + "\r\n");
    wrtr.flush();
  }

  // static methods

  /**
   * Static entry point to send mail.
   * @param to address
   * @param from address
   * @param subject line
   * @param message content
   * @param smtpHost mail server
   * @param smtpPort mail port
   * @param localHostName to redirect bounces
   * @throws IOException
   */
  public static void sendMail(String to, String from, String subject,
                              String message, String smtpHost, int smtpPort,
                              String localHostName) throws IOException {
    SmtpMailer mailer = new SmtpMailer();
    mailer.doSMTP(to, from, subject, message, smtpHost, smtpPort, localHostName);
  }

  public void useTestStreams(PrintWriter writer, BufferedReader reader) {
    wrtr = writer;
    rdr = reader;
    useTestStreams = true;
  }

}
