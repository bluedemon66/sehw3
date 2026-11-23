package edu.nju.sehw.takeout.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Loads explicit PKCS12 key material for the local HTTPS demonstration. */
public final class TlsContexts {
  private TlsContexts() {}

  /** Loads the server key and its certificate chain. */
  public static SSLContext server(Path keyStore, char[] password)
      throws IOException, GeneralSecurityException {
    var factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    factory.init(load(keyStore, password), password);
    var context = SSLContext.getInstance("TLS");
    context.init(factory.getKeyManagers(), null, null);
    return context;
  }

  /** Loads the certificates trusted by the client. */
  public static SSLContext client(Path trustStore, char[] password)
      throws IOException, GeneralSecurityException {
    var factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(load(trustStore, password));
    var context = SSLContext.getInstance("TLS");
    context.init(null, factory.getTrustManagers(), null);
    return context;
  }

  private static KeyStore load(Path path, char[] password)
      throws IOException, GeneralSecurityException {
    var store = KeyStore.getInstance("PKCS12");
    try (var input = Files.newInputStream(path)) {
      store.load(input, password);
    }
    return store;
  }
}
