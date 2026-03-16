package com.czt.bbt.di;

import com.czt.bbt.api.BusApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class AppModule_ProvideBusApiServiceFactory implements Factory<BusApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideBusApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public BusApiService get() {
    return provideBusApiService(retrofitProvider.get());
  }

  public static AppModule_ProvideBusApiServiceFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideBusApiServiceFactory(retrofitProvider);
  }

  public static BusApiService provideBusApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBusApiService(retrofit));
  }
}
