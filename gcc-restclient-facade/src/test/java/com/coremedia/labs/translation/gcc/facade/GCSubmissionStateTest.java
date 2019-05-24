package com.coremedia.labs.translation.gcc.facade;

import org.gs4tr.gcc.restclient.model.State;
import org.gs4tr.gcc.restclient.model.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GCSubmissionStateTest {
  @Mock
  private State stateFromRestResponse;

  @ParameterizedTest
  @EnumSource(SubmissionStatus.class)
  void someSubmissionStateForAnyGccState(SubmissionStatus stateForRestRequest) {
    Mockito.when(stateFromRestResponse.getStateName()).thenReturn(stateForRestRequest.text());
    assertThat(GCSubmissionState.fromSubmissionState(stateFromRestResponse)).isNotNull();
  }

  @Test
  void acceptNullAsStateFromResponse() {
    // Directly after creating a submission its state may be null.
    assertThat(GCSubmissionState.fromSubmissionState(null)).isEqualTo(GCSubmissionState.OTHER);
  }

  @Test
  void robustnessForNullStateNameFromResponse() {
    Mockito.when(stateFromRestResponse.getStateName()).thenReturn(null);
    assertThat(GCSubmissionState.fromSubmissionState(stateFromRestResponse)).isEqualTo(GCSubmissionState.OTHER);
  }
}
