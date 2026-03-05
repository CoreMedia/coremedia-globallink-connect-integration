package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import org.jspecify.annotations.NullMarked;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration(proxyBeanMethods = false)
@NullMarked
public class GCExchangeFacadeConfiguration {
  @Scope(SCOPE_SINGLETON)
  @Bean
  public GCExchangeFacade gcExchangeFacade() {
    return Mockito.mock(GCExchangeFacade.class, invocation -> {
      throw new IllegalStateException("Unexpected method called on GCExchangeFacade Mock. You may require to extend your mock setup. Invocation: " + invocation);
    });
  }
}
