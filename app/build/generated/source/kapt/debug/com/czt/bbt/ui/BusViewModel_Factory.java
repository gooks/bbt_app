package com.czt.bbt.ui;

import android.content.Context;
import com.czt.bbt.data.BusRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class BusViewModel_Factory implements Factory<BusViewModel> {
  private final Provider<BusRepository> repositoryProvider;

  private final Provider<FusedLocationProviderClient> fusedLocationClientProvider;

  private final Provider<Context> contextProvider;

  public BusViewModel_Factory(Provider<BusRepository> repositoryProvider,
      Provider<FusedLocationProviderClient> fusedLocationClientProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.fusedLocationClientProvider = fusedLocationClientProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public BusViewModel get() {
    return newInstance(repositoryProvider.get(), fusedLocationClientProvider.get(), contextProvider.get());
  }

  public static BusViewModel_Factory create(Provider<BusRepository> repositoryProvider,
      Provider<FusedLocationProviderClient> fusedLocationClientProvider,
      Provider<Context> contextProvider) {
    return new BusViewModel_Factory(repositoryProvider, fusedLocationClientProvider, contextProvider);
  }

  public static BusViewModel newInstance(BusRepository repository,
      FusedLocationProviderClient fusedLocationClient, Context context) {
    return new BusViewModel(repository, fusedLocationClient, context);
  }
}
