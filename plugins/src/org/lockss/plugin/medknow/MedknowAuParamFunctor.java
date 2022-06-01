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
