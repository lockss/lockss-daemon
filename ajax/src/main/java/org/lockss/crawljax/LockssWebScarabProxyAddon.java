package org.lockss.crawljax;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.crawljax.AjaxRequestResponse.IndexEntry;
import org.owasp.webscarab.httpclient.HTTPClient;
import org.owasp.webscarab.model.NamedValue;
import org.owasp.webscarab.model.Request;
import org.owasp.webscarab.model.Response;
import org.owasp.webscarab.plugin.proxy.ProxyPlugin;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * LockssWebScarabProxyAddon:  WebScarab Proxy Plugin which will
 * write the Request-Response sequence as a Json File.
 *
 */
public class LockssWebScarabProxyAddon extends ProxyPlugin {
  /**
   * The factory used for obtaining a json generator
   */
  private JsonFactory factory = new JsonFactory();
  /**
   * The location for our json files
   */
  private File fCacheDir;
  /**
   * The index of url to json file name
   */
  private List<IndexEntry> m_index = new LinkedList<>();

  /**
   * Constructor.
   */
  public LockssWebScarabProxyAddon(String cacheDir) {
    fCacheDir = new File(cacheDir);
    fCacheDir.mkdirs();
    assert (fCacheDir.exists() && fCacheDir.canWrite());
  }

  /**
   * Clear the request cache
   */
  public synchronized void  clearCache() {
    m_index.clear();
  }
  /**
   * Add request url <-> json file mapping
   * to the local index list.
   *
   */
  public synchronized void addToIndex(String url,
                                      String jsonFileName) {
    IndexEntry entry = new IndexEntry();
    entry.setUrl(url);
    entry.setFile(jsonFileName);
    m_index.add(entry);
  }

  /**
   * write the index out to the output stream.  This is a
   * simple json file of and array object of NamedValue
   * @throws IOException
   */
  public synchronized void writeIndex() throws IOException {
    File indexFile = new File(fCacheDir,
                              AjaxRequestResponse.INDEX_FILE_NAME);
    JsonGenerator generator =
        factory.createGenerator(indexFile, JsonEncoding.UTF8);
    ObjectMapper mapper = new ObjectMapper();
    generator.writeStartArray();
    for (IndexEntry entry : m_index) {
      generator.writeStartObject();
      generator.writeStringField("url", entry.getUrl());
      generator.writeStringField("file", entry.getFile());
      generator.writeEndObject();

    }
    generator.writeEndArray();
    generator.flush();
    generator.close();
  }

  /**
   * Write out the AjaxRequestResponse to a jsonFile.
   * @param request the request to write
   * @param response the response to the request
   * @param jsonFile the file to output json
   * @throws IOException if write fails.
   */
  public synchronized void writeJson(final Request request,
                                     final Response response,
                                     File jsonFile)
  throws IOException {
    JsonGenerator generator =
      factory.createGenerator(jsonFile, JsonEncoding.UTF8);
    generator.writeStartObject(); // start req-resp object
    // the Request headers
    generator.writeObjectFieldStart("request"); // start request object
    generator.writeStringField("method", request.getMethod());
    generator.writeStringField("url", request.getURL().toString());
    generator.writeStringField("version", request.getVersion());
    generator.writeArrayFieldStart("headers"); //start headers
    NamedValue[] headers = request.getHeaders();
    for (NamedValue header : headers) {
      generator.writeStartObject();
      generator.writeStringField("name", header.getName());
      generator.writeStringField("value", header.getValue());
      generator.writeEndObject();
    }
    generator.writeEndArray();  // end headers
    generator.writeEndObject(); // end request
    // the Response headers
    generator.writeObjectFieldStart("response"); //start response object
    generator.writeStringField("status", response.getStatus());
    generator.writeStringField("message", response.getMessage());
    headers = request.getHeaders();
    generator.writeArrayFieldStart("headers"); //start headers
    for (NamedValue header : headers) {
      generator.writeStartObject();
      generator.writeStringField("name", header.getName());
      generator.writeStringField("value", header.getValue());
      generator.writeEndObject();
    }
    generator.writeEndArray(); // end headers
    byte[] content = response.getContent();
    generator.writeBinaryField("content", content);
    generator.writeEndObject(); //end response
    generator.writeEndObject(); //end req-resp object
    generator.flush();
    generator.close();
  }


  /**
   * The plugin name.
   *
   * @return The plugin name.
   */
  @Override
  public String getPluginName() {
    return "LOCKSS Request/Response Cache Proxy Addon";
  }

  @Override
  public HTTPClient getProxyPlugin(HTTPClient in) {
    return new Plugin(in);
  }

  List<IndexEntry> getIndex()
  {
    return m_index;
  }
  void setIndex(List<IndexEntry> index) {
    m_index = index;
  }


  /**
   * The actual WebScarab plugin.
   */
  private class Plugin implements HTTPClient {

    private HTTPClient client;

    /**
     * Constructor.
     *
     * @param in HTTPClient
     */
    public Plugin(HTTPClient in) {
      this.client = in;
    }

    /**
     * Buffer the request.
     *
     * @param request The incoming request.
     *
     * @return The response.
     *
     * @throws java.io.IOException on read write error.
     */
    @Override
    public Response fetchResponse(Request request) throws IOException {
      File jsonFile = new File(fCacheDir, m_index.size() + ".json");
      addToIndex(request.getURL().toString(), jsonFile.getAbsolutePath());
      Response response = this.client.fetchResponse(request);
      // fetch response throws on error so no need to check response == null
      writeJson(request, response, jsonFile);
      return response;
    }
  }
}
