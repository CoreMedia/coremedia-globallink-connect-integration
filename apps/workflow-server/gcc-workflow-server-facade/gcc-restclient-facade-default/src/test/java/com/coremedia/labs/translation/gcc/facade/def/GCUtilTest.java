package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.assertj.core.api.Assertions;
import org.gs4tr.gcc.restclient.dto.PageableResponseData;
import org.gs4tr.gcc.restclient.request.PageableRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link GCUtil}.
 */
class GCUtilTest {
  @SuppressWarnings("UseOfObsoleteDateTimeApi")
  @ParameterizedTest
  @DisplayName("toUnixDateUtc: Dates should be represented as UTC")
  @ArgumentsSource(InstantArgumentsProvider.class)
  void toUnixDateUtcShouldRepresentDatesAsUtc(ZonedDateTime probe) {
    Date actualDate = GCUtil.toUnixDateUtc(probe);
    ZonedDateTime expectedDateTime = probe.withZoneSameInstant(ZoneOffset.UTC);
    ZonedDateTime actualDateTime = ZonedDateTime.ofInstant(actualDate.toInstant(), ZoneOffset.UTC);
    assertThat(actualDateTime).isCloseTo(expectedDateTime, Assertions.within(1L, MILLIS));
  }

  @ParameterizedTest
  @DisplayName("textToHtml: Text should be transformed to HTML")
  @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
          text         | expected
          &            | &amp;
          <            | &lt;
          >            | &gt;
          "            | &quot;
          NL           | <br>
          CR           | <br>
          CRNL         | <br>
    """)
  void shouldTransformTextToHtml(@NonNull String text, @NonNull String expected) {
    String textFixture = text
      .replace("NL", "\n")
      .replace("CR", "\r");
    assertThat(GCUtil.textToHtml(textFixture)).isEqualTo(expected);
  }

  @ParameterizedTest
  @DisplayName("textToHtml: Text should be transformed to HTML")
  @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
          text         | expected
          "\u0000"     | "\u0000"
          "\t"         | "\t"
          a            | a
          ö            | ö
          \uFFFF       | \uFFFF
          \uD800\uDC00 | (x10000)
          \uD83D\uDD4A | (x1F54A)
          \uDBFF\uDFFF | (x10FFFF)
    """)
  void shouldTransformTextContainOnlyBmpCharacters(@NonNull String text, @NonNull String expected) {
    assertThat(GCUtil.textToBmp(text)).isEqualTo(expected);
  }

  @Nested
  @DisplayName("Tests for processAllPages")
  @ExtendWith(MockitoExtension.class)
  class ProcessAllPages {
    @Mock
    private PageableRequest request;
    @Mock
    private PageableResponseData responseData;

    @Test
    @DisplayName("Results from all pages shall be retrieved.")
    void retrieveResultsFromAllPages() {
      AtomicInteger invocations = new AtomicInteger();
      long numTotalPages = 2L;
      Mockito.when(responseData.getTotalResultPagesCount()).thenReturn(numTotalPages);
      GCUtil.processAllPages(() -> request, r -> {
        invocations.incrementAndGet();
        return responseData;
      });
      ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
      Mockito.verify(request, Mockito.atLeastOnce()).setPageNumber(captor.capture());
      assertThat(invocations.get()).isEqualTo(numTotalPages);
      assertThat(captor.getAllValues()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("On Request Failure exceptions shall be wrapped to GCFacadeCommunicationException.")
    void forwardExceptionOnRequestFailure() {
      AtomicInteger invocations = new AtomicInteger();
      long numTotalPages = 2L;
      Mockito.when(responseData.getTotalResultPagesCount()).thenReturn(numTotalPages);
      assertThatThrownBy(() -> GCUtil.processAllPages(
        () -> request,
        r -> {
          if (invocations.get() < 1) {
            invocations.incrementAndGet();
            return responseData;
          } else {
            throw new RuntimeException("Provoked exception.");
          }
        })
      )
        .isInstanceOf(GCFacadeCommunicationException.class);
      ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
      Mockito.verify(request, Mockito.atLeastOnce()).setPageNumber(captor.capture());
      assertThat(captor.getAllValues()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("GCC response to pageable requests may not have set total page number.")
    void assumeResultEndForTotalPagesUnset() {
      AtomicInteger invocations = new AtomicInteger();
      int numTotalPages = 1;
      Mockito.when(responseData.getTotalResultPagesCount()).thenReturn(null);
      GCUtil.processAllPages(() -> request, r -> {
        invocations.incrementAndGet();
        return responseData;
      });
      ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
      Mockito.verify(request, Mockito.atLeastOnce()).setPageNumber(captor.capture());
      assertThat(invocations.get()).isEqualTo(numTotalPages);
      assertThat(captor.getAllValues()).containsExactly(1L);
    }
  }

  private static final class InstantArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      LocalDateTime someTime = LocalDateTime.of(2018, 7, 15, 13, 11, 30, 0);
      return Stream.concat(
          Stream.of(
            ZonedDateTime.of(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC), ZoneOffset.UTC),
            ZonedDateTime.of(LocalDateTime.of(2018, 10, 28, 2, 0, 0), ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(LocalDateTime.of(2018, 10, 28, 3, 0, 0), ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
          ),
          ZoneId.getAvailableZoneIds().stream()
            .map(ZoneId::of)
            .map(z -> ZonedDateTime.of(someTime, z))
        )
        .map(Arguments::of);
    }
  }
}
