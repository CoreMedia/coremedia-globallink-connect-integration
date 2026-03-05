package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.google.common.io.ByteSource;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiPredicate;

/**
 * Simple consumer to read task data into a string builder.
 *
 * @param xliffResult the string builder to write the result to
 */
@NullMarked
public record TaskDataConsumer(StringBuilder xliffResult) implements BiPredicate<InputStream, GCTaskModel> {
  @Override
  public boolean test(InputStream is, GCTaskModel task) {
    ByteSource byteSource = new InputStreamByteSource(is);
    try {
      byteSource.asCharSource(StandardCharsets.UTF_8).copyTo(xliffResult);
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  private static final class InputStreamByteSource extends ByteSource {
    private final InputStream is;

    private InputStreamByteSource(InputStream is) {
      this.is = is;
    }

    @Override
    public InputStream openStream() {
      return is;
    }
  }
}
