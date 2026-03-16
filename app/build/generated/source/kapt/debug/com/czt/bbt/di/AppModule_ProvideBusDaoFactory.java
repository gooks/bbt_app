package com.czt.bbt.di;

import com.czt.bbt.data.BusDao;
import com.czt.bbt.data.BusDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AppModule_ProvideBusDaoFactory implements Factory<BusDao> {
  private final Provider<BusDatabase> databaseProvider;

  public AppModule_ProvideBusDaoFactory(Provider<BusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BusDao get() {
    return provideBusDao(databaseProvider.get());
  }

  public static AppModule_ProvideBusDaoFactory create(Provider<BusDatabase> databaseProvider) {
    return new AppModule_ProvideBusDaoFactory(databaseProvider);
  }

  public static BusDao provideBusDao(BusDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBusDao(database));
  }
}
