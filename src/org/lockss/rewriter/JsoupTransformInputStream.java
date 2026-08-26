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
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.commons.io.input.ReaderInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.parser.*;
import org.lockss.util.*;

public class JsoupTransformInputStream extends InputStream implements EncodedThing {

  private static final Logger log = Logger.getLogger(JsoupTransformInputStream.class);

  public static interface JsoupTransform {
    public void transform(Document doc) throws Exception;
  }
  
  /*
   * Internal state
   */
  
  protected boolean built = false;

  protected boolean closed = false;

  protected List<JsoupTransform> transforms = new ArrayList<>();
  
  /*
   * Input side
   */
  
  protected InputStream in = null;
  
  protected String inCharsetName = null;
  
  protected String baseUri = null;
  
  /*
   * Output side
   */
  
  protected InputStream out = null;
  
  protected String outCharsetName = null;
  
  protected Integer dtfosThreshold = null;
  
  public static final int DEFAULT_DTFOS_THRESHOLD = 1*1024*1024; // 1 MiB
  
  public JsoupTransformInputStream() {
    // Intentionally left blank
  }
  
  public JsoupTransformInputStream(InputStream in,
                                   String inCharsetName,
                                   String baseUri)
      throws IllegalArgumentException, IllegalStateException {
    setInputSource(in, inCharsetName, baseUri);
  }
  
  public JsoupTransformInputStream(File file,
                                   String inCharsetName,
                                   String baseUri)
      throws FileNotFoundException, IllegalArgumentException, IllegalStateException {
    setInputSource(file, inCharsetName, baseUri);
  }

  public JsoupTransformInputStream(String input,
                                   String inCharsetName,
                                   String baseUri)
      throws IOException, IllegalArgumentException, IllegalStateException {
    setInputSource(input, inCharsetName, baseUri);
  }

  public JsoupTransformInputStream setInputSource(InputStream in,
                                                  String inCharsetName,
                                                  String baseUri)
      throws IllegalArgumentException, IllegalStateException {
    setInputStream(in);
    setInputCharsetName(inCharsetName);
    setBaseUri(baseUri);
    return this;
  }
  
  public JsoupTransformInputStream setInputSource(File file,
                                                  String inCharsetName,
                                                  String baseUri)
      throws FileNotFoundException, IllegalArgumentException, IllegalStateException {
    setInputFile(file);
    setInputCharsetName(inCharsetName);
    setBaseUri(baseUri);
    return this;
  }

  public JsoupTransformInputStream setInputSource(String input,
                                                  String inCharsetName,
                                                  String baseUri)
      throws IOException, IllegalArgumentException, IllegalStateException {
    setInputCharsetName(inCharsetName); // Must do this first
    setInputString(input);
    setBaseUri(baseUri);
    return this;
}

  public JsoupTransformInputStream setInputStream(InputStream in)
      throws IllegalArgumentException, IllegalStateException {
    throwIfBuiltOrClosed();
    throwIfVarNullOrFieldSet(in, this.in, "Input stream");
    this.in = in;
    return this;
  }

  public JsoupTransformInputStream setInputFile(File file)
      throws FileNotFoundException, IllegalArgumentException, IllegalStateException {
    if (file == null) {
      throw new IllegalArgumentException("Input file is null");
    }
    return setInputStream(new BufferedInputStream(new FileInputStream(file)));
  }

  public JsoupTransformInputStream setInputString(String input)
      throws IOException, IllegalArgumentException, IllegalStateException {
    if (input == null) {
      throw new IllegalArgumentException("Input string is null");
    }
    if (inCharsetName == null) {
      throw new IllegalStateException("Input charset name is not set");
    }
    return setInputStream(ReaderInputStream.builder().setReader(new StringReader(input))
                                                     .setCharset(inCharsetName)
                                                     .get());
  }

  public JsoupTransformInputStream setInputCharsetName(String inCharsetName)
      throws IllegalArgumentException, IllegalStateException {
    throwIfBuiltOrClosed();
    throwIfVarNullOrFieldSet(inCharsetName, this.inCharsetName, "Input charset name");
    this.inCharsetName = inCharsetName;
    return this;
  }

  public JsoupTransformInputStream setBaseUri(String baseUri)
      throws IllegalArgumentException, IllegalStateException {
    throwIfBuiltOrClosed();
    throwIfVarNullOrFieldSet(baseUri, this.baseUri, "Base URI");
    this.baseUri = baseUri;
    return this;
  }

