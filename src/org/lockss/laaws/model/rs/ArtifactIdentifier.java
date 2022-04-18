
/*
 * 2017-2022, Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.model.rs;


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Objects;


/**
 * Class that serves as an identifier for artifacts.
 *
 * Artifacts are identified uniquely by the tuple of (CollectionID, AUID, URL, Version). Within the context of a LOCKSS
 * repository, they are also uniquely identified by their artifact ID.
 *
 * Comparable is implemented to allow for an ordering of artifacts.
 */
public class ArtifactIdentifier implements Serializable {
  private String artifactId;
  private final String collection;
  private final String auid;
  private final String uri;
  private final Integer version;

  public ArtifactIdentifier(String collection, String auid, String uri, Integer version) {
    this(null, collection, auid, uri, version);
  }

  public ArtifactIdentifier(String id, String collection, String auid, String uri, Integer version) {
    this.artifactId = id;
    this.collection = collection;
    this.auid = auid;
    this.uri = uri;
    this.version = version;
  }

  /**
   * Returns the collection name encoded in this artifact identifier.
   *
   * @return Collection name
   */
  public String getCollection() {
    return collection;
  }

  /**
   * Returns the Archival Unit ID (AUID) encoded in this artifact identifier.
   *
   * @return Archival unit ID
   */
  public String getAuid() {
    return auid;
  }

  /**
   * Returns the URI component in this artifact identifier.
   *
   * @return ArtifactData URI
   */
  public String getUri() {
    return uri;
  }

  /**
   * Returns the version component encoded in this artifact identifier.
   *
   * @return ArtifactData version
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Returns the internal artifactId component encoded in this artifact identifier.
   *
   * @return Internal artifactId
   */
  public String getId() {
    return artifactId;
  }

  /**
   * Sets the internal artifactId encoded within this artifact identifier.
   *
   * @param id
   */
  public void setId(String id) {
    this.artifactId = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArtifactIdentifier that = (ArtifactIdentifier) o;
    return artifactId.equals(that.artifactId) && collection.equals(that.collection) && auid.equals(
        that.auid) && uri.equals(that.uri) && version.equals(that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactId, collection, auid, uri, version);
  }

  /**
   *
   * @return
   */
  @Override
  public String toString() {
    return "ArtifactIdentifier{" +
        "artifactId='" + artifactId + '\'' +
        ", collection='" + collection + '\'' +
        ", auid='" + auid + '\'' +
        ", uri='" + uri + '\'' +
        ", version='" + version + '\'' +
        '}';
  }

  /**
   * Returns the artifact stem of this artifact identifier, which represents a tuple
   * containing the collection ID, AUID, and URL.
   *
   * @return A {@link ArtifactStem} containing the artifact stem of this artifact identifier.
   */
  @JsonIgnore
  public ArtifactStem getArtifactStem() {
    return new ArtifactStem(getCollection(), getAuid(), getUri());
  }

  /**
   * Struct representing a tuple of collection ID, AUID, and URL. Used for artifact version locking.
   */
  public static class ArtifactStem {
    private final String collection;
    private final String auid;
    private final String uri;

    public ArtifactStem(String collection, String auid, String uri) {
      this.collection = collection;
      this.auid = auid;
      this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ArtifactStem that = (ArtifactStem) o;
      return collection.equals(that.collection) && auid.equals(that.auid) && uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
      return Objects.hash(collection, auid, uri);
    }
  }
}
