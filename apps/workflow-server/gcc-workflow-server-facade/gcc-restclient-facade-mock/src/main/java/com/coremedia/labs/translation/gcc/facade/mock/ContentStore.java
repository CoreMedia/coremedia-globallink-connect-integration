package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCFacadeIOException;
import com.google.common.io.ByteSource;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.core.io.InputStreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Part of mocking the content API. It will remember contents to be translated
 * until they are used within a translation submission.
 */
@DefaultAnnotation(NonNull.class)
final class ContentStore {
  private final Map<String, String> store = new HashMap<>();

  /**
   * Adds the given content to the store and returns an ID
   * which you can later use to retrieve the content from the store.
   *
   * @param resource resource to read
   * @return ID
   * @throws GCFacadeIOException if the resource could not be read
   */
  String addContent(InputStreamSource resource) {
    String id = UUID.randomUUID().toString();
    try (InputStream is = resource.getInputStream()) {
      ByteSource source = new ByteSource() {
        @Override
        public InputStream openStream() {
          return is;
        }
      };
      synchronized (store) {
        store.put(id, source.asCharSource(StandardCharsets.UTF_8).read());
      }
    } catch (IOException e) {
      throw new GCFacadeIOException(e, "Failed to read resource: " + resource);
    }
    return id;
  }

  /**
   * Removes the content with the given ID.
   *
   * @param id content to remove
   * @return data of the content which got removed
   */
  String removeContent(String id) {
    synchronized (store) {
      return store.remove(id);
    }
  }

}
