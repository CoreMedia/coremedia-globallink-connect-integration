package com.coremedia.labs.translation.gcc.facade.mock;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Locale;

/**
 * Represents a content which is part of a translation.
 */
@NullMarked
record SubmissionContent(String fileId, String fileContent, List<Locale> targetLocales) {
  SubmissionContent(String fileId, String fileContent, List<Locale> targetLocales) {
    this.fileId = fileId;
    this.fileContent = fileContent;
    this.targetLocales = ImmutableList.copyOf(targetLocales);
  }

  @Override
  public List<Locale> targetLocales() {
    return ImmutableList.copyOf(targetLocales);
  }
}
