/*
 * 2022, Board of Trustees of Leland Stanford Jr. University,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.lockss.laaws.client;

import java.util.Map;

/** Representing a Server configuration. */
public class ServerConfiguration {
  public String URL;
  public String description;
  public Map<String, ServerVariable> variables;

  /**
   * @param URL A URL to the target host.
   * @param description A description of the host designated by the URL.
   * @param variables A map between a variable name and its value. The value is used for
   *     substitution in the server's URL template.
   */
  public ServerConfiguration(
      String URL, String description, Map<String, ServerVariable> variables) {
    this.URL = URL;
    this.description = description;
    this.variables = variables;
  }

  /**
   * Format URL template using given variables.
   *
   * @param variables A map between a variable name and its value.
   * @return Formatted URL.
   */
  public String URL(Map<String, String> variables) {
    String url = this.URL;

    // go through variables and replace placeholders
    for (Map.Entry<String, ServerVariable> variable : this.variables.entrySet()) {
      String name = variable.getKey();
      ServerVariable serverVariable = variable.getValue();
      String value = serverVariable.defaultValue;

      if (variables != null && variables.containsKey(name)) {
        value = variables.get(name);
        if (serverVariable.enumValues.size() > 0 && !serverVariable.enumValues.contains(value)) {
          throw new RuntimeException(
              "The variable " + name + " in the server URL has invalid value " + value + ".");
        }
      }
      url = url.replaceAll("\\{" + name + "\\}", value);
    }
    return url;
  }

  /**
   * Format URL template using default server variables.
   *
   * @return Formatted URL.
   */
  public String URL() {
    return URL(null);
  }
}
