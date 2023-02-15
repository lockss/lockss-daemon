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

package org.lockss.plugin.taylorandfrancis;

import org.lockss.test.LockssTestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Properties;

public class TestTafLoginPageChecker extends LockssTestCase {

  private boolean isLogin(String inString) throws Exception {
    // constructs props and
    Properties props = new Properties();
    props.setProperty("Content-Type", "text/html");
    TafLoginPageChecker lpc = new TafLoginPageChecker();
    return lpc.isLoginPage(
        props,
        new BufferedReader(
            new StringReader(inString)
        )
    );
  }

  private static final String accessDenialWidget =
    // frankensteined snippet from new theme
    "<div class=\"widget-body body body-none body-compact-all\">\n" +
      "<!-- // hide it from old browsers -->" +
        "<!-- ommitted content -->" +
      "<div id=\"accessDenialWidget\">\n" +
        "<div class=\"left-col\">\n" +
          "<div class=\"sso-links\">\n" +
            "<h3>Log in via your institution</h3>\n" +
            "<a href=\"/action/ssostart?federationSelect=All&amp;redirectUri=%2Fdoi%2Ffull%2F10.1080%2F03932729.2015.1085946\">Shibboleth</a>\n" +
            "<a href=\"/action/ssostart?idp=https%3A%2F%2Fidp.eduserv.org.uk%2Fopenathens&amp;redirectUri=%2Fdoi%2Ffull%2F10.1080%2F03932729.2015.1085946\">OpenAthens</a>\n" +
          "</div>\n" +
          "<div class=\"login-form\">\n" +
            "<h3>Log in to Taylor &amp; Francis Online</h3>\n" +
            "<div class=\"login\">\n" +
              "<div class=\"loginForm\">\n" +
                "<h4 class=\"accessDenialLoginHeader\"></h4>\n" +
                "<p class=\"note\"></p>\n" +
                "<form action=\"https://www.tandfonline.com/action/doLogin\" class=\"note denialLogin\" method=\"post\" name=\"frmLogin\"><input name=\"loginUri\" type=\"hidden\" value=\"/doi/full/10.1080/03932729.2015.1085946\">\n" +
                  "<input name=\"redirectUri\" type=\"hidden\" value=\"/doi/full/10.1080/03932729.2015.1085946\">\n" +
                  "<label for=\"login\">\n" +
                    "<span class=\"off-screen\">Username</span>\n" +
                    "<input class=\"textInput\" id=\"login\" name=\"login\" placeholder=\"Enter your email\" size=\"15\" type=\"text\" value=\"\">\n" +
                  "</label>\n" +
                  "<label for=\"password\">\n" +
                    "<span class=\"off-screen\">Password</span>\n" +
                    "<input autocomplete=\"off\" class=\"textInput\" id=\"password\" name=\"password\" placeholder=\"Enter your password\" type=\"password\" value=\"\">\n" +
                  "</label>\n" +
                  "<div class=\"passwordReminder\">\n" +
                    "<a href=\"https://www.tandfonline.com/action/requestResetPassword?redirectUri=%2Fdoi%2Ffull%2F10.1080%2F03932729.2015.1085946\">Forgot password?</a>\n" +
                  "</div>\n" +
                  "<label class=\"save-password\" for=\"savePassword\">\n" +
                    "<input id=\"savePassword\" name=\"savePassword\" type=\"checkbox\" value=\"1\">\n" +
                    "<span>Remember Me</span>\n" +
                  "</label>\n" +
                  "<a class=\"submit-login\" href=\"#\">Log in</a>\n" +
                  "<input class=\"ecommLoginSigninButton\" name=\"submit\" type=\"submit\" value=\"Log in\"/>\n" +
                "</form>\n" +
              "</div>\n" +
            "<div>\n" +
          "</div>\n" +
          "<div class=\"options-links\">\n" +
            "<div id=\"openAthensSignIn\">\n" +
              "<a href=\"/action/ssostart?idp=https%3A%2F%2Fidp.eduserv.org.uk%2Fopenathens&amp;redirectUri=https%3A%2F%2Fwww.tandfonline.com%2Fdoi%2Ffull%2F10.1080%2F03932729.2015.1085946\">OpenAthens</a>\n" +
            "</div>\n" +
            "<span id=\"institutionLogin\">\n" +
              "<a href=\"/action/ssostart?redirectUri=%2Fdoi%2Ffull%2F10.1080%2F03932729.2015.1085946\">Shibboleth</a>\n" +
            "</span>\n" +
            "<div class=\"deepDyve\">\n" +
            "</div>\n" +
          "</div>\n" +
        "</div>\n" +
      "</div>\n" +
    "</div>\n" +
    "<!-- ommitted content -->" +
  "</div>\n" +
  "<!-- ommitted content -->";

  private static final String accessDenied =
     // frankensteined snippet from old theme
    "<body class=\"script\">\n" +
      "<script type=\"text/javascript\">document.body.className=\"script\";</script>\n" +
      "<div class=\"accessDenied pageArticle\" id=\"doc\">\n" +
        "<div id=\"hd\">\n" +
          "<div class=\"section clear\">" +
            "<!-- ommitted content -->" +
          "</div>\n" +
        "</div>\n" +
      "</div>\n" +
    "<!-- ommitted content -->" +
    "</body>";

  public void testAccessDenialWidget() throws Exception {
    assertTrue(isLogin(accessDenialWidget));
  }

  public void testAccessDenied() throws Exception {
    assertTrue(isLogin(accessDenied));
  }
}

