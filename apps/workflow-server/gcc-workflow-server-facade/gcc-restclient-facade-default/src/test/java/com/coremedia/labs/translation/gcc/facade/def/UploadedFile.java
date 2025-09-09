package com.coremedia.labs.translation.gcc.facade.def;

import org.gs4tr.gcc.restclient.model.GCFile;

import java.time.Instant;

public record UploadedFile(
  String id,
  String contentId,
  String fileType,
  Instant updatedAt
) {
  public static UploadedFile of(GCFile gcFile) {
    String id = gcFile.getId();
    String contentId = gcFile.getContentId();
    String fileType = gcFile.getFileType();
    Instant updatedAt = gcFile.getUpdatedAt().toInstant();
    return new UploadedFile(id, contentId, fileType, updatedAt);
  }
}
