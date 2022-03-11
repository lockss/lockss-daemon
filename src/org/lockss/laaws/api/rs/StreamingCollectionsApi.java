package org.lockss.laaws.api.rs;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.laaws.V2AuMover.DigestCachedUrl;
import org.lockss.laaws.client.ApiCallback;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.client.ApiResponse;
import org.lockss.laaws.client.Configuration;
import org.lockss.laaws.client.Pair;
import org.lockss.laaws.client.V2RestClient;
import org.lockss.laaws.model.rs.Artifact;

public class StreamingCollectionsApi extends CollectionsApi {

  private V2RestClient apiClient;

  public StreamingCollectionsApi() {
    super(Configuration.getDefaultApiClient());
  }

  public StreamingCollectionsApi(V2RestClient apiClient) {
    super(apiClient);
    this.apiClient = apiClient;
  }

  public void setApiClient(V2RestClient apiClient) {
    super.setApiClient(apiClient);
    this.apiClient = apiClient;
  }

  /**
   * Build call for createArtifact
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param auid           Archival Unit ID (AUID) of new artifact (required)
   * @param uri            URI represented by this artifact (required)
   * @param artifact       Artifact data (required)
   * @param collectionDate Artifact collection/crawl date (milliseconds since epoch; UTC) (optional)
   * @param _callback      Callback for upload/download progress
   * @return Call to execute
   * @throws ApiException If fail to serialize the request body object
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 201 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 302 </td><td> Duplicate content; artifact not created </td><td>  * Location - Repository query URL to duplicate artifacts <br>  </td></tr>
   * <tr><td> 400 </td><td> Invalid input </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to create artifacts </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Internal error creating artifact </td><td>  -  </td></tr>
   * </table>
   */
  public okhttp3.Call createArtifactCall(String collectionid, String auid, String uri,
    DigestCachedUrl artifact, Long collectionDate, final ApiCallback _callback) throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/collections/{collectionid}/artifacts"
      .replaceAll("\\{" + "collectionid" + "\\}", apiClient.escapeString(collectionid));

    List<Pair> localVarQueryParams = new ArrayList<>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    if (auid != null) {
      localVarFormParams.put("auid", auid);
    }

    if (uri != null) {
      localVarFormParams.put("uri", uri);
    }

    if (collectionDate != null) {
      localVarFormParams.put("collectionDate", collectionDate);
    }

    if (artifact != null) {
      localVarFormParams.put("artifact", artifact);
    }

    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {
      "multipart/form-data"
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{ "basicAuth" };
    return apiClient.buildCall(localVarPath, "POST", localVarQueryParams,
      localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams,
      localVarFormParams, localVarAuthNames, _callback);
  }

  @SuppressWarnings("rawtypes")
  private okhttp3.Call createArtifactValidateBeforeCall(String collectionid, String auid,
    String uri, DigestCachedUrl artifact, Long collectionDate, final ApiCallback _callback)
    throws ApiException {

    // verify the required parameter 'collectionid' is set
    if (collectionid == null) {
      throw new ApiException(
        "Missing the required parameter 'collectionid' when calling createArtifact(Async)");
    }

    // verify the required parameter 'auid' is set
    if (auid == null) {
      throw new ApiException(
        "Missing the required parameter 'auid' when calling createArtifact(Async)");
    }

    // verify the required parameter 'uri' is set
    if (uri == null) {
      throw new ApiException(
        "Missing the required parameter 'uri' when calling createArtifact(Async)");
    }

    // verify the required parameter 'artifact' is set
    if (artifact == null) {
      throw new ApiException(
        "Missing the required parameter 'artifact' when calling createArtifact(Async)");
    }

    okhttp3.Call localVarCall = createArtifactCall(collectionid, auid, uri, artifact,
      collectionDate, _callback);
    return localVarCall;

  }

