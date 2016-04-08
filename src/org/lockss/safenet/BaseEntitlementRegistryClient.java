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
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("api_key", apiKey);
    parameters.put("identifier_value", issn);
    parameters.put("institution", institution);
    parameters.put("start", start);
    parameters.put("end", end);

    JsonNode entitlements = callEntitlementRegistry("/entitlements", parameters);
    if (entitlements != null) {
      for(JsonNode entitlement : entitlements) {
        JsonNode entitlementInstitution = entitlement.get("institution");
        if (entitlementInstitution != null && entitlementInstitution.asText().equals(institution)) {
          log.warning("TODO: Verify title and dates");
          return true;
        }
      }

      // Valid request, but the entitlements don't match the information we passed, which should never happen
      throw new IOException("No matching entitlements returned from entitlement registry");
    }

    //Valid request, no entitlements found
    return false;
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

  public String getPublisher(String issn, String start, String end) throws IOException {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("identifier", issn);
    Date startDate = extractDate(start);
    Date endDate = extractDate(end);
    JsonNode titles = callEntitlementRegistry("/titles", parameters);
    if (titles != null) {
      List<String> foundPublishers = new ArrayList<String>();
      for(JsonNode title : titles) {
        JsonNode publishers = title.get("publishers");
        for(JsonNode publisher : publishers) {
          Date foundStartDate = extractDate(publisher, "start");
          Date foundEndDate = extractDate(publisher, "end");

          if ( foundStartDate != null && ( startDate == null || foundStartDate.after(startDate) ) ) {
              continue;
          }
          if ( foundEndDate != null && ( endDate == null || foundEndDate.before(endDate) ) ) {
              continue;
          }
          foundPublishers.add(publisher.get("id").asText());
        }
      }
      if (foundPublishers.size() > 1) {
        // Valid request, but there are multiple publishers for the date range, which should never happen
        throw new IOException("Multiple matching publishers returned from entitlement registry");
      }
      if (foundPublishers.size() == 1) {
          return foundPublishers.get(0);
      }
    }
    // Valid request, no publisher found
    return null;
  }

  public PublisherWorkflow getPublisherWorkflow(String publisherGuid) throws IOException {
    Map<String, String> parameters = new HashMap<String, String>();
    JsonNode publisher = callEntitlementRegistry("/publishers/"+publisherGuid, parameters);
    if (publisher != null) {
      JsonNode foundGuid = publisher.get("id");
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
      return institution.get("id").asText();
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
