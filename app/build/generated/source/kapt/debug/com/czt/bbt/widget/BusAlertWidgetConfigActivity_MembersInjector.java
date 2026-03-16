package com.czt.bbt.widget;

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
public final class BusAlertWidgetConfigActivity_MembersInjector implements MembersInjector<BusAlertWidgetConfigActivity> {
  private final Provider<BusDatabase> databaseProvider;

  public BusAlertWidgetConfigActivity_MembersInjector(Provider<BusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  public static MembersInjector<BusAlertWidgetConfigActivity> create(
      Provider<BusDatabase> databaseProvider) {
    return new BusAlertWidgetConfigActivity_MembersInjector(databaseProvider);
  }

  @Override
  public void injectMembers(BusAlertWidgetConfigActivity instance) {
    injectDatabase(instance, databaseProvider.get());
  }

  @InjectedFieldSignature("com.czt.bbt.widget.BusAlertWidgetConfigActivity.database")
  public static void injectDatabase(BusAlertWidgetConfigActivity instance, BusDatabase database) {
    instance.database = database;
  }
}
