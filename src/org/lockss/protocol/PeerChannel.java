/*
 * $Id: PeerChannel.java,v 1.1 2005-05-18 05:42:38 tlipkis Exp $
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

package org.lockss.protocol;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

/** Manages a stream connection to a peer.  Channels are ephemeral, coming
 * and going as needed.
 */
interface PeerChannel {

  static final int HEADER_OFF_CHECK = 0;
  static final int HEADER_OFF_OP = 1;
  static final int HEADER_OFF_LEN = 2;
  static final int HEADER_OFF_PROTO = 6;
  static final int HEADER_LEN = 10;

  static final byte HEADER_CHECK = (byte)0xff;
  static final byte OP_PEERID = 0x01;
  static final byte OP_DATA = 0x02;

  static final int MAX_PEERID_LEN = 50;

}
