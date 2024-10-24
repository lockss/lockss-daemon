/*
 * Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * LOCKSS Repository Service REST API
 * REST API of the LOCKSS Repository Service
 *
 * The version of the OpenAPI document: 2.0.0
 * Contact: lockss-support@lockss.org
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package org.lockss.laaws.api.rs;

import com.google.gson.reflect.TypeToken;
import org.lockss.laaws.client.*;
import org.lockss.laaws.model.rs.RepositoryInfo;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoApi {
  private V2RestClient apiClient;
  private int localHostIndex;
  private String localCustomBaseUrl;

  public RepoApi() {
    this(Configuration.getDefaultApiClient());
  }

  public RepoApi(V2RestClient apiClient) {
    this.apiClient = apiClient;
  }

  public V2RestClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(V2RestClient apiClient) {
    this.apiClient = apiClient;
  }

  public int getHostIndex() {
    return localHostIndex;
  }

  public void setHostIndex(int hostIndex) {
    this.localHostIndex = hostIndex;
  }

  public String getCustomBaseUrl() {
    return localCustomBaseUrl;
  }

  public void setCustomBaseUrl(String customBaseUrl) {
    this.localCustomBaseUrl = customBaseUrl;
  }

  /**
   * Build call for getNamespaces
   * @param _callback Callback for upload/download progress
   * @return Call to execute
   * @throws ApiException If fail to serialize the request body object
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> Status 200 </td><td>  -  </td></tr>
   </table>
   */
  public okhttp3.Call getNamespacesCall(final ApiCallback _callback) throws ApiException {
    String basePath = null;
    // Operation Servers
    String[] localBasePaths = new String[] {};

    // Determine Base Path to Use
    if (localCustomBaseUrl != null) {
      basePath = localCustomBaseUrl;
    } else if (localBasePaths.length > 0) {
      basePath = localBasePaths[localHostIndex];
    } else {
      basePath = null;
    }

    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/namespaces";

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    final String[] localVarAccepts = {"application/json"};
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {};
    final String localVarContentType =
        apiClient.selectHeaderContentType(localVarContentTypes);
    if (localVarContentType != null) {
      localVarHeaderParams.put("Content-Type", localVarContentType);
    }

    String[] localVarAuthNames = new String[] {"basicAuth"};
    return apiClient.buildCall(basePath, localVarPath, "GET", localVarQueryParams,
        localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams,
        localVarFormParams, localVarAuthNames, _callback);
  }

  @SuppressWarnings("rawtypes")
  private okhttp3.Call getNamespacesValidateBeforeCall(final ApiCallback _callback)
      throws ApiException {
    return getNamespacesCall(_callback);
  }

  /**
   * Get namespaces of the committed artifacts in the repository
   *
   * @return List&lt;String&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
   response body
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> Status 200 </td><td>  -  </td></tr>
   </table>
   */
  public List<String> getNamespaces() throws ApiException {
    ApiResponse<List<String>> localVarResp = getNamespacesWithHttpInfo();
    return localVarResp.getData();
  }

  /**
   * Get namespaces of the committed artifacts in the repository
   *
   * @return ApiResponse&lt;List&lt;String&gt;&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
   response body
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> Status 200 </td><td>  -  </td></tr>
   </table>
   */
  public ApiResponse<List<String>> getNamespacesWithHttpInfo() throws ApiException {
    okhttp3.Call localVarCall = getNamespacesValidateBeforeCall(null);
    Type localVarReturnType = new TypeToken<List<String>>() {}.getType();
    return apiClient.execute(localVarCall, localVarReturnType);
  }

  /**
   * Get namespaces of the committed artifacts in the repository (asynchronously)
   *
   * @param _callback The callback to be executed when the API call finishes
   * @return The request call
   * @throws ApiException If fail to process the API call, e.g. serializing the request body object
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> Status 200 </td><td>  -  </td></tr>
   </table>
   */
  public okhttp3.Call getNamespacesAsync(final ApiCallback<List<String>> _callback)
      throws ApiException {
    okhttp3.Call localVarCall = getNamespacesValidateBeforeCall(_callback);
    Type localVarReturnType = new TypeToken<List<String>>() {}.getType();
    apiClient.executeAsync(localVarCall, localVarReturnType, _callback);
    return localVarCall;
  }
  /**
   * Build call for getRepositoryInformation
   * @param _callback Callback for upload/download progress
   * @return Call to execute
   * @throws ApiException If fail to serialize the request body object
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The repository information </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public okhttp3.Call getRepositoryInformationCall(final ApiCallback _callback)
      throws ApiException {
    String basePath = null;
    // Operation Servers
    String[] localBasePaths = new String[] {};

    // Determine Base Path to Use
    if (localCustomBaseUrl != null) {
      basePath = localCustomBaseUrl;
    } else if (localBasePaths.length > 0) {
      basePath = localBasePaths[localHostIndex];
    } else {
      basePath = null;
    }

    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/repoinfo";

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    final String[] localVarAccepts = {"application/json"};
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {};
    final String localVarContentType =
        apiClient.selectHeaderContentType(localVarContentTypes);
    if (localVarContentType != null) {
      localVarHeaderParams.put("Content-Type", localVarContentType);
    }

    String[] localVarAuthNames = new String[] {"basicAuth"};
    return apiClient.buildCall(basePath, localVarPath, "GET", localVarQueryParams,
        localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams,
        localVarFormParams, localVarAuthNames, _callback);
  }

  @SuppressWarnings("rawtypes")
  private okhttp3.Call getRepositoryInformationValidateBeforeCall(final ApiCallback _callback)
      throws ApiException {
    return getRepositoryInformationCall(_callback);
  }

  /**
   * Get repository information
   * Get properties of the repository
   * @return RepositoryInfo
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
   response body
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The repository information </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public RepositoryInfo getRepositoryInformation() throws ApiException {
    ApiResponse<RepositoryInfo> localVarResp = getRepositoryInformationWithHttpInfo();
    return localVarResp.getData();
  }

  /**
   * Get repository information
   * Get properties of the repository
   * @return ApiResponse&lt;RepositoryInfo&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
   response body
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The repository information </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public ApiResponse<RepositoryInfo> getRepositoryInformationWithHttpInfo() throws ApiException {
    okhttp3.Call localVarCall = getRepositoryInformationValidateBeforeCall(null);
    Type localVarReturnType = new TypeToken<RepositoryInfo>() {}.getType();
    return apiClient.execute(localVarCall, localVarReturnType);
  }

  /**
   * Get repository information (asynchronously)
   * Get properties of the repository
   * @param _callback The callback to be executed when the API call finishes
   * @return The request call
   * @throws ApiException If fail to process the API call, e.g. serializing the request body object
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The repository information </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public okhttp3.Call getRepositoryInformationAsync(final ApiCallback<RepositoryInfo> _callback)
      throws ApiException {
    okhttp3.Call localVarCall = getRepositoryInformationValidateBeforeCall(_callback);
    Type localVarReturnType = new TypeToken<RepositoryInfo>() {}.getType();
    apiClient.executeAsync(localVarCall, localVarReturnType, _callback);
    return localVarCall;
  }
  /**
   * Build call for getSupportedChecksumAlgorithms
   * @param _callback Callback for upload/download progress
   * @return Call to execute
   * @throws ApiException If fail to serialize the request body object
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The names of the supported checksum algorithms </td><td>  -  </td></tr>
      <tr><td> 400 </td><td> Bad Request </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public okhttp3.Call getSupportedChecksumAlgorithmsCall(final ApiCallback _callback)
      throws ApiException {
    String basePath = null;
    // Operation Servers
    String[] localBasePaths = new String[] {};

    // Determine Base Path to Use
    if (localCustomBaseUrl != null) {
      basePath = localCustomBaseUrl;
    } else if (localBasePaths.length > 0) {
      basePath = localBasePaths[localHostIndex];
    } else {
      basePath = null;
    }

    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/checksumalgorithms";

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    final String[] localVarAccepts = {"application/json"};
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {};
    final String localVarContentType =
        apiClient.selectHeaderContentType(localVarContentTypes);
    if (localVarContentType != null) {
      localVarHeaderParams.put("Content-Type", localVarContentType);
    }

    String[] localVarAuthNames = new String[] {"basicAuth"};
    return apiClient.buildCall(basePath, localVarPath, "GET", localVarQueryParams,
        localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams,
        localVarFormParams, localVarAuthNames, _callback);
  }

  @SuppressWarnings("rawtypes")
  private okhttp3.Call getSupportedChecksumAlgorithmsValidateBeforeCall(final ApiCallback _callback)
      throws ApiException {
    return getSupportedChecksumAlgorithmsCall(_callback);
  }

  /**
   * Get the supported checksum algorithms
   * Get a list of the names of the supported checksum algorithms
   * @return List&lt;String&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
   response body
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The names of the supported checksum algorithms </td><td>  -  </td></tr>
      <tr><td> 400 </td><td> Bad Request </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public List<String> getSupportedChecksumAlgorithms() throws ApiException {
    ApiResponse<List<String>> localVarResp = getSupportedChecksumAlgorithmsWithHttpInfo();
    return localVarResp.getData();
  }

  /**
   * Get the supported checksum algorithms
   * Get a list of the names of the supported checksum algorithms
   * @return ApiResponse&lt;List&lt;String&gt;&gt;
   * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
   response body
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The names of the supported checksum algorithms </td><td>  -  </td></tr>
      <tr><td> 400 </td><td> Bad Request </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public ApiResponse<List<String>> getSupportedChecksumAlgorithmsWithHttpInfo()
      throws ApiException {
    okhttp3.Call localVarCall = getSupportedChecksumAlgorithmsValidateBeforeCall(null);
    Type localVarReturnType = new TypeToken<List<String>>() {}.getType();
    return apiClient.execute(localVarCall, localVarReturnType);
  }

  /**
   * Get the supported checksum algorithms (asynchronously)
   * Get a list of the names of the supported checksum algorithms
   * @param _callback The callback to be executed when the API call finishes
   * @return The request call
   * @throws ApiException If fail to process the API call, e.g. serializing the request body object
   * @http.response.details
   <table summary="Response Details" border="1">
      <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
      <tr><td> 200 </td><td> The names of the supported checksum algorithms </td><td>  -  </td></tr>
      <tr><td> 400 </td><td> Bad Request </td><td>  -  </td></tr>
      <tr><td> 401 </td><td> Unauthorized </td><td>  -  </td></tr>
      <tr><td> 500 </td><td> Internal Server Error </td><td>  -  </td></tr>
   </table>
   */
  public okhttp3.Call getSupportedChecksumAlgorithmsAsync(final ApiCallback<List<String>> _callback)
      throws ApiException {
    okhttp3.Call localVarCall = getSupportedChecksumAlgorithmsValidateBeforeCall(_callback);
    Type localVarReturnType = new TypeToken<List<String>>() {}.getType();
    apiClient.executeAsync(localVarCall, localVarReturnType, _callback);
    return localVarCall;
  }
}
