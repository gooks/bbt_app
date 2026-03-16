package com.czt.bbt.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BusDatabase_Impl extends BusDatabase {
  private volatile BusDao _busDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ride_alerts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `busNumber` TEXT NOT NULL, `busRouteId` TEXT NOT NULL, `destinationStationName` TEXT NOT NULL, `destinationStationId` TEXT NOT NULL, `destinationStationSeq` INTEGER NOT NULL, `shareEmails` TEXT NOT NULL, `shareKakao` INTEGER NOT NULL, `shareMemo` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `arrival_alerts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stationName` TEXT NOT NULL, `stationId` TEXT NOT NULL, `targetBusNumbers` TEXT NOT NULL, `targetBusNames` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ride_histories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `boardingTime` INTEGER NOT NULL, `boardingStationName` TEXT NOT NULL, `busNumber` TEXT NOT NULL, `plateNumber` TEXT, `alightTime` INTEGER, `alightStationName` TEXT, `isManuallyStopped` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `system_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `tag` TEXT NOT NULL, `message` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `route_station_cache` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routeId` TEXT NOT NULL, `stationId` TEXT NOT NULL, `stationName` TEXT NOT NULL, `stationSeq` INTEGER NOT NULL, `x` REAL NOT NULL, `y` REAL NOT NULL, `mobileNo` TEXT, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '83839eac56050ccf1dd9696f3ce52252')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `ride_alerts`");
        db.execSQL("DROP TABLE IF EXISTS `arrival_alerts`");
        db.execSQL("DROP TABLE IF EXISTS `ride_histories`");
        db.execSQL("DROP TABLE IF EXISTS `system_logs`");
        db.execSQL("DROP TABLE IF EXISTS `route_station_cache`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsRideAlerts = new HashMap<String, TableInfo.Column>(9);
        _columnsRideAlerts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("busNumber", new TableInfo.Column("busNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("busRouteId", new TableInfo.Column("busRouteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("destinationStationName", new TableInfo.Column("destinationStationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("destinationStationId", new TableInfo.Column("destinationStationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("destinationStationSeq", new TableInfo.Column("destinationStationSeq", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("shareEmails", new TableInfo.Column("shareEmails", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("shareKakao", new TableInfo.Column("shareKakao", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideAlerts.put("shareMemo", new TableInfo.Column("shareMemo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRideAlerts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRideAlerts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRideAlerts = new TableInfo("ride_alerts", _columnsRideAlerts, _foreignKeysRideAlerts, _indicesRideAlerts);
        final TableInfo _existingRideAlerts = TableInfo.read(db, "ride_alerts");
        if (!_infoRideAlerts.equals(_existingRideAlerts)) {
          return new RoomOpenHelper.ValidationResult(false, "ride_alerts(com.czt.bbt.model.RideAlert).\n"
                  + " Expected:\n" + _infoRideAlerts + "\n"
                  + " Found:\n" + _existingRideAlerts);
        }
        final HashMap<String, TableInfo.Column> _columnsArrivalAlerts = new HashMap<String, TableInfo.Column>(5);
        _columnsArrivalAlerts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArrivalAlerts.put("stationName", new TableInfo.Column("stationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArrivalAlerts.put("stationId", new TableInfo.Column("stationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArrivalAlerts.put("targetBusNumbers", new TableInfo.Column("targetBusNumbers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArrivalAlerts.put("targetBusNames", new TableInfo.Column("targetBusNames", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysArrivalAlerts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesArrivalAlerts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoArrivalAlerts = new TableInfo("arrival_alerts", _columnsArrivalAlerts, _foreignKeysArrivalAlerts, _indicesArrivalAlerts);
        final TableInfo _existingArrivalAlerts = TableInfo.read(db, "arrival_alerts");
        if (!_infoArrivalAlerts.equals(_existingArrivalAlerts)) {
          return new RoomOpenHelper.ValidationResult(false, "arrival_alerts(com.czt.bbt.model.ArrivalAlert).\n"
                  + " Expected:\n" + _infoArrivalAlerts + "\n"
                  + " Found:\n" + _existingArrivalAlerts);
        }
        final HashMap<String, TableInfo.Column> _columnsRideHistories = new HashMap<String, TableInfo.Column>(9);
        _columnsRideHistories.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("boardingTime", new TableInfo.Column("boardingTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("boardingStationName", new TableInfo.Column("boardingStationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("busNumber", new TableInfo.Column("busNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("plateNumber", new TableInfo.Column("plateNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("alightTime", new TableInfo.Column("alightTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("alightStationName", new TableInfo.Column("alightStationName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRideHistories.put("isManuallyStopped", new TableInfo.Column("isManuallyStopped", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRideHistories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRideHistories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRideHistories = new TableInfo("ride_histories", _columnsRideHistories, _foreignKeysRideHistories, _indicesRideHistories);
        final TableInfo _existingRideHistories = TableInfo.read(db, "ride_histories");
        if (!_infoRideHistories.equals(_existingRideHistories)) {
          return new RoomOpenHelper.ValidationResult(false, "ride_histories(com.czt.bbt.model.RideHistory).\n"
                  + " Expected:\n" + _infoRideHistories + "\n"
                  + " Found:\n" + _existingRideHistories);
        }
        final HashMap<String, TableInfo.Column> _columnsSystemLogs = new HashMap<String, TableInfo.Column>(4);
        _columnsSystemLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystemLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystemLogs.put("tag", new TableInfo.Column("tag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystemLogs.put("message", new TableInfo.Column("message", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSystemLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSystemLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSystemLogs = new TableInfo("system_logs", _columnsSystemLogs, _foreignKeysSystemLogs, _indicesSystemLogs);
        final TableInfo _existingSystemLogs = TableInfo.read(db, "system_logs");
        if (!_infoSystemLogs.equals(_existingSystemLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "system_logs(com.czt.bbt.model.SystemLog).\n"
                  + " Expected:\n" + _infoSystemLogs + "\n"
                  + " Found:\n" + _existingSystemLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsRouteStationCache = new HashMap<String, TableInfo.Column>(9);
        _columnsRouteStationCache.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("routeId", new TableInfo.Column("routeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("stationId", new TableInfo.Column("stationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("stationName", new TableInfo.Column("stationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("stationSeq", new TableInfo.Column("stationSeq", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("x", new TableInfo.Column("x", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("y", new TableInfo.Column("y", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("mobileNo", new TableInfo.Column("mobileNo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteStationCache.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRouteStationCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRouteStationCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRouteStationCache = new TableInfo("route_station_cache", _columnsRouteStationCache, _foreignKeysRouteStationCache, _indicesRouteStationCache);
        final TableInfo _existingRouteStationCache = TableInfo.read(db, "route_station_cache");
        if (!_infoRouteStationCache.equals(_existingRouteStationCache)) {
          return new RoomOpenHelper.ValidationResult(false, "route_station_cache(com.czt.bbt.model.CachedRouteStation).\n"
                  + " Expected:\n" + _infoRouteStationCache + "\n"
                  + " Found:\n" + _existingRouteStationCache);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "83839eac56050ccf1dd9696f3ce52252", "fb677a22d291d1507191daff8e947fe6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "ride_alerts","arrival_alerts","ride_histories","system_logs","route_station_cache");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `ride_alerts`");
      _db.execSQL("DELETE FROM `arrival_alerts`");
      _db.execSQL("DELETE FROM `ride_histories`");
      _db.execSQL("DELETE FROM `system_logs`");
      _db.execSQL("DELETE FROM `route_station_cache`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BusDao.class, BusDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BusDao busDao() {
    if (_busDao != null) {
      return _busDao;
    } else {
      synchronized(this) {
        if(_busDao == null) {
          _busDao = new BusDao_Impl(this);
        }
        return _busDao;
      }
    }
  }
}
