/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.laaws.api.cfg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.reflect.TypeToken;
import org.lockss.account.UserAccount;
import org.lockss.laaws.client.*;

import java.lang.reflect.Type;
import java.util.*;

public class UsersApi {
  private V2RestClient apiClient;
  private int localHostIndex;
  private String localCustomBaseUrl;

  public UsersApi() {
    this(Configuration.getDefaultApiClient());
  }

  public UsersApi(V2RestClient apiClient) {
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

  public UserAccount[] postUsers(Collection<UserAccount> accts) throws ApiException {
    ApiResponse<UserAccount[]> localVarResp = postUsersWithHttpInfo(accts);
    return localVarResp.getData();
  }

  public ApiResponse<UserAccount[]> postUsersWithHttpInfo(Collection<UserAccount> accts)
      throws ApiException {
    okhttp3.Call localVarCall = postUsersValidateBeforeCall(accts, null);
//    Type localVarReturnType = new TypeToken<List<UserAccount>>() {}.getType();
    Type localVarReturnType = new TypeToken<UserAccount[]>() {}.getType();
    return apiClient.execute(localVarCall, localVarReturnType);
  }

  private okhttp3.Call postUsersValidateBeforeCall(Collection<UserAccount> accts, final ApiCallback _callback)
      throws ApiException {
    // verify the required parameter 'auIds' is set
    if (accts == null) {
      throw new ApiException("Missing the required parameter 'accts' when calling postUsers(Async)");
    }

    okhttp3.Call localVarCall = postUsersCall(accts, _callback);
    return localVarCall;
  }

  public okhttp3.Call postUsersCall(Collection<UserAccount> accts, final ApiCallback _callback)
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

    // create path and map variables
    String localVarPath = "/users";

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

    final String[] localVarContentTypes = {"application/json"};
    final String localVarContentType =
        apiClient.selectHeaderContentType(localVarContentTypes);
    if (localVarContentType != null) {
      localVarHeaderParams.put("Content-Type", localVarContentType);
    }

    String[] localVarAuthNames = new String[] {"basicAuth"};

    try {
      // Serialize user accounts using Jackson2 rather than Gson
      ObjectWriter objWriter = UserAccount.getUserAccountObjectWriter();
      Object localVarPostBody = objWriter.writeValueAsString(accts);

      return apiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams,
          localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams,
          localVarFormParams, localVarAuthNames, _callback);
    } catch (JsonProcessingException e) {
      throw new ApiException("Could not serialize user accounts");
    }
  }
}