  public JsoupTransformInputStream setOutputCharsetName(String outCharsetName)
      throws IllegalArgumentException, IllegalStateException {
    throwIfBuiltOrClosed();
    throwIfVarNullOrFieldSet(outCharsetName, this.outCharsetName, "Output charset name");
    this.outCharsetName = outCharsetName;
    return this;
  }

  public JsoupTransformInputStream setDtfosThreshold(int dtfosThreshold)
      throws IllegalArgumentException {
    throwIfBuiltOrClosed();
    if (dtfosThreshold < 0) {
      throw new IllegalArgumentException("DTFOS threshold is negative");
    }
    if (dtfosThreshold == 0) {
      throw new IllegalArgumentException("DTFOS threshold is zero");
    }
    if (this.dtfosThreshold != null) {
      throw new IllegalArgumentException("DTFOS threshold is already set");
    }
    this.dtfosThreshold = Integer.valueOf(dtfosThreshold);
    return this;
  }
  
  public JsoupTransformInputStream addTransform(JsoupTransform jxf) {
    throwIfBuiltOrClosed();
    if (jxf == null) {
      throw new IllegalArgumentException("Transform is null");
    }
    transforms.add(jxf);
    return this;
  }
  
//  public JsoupTransformInputStream addTransform(Consumer<Document> jxf) {
//    if (jxf == null) {
//      throw new IllegalArgumentException("Transform cannot be null");
//    }
//    addTransform(new JsoupTransform() {
//      @Override
//      public void transform(Document doc) throws IOException {
//        jxf.accept(doc);
//      }
//    });
//    return this;
//  }
  
  public String getCharset() throws IOException {
    getOut();
    return outCharsetName;
  }

  protected void doOutput(Document doc) throws IOException {
    DeferredTempFileOutputStream dtfos = null;
    OutputStreamWriter osw = null;
    try {
      // Decide output charset
      if (outCharsetName != null && !outCharsetName.equalsIgnoreCase(inCharsetName)) {
        log.debug2("Changing charset to: " + outCharsetName);
      }
      if (outCharsetName == null) {
        Charset docCharset = doc.charset();
        if (docCharset != null) {
          outCharsetName = docCharset.name();
	  log.debug2("Document changes charset to: " + outCharsetName); 
        }
      }
      if (outCharsetName == null) {
        outCharsetName = inCharsetName;
      }
      // Configure output
      doc.outputSettings().prettyPrint(false);
      doc.outputSettings().syntax(Syntax.html);
      // Output to deferred temp file
      if (dtfosThreshold == null) {
        dtfosThreshold = Integer.valueOf(DEFAULT_DTFOS_THRESHOLD);
      }
      dtfos = new DeferredTempFileOutputStream(dtfosThreshold.intValue());
      osw = new OutputStreamWriter(dtfos, outCharsetName);
      doc.html(osw);
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
      throw new IOException("Input charset name is not set");
    }
    if (baseUri == null) {
      throw new IOException("Underlying base URI is not set");
    }
    if (in instanceof EncodedThing) {
      String actualInCharsetName = ((EncodedThing)in).getCharset();
      if (!StringUtil.isNullString(actualInCharsetName)) {
	if (!actualInCharsetName.equals(inCharsetName)) {
	  log.debug2("Using input stream's charset: " + actualInCharsetName);
	}
	inCharsetName = actualInCharsetName;
      }
    }
    Parser parser = Parser.htmlParser().settings(ParseSettings.preserveCase);
    return Jsoup.parse(in, inCharsetName, baseUri, parser);
  }
  
  protected void process() throws IOException {
    built = true;
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
  
  protected void throwIfBuiltOrClosed() throws IllegalStateException {
    if (built) {
      throw new IllegalStateException("Input stream is already built");
    }
    if (closed) {
      throw new IllegalStateException("Input stream is closed");
    }
  }
  
  protected void throwIfVarNullOrFieldSet(Object variable,
                                          Object field,
                                          String thing)
      throws IllegalArgumentException {
    if (variable == null) {
      throw new IllegalArgumentException(thing + " is null");
    }
    if (field != null) {
      throw new IllegalArgumentException(thing + " is already set");
    }
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
