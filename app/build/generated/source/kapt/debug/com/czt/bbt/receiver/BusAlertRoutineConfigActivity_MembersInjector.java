package com.czt.bbt.receiver;

import com.czt.bbt.data.BusDatabase;
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
public final class BusAlertRoutineConfigActivity_MembersInjector implements MembersInjector<BusAlertRoutineConfigActivity> {
  private final Provider<BusDatabase> databaseProvider;

  public BusAlertRoutineConfigActivity_MembersInjector(Provider<BusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  public static MembersInjector<BusAlertRoutineConfigActivity> create(
      Provider<BusDatabase> databaseProvider) {
    return new BusAlertRoutineConfigActivity_MembersInjector(databaseProvider);
  }

  @Override
  public void injectMembers(BusAlertRoutineConfigActivity instance) {
    injectDatabase(instance, databaseProvider.get());
  }

  @InjectedFieldSignature("com.czt.bbt.receiver.BusAlertRoutineConfigActivity.database")
  public static void injectDatabase(BusAlertRoutineConfigActivity instance, BusDatabase database) {
    instance.database = database;
  }
}
