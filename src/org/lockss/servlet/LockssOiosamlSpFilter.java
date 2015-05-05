/*
 * $Id$
 */

/*

Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.servlet;

import dk.itst.oiosaml.configuration.SAMLConfiguration;
import dk.itst.oiosaml.configuration.SAMLConfigurationFactory;
import dk.itst.oiosaml.error.Layer;
import dk.itst.oiosaml.error.WrappedException;
import dk.itst.oiosaml.logging.Audit;
import dk.itst.oiosaml.logging.Operation;
import dk.itst.oiosaml.sp.UserAssertion;
import dk.itst.oiosaml.sp.UserAssertionHolder;
import dk.itst.oiosaml.sp.develmode.DevelMode;
import dk.itst.oiosaml.sp.develmode.DevelModeImpl;
import dk.itst.oiosaml.sp.metadata.CRLChecker;
import dk.itst.oiosaml.sp.metadata.IdpMetadata;
import dk.itst.oiosaml.sp.metadata.SPMetadata;
import dk.itst.oiosaml.sp.service.SAMLHttpServletRequest;
import dk.itst.oiosaml.sp.service.SPFilter;
import dk.itst.oiosaml.sp.service.session.Request;
import dk.itst.oiosaml.sp.service.session.SessionCleaner;
import dk.itst.oiosaml.sp.service.session.SessionHandler;
import dk.itst.oiosaml.sp.service.session.SessionHandlerFactory;
import dk.itst.oiosaml.sp.service.util.Constants;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.configuration.Configuration;
import org.lockss.util.Logger;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;

public class LockssOiosamlSpFilter extends SPFilter {
  private static Logger log = Logger.getLogger("LockssOiosamlSpFilter");
  private CRLChecker crlChecker = new CRLChecker();
  private boolean filterInitialized;
  private SAMLConfiguration conf;
  private String hostname;
  private SessionHandlerFactory sessionHandlerFactory;
  private AtomicBoolean cleanerRunning = new AtomicBoolean(false);
  private DevelMode develMode;

  /**
   * Static initializer for bootstrapping OpenSAML.
   */
  static {
    try {
      DefaultBootstrap.bootstrap();
    } catch (ConfigurationException e) {
      throw new WrappedException(Layer.DATAACCESS, e);
    }
  }

  public void destroy() {
    SessionCleaner.stopCleaner();
    crlChecker.stopChecker();
    if (sessionHandlerFactory != null) {
      sessionHandlerFactory.close();
    }
    SessionHandlerFactory.Factory.close();
  }

  /**
   * Check whether the user is authenticated i.e. having session with a valid
   * assertion. If the user is not authenticated an &lt;AuthnRequest&gt; is sent
   * to the Login Site.
   * 
   * @param request
   *          The servletRequest
   * @param response
   *          The servletResponse
   */
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    final String DEBUG_HEADER = "doFilter(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (!(request instanceof HttpServletRequest)) {
      throw new RuntimeException("Not supported operation...");
    }

    HttpServletRequest servletRequest = ((HttpServletRequest) request);
    Audit.init(servletRequest);

    if (!isFilterInitialized()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "isFilterInitialized() = false");

      try {
	Configuration conf = SAMLConfigurationFactory.getConfiguration()
	    .getSystemConfiguration();
	setRuntimeConfiguration(conf);
      } catch (IllegalStateException e) {
	request.getRequestDispatcher("/saml/configure").forward(request,
	    response);
	if (log.isDebug2()) {
	  log.debug2(DEBUG_HEADER + "Forwarded to /saml/configure.");
	  log.debug2(DEBUG_HEADER + "Done.");
	}
	return;
      }
    } else {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "isFilterInitialized() = true");
    }

    if (conf.getSystemConfiguration().getBoolean(Constants.PROP_DEVEL_MODE,
	false)) {
      log.warning("Running in developer mode, skipped regular filter.");
      develMode.doFilter(servletRequest, (HttpServletResponse) response, chain,
	  conf.getSystemConfiguration());
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    if (cleanerRunning.compareAndSet(false, true)) {
      int maxInactiveInterval =
	  ((HttpServletRequest) request).getSession().getMaxInactiveInterval();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "maxInactiveInterval = "
	  + maxInactiveInterval);

      if (maxInactiveInterval < 0) {
	maxInactiveInterval = 3600;
      }

      SessionCleaner.startCleaner(sessionHandlerFactory.getHandler(),
	  maxInactiveInterval, 30);
    }

    SessionHandler sessionHandler = sessionHandlerFactory.getHandler();

    if (servletRequest.getServletPath().equals(conf.getSystemConfiguration()
	.getProperty(Constants.PROP_SAML_SERVLET))) {
      if (log.isDebug())
	log.debug(DEBUG_HEADER + "Request to SAML servlet, access granted");
      chain.doFilter(
	  new SAMLHttpServletRequest(servletRequest, hostname, null), response);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return;
    }

    final HttpSession session = servletRequest.getSession();
    if (log.isDebug())
      log.debug(DEBUG_HEADER + "session.getId() = " + session.getId());

    Boolean forceAuthn = false;
    if (request.getParameterMap()
	.containsKey(Constants.QUERY_STRING_FORCE_AUTHN)) {
      String forceAuthnAsString =
	  request.getParameter(Constants.QUERY_STRING_FORCE_AUTHN);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "forceAuthnAsString = " + forceAuthnAsString);

      forceAuthn = forceAuthnAsString.toLowerCase().equals("true");
    }

    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "forceAuthn = " + forceAuthn);

    // Is the user logged in?
    if (sessionHandler.isLoggedIn(session.getId())
	&& session.getAttribute(Constants.SESSION_USER_ASSERTION) != null
	&& !forceAuthn) {
      int actualAssuranceLevel = sessionHandler.getAssertion(session.getId())
	  .getAssuranceLevel();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "actualAssuranceLevel = "
	  + actualAssuranceLevel);

      int assuranceLevel = conf.getSystemConfiguration().getInt(
	  Constants.PROP_ASSURANCE_LEVEL);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "assuranceLevel = " + assuranceLevel);

      if (actualAssuranceLevel > 0 && actualAssuranceLevel < assuranceLevel) {
	sessionHandler.logOut(session);
	log.warning("Assurance level too low: " + actualAssuranceLevel
	    + ", required: " + assuranceLevel);
	throw new RuntimeException("Assurance level too low: "
	    + actualAssuranceLevel + ", required: " + assuranceLevel);
      }

      UserAssertion ua = (UserAssertion) session
	  .getAttribute(Constants.SESSION_USER_ASSERTION);
      if (log.isDebug()) {
	log.debug(DEBUG_HEADER + "Everything is OK.");
	log.debug(DEBUG_HEADER + "Subject: " + ua.getSubject());
	log.debug(DEBUG_HEADER + "NameID Format: " + ua.getNameIDFormat());
	log.debug(DEBUG_HEADER + "Common Name: " + ua.getCommonName());
	log.debug(DEBUG_HEADER + "UserId: " + ua.getUserId());
	log.debug(DEBUG_HEADER + "Authenticated?: " + ua.isAuthenticated());
	log.debug(DEBUG_HEADER + "Signed?: " + ua.isSigned());
      }

      Audit.log(Operation.ACCESS, servletRequest.getRequestURI());

      try {
	UserAssertionHolder.set(ua);
	HttpServletRequestWrapper requestWrap = new SAMLHttpServletRequest(
	    servletRequest, ua, hostname);
	chain.doFilter(requestWrap, response);
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
	return;
      } finally {
	UserAssertionHolder.set(null);
      }
    } else {
      session.removeAttribute(Constants.SESSION_USER_ASSERTION);
      UserAssertionHolder.set(null);
      saveRequestAndGotoLogin((HttpServletResponse) response, servletRequest);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  protected void saveRequestAndGotoLogin(HttpServletResponse response,
      HttpServletRequest request) throws ServletException, IOException {
    final String DEBUG_HEADER = "saveRequestAndGotoLogin(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    SessionHandler sessionHandler = sessionHandlerFactory.getHandler();
    String relayState =
	sessionHandler.saveRequest(Request.fromHttpRequest(request));
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "relayState = " + relayState);

    String protocol = conf.getSystemConfiguration()
	.getString(Constants.PROP_PROTOCOL, "saml20");
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "protocol = " + protocol);

    String loginUrl = conf.getSystemConfiguration()
	.getString(Constants.PROP_SAML_SERVLET, "/saml");
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "loginUrl = " + loginUrl);

    String protocolUrl = conf.getSystemConfiguration()
	.getString(Constants.PROP_PROTOCOL + "." + protocol);
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "protocolUrl = " + protocolUrl);

    if (protocolUrl == null) {
      throw new RuntimeException("No protocol url configured for "
	  + Constants.PROP_PROTOCOL + "." + protocol);
    }

    loginUrl += protocolUrl;

    if (log.isDebug())
      log.debug("Redirected to " + protocol + " login handler at " + loginUrl);

    RequestDispatcher dispatch = request.getRequestDispatcher(loginUrl);
    dispatch.forward(new SAMLHttpServletRequest(request, hostname, relayState),
	response);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  public void init(FilterConfig filterConfig) throws ServletException {
    conf = SAMLConfigurationFactory.getConfiguration();

    if (conf.isConfigured()) {
      try {
	Configuration conf = SAMLConfigurationFactory.getConfiguration()
	    .getSystemConfiguration();
	if (conf.getBoolean(Constants.PROP_DEVEL_MODE, false)) {
	  develMode = new DevelModeImpl();
	  setConfiguration(conf);
	  setFilterInitialized(true);
	  return;
	}
	setRuntimeConfiguration(conf);
	setFilterInitialized(true);
	return;
      } catch (IllegalStateException e) {
	log.error("Unable to configure", e);
      }
    }
    setFilterInitialized(false);
  }

  private void setRuntimeConfiguration(Configuration conf) {
    restartCRLChecker(conf);
    setFilterInitialized(true);
    setConfiguration(conf);
    if (!IdpMetadata.getInstance().enableDiscovery()) {
      log.info("Discovery profile disabled, only one metadata file found");
    } else {
      if (conf.getString(Constants.DISCOVERY_LOCATION) == null) {
	throw new IllegalStateException(
	    "Discovery location cannot be null when discovery profile is active"
	    );
      }
    }
    setHostname();
    sessionHandlerFactory = SessionHandlerFactory.Factory.newInstance(conf);
    sessionHandlerFactory.getHandler().resetReplayProtection(
	conf.getInt(Constants.PROP_NUM_TRACKED_ASSERTIONIDS));
    log.info("Home url: " + conf.getString(Constants.PROP_HOME));
    log.info("Assurance level: " + conf.getInt(Constants.PROP_ASSURANCE_LEVEL));
    log.info("SP entity ID: " + SPMetadata.getInstance().getEntityID());
    log.info("Base hostname: " + hostname);
  }

  private void setHostname() {
    final String DEBUG_HEADER = "setHostname(): ";
    String url = SPMetadata.getInstance().getDefaultAssertionConsumerService()
	.getLocation();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "url = " + url);

    setHostname(url.substring(0, url.indexOf('/', 8)));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "hostname = " + hostname);
  }

  private void restartCRLChecker(Configuration conf) {
    final String DEBUG_HEADER = "restartCRLChecker(): ";
    crlChecker.stopChecker();
    int period = conf.getInt(Constants.PROP_CRL_CHECK_PERIOD, 600);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "period = " + period);

    if (period > 0) {
      crlChecker.startChecker(period, IdpMetadata.getInstance(), conf);
    }
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setFilterInitialized(boolean b) {
    filterInitialized = b;
  }

  public boolean isFilterInitialized() {
    return filterInitialized;
  }

  public void setConfiguration(Configuration configuration) {
    SAMLConfigurationFactory.getConfiguration().setConfiguration(configuration);
    conf = SAMLConfigurationFactory.getConfiguration();
  }

  public void setSessionHandlerFactory(
      SessionHandlerFactory sessionHandlerFactory) {
    this.sessionHandlerFactory = sessionHandlerFactory;
  }

  public void setDevelMode(DevelMode develMode) {
    this.develMode = develMode;
  }
}
