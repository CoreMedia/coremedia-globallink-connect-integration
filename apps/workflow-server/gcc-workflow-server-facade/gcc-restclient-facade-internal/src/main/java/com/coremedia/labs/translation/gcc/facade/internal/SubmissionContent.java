package com.coremedia.labs.translation.gcc.facade.internal;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Represents a content which is part of a translation.
 */
@DefaultAnnotation(NonNull.class)
class SubmissionContent {
  private final String fileId;
  private final String fileContent;
  private final List<Locale> targetLocales;

  SubmissionContent(String fileId, String fileContent, List<Locale> targetLocales) {
    this.fileId = fileId;
    this.fileContent = fileContent;
    this.targetLocales = ImmutableList.copyOf(targetLocales);
  }

  public String getFileId() {
    return fileId;
  }

  public String getFileContent() {
    return fileContent;
  }

  public List<Locale> getTargetLocales() {
    return ImmutableList.copyOf(targetLocales);
  }


  @Override
  public String toString() {
    return new StringJoiner(", ", SubmissionContent.class.getSimpleName() + "[", "]")
      .add("fileContent='" + fileContent + "'")
      .add("fileId='" + fileId + "'")
      .add("targetLocales=" + targetLocales)
      .toString();
  }
}
