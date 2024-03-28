package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Collection;
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
   * Create a submission that does not have a state yet.
   * This state is reflected by {@link GCSubmissionState#OTHER}.
   *
   * @param submissionId    the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   * @deprecated Use {@link #builder(long)} instead.
   */
  @Deprecated(since = "2401.3")
  public GCSubmissionModel(long submissionId, List<String> pdSubmissionIds) {
    this(submissionId, pdSubmissionIds, GCSubmissionState.OTHER);
  }

  /**
   * Create a submission that does not have a state yet.
   *
   * @param submissionId    the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   * @param state           the state to represent
   * @deprecated Use {@link #builder(long)} instead.
   */
  @Deprecated(since = "2401.3")
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
    return submissionId == that.submissionId && Objects.equals(pdSubmissionIds, that.pdSubmissionIds) && state == that.state;
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionId, pdSubmissionIds, state);
  }

  @Override
  public String toString() {
    return String.valueOf(submissionId);
  }

  /**
   * Creates a builder for submission model with required submission-ID.
   *
   * @param submissionId submission ID
   * @return builder
   */
  public static Builder builder(long submissionId) {
    return new Builder(submissionId);
  }

  /**
   * Builder for submission model.
   */
  public static final class Builder {
    private final long submissionId;
    @NonNull
    private List<String> pdSubmissionIds = List.of();
    @NonNull
    private GCSubmissionState state = GCSubmissionState.OTHER;

    private Builder(long submissionId) {
      this.submissionId = submissionId;
    }

    /**
     * Ids to be shown in clients such as CoreMedia Studio.
     *
     * @param pdSubmissionIds Project-Director Submission IDs
     * @return self-reference
     */
    @NonNull
    public Builder pdSubmissionIds(@NonNull Collection<String> pdSubmissionIds) {
      this.pdSubmissionIds = List.copyOf(pdSubmissionIds);
      return this;
    }

    /**
     * State to represent.
     *
     * @param state submission state
     * @return self-reference
     */
    @NonNull
    public Builder state(@NonNull GCSubmissionState state) {
      this.state = state;
      return this;
    }

    /**
     * Creates an immutable instance of the model.
     *
     * @return model
     */
    @NonNull
    public GCSubmissionModel build() {
      return new GCSubmissionModel(submissionId, pdSubmissionIds, state);
    }
  }
}
