package org.lockss.plugin.highwire;

import org.lockss.daemon.AuParamType;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.AuParamFunctor;
import org.lockss.plugin.base.BaseAuParamFunctor;

public class HighWireParamFunctor  extends BaseAuParamFunctor {

  public HighWireParamFunctor() {
  }

  public Object apply(AuParamFunctor.FunctorData fd, String fn, Object arg, AuParamType type) throws PluginException {
    try {
      return fn.equals("del_www_and_scheme") ? super.apply(fd, "url_host", super.apply(fd, "del_www", arg, type), type) : super.apply(fd, fn, arg, type);
    } catch (ClassCastException var6) {
      throw new PluginException.BehaviorException("Illegal arg type", var6);
    }
  }

  public AuParamType type(AuParamFunctor.FunctorData fd, String fn) {
    return fn.equals("del_www_and_scheme") ? AuParamType.String : super.type(fd, fn);
  }
}