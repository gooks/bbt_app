package com.czt.bbt.service;

import com.czt.bbt.data.BusRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BusAlertService_MembersInjector implements MembersInjector<BusAlertService> {
  private final Provider<BusRepository> repositoryProvider;

  private final Provider<FusedLocationProviderClient> fusedLocationClientProvider;

  public BusAlertService_MembersInjector(Provider<BusRepository> repositoryProvider,
      Provider<FusedLocationProviderClient> fusedLocationClientProvider) {
    this.repositoryProvider = repositoryProvider;
    this.fusedLocationClientProvider = fusedLocationClientProvider;
  }

  public static MembersInjector<BusAlertService> create(Provider<BusRepository> repositoryProvider,
      Provider<FusedLocationProviderClient> fusedLocationClientProvider) {
    return new BusAlertService_MembersInjector(repositoryProvider, fusedLocationClientProvider);
  }

  @Override
  public void injectMembers(BusAlertService instance) {
    injectRepository(instance, repositoryProvider.get());
    injectFusedLocationClient(instance, fusedLocationClientProvider.get());
  }

  @InjectedFieldSignature("com.czt.bbt.service.BusAlertService.repository")
  public static void injectRepository(BusAlertService instance, BusRepository repository) {
    instance.repository = repository;
  }

  @InjectedFieldSignature("com.czt.bbt.service.BusAlertService.fusedLocationClient")
  public static void injectFusedLocationClient(BusAlertService instance,
      FusedLocationProviderClient fusedLocationClient) {
    instance.fusedLocationClient = fusedLocationClient;
  }
}
