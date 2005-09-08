/*
 * $Id: IcpSender.java,v 1.1.2.1 2005-09-08 01:03:18 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy.icp;

import java.io.IOException;
import java.net.InetAddress;

/**
 * <p>An abstraction for classes that are able to send ICP messages
 * to recipients.</p>
 * @author Thib Guicherd-Callin
 */
public interface IcpSender {

  /**
   * <p>Sends the given message to the recipient, on the default ICP 
   * port.</p>
   * @param message   An ICP message.
   * @param recipient A recipient address.
   * @throws IOException if an input/output error occurs.
   * @see IcpMessage#ICP_PORT
   * @see #send(IcpMessage, InetAddress, int)
   */
  void send(IcpMessage message,
            InetAddress recipient)
      throws IOException;
  
  /**
   * <p>Sned the given message to the recipient.</p>
   * @param message   An ICP message.
   * @param recipient A recipient address.
   * @param port      A remote port number.
   * @throws IOException if an input/output error occurs.
   */
  void send(IcpMessage message,
            InetAddress recipient,
            int port)
      throws IOException;
  
}
