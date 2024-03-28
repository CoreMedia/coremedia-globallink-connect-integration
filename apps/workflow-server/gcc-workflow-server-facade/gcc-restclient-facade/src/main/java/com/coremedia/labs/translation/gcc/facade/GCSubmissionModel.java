package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Model to store the data of a Submission returned by the GCC-Client.
 */
public class GCSubmissionModel {
  private final long submissionId;
  @NonNull
  private final List<String> pdSubmissionIds;
  @NonNull
  private final GCSubmissionState state;
  @Nullable
  private final String submitter;

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
   * Create a submission.
   *
   * @param submissionId    the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   * @param state           the state to represent
   * @deprecated Use {@link #builder(long)} instead.
   */
  @Deprecated(since = "2401.3")
  public GCSubmissionModel(long submissionId, @NonNull Collection<String> pdSubmissionIds, @NonNull GCSubmissionState state) {
    this(submissionId, pdSubmissionIds, state, null);
  }

  /**
   * Create a submission.
   *
   * @param submissionId    the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   * @param state           the state to represent
   * @param submitter       submitter; possibly unset, if not available
   */
  private GCSubmissionModel(long submissionId,
                            @NonNull Collection<String> pdSubmissionIds,
                            @NonNull GCSubmissionState state,
                            @Nullable String submitter) {
    this.submissionId = submissionId;
    this.pdSubmissionIds = List.copyOf(pdSubmissionIds);
    this.state = Objects.requireNonNull(state);
    this.submitter = submitter;
  }

  public long getSubmissionId() {
    return submissionId;
  }

  @NonNull
  public List<String> getPdSubmissionIds() {
    return pdSubmissionIds;
  }

  @NonNull
  public GCSubmissionState getState() {
    return state;
  }

  @NonNull
  public Optional<String> findSubmitter() {
    return Optional.ofNullable(submitter);
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
    return submissionId == that.submissionId && Objects.equals(pdSubmissionIds, that.pdSubmissionIds) && state == that.state && Objects.equals(submitter, that.submitter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionId, pdSubmissionIds, state, submitter);
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
    @Nullable
    private String submitter;

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
      this.state = Objects.requireNonNull(state);
      return this;
    }

    /**
     * The name of the submitter.
     *
     * @param submitter name
     * @return self-reference
     */
    @NonNull
    public Builder submitter(@Nullable String submitter) {
      this.submitter = submitter;
      return this;
    }

    /**
     * Creates an immutable instance of the model.
     *
     * @return model
     */
    @NonNull
    public GCSubmissionModel build() {
      return new GCSubmissionModel(submissionId, pdSubmissionIds, state, submitter);
    }

    @Override
    public String toString() {
      return "%s[submissionId=%s, pdSubmissionIds=%s, state=%s, submitter=%s]".formatted(MethodHandles.lookup().lookupClass().getSimpleName(), submissionId, pdSubmissionIds, state, submitter);
    }
  }
}
