/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.rewriter;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.lockss.util.*;

public class JsoupTransformInputStream extends InputStream {

  public static interface JsoupTransform {
    public void transform(Document doc) throws Exception;
  }
  
  protected InputStream in = null;
  
  protected InputStream out = null;
  
  protected List<JsoupTransform> transforms = new ArrayList<>();
  
  protected String inCharsetName = null;
  
  protected String baseUri = null;
  
  protected boolean closed = false;

  public JsoupTransformInputStream() {
    // Intentionally left blank
  }
  
  public JsoupTransformInputStream(InputStream in,
                                   String inCharsetName,
                                   String baseUri) {
    setInput(in, inCharsetName, baseUri);
  }
  
  public JsoupTransformInputStream setInput(InputStream in,
                                            String inCharsetName,
                                            String baseUri) {
    setInputStream(in);
    setInputCharsetName(inCharsetName);
    setBaseUri(baseUri);
    return this;
  }
  
  public JsoupTransformInputStream setInputStream(InputStream in)
      throws IllegalArgumentException, IllegalStateException {
    if (in == null) {
      throw new IllegalArgumentException("Input stream cannot be null");
    }
    if (this.in != null) {
      throw new IllegalStateException("Input stream is already set");
    }
    this.in = in;
    return this;
  }
  
  public JsoupTransformInputStream setInputCharsetName(String inCharsetName)
      throws IllegalArgumentException, IllegalStateException {
    if (inCharsetName == null) {
      throw new IllegalArgumentException("Input charset name cannot be null");
    }
    if (this.inCharsetName != null) {
      throw new IllegalStateException("Input charset name is already set");
    }
    this.inCharsetName = inCharsetName;
    return this;
  }

  public JsoupTransformInputStream setBaseUri(String baseUri)
      throws IllegalArgumentException, IllegalStateException {
    if (baseUri == null) {
      throw new IllegalArgumentException("Base URI cannot be null");
    }
    if (this.baseUri != null) {
      throw new IllegalStateException("Base URI is already set");
    }
    this.baseUri = baseUri;
    return this;
  }

  public JsoupTransformInputStream addTransform(JsoupTransform jxf) {
    if (jxf == null) {
      throw new IllegalArgumentException("Transform cannot be null");
    }
    transforms.add(jxf);
    return this;
  }
  
  public JsoupTransformInputStream addTransform(Consumer<Document> jxf) {
    if (jxf == null) {
      throw new IllegalArgumentException("Transform cannot be null");
    }
    addTransform(new JsoupTransform() {
      @Override
      public void transform(Document doc) throws IOException {
        jxf.accept(doc);
      }
    });
    return this;
  }
  
  protected void doOutput(Document doc) throws IOException {
    DeferredTempFileOutputStream dtfos = null;
    OutputStreamWriter osw = null;
    try {
      dtfos = new DeferredTempFileOutputStream(1*1024*1024); // FIXME
      osw = new OutputStreamWriter(dtfos, inCharsetName);
      osw.write(doc.outerHtml());
      osw.close();
      out = dtfos.getDeleteOnCloseInputStream();
    }
    catch (IOException | RuntimeException exc) {
      if (dtfos != null) {
        dtfos.deleteTempFile();
      }
      throw exc;
    }
  }
  
  protected Document parse() throws IOException {
    if (in == null) {
      throw new IOException("Underlying input stream is not set");
    }
    if (inCharsetName == null) {
      throw new IOException("Underlying input charset name is not set");
    }
    if (baseUri == null) {
      throw new IOException("Underlying base URI is not set");
    }
    return Jsoup.parse(in, inCharsetName, baseUri);
  }
  
  protected void process() throws IOException {
    Document doc = parse();
    transform(doc);
    doOutput(doc);
  }
  
  protected void transform(Document doc) throws IOException {
    try {
      for (JsoupTransform jxf : transforms) {
        jxf.transform(doc);
      }
    }
    catch (Exception exc) {
      throw new IOException(exc);
    }
  }

  protected InputStream getOut() throws IOException {
    if (closed) {
      throw new IOException("Input stream is closed");
    }
    if (out == null) {
      process();
    }
    return out;
  }
  
  /*
   * InputStream methods
   */
  
  @Override
  public int available() throws IOException {
    return getOut().available();
  }

  @Override
  public void close() throws IOException {
    closed = true;
    IOUtil.safeClose(in);
    in = null;
    IOUtil.safeClose(out);
    out = null;
  }

  @Override
  public synchronized void mark(int readlimit) {
    try {
      getOut().mark(readlimit);
    }
    catch (IOException exc) {
      throw new UncheckedIOException(exc);
    }
  }

  @Override
  public boolean markSupported() {
    try {
      return getOut().markSupported();
    }
    catch (IOException exc) {
      throw new UncheckedIOException(exc);
    }
  }

  @Override
  public int read() throws IOException {
    return getOut().read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return getOut().read(b, off, len);
  }

  @Override
  public int read(byte[] b) throws IOException {
    return getOut().read(b);
  }

  @Override
  public synchronized void reset() throws IOException {
    getOut().reset();
  }

  @Override
  public long skip(long n) throws IOException {
    return getOut().skip(n);
  }
  
}
