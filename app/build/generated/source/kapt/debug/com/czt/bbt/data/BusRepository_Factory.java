package com.czt.bbt.data;

import android.content.Context;
import com.czt.bbt.api.BusApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class BusRepository_Factory implements Factory<BusRepository> {
  private final Provider<BusApiService> apiServiceProvider;

  private final Provider<BusDao> busDaoProvider;

  private final Provider<Context> contextProvider;

  public BusRepository_Factory(Provider<BusApiService> apiServiceProvider,
      Provider<BusDao> busDaoProvider, Provider<Context> contextProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.busDaoProvider = busDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public BusRepository get() {
    return newInstance(apiServiceProvider.get(), busDaoProvider.get(), contextProvider.get());
  }

  public static BusRepository_Factory create(Provider<BusApiService> apiServiceProvider,
      Provider<BusDao> busDaoProvider, Provider<Context> contextProvider) {
    return new BusRepository_Factory(apiServiceProvider, busDaoProvider, contextProvider);
  }

  public static BusRepository newInstance(BusApiService apiService, BusDao busDao,
      Context context) {
    return new BusRepository(apiService, busDao, context);
  }
}
