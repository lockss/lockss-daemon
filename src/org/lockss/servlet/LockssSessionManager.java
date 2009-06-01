// ========================================================================
// $Id: LockssSessionManager.java,v 1.1 2009-06-01 07:53:32 tlipkis Exp $
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.lockss.servlet;

import java.util.*;
import org.mortbay.jetty.servlet.*;
import javax.servlet.http.*;
import org.lockss.util.*;


/* ------------------------------------------------------------ */
/** An in-memory implementation of SessionManager.
 *
 * @version $Id: LockssSessionManager.java,v 1.1 2009-06-01 07:53:32 tlipkis Exp $
 * @author Greg Wilkins (gregw)
 */
public class LockssSessionManager extends AbstractSessionManager {
  private static Logger log = Logger.getLogger("LockssSessionManager");

    /* ------------------------------------------------------------ */
    public LockssSessionManager()
    {
        super();
    }
    
    /* ------------------------------------------------------------ */
    public LockssSessionManager(Random random)
    {
        super(random);
    }

  public HttpSession getHttpSession(String id) {
    HttpSession res = super.getHttpSession(id);
    if (log.isDebug2()) log.debug2("getHttpSession("+id+"): " + res);
    return res;
  }

    /* ------------------------------------------------------------ */
    protected AbstractSessionManager.Session newSession(HttpServletRequest request)
    {
        return new Session(request);
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class Session extends AbstractSessionManager.Session
    {
        /* ------------------------------------------------------------- */
        protected Session(HttpServletRequest request)
        {
            super(request);
        }
        
        /* ------------------------------------------------------------ */
        protected Map newAttributeMap()
        {
            return new HashMap(3);
        }
    }
    
}
