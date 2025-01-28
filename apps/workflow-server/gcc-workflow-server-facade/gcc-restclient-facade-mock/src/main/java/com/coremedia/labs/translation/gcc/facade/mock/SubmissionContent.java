package com.coremedia.labs.translation.gcc.facade.mock;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.List;
import java.util.Locale;

/**
 * Represents a content which is part of a translation.
 */
@DefaultAnnotation(NonNull.class)
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
