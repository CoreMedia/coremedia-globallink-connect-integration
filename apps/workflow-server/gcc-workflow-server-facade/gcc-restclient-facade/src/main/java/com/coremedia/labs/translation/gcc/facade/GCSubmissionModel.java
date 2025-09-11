package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Model to store the data of a Submission returned by the GCC-Client.
 */
@NullMarked
public class GCSubmissionModel {
  private final long submissionId;
  private final List<String> pdSubmissionIds;
  private final String name;
  private final GCSubmissionState state;
  private final @Nullable String submitter;
  private final boolean error;

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
  public GCSubmissionModel(long submissionId, Collection<String> pdSubmissionIds, GCSubmissionState state) {
    this(submissionId, pdSubmissionIds, "", state, null, false);
  }

  /**
   * Create a submission.
   *
   * @param submissionId    the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   * @param name            the name of the submission
   * @param state           the state to represent
   * @param submitter       submitter; possibly unset, if not available
   * @param error           if the submission is in an error state
   */
  private GCSubmissionModel(long submissionId,
                            Collection<String> pdSubmissionIds,
                            String name,
                            GCSubmissionState state,
                            @Nullable String submitter,
                            boolean error) {
    this.submissionId = submissionId;
    this.pdSubmissionIds = List.copyOf(pdSubmissionIds);
    this.name = name;
    this.state = Objects.requireNonNull(state);
    this.submitter = submitter;
    this.error = error;
  }

  public long getSubmissionId() {
    return submissionId;
  }

  public List<String> getPdSubmissionIds() {
    return pdSubmissionIds;
  }

  public String getName() {
    return name;
  }

  public GCSubmissionState getState() {
    return state;
  }

  public Optional<String> findSubmitter() {
    return Optional.ofNullable(submitter);
  }

  /**
   * If the submission reached an error state.
   */
  public boolean isError() {
    return error;
  }

  public String describe() {
    return "%s[error=%s, name=%s, pdSubmissionIds=%s, state=%s, submissionId=%s, submitter=%s]".formatted(
      lookup().lookupClass().getSimpleName(),
      error,
      name,
      pdSubmissionIds,
      state,
      submissionId,
      submitter
    );
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    GCSubmissionModel that = (GCSubmissionModel) object;
    return submissionId == that.submissionId && error == that.error && Objects.equals(pdSubmissionIds, that.pdSubmissionIds) && Objects.equals(name, that.name) && state == that.state && Objects.equals(submitter, that.submitter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionId, pdSubmissionIds, name, state, submitter, error);
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
    private List<String> pdSubmissionIds = List.of();
    private GCSubmissionState state = GCSubmissionState.OTHER;
    private @Nullable String submitter;
    private boolean error;
    private String name = "";

    private Builder(long submissionId) {
      this.submissionId = submissionId;
    }

    /**
     * Ids to be shown in clients such as CoreMedia Studio.
     *
     * @param pdSubmissionIds Project-Director Submission IDs
     * @return self-reference
     */
    public Builder pdSubmissionIds(Collection<String> pdSubmissionIds) {
      this.pdSubmissionIds = List.copyOf(pdSubmissionIds);
      return this;
    }

    /**
     * State to represent.
     *
     * @param state submission state
     * @return self-reference
     */
    public Builder state(GCSubmissionState state) {
      this.state = Objects.requireNonNull(state);
      return this;
    }

    /**
     * The name of the submitter.
     *
     * @param submitter name
     * @return self-reference
     */
    public Builder submitter(@Nullable String submitter) {
      this.submitter = submitter;
      return this;
    }

    /**
     * The submission name.
     *
     * @param name name
     * @return self-reference
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Error state of the submission.
     *
     * @param error state
     * @return self-reference
     */
    public Builder error(boolean error) {
      this.error = error;
      return this;
    }

    /**
     * Creates an immutable instance of the model.
     *
     * @return model
     */
    public GCSubmissionModel build() {
      return new GCSubmissionModel(submissionId, pdSubmissionIds, name, state, submitter, error);
    }

    @Override
    public String toString() {
      return "%s[error=%s, name=%s, pdSubmissionIds=%s, state=%s, submissionId=%s, submitter=%s]".formatted(lookup().lookupClass().getSimpleName(), error, name, pdSubmissionIds, state, submissionId, submitter);
    }
  }
}
