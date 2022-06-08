/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.medknow;

import org.lockss.daemon.AuParamType;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.AuParamFunctor;
import org.lockss.plugin.base.BaseAuParamFunctor;
import org.lockss.util.Logger;

public class MedknowAuParamFunctor extends BaseAuParamFunctor {
  private static final Logger log = Logger.getLogger(MedknowAuParamFunctor.class);

  public static final AuParamFunctor SINGLETON = new MedknowAuParamFunctor();

  /** Custom apply for our single function, otherwise defaults to parent */
  public Object apply(AuParamFunctor.FunctorData fd, String fn,
                      Object arg, AuParamType type)
      throws PluginException {
    try {
      if (fn.equals("del_www_and_scheme")) {
        return super.apply(
            fd,
            "url_host",
            super.apply(fd, "del_www", arg, type),
            type
        );
      } else {
        return super.apply(fd, fn, arg, type);
      }
    } catch (ClassCastException e) {
      throw new PluginException.BehaviorException("Illegal arg type", e);
    }
  }

  /** Return AuParamType.String if the function is the custom function for this
   * plugin, otherwise, default to the parent. */
  public AuParamType type(AuParamFunctor.FunctorData fd, String fn) {
    if (fn.equals("del_www_and_scheme")) {
      return AuParamType.String;
    }
    return super.type(fd, fn);
  }

}
