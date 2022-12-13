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

package org.lockss.laaws.model.rs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.lockss.laaws.client.JSON;

/**
 * AuSize
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class AuSize implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SERIALIZED_NAME_TOTAL_LATEST_VERSIONS = "totalLatestVersions";
  @SerializedName(SERIALIZED_NAME_TOTAL_LATEST_VERSIONS) private Long totalLatestVersions;

  public static final String SERIALIZED_NAME_TOTAL_ALL_VERSIONS = "totalAllVersions";
  @SerializedName(SERIALIZED_NAME_TOTAL_ALL_VERSIONS) private Long totalAllVersions;

  public static final String SERIALIZED_NAME_TOTAL_WARC_SIZE = "totalWarcSize";
  @SerializedName(SERIALIZED_NAME_TOTAL_WARC_SIZE) private Long totalWarcSize;

  public AuSize() {}

  public AuSize totalLatestVersions(Long totalLatestVersions) {
    this.totalLatestVersions = totalLatestVersions;
    return this;
  }

  /**
   * Get totalLatestVersions
   * @return totalLatestVersions
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Long getTotalLatestVersions() {
    return totalLatestVersions;
  }

  public void setTotalLatestVersions(Long totalLatestVersions) {
    this.totalLatestVersions = totalLatestVersions;
  }

  public AuSize totalAllVersions(Long totalAllVersions) {
    this.totalAllVersions = totalAllVersions;
    return this;
  }

  /**
   * Get totalAllVersions
   * @return totalAllVersions
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Long getTotalAllVersions() {
    return totalAllVersions;
  }

  public void setTotalAllVersions(Long totalAllVersions) {
    this.totalAllVersions = totalAllVersions;
  }

  public AuSize totalWarcSize(Long totalWarcSize) {
    this.totalWarcSize = totalWarcSize;
    return this;
  }

  /**
   * Get totalWarcSize
   * @return totalWarcSize
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Long getTotalWarcSize() {
    return totalWarcSize;
  }

  public void setTotalWarcSize(Long totalWarcSize) {
    this.totalWarcSize = totalWarcSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuSize auSize = (AuSize) o;
    return Objects.equals(this.totalLatestVersions, auSize.totalLatestVersions)
        && Objects.equals(this.totalAllVersions, auSize.totalAllVersions)
        && Objects.equals(this.totalWarcSize, auSize.totalWarcSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalLatestVersions, totalAllVersions, totalWarcSize);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuSize {\n");
    sb.append("    totalLatestVersions: ")
        .append(toIndentedString(totalLatestVersions))
        .append("\n");
    sb.append("    totalAllVersions: ").append(toIndentedString(totalAllVersions)).append("\n");
    sb.append("    totalWarcSize: ").append(toIndentedString(totalWarcSize)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  public static HashSet<String> openapiFields;
  public static HashSet<String> openapiRequiredFields;

  static {
    // a set of all properties/fields (JSON key names)
    openapiFields = new HashSet<String>();
    openapiFields.add("totalLatestVersions");
    openapiFields.add("totalAllVersions");
    openapiFields.add("totalWarcSize");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Object and throws an exception if issues found
   *
   * @param jsonObj JSON Object
   * @throws IOException if the JSON Object is invalid with respect to AuSize
   */
  public static void validateJsonObject(JsonObject jsonObj) throws IOException {
    if (jsonObj == null) {
      if (!AuSize.openapiRequiredFields.isEmpty()) { // has required fields but JSON object is null
        throw new IllegalArgumentException(String.format(
            "The required field(s) %s in AuSize is not found in the empty JSON string",
            AuSize.openapiRequiredFields.toString()));
      }
    }

    Set<Entry<String, JsonElement>> entries = jsonObj.entrySet();
    // check to see if the JSON string contains additional fields
    for (Entry<String, JsonElement> entry : entries) {
      if (!AuSize.openapiFields.contains(entry.getKey())) {
        throw new IllegalArgumentException(String.format(
            "The field `%s` in the JSON string is not defined in the `AuSize` properties. JSON: %s",
            entry.getKey(), jsonObj.toString()));
      }
    }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (!AuSize.class.isAssignableFrom(type.getRawType())) {
        return null; // this class only serializes 'AuSize' and its subtypes
      }
      final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
      final TypeAdapter<AuSize> thisAdapter =
          gson.getDelegateAdapter(this, TypeToken.get(AuSize.class));

      return (TypeAdapter<T>) new TypeAdapter<AuSize>() {
        @Override
        public void write(JsonWriter out, AuSize value) throws IOException {
          JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
          elementAdapter.write(out, obj);
        }

        @Override
        public AuSize read(JsonReader in) throws IOException {
          JsonObject jsonObj = elementAdapter.read(in).getAsJsonObject();
          validateJsonObject(jsonObj);
          return thisAdapter.fromJsonTree(jsonObj);
        }
      }.nullSafe();
    }
  }

  /**
   * Create an instance of AuSize given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of AuSize
   * @throws IOException if the JSON string is invalid with respect to AuSize
   */
  public static AuSize fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, AuSize.class);
  }

  /**
   * Convert an instance of AuSize to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}