  /**
   * Create an artifact
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param auid           Archival Unit ID (AUID) of new artifact (required)
   * @param uri            URI represented by this artifact (required)
   * @param artifact       Artifact data (required)
   * @param collectionDate Artifact collection/crawl date (milliseconds since epoch; UTC) (optional)
   * @return Artifact
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 201 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 302 </td><td> Duplicate content; artifact not created </td><td>  * Location - Repository query URL to duplicate artifacts <br>  </td></tr>
   * <tr><td> 400 </td><td> Invalid input </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to create artifacts </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Internal error creating artifact </td><td>  -  </td></tr>
   * </table>
   */
  public Artifact createArtifact(String collectionid, String auid, String uri, DigestCachedUrl artifact,
    Long collectionDate) throws ApiException {
    ApiResponse<Artifact> localVarResp = createArtifactWithHttpInfo(collectionid, auid, uri,
      artifact, collectionDate);
    return localVarResp.getData();
  }

  /**
   * Create an artifact
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param auid           Archival Unit ID (AUID) of new artifact (required)
   * @param uri            URI represented by this artifact (required)
   * @param artifact       Artifact data (required)
   * @param collectionDate Artifact collection/crawl date (milliseconds since epoch; UTC) (optional)
   * @return ApiResponse&lt;Artifact&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 201 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 302 </td><td> Duplicate content; artifact not created </td><td>  * Location - Repository query URL to duplicate artifacts <br>  </td></tr>
   * <tr><td> 400 </td><td> Invalid input </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to create artifacts </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Internal error creating artifact </td><td>  -  </td></tr>
   * </table>
   */
  public ApiResponse<Artifact> createArtifactWithHttpInfo(String collectionid, String auid,
    String uri, DigestCachedUrl artifact, Long collectionDate) throws ApiException {
    okhttp3.Call localVarCall = createArtifactValidateBeforeCall(collectionid, auid, uri, artifact,
      collectionDate, null);
    Type localVarReturnType = new TypeToken<Artifact>() {
    }.getType();
    return apiClient.execute(localVarCall, localVarReturnType);
  }

  /**
   * Create an artifact (asynchronously)
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param auid           Archival Unit ID (AUID) of new artifact (required)
   * @param uri            URI represented by this artifact (required)
   * @param artifact       Artifact data (required)
   * @param collectionDate Artifact collection/crawl date (milliseconds since epoch; UTC) (optional)
   * @param _callback      The callback to be executed when the API call finishes
   * @return The request call
   * @throws ApiException If fail to process the API call, e.g. serializing the request body object
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 201 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 302 </td><td> Duplicate content; artifact not created </td><td>  * Location - Repository query URL to duplicate artifacts <br>  </td></tr>
   * <tr><td> 400 </td><td> Invalid input </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to create artifacts </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Internal error creating artifact </td><td>  -  </td></tr>
   * </table>
   */
  public okhttp3.Call createArtifactAsync(String collectionid, String auid, String uri,
    DigestCachedUrl artifact, Long collectionDate, final ApiCallback<Artifact> _callback)
    throws ApiException {

    okhttp3.Call localVarCall = createArtifactValidateBeforeCall(collectionid, auid, uri, artifact,
      collectionDate, _callback);
    Type localVarReturnType = new TypeToken<Artifact>() {
    }.getType();
    apiClient.executeAsync(localVarCall, localVarReturnType, _callback);
    return localVarCall;
  }

  /**
   * Build call for getArtifact
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param artifactid     Identifier of the artifact (required)
   * @param includeContent Controls whether to include the artifact content part in multipart response (optional, default to ALWAYS)
   * @param _callback      Callback for upload/download progress
   * @return Call to execute
   * @throws ApiException If fail to serialize the request body object
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 200 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to retrieve artifact </td><td>  -  </td></tr>
   * <tr><td> 404 </td><td> Artifact not found </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Could not read from external resource </td><td>  -  </td></tr>
   * </table>
   */
  public okhttp3.Call getArtifactCall(String collectionid, String artifactid, String includeContent,
      final ApiCallback _callback) throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/collections/{collectionid}/artifacts/{artifactid}"
        .replaceAll("\\{" + "collectionid" + "\\}",
            apiClient.escapeString(collectionid.toString()))
        .replaceAll("\\{" + "artifactid" + "\\}",
            apiClient.escapeString(artifactid.toString()));

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    if (includeContent != null) {
      localVarQueryParams.addAll(
          apiClient.parameterToPair("includeContent", includeContent));
    }

