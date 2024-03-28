package com.coremedia.labs.translation.gcc.facade.disabled;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCFacadeException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gs4tr.gcc.restclient.GCExchange;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * <p>
 * This facade will do essentially nothing. It will signal this either via
 * throwing exceptions or by providing default answers. One intended use case
 * is to disable communication to GCC by intention, for example for service
 * maintenance slots.
 * </p>
 * <p>
 * To get an instance of this facade, use {@link GCExchangeFacadeSessionProvider}.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public final class DisabledGCExchangeFacade implements GCExchangeFacade {
  private static final GCExchangeFacade INSTANCE = new DisabledGCExchangeFacade();

  private DisabledGCExchangeFacade() {
  }

  static GCExchangeFacade getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException if called
   * @implNote Will throw an {@link UnsupportedOperationException} as there is no delegate available.
   */
  @Override
  public GCExchange getDelegate() {
    throw new UnsupportedOperationException("This facade does not provide a delegate.");
  }

  @Override
  public String uploadContent(String fileName, Resource resource, Locale sourceLocale) {
    throw createDisabledException();
  }

  @Override
  public long submitSubmission(@Nullable String subject, @Nullable String comment, ZonedDateTime dueDate, @Nullable String workflow, @Nullable String submitter, Locale sourceLocale, Map<String, List<Locale>> contentMap) {
    throw createDisabledException();
  }

  @Override
  public int cancelSubmission(long submissionId) {
    throw createDisabledException();
  }

  @Override
  public void downloadCompletedTasks(long submissionId, BiPredicate<? super InputStream, ? super GCTaskModel> taskDataConsumer) {
    throw createDisabledException();
  }

  @Override
  public void confirmCompletedTasks(long submissionId, Set<? super Locale> completedLocales) {
    throw createDisabledException();
  }

  @Override
  public void confirmCancelledTasks(long submissionIds) {
    throw createDisabledException();
  }

  @Override
  public GCSubmissionModel getSubmission(long submissionId) {
    return GCSubmissionModel.builder(submissionId).build();
  }

  private static GCFacadeException createDisabledException() {
    return new GCFacadeException("GCC Service disabled.");
  }

}
