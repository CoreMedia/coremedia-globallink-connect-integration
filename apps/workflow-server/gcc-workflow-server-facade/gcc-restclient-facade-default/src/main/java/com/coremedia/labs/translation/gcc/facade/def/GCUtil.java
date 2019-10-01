package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.gs4tr.gcc.restclient.dto.PageableResponseData;
import org.gs4tr.gcc.restclient.request.PageableRequest;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;

/**
 * Utility class for GCC Facade.
 */
@DefaultAnnotation(NonNull.class)
final class GCUtil {
  private GCUtil() {
    // Utility class
  }

  /**
   * Timestamps for GCC are always in UTC timezone. Thus, this method
   * converts the given zoned date-time object into a date for UTC timezone.
   *
   * @param dateTime date-time object to convert
   * @return date in UTC timezone
   */
  static Date toUnixDateUtc(ZonedDateTime dateTime) {
    ZonedDateTime utcDateTime = dateTime.withZoneSameInstant(UTC);
    return Date.from(utcDateTime.toInstant());
  }

  /**
   * Executes the {@link PageableRequest} until all results were received. Note,
   * that because of asynchronous updates on server this method cannot guarantee
   * to process all pages as well as some results may be duplicated because
   * the moved to a different page meanwhile.
   *
   * @param rawRequestSupplier creates the raw request with default paging configuration; note, that you may override
   *                           the page size for this raw request which will be taken into account during pagination
   * @param requestExecutor    executes the requests; RuntimeExceptions thrown by this executor will be wrapped into a
   *                           {@link GCFacadeCommunicationException}; the returned response is just used to extract
   *                           the total page number, so it is task of the executor to merge all results
   * @param <I>                the request type
   * @param <O>                the response type
   * @throws GCFacadeCommunicationException if pagination has been interrupted by a {@code RuntimeException} during
   *                           request processing
   */
  static <I extends PageableRequest, O extends PageableResponseData> void processAllPages(Supplier<I> rawRequestSupplier, Function<I, O> requestExecutor) {
    // Initial Page Number
    long currentPageNumber = 1L;
    Long totalPageNumber;

    I request = rawRequestSupplier.get();

    do {
      request.setPageNumber(currentPageNumber);

      O response;
      try {
        response = requestExecutor.apply(request);
      } catch (RuntimeException e) {
        throw new GCFacadeCommunicationException(e, String.format("Failure while processing page %d for request: %s.", currentPageNumber, request));
      }
      totalPageNumber = response.getTotalResultPagesCount();
      if (totalPageNumber == null) {
        // As it seems for empty pages it may happen, that the total pages count
        // is null. Thus, taken as indicator, that we are done with
        // pagination.
        break;
      }
      currentPageNumber++;
    } while (currentPageNumber <= totalPageNumber);
  }
}
