package com.coremedia.labs.translation.gcc.facade;

import java.util.List;
import java.util.Objects;

/**
 * Model to store the data of a Submission returned by the GCC-Client.
 */
public class GCSubmissionModel {
  private final long submissionId;
  private final List<String> pdSubmissionIds;
  private final GCSubmissionState state;

  /**
   * Create a submission that does not have a state yet. This state is reflected by {@link GCSubmissionState#OTHER}.
   * @param submissionId the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   */
  public GCSubmissionModel(long submissionId, List<String> pdSubmissionIds) {
    this.submissionId = submissionId;
    this.pdSubmissionIds = pdSubmissionIds;
    this.state = GCSubmissionState.OTHER;
  }

  public GCSubmissionModel(long submissionId, List<String> pdSubmissionIds, GCSubmissionState state) {
    this.submissionId = submissionId;
    this.pdSubmissionIds = pdSubmissionIds;
    this.state = state;
  }

  public long getSubmissionId() {
    return submissionId;
  }

  public List<String> getPdSubmissionIds() {
    return pdSubmissionIds;
  }

  public GCSubmissionState getState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GCSubmissionModel that = (GCSubmissionModel) o;
    return submissionId == that.submissionId &&
            Objects.equals(state, that.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionId, state);
  }

  @Override
  public String toString() {
    return String.valueOf(submissionId);
  }
}
