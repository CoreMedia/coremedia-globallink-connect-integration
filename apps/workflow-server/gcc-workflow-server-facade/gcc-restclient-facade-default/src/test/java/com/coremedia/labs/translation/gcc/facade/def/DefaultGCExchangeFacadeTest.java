package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeIOException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.assertj.core.api.SoftAssertions;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.dto.MessageResponse;
import org.gs4tr.gcc.restclient.model.GCSubmission;
import org.gs4tr.gcc.restclient.model.GCTask;
import org.gs4tr.gcc.restclient.model.Status;
import org.gs4tr.gcc.restclient.model.SubmissionStatus;
import org.gs4tr.gcc.restclient.model.TaskStatus;
import org.gs4tr.gcc.restclient.operation.SubmissionSubmit;
import org.gs4tr.gcc.restclient.operation.Submissions;
import org.gs4tr.gcc.restclient.operation.Tasks;
import org.gs4tr.gcc.restclient.request.SubmissionSubmitRequest;
import org.gs4tr.gcc.restclient.request.TaskListRequest;
import org.gs4tr.gcc.restclient.request.UploadFileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultGCExchangeFacade} by mocking the GCC REST Client API.
 */
@ExtendWith(MockitoExtension.class)
@DefaultAnnotation(NonNull.class)
class DefaultGCExchangeFacadeTest {
  private static final String LOREM_IPSUM = "Lorem Ipsum";
  @Mock
  private GCExchange gcExchange;

