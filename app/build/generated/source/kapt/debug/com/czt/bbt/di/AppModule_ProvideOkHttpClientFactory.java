package com.czt.bbt.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

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
public final class AppModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<HttpLoggingInterceptor> loggingInterceptorProvider;

  public AppModule_ProvideOkHttpClientFactory(
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider) {
    this.loggingInterceptorProvider = loggingInterceptorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(loggingInterceptorProvider.get());
  }

  public static AppModule_ProvideOkHttpClientFactory create(
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider) {
    return new AppModule_ProvideOkHttpClientFactory(loggingInterceptorProvider);
  }

  public static OkHttpClient provideOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideOkHttpClient(loggingInterceptor));
  }
}