    final String[] localVarAccepts = {
        "multipart/form-data"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(
        localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"basicAuth"};
    return apiClient.buildCall(localVarPath, "GET", localVarQueryParams,
        localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams,
        localVarFormParams, localVarAuthNames, _callback);
  }

  @SuppressWarnings("rawtypes")
  private okhttp3.Call getArtifactValidateBeforeCall(String collectionid, String artifactid,
      String includeContent, final ApiCallback _callback) throws ApiException {

    // verify the required parameter 'collectionid' is set
    if (collectionid == null) {
      throw new ApiException(
          "Missing the required parameter 'collectionid' when calling getArtifact(Async)");
    }

    // verify the required parameter 'artifactid' is set
    if (artifactid == null) {
      throw new ApiException(
          "Missing the required parameter 'artifactid' when calling getArtifact(Async)");
    }

    okhttp3.Call localVarCall = getArtifactCall(collectionid, artifactid, includeContent,
        _callback);
    return localVarCall;

  }

  /**
   * Get artifact content and metadata
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param artifactid     Identifier of the artifact (required)
   * @param includeContent Controls whether to include the artifact content part in multipart response (optional, default to ALWAYS)
   * @return File
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 200 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to retrieve artifact </td><td>  -  </td></tr>
   * <tr><td> 404 </td><td> Artifact not found </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Could not read from external resource </td><td>  -  </td></tr>
   * </table>
   */
  public File getArtifact(String collectionid, String artifactid, String includeContent)
      throws ApiException {
    ApiResponse<File> localVarResp = getArtifactWithHttpInfo(collectionid, artifactid,
        includeContent);
    return localVarResp.getData();
  }

  /**
   * Get artifact content and metadata
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param artifactid     Identifier of the artifact (required)
   * @param includeContent Controls whether to include the artifact content part in multipart response (optional, default to ALWAYS)
   * @return ApiResponse&lt;File&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 200 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to retrieve artifact </td><td>  -  </td></tr>
   * <tr><td> 404 </td><td> Artifact not found </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Could not read from external resource </td><td>  -  </td></tr>
   * </table>
   */
  public ApiResponse<File> getArtifactWithHttpInfo(String collectionid, String artifactid,
      String includeContent) throws ApiException {
    okhttp3.Call localVarCall = getArtifactValidateBeforeCall(collectionid, artifactid,
        includeContent, null);
    Type localVarReturnType = new TypeToken<File>() {
    }.getType();
    return apiClient.execute(localVarCall, localVarReturnType);
  }

  /**
   * Get artifact content and metadata (asynchronously)
   *
   * @param collectionid   Collection containing the artifact (required)
   * @param artifactid     Identifier of the artifact (required)
   * @param includeContent Controls whether to include the artifact content part in multipart response (optional, default to ALWAYS)
   * @param _callback      The callback to be executed when the API call finishes
   * @return The request call
   * @throws ApiException If fail to process the API call, e.g. serializing the request body object
   * @http.response.details <table summary="Response Details" border="1">
   * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
   * <tr><td> 200 </td><td> Artifact created </td><td>  -  </td></tr>
   * <tr><td> 401 </td><td> Unauthorized request </td><td>  -  </td></tr>
   * <tr><td> 403 </td><td> Client not authorized to retrieve artifact </td><td>  -  </td></tr>
   * <tr><td> 404 </td><td> Artifact not found </td><td>  -  </td></tr>
   * <tr><td> 502 </td><td> Could not read from external resource </td><td>  -  </td></tr>
   * </table>
   */
  public okhttp3.Call getArtifactAsync(String collectionid, String artifactid,
      String includeContent, final ApiCallback<File> _callback) throws ApiException {

    okhttp3.Call localVarCall = getArtifactValidateBeforeCall(collectionid, artifactid,
        includeContent, _callback);
    Type localVarReturnType = new TypeToken<File>() {
    }.getType();
    apiClient.executeAsync(localVarCall, localVarReturnType, _callback);
    return localVarCall;
  }

}