  @Test
  void ignoreFailuresOnClose() {
    when(gcExchange.logout()).thenThrow(RuntimeException.class);
    assertThatCode(() -> {
      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        assertThat(facade.getDelegate()).isNotNull();
      }
    }).doesNotThrowAnyException();
  }

  @ParameterizedTest(name = "[{index}] Missing required parameter: ''{0}''")
  @DisplayName("Constructor should signal missing required keys in configuration.")
  @ValueSource(strings = {
          GCConfigProperty.KEY_URL,
          GCConfigProperty.KEY_USERNAME,
          GCConfigProperty.KEY_PASSWORD,
          GCConfigProperty.KEY_KEY
  })
  void failOnMissingRequiredConfiguration(String excludedKey) {
    Map<String, Object> config = new HashMap<>();
    List<String> requiredKeys = new ArrayList<>(asList(
            GCConfigProperty.KEY_URL,
            GCConfigProperty.KEY_USERNAME,
            GCConfigProperty.KEY_PASSWORD,
            GCConfigProperty.KEY_KEY
    ));
    requiredKeys.remove(excludedKey);

    for (String requiredKey : requiredKeys) {
      config.put(requiredKey, "non-null");
    }
    assertThatCode(() -> new DefaultGCExchangeFacade(config)).hasMessageContaining(excludedKey);
  }

  @Nested
  @DisplayName("Tests for uploadContent")
  class UploadContent {
    @Test
    @DisplayName("Test for successful upload.")
    void happyPath(TestInfo testInfo) {
      String expectedFileId = "1234-5678";
      String expectedFileName = testInfo.getDisplayName();
      byte[] expectedContent = {42};

      ArgumentCaptor<UploadFileRequest> uploadFileRequestCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);
      when(gcExchange.uploadContent(any())).thenReturn(expectedFileId);

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        String actualFileId = facade.uploadContent(expectedFileName, new ByteArrayResource(expectedContent));

        assertThat(actualFileId).isEqualTo(expectedFileId);

        verify(gcExchange).uploadContent(uploadFileRequestCaptor.capture());
        UploadFileRequest actualRequest = uploadFileRequestCaptor.getValue();
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(actualRequest.getFileName()).isEqualTo(expectedFileName);
        assertions.assertThat(actualRequest.getFileType()).isEqualTo("xliff");
        assertions.assertThat(actualRequest.getContents()).isEqualTo(expectedContent);
        assertions.assertAll();
      }
    }

    @Test
    @DisplayName("IOExceptions should be wrapped into GCFacadeIOException")
    void wrapIOExceptions() throws Exception {
      String someResourceFilename = "some resource filename";
      String someFilename = "some filename";
      Resource resource = Mockito.mock(Resource.class);
      Mockito.when(resource.getInputStream()).thenThrow(IOException.class);
      Mockito.when(resource.getFilename()).thenReturn(someResourceFilename);

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        assertThatThrownBy(() -> facade.uploadContent(someFilename, resource))
                .isInstanceOf(GCFacadeIOException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining(someResourceFilename)
                .hasMessageContaining(someFilename);
      }
    }

    @Test
    void wrapExceptionsDuringUpload(TestInfo testInfo) {
      String expectedFileName = testInfo.getDisplayName();
      when(gcExchange.uploadContent(any())).thenThrow(RuntimeException.class);

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        assertThatThrownBy(() -> facade.uploadContent(expectedFileName, new ByteArrayResource(new byte[]{42})))
                .isInstanceOf(GCFacadeCommunicationException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining(expectedFileName);
      }
    }
  }

  @Nested
  @DisplayName("Tests for submitSubmission")
  class SubmitSubmission {
    @Mock
    private SubmissionSubmit.SubmissionSubmitResponseData response;

    @Test
    @DisplayName("Test for successful submission.")
    void happyPath(TestInfo testInfo) {
      String subject = testInfo.getDisplayName();
      LocalDateTime dueDateLocal = LocalDateTime.now().plusHours(2);
      ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
      ZonedDateTime dueDate = ZonedDateTime.of(dueDateLocal, zoneOffset);
      Date expectedUtcDueDate = Date.from(dueDateLocal.atOffset(zoneOffset).toInstant());
      Locale sourceLocale = Locale.US;
      Locale targetLocale = Locale.FRANCE;
      String fileId = "1234-5678";
      long expectedSubmissionId = 42L;

      ArgumentCaptor<SubmissionSubmitRequest> submissionSubmitRequestCaptor = ArgumentCaptor.forClass(SubmissionSubmitRequest.class);
      when(gcExchange.submitSubmission(any())).thenReturn(response);
      when(response.getSubmissionId()).thenReturn(expectedSubmissionId);

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        long submissionId = facade.submitSubmission(subject, dueDate, sourceLocale, singletonMap(fileId, singletonList(targetLocale)));

        assertThat(submissionId).isEqualTo(expectedSubmissionId);
        verify(gcExchange).submitSubmission(submissionSubmitRequestCaptor.capture());
        SubmissionSubmitRequest request = submissionSubmitRequestCaptor.getValue();

        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(request.getSubmissionName()).contains(subject);
        assertions.assertThat(request.getDueDate()).isEqualTo(expectedUtcDueDate);
        assertions.assertThat(request.getSourceLocale()).isEqualTo(sourceLocale.toLanguageTag());
        assertions.assertThat(request.getContentLocales()).hasSize(1);
        assertions.assertAll();
      }
    }

    @Test
    @DisplayName("Correctly deal with communication errors.")
    void dealWithCommunicationExceptions(TestInfo testInfo) {
      String subject = testInfo.getDisplayName();
      LocalDateTime dueDateLocal = LocalDateTime.now().plusHours(2);
      ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
      ZonedDateTime dueDate = ZonedDateTime.of(dueDateLocal, zoneOffset);
      Locale sourceLocale = Locale.US;
      Locale targetLocale = Locale.FRANCE;
      String fileId = "1234-5678";

      when(gcExchange.submitSubmission(any())).thenThrow(RuntimeException.class);

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        assertThatThrownBy(() -> facade.submitSubmission(subject, dueDate, sourceLocale, singletonMap(fileId, singletonList(targetLocale))))
                .isInstanceOf(GCFacadeCommunicationException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining(subject)
                .hasMessageContaining(sourceLocale.toString())
        ;
      }
    }
  }

  @Nested
  @DisplayName("Tests for downloadCompletedTasks")
  class DownloadCompletedTasks {
    @Mock
    private Tasks.TasksResponseData tasksListResponse;

    @BeforeEach
    void setUp() {
      when(gcExchange.getTasksList(any())).thenReturn(tasksListResponse);
    }

    @Nested
    @DisplayName("Tests for downloading single tasks")
    class SingleTasks {
      @Mock
      private GCTask gcTask;
      private final long expectedSubmissionId = 1234L;
      private final long expectedTaskId = 5678L;
      private final byte[] expectedContent = LOREM_IPSUM.getBytes(StandardCharsets.UTF_8);

      @Test
      @DisplayName("Test for successful download.")
      void happyPath() {
        when(tasksListResponse.getTasks()).thenReturn(singletonList(gcTask));
        when(gcTask.getTaskId()).thenReturn(expectedTaskId);
        when(gcTask.getStatus()).thenReturn(TaskStatus.Completed.text());
        org.gs4tr.gcc.restclient.model.Locale gccLocale = new org.gs4tr.gcc.restclient.model.Locale();
        gccLocale.setLocale("de-DE");
        when(gcTask.getTargetLocale()).thenReturn(gccLocale);
        when(gcExchange.downloadTask(eq(expectedTaskId))).thenReturn(new ByteArrayInputStream(expectedContent));
        when(gcExchange.confirmTask(eq(expectedTaskId))).thenReturn(true);

        try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
          String[] actualContentRead = {null};

          facade.downloadCompletedTasks(expectedSubmissionId, new HappyPathTaskDataConsumer(actualContentRead));

          assertThat(actualContentRead[0]).isEqualTo(LOREM_IPSUM);
        }
      }

      private class HappyPathTaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
        private final String[] actualContentRead;

        private HappyPathTaskDataConsumer(String[] actualContentRead) {
          this.actualContentRead = actualContentRead;
        }

        @Override
        public boolean test(InputStream is, GCTaskModel task) {
          try {
            actualContentRead[0] = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            return true;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

      @Test
      @DisplayName("Test for failure on processing input stream.")
      void unhappyPathInputStream() {
        when(tasksListResponse.getTasks()).thenReturn(singletonList(gcTask));
        when(gcTask.getTaskId()).thenReturn(expectedTaskId);
        when(gcTask.getStatus()).thenReturn(TaskStatus.Completed.text());
        org.gs4tr.gcc.restclient.model.Locale locale = new org.gs4tr.gcc.restclient.model.Locale();
        locale.setLocale("de-DE");
        when(gcTask.getTargetLocale()).thenReturn(locale);
        when(gcExchange.downloadTask(eq(expectedTaskId))).thenReturn(new ByteArrayInputStream(expectedContent));

        try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
          assertThatThrownBy(() -> facade.downloadCompletedTasks(expectedSubmissionId, new ExceptionTaskDataConsumer()))
                  .isInstanceOf(GCFacadeCommunicationException.class)
                  .hasCauseInstanceOf(RuntimeException.class)
                  .hasMessageContaining(String.valueOf(expectedTaskId));
        }

        verify(gcExchange, never()).confirmTask(any());
      }

      private class ExceptionTaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
        @Override
        public boolean test(InputStream inputStream, GCTaskModel task) {
          throw new RuntimeException("Provoked failure while reading input stream.");
        }
      }

      @Test
      @DisplayName("Test for failure on getTasksList()")
      void unhappyPathFailureOnGetTaskList() {
        when(gcExchange.getTasksList(any())).thenThrow(RuntimeException.class);

        try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
          assertThatThrownBy(() -> facade.downloadCompletedTasks(expectedSubmissionId, new TrueTaskDataConsumer()))
                  .isInstanceOf(GCFacadeCommunicationException.class)
                  .hasCauseInstanceOf(RuntimeException.class);
        }

        verify(gcExchange, never()).downloadTask(any());
        verify(gcExchange, never()).confirmTask(any());
      }

      private class TrueTaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
        @Override
        public boolean test(InputStream inputStream, GCTaskModel task) {
          return true;
        }
      }

      @Test
      @DisplayName("Test for failure on downloadTask()")
      void unhappyPathFailureOnDownloadTask() {
        when(tasksListResponse.getTasks()).thenReturn(singletonList(gcTask));
        when(gcTask.getTaskId()).thenReturn(expectedTaskId);
        when(gcTask.getStatus()).thenReturn(TaskStatus.Completed.text());
        org.gs4tr.gcc.restclient.model.Locale locale = new org.gs4tr.gcc.restclient.model.Locale();
        locale.setLocale("de-DE");
        when(gcTask.getTargetLocale()).thenReturn(locale);
        when(gcExchange.downloadTask(eq(expectedTaskId))).thenThrow(RuntimeException.class);

        try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
          assertThatThrownBy(() -> facade.downloadCompletedTasks(expectedSubmissionId, new TrueTaskDataConsumer()))
                  .isInstanceOf(GCFacadeCommunicationException.class)
                  .hasCauseInstanceOf(RuntimeException.class)
                  .hasMessageContaining(String.valueOf(expectedTaskId));
        }

        verify(gcExchange, never()).confirmTask(any());

      }

      @Test
      @DisplayName("Test for failure on confirmTask()")
      void unhappyPathFailureOnConfirmTask() {
        when(tasksListResponse.getTasks()).thenReturn(singletonList(gcTask));
        when(gcTask.getTaskId()).thenReturn(expectedTaskId);
        when(gcTask.getStatus()).thenReturn(TaskStatus.Completed.text());
        when(gcExchange.downloadTask(eq(expectedTaskId))).thenReturn(new ByteArrayInputStream(expectedContent));
        org.gs4tr.gcc.restclient.model.Locale locale = new org.gs4tr.gcc.restclient.model.Locale();
        locale.setLocale("de-DE");
        when(gcTask.getTargetLocale()).thenReturn(locale);
        when(gcExchange.confirmTask(eq(expectedTaskId))).thenThrow(RuntimeException.class);

        try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
          assertThatThrownBy(() -> facade.downloadCompletedTasks(expectedSubmissionId, new TrueTaskDataConsumer()))
                  .isInstanceOf(GCFacadeCommunicationException.class)
                  .hasCauseInstanceOf(RuntimeException.class)
                  .hasMessageContaining(String.valueOf(expectedTaskId));
        }
      }
    }
  }

  @Nested
  @DisplayName("Tests for confirmCancelledTasks")
  class ConfirmCancelledTasks {
    @Mock
    private Tasks.TasksResponseData tasksListResponse;
    @Mock
    private MessageResponse messageResponse;
    @Mock
    private GCTask gcTask;
    @Captor
    private ArgumentCaptor<Long> taskIdCaptor;
    @Captor
    private ArgumentCaptor<TaskListRequest> taskListRequestCaptor;


    @BeforeEach
    void setUp() {
      when(gcExchange.getTasksList(any())).thenReturn(tasksListResponse);
    }

    @Test
    @DisplayName("Test for successful cancellation confirmation.")
    void happyPath() {
      long submissionId = 42L;
      long taskId = 21L;

      when(tasksListResponse.getTasks()).thenReturn(singletonList(gcTask));
      when(gcTask.getTaskId()).thenReturn(taskId);
      when(gcTask.getStatus()).thenReturn(TaskStatus.Cancelled.text());
      org.gs4tr.gcc.restclient.model.Locale locale = new org.gs4tr.gcc.restclient.model.Locale();
      locale.setLocale("de-DE");
      when(gcTask.getTargetLocale()).thenReturn(locale);
      when(gcExchange.confirmTaskCancellation(any())).thenReturn(messageResponse);
      when(messageResponse.getStatus()).thenReturn(200);

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        facade.confirmCancelledTasks(submissionId);
      }

      verify(gcExchange).getTasksList(taskListRequestCaptor.capture());
      TaskListRequest request = taskListRequestCaptor.getValue();
      assertThat(request.getIsCancelConfirmed()).isEqualTo(0);
      assertThat(request.getStatuses()).containsExactly(TaskStatus.Cancelled.text());

      verify(gcExchange).confirmTaskCancellation(taskIdCaptor.capture());
      Long value = taskIdCaptor.getValue();
      assertThat(value).isEqualTo(taskId);
    }

    @Test
    @DisplayName("For empty task lists, the confirmation must not be performed.")
    void skipConfirmationForEmptyTaskList() {
      long submissionId = 42L;

      when(tasksListResponse.getTasks()).thenReturn(emptyList());

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        facade.confirmCancelledTasks(submissionId);
      }

      verify(gcExchange, never()).confirmTaskCancellation(any());
    }
  }

  @Nested
  @DisplayName("Tests for getSubmissionState")
  class GetSubmissionState {
    private static final long SUBMISSION_ID = 42L;

    @Mock
    private GCSubmission submission;
    @Mock
    private GCSubmission additionalSubmission;
    @Mock
    private Submissions.SubmissionsResponseData submissionsResponseData;
    @Mock
    private Status submissionState;
    @Mock
    private Status additionalSubmissionState;

    @BeforeEach
    void setUp() {
      when(gcExchange.getSubmissionsList(any())).thenReturn(submissionsResponseData);
      when(submissionsResponseData.getSubmissions()).thenReturn(singletonList(submission));

      lenient().when(submission.getStatus()).thenReturn(submissionState);
      lenient().when(submissionState.getStatusName()).thenReturn(SubmissionStatus.Started.text());
    }

    @Test
    @DisplayName("Standard Submission State Retrieval")
    void happyPath() {
      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);
        assertThat(state).isEqualTo(GCSubmissionState.STARTED);
      }
    }

    @Test
    @DisplayName("Submission Unavailable, State: OTHER")
    void dealWithUnavailableSubmission() {
      when(submissionsResponseData.getSubmissions()).thenReturn(emptyList());

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);
        assertThat(state).isEqualTo(GCSubmissionState.OTHER);
      }
    }

    @Test
    @DisplayName("Robustness: Handle multiple submissions found.")
    void dealWithTooManySubmissionsFound() {
      lenient().when(additionalSubmission.getStatus()).thenReturn(additionalSubmissionState);
      lenient().when(additionalSubmissionState.getStatusName()).thenReturn(SubmissionStatus.Translate.text());

      when(submissionsResponseData.getSubmissions()).thenReturn(asList(submission, additionalSubmission));

      try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
        GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);
        // Does not matter, just one should be chosen.
        assertThat(state).isIn(GCSubmissionState.STARTED, GCSubmissionState.TRANSLATE);
      }
    }

    @Nested
    @DisplayName("Support for artificial cancelled states")
    class ArtificialCancelledStates {
      @Mock
      private Tasks.TasksResponseData tasksListResponse;
      @Mock
      private GCTask task;
      @Mock
      private GCTask additionalTask1;
      @Mock
      private GCTask additionalTask2;

      @BeforeEach
      void setUp() {
        when(gcExchange.getTasksList(any())).thenReturn(tasksListResponse);
        when(tasksListResponse.getTasks()).thenReturn(singletonList(task));
        when(submission.getIsCancelled()).thenReturn(Boolean.TRUE);
      }

      @Nested
      @DisplayName("Support for artificial submission state: Cancelled")
      class ArtificialCancelledState {
        @Test
        @DisplayName("'cancelled', if task is not in cancelled state")
        void taskNotCancelled() {
          when(task.getStatus()).thenReturn(TaskStatus.Processing.text());

          try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
            GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);

            assertThat(state).isEqualTo(GCSubmissionState.CANCELLED);
          }
        }

        @Test
        @DisplayName("'cancelled', if task is cancelled but cancellation not confirmed")
        void taskUnconfirmedCancellation() {
          when(task.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(task.getIsCancelConfirmed()).thenReturn(Boolean.FALSE);

          try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
            GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);

            assertThat(state).isEqualTo(GCSubmissionState.CANCELLED);
          }
        }

        @Test
        @DisplayName("'cancelled', if not all tasks have their cancellation confirmed")
        void someTasksUnconfirmedCancellation() {
          when(tasksListResponse.getTasks()).thenReturn(asList(task, additionalTask1, additionalTask2));
          when(task.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(task.getIsCancelConfirmed()).thenReturn(Boolean.TRUE);
          when(additionalTask1.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(additionalTask1.getIsCancelConfirmed()).thenReturn(Boolean.FALSE);
          when(additionalTask2.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(additionalTask2.getIsCancelConfirmed()).thenReturn(Boolean.TRUE);

          try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
            GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);

            assertThat(state).isEqualTo(GCSubmissionState.CANCELLED);
          }
        }
      }

      @Nested
      @DisplayName("Support for artificial submission state: Cancellation Confirmed")
      class ArtificialCancellationConfirmedState {
        @Test
        @DisplayName("'cancellation confirmed', if all tasks are confirmed")
        void taskIsCancellationConfirmed() {
          when(task.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(task.getIsCancelConfirmed()).thenReturn(Boolean.TRUE);

          try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
            GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);

            assertThat(state).isEqualTo(GCSubmissionState.CANCELLATION_CONFIRMED);
          }
        }

        /**
         * This tests the partial cancellation case. As example the submission got
         * cancelled while some tasks were already delivered. In this case, the
         * already delivered tasks shall be ignored for cancellation confirmed
         * evaluation.
         */
        @Test
        @DisplayName("'cancellation confirmed', if all tasks are either confirmed or delivered")
        void tasksAreCancellationConfirmedOrDelivered() {
          when(tasksListResponse.getTasks()).thenReturn(asList(task, additionalTask1, additionalTask2));
          when(task.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(task.getIsCancelConfirmed()).thenReturn(Boolean.TRUE);
          when(additionalTask1.getStatus()).thenReturn(TaskStatus.Delivered.text());
          when(additionalTask2.getStatus()).thenReturn(TaskStatus.Cancelled.text());
          when(additionalTask2.getIsCancelConfirmed()).thenReturn(Boolean.TRUE);

          try (GCExchangeFacade facade = new MockDefaultGCExchangeFacade(gcExchange)) {
            GCSubmissionState state = facade.getSubmissionState(SUBMISSION_ID);

            assertThat(state).isEqualTo(GCSubmissionState.CANCELLATION_CONFIRMED);
          }
        }
      }
    }
  }

  private static final class MockDefaultGCExchangeFacade extends DefaultGCExchangeFacade {
    MockDefaultGCExchangeFacade(GCExchange delegate) {
      super(delegate, "xliff");
    }
  }
}
