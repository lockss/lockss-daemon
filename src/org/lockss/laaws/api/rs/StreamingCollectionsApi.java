package org.lockss.laaws.api.rs;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.plugin.CachedUrl;
import org.lockss.laaws.client.ApiCallback;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.client.ApiResponse;
import org.lockss.laaws.client.Pair;
import org.lockss.laaws.client.ProgressRequestBody;
import org.lockss.laaws.client.ProgressResponseBody;
import org.lockss.laaws.client.V2RestClient;
import org.lockss.laaws.client.RestRepoConfiguration;
import org.lockss.laaws.model.rs.Artifact;

public class StreamingCollectionsApi extends CollectionsApi {
  private V2RestClient apiClient;

  public StreamingCollectionsApi() {
    super(RestRepoConfiguration.getDefaultApiClient());
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
   * @param auid  (required)
   * @param uri  (required)
   * @param collectionDate  (required)
   * @param artifact  (required)
   * @param collectionid Collection containing the artifact (required)
   * @param progressListener Progress listener
   * @param progressRequestListener Progress request listener
   * @return Call to execute
   * @throws ApiException If fail to serialize the request body object
   */
  public com.squareup.okhttp.Call createArtifactCall(String auid, String uri, Long collectionDate, CachedUrl artifact, String collectionid, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/collections/{collectionid}/artifacts"
        .replaceAll("\\{" + "collectionid" + "\\}", apiClient.escapeString(collectionid));

    List<Pair> localVarQueryParams = new ArrayList<>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<>();

    Map<String, String> localVarHeaderParams = new HashMap<>();

    Map<String, Object> localVarFormParams = new HashMap<>();
    if (auid != null)
      localVarFormParams.put("auid", auid);
    if (uri != null)
      localVarFormParams.put("uri", uri);
    if (collectionDate != null)
      localVarFormParams.put("collectionDate", collectionDate);
    if (artifact != null) {
      localVarFormParams.put("artifact", artifact);
    }
    final String[] localVarAccepts = {
        "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

    final String[] localVarContentTypes = {
        "multipart/form-data"
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    if(progressListener != null) {
      apiClient.getHttpClient().networkInterceptors().add(chain -> {
        com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder()
            .body(new ProgressResponseBody(originalResponse.body(), progressListener))
            .build();
      });
    }

    String[] localVarAuthNames = new String[] { "basicAuth" };
    return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
  }

  private com.squareup.okhttp.Call createArtifactValidateBeforeCall(String auid, String uri, Long collectionDate, CachedUrl artifact, String collectionid, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
    // verify the required parameter 'auid' is set
    if (auid == null) {
      throw new ApiException("Missing the required parameter 'auid' when calling createArtifact(Async)");
    }
    // verify the required parameter 'uri' is set
    if (uri == null) {
      throw new ApiException("Missing the required parameter 'uri' when calling createArtifact(Async)");
    }
    // verify the required parameter 'collectionDate' is set
    if (collectionDate == null) {
      throw new ApiException("Missing the required parameter 'collectionDate' when calling createArtifact(Async)");
    }
    // verify the required parameter 'artifact' is set
    if (artifact == null) {
      throw new ApiException("Missing the required parameter 'artifact' when calling createArtifact(Async)");
    }
    // verify the required parameter 'collectionid' is set
    if (collectionid == null) {
      throw new ApiException("Missing the required parameter 'collectionid' when calling createArtifact(Async)");
    }

    com.squareup.okhttp.Call call = createArtifactCall(auid, uri, collectionDate, artifact, collectionid, progressListener, progressRequestListener);
    return call;
  }

  /**
   * Create an artifact
   *
   * @param auid  (required)
   * @param uri  (required)
   * @param collectionDate  (required)
   * @param artifact  (required)
   * @param collectionid Collection containing the artifact (required)
   * @return Artifact
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
   */
  public org.lockss.laaws.model.rs.Artifact createArtifact(String auid, String uri, Long collectionDate, CachedUrl artifact,
        String collectionid) throws ApiException {
    ApiResponse<Artifact> resp = createArtifactWithHttpInfo(auid, uri, collectionDate, artifact, collectionid);
    return resp.getData();
  }

  /**
   * Create an artifact
   *
   * @param auid  (required)
   * @param uri  (required)
   * @param collectionDate  (required)
   * @param artifact  (required)
   * @param collectionid Collection containing the artifact (required)
   * @return ApiResponse&lt;Artifact&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
   */
  public ApiResponse<Artifact> createArtifactWithHttpInfo(String auid, String uri, Long collectionDate, CachedUrl artifact, String collectionid) throws ApiException {
    com.squareup.okhttp.Call call = createArtifactValidateBeforeCall(auid, uri, collectionDate, artifact, collectionid, null, null);
    Type localVarReturnType = new TypeToken<Artifact>(){}.getType();
    return apiClient.execute(call, localVarReturnType);
  }

  /**
   * Create an artifact (asynchronously)
   *
   * @param auid  (required)
   * @param uri  (required)
   * @param collectionDate  (required)
   * @param artifact  (required)
   * @param collectionid Collection containing the artifact (required)
   * @param callback The callback to be executed when the API call finishes
   * @return The request call
   * @throws ApiException If fail to process the API call, e.g. serializing the request body object
   */
  public com.squareup.okhttp.Call createArtifactAsync(String auid, String uri, Long collectionDate, CachedUrl artifact, String collectionid, final ApiCallback<Artifact> callback) throws ApiException {

    ProgressResponseBody.ProgressListener progressListener = null;
    ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

    if (callback != null) {
      progressListener = (bytesRead, contentLength, done) -> callback.onDownloadProgress(bytesRead, contentLength, done);

      progressRequestListener = (bytesWritten, contentLength, done) -> callback.onUploadProgress(bytesWritten, contentLength, done);
    }

    com.squareup.okhttp.Call call = createArtifactValidateBeforeCall(auid, uri, collectionDate, artifact, collectionid, progressListener, progressRequestListener);
    Type localVarReturnType = new TypeToken<Artifact>(){}.getType();
    apiClient.executeAsync(call, localVarReturnType, callback);
    return call;
  }


}
