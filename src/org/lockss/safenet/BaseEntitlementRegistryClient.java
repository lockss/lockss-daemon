package org.lockss.safenet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import org.lockss.app.BaseLockssManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.Configuration;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.LockssUrlConnection;

public class BaseEntitlementRegistryClient extends BaseLockssManager implements EntitlementRegistryClient, ConfigurableManager {

  private static final Logger log = Logger.getLogger(BaseEntitlementRegistryClient.class);

  public static final String PREFIX = Configuration.PREFIX + "safenet.";
  public static final String PARAM_ER_URI = PREFIX + "registryUri";
  static final String DEFAULT_ER_URI = "";
  public static final String PARAM_ER_APIKEY = PREFIX + "apiKey";
  static final String DEFAULT_ER_APIKEY = "";
  private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  private ObjectMapper objectMapper;
  private String erUri;
  private String apiKey;

  public BaseEntitlementRegistryClient() {
    this.objectMapper = new ObjectMapper();
  }

  public void setConfig(Configuration config, Configuration oldConfig, Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      erUri = config.get(PARAM_ER_URI, DEFAULT_ER_URI);
      apiKey = config.get(PARAM_ER_APIKEY, DEFAULT_ER_APIKEY);
    }
  }

  public boolean isUserEntitled(String issn, String institution, String start, String end) throws IOException {
      JsonNode entitlement = this.findMatchingEntitlement(issn, institution, start, end);
      return entitlement != null;
  }

  private JsonNode findMatchingEntitlement(String issn, String institution, String start, String end) throws IOException {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("identifier_value", issn);
    parameters.put("institution", institution);
    parameters.put("start", start);
    parameters.put("end", end);
    parameters.put("validate", "1");

    JsonNode entitlements = callEntitlementRegistry("/entitlements", parameters);
    if (entitlements != null) {
      for(JsonNode entitlement : entitlements) {
        JsonNode entitlementInstitution = entitlement.get("institution");
        log.debug("Checking entitlement " + entitlement.toString());
        if (entitlementInstitution != null && entitlementInstitution.asText().endsWith(institution + "/")) {
          log.warning("TODO: Verify title and dates");
          return entitlement;
        }
      }

      // Valid request, but the entitlements don't match the information we passed, which should never happen
      log.error("Entitlements returned from entitlement registry do not match passed parameters");
    }

    //Valid request, no entitlements found
    return null;
  }

  private Date extractDate(String value) throws IOException {
      if ( value == null || value.equals("null")) {
          return null;
      }
      try {
          return dateFormat.parse(value);
      }
      catch ( ParseException e ) {
          throw new IOException("Could not parse date " + value);
      }
  }
  private Date extractDate(JsonNode node, String key) throws IOException {
      JsonNode value = node.get(key);
      if ( value == null ) {
          return null;
      }
      return extractDate(value.asText());
  }

  public String getPublisher(String issn, String institution, String start, String end) throws IOException {
    JsonNode entitlement = this.findMatchingEntitlement(issn, institution, start, end);
    if ( entitlement == null ) {
        return null;
    }

    String url = entitlement.get("publisher").asText();
    String[] parts = url.split("/");
    return parts[parts.length - 1];
  }

  public PublisherWorkflow getPublisherWorkflow(String publisherGuid) throws IOException {
    Map<String, String> parameters = new HashMap<String, String>();
    JsonNode publisher = callEntitlementRegistry("/publishers/"+publisherGuid, parameters);
    if (publisher != null) {
      JsonNode foundGuid = publisher.get("guid");
      if (foundGuid != null && foundGuid.asText().equals(publisherGuid)) {
        JsonNode foundWorkflow = publisher.get("workflow");
        if(foundWorkflow != null) {
          try {
            return Enum.valueOf(PublisherWorkflow.class, foundWorkflow.asText().toUpperCase());
          }
          catch (IllegalArgumentException e) {
            // Valid request, but workflow didn't match ones we've implemented, which should never happen
            throw new IOException("No valid workflow returned from entitlement registry: " + foundWorkflow.asText().toUpperCase());
          }
        }
        else {
            log.warning("No workflow set for publisher, defaulting to PRIMARY_SAFENET");
            return PublisherWorkflow.PRIMARY_SAFENET;
        }
      }
    }
    // Valid request, but no valid workflow information was returned, which should never happen
    throw new IOException("No valid workflow returned from entitlement registry");
  }

  public String getInstitution(String scope) throws IOException {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("scope", scope);

    JsonNode institutions = callEntitlementRegistry("/institutions", parameters);
    if (institutions != null) {
      if (institutions.size() == 0) {
        throw new IOException("No matching institutions returned from entitlement registry");
      }
      if (institutions.size() > 1) {
        throw new IOException("Multiple matching institutions returned from entitlement registry");
      }
      JsonNode institution = institutions.get(0);
      if (!scope.equals(institution.get("scope").asText())) {
        throw new IOException("No matching institutions returned from entitlement registry");
      }
      return institution.get("guid").asText();
    }
    throw new IOException("No matching institutions returned from entitlement registry");
  }

  private JsonNode callEntitlementRegistry(String endpoint, Map<String, String> parameters) throws IOException {
    return callEntitlementRegistry(endpoint, mapToPairs(parameters));
  }

  private JsonNode callEntitlementRegistry(String endpoint, List<NameValuePair> parameters) throws IOException {
    LockssUrlConnection connection = null;
    try {
      URIBuilder builder = new URIBuilder(erUri);
      builder.setPath(builder.getPath() + endpoint);
      if(!parameters.isEmpty()) {
        builder.setParameters(parameters);
      }

      String url = builder.toString();
      log.debug("Connecting to ER at " + url);
      connection = openConnection(url);
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Authorization", "Token " + apiKey);
      connection.execute();
      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        return objectMapper.readTree(connection.getResponseInputStream());
      }
      else if (responseCode == 204) {
        // Valid request, but empty response
        return null;
      }
      else {
        throw new IOException("Error communicating with entitlement registry. Response was " + responseCode + " " + connection.getResponseMessage());
      }
    }
    catch (URISyntaxException e) {
      throw new IOException("Couldn't contact entitlement registry", e);
    }
    finally {
      if(connection != null) {
        IOUtil.safeRelease(connection);
      }
    }
  }

  // protected so that it can be overriden with mock connections in tests
  protected LockssUrlConnection openConnection(String url) throws IOException {
    return UrlUtil.openConnection(url);
  }

  protected static List<NameValuePair> mapToPairs(Map<String, String> params) {
    List<NameValuePair> pairs = new ArrayList<NameValuePair>();
    for(String key : params.keySet()) {
      pairs.add(new BasicNameValuePair(key, params.get(key)));
    }
    return pairs;
  }

}
