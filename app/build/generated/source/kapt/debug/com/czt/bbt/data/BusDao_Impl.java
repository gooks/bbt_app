package com.czt.bbt.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.czt.bbt.model.ArrivalAlert;
import com.czt.bbt.model.CachedRouteStation;
import com.czt.bbt.model.Converters;
import com.czt.bbt.model.RideAlert;
import com.czt.bbt.model.RideHistory;
import com.czt.bbt.model.SystemLog;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BusDao_Impl implements BusDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RideAlert> __insertionAdapterOfRideAlert;

  private final Converters __converters = new Converters();

  private final EntityInsertionAdapter<ArrivalAlert> __insertionAdapterOfArrivalAlert;

  private final EntityInsertionAdapter<RideHistory> __insertionAdapterOfRideHistory;

  private final EntityInsertionAdapter<CachedRouteStation> __insertionAdapterOfCachedRouteStation;

  private final EntityInsertionAdapter<SystemLog> __insertionAdapterOfSystemLog;

  private final EntityDeletionOrUpdateAdapter<RideAlert> __deletionAdapterOfRideAlert;

  private final EntityDeletionOrUpdateAdapter<ArrivalAlert> __deletionAdapterOfArrivalAlert;

  private final EntityDeletionOrUpdateAdapter<RideHistory> __deletionAdapterOfRideHistory;

  private final EntityDeletionOrUpdateAdapter<RideAlert> __updateAdapterOfRideAlert;

  private final EntityDeletionOrUpdateAdapter<ArrivalAlert> __updateAdapterOfArrivalAlert;

  private final EntityDeletionOrUpdateAdapter<RideHistory> __updateAdapterOfRideHistory;

  private final SharedSQLiteStatement __preparedStmtOfClearOldCache;

  private final SharedSQLiteStatement __preparedStmtOfClearSystemLogs;

  public BusDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRideAlert = new EntityInsertionAdapter<RideAlert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `ride_alerts` (`id`,`busNumber`,`busRouteId`,`destinationStationName`,`destinationStationId`,`destinationStationSeq`,`shareEmails`,`shareKakao`,`shareMemo`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideAlert entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getBusNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getBusNumber());
        }
        if (entity.getBusRouteId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getBusRouteId());
        }
        if (entity.getDestinationStationName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDestinationStationName());
        }
        if (entity.getDestinationStationId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDestinationStationId());
        }
        statement.bindLong(6, entity.getDestinationStationSeq());
        final String _tmp = __converters.fromStringList(entity.getShareEmails());
        if (_tmp == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp);
        }
        final int _tmp_1 = entity.getShareKakao() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        if (entity.getShareMemo() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getShareMemo());
        }
      }
    };
    this.__insertionAdapterOfArrivalAlert = new EntityInsertionAdapter<ArrivalAlert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `arrival_alerts` (`id`,`stationName`,`stationId`,`targetBusNumbers`,`targetBusNames`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArrivalAlert entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getStationName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getStationName());
        }
        if (entity.getStationId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getStationId());
        }
        final String _tmp = __converters.fromStringList(entity.getTargetBusNumbers());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.fromStringList(entity.getTargetBusNames());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_1);
        }
      }
    };
    this.__insertionAdapterOfRideHistory = new EntityInsertionAdapter<RideHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `ride_histories` (`id`,`date`,`boardingTime`,`boardingStationName`,`busNumber`,`plateNumber`,`alightTime`,`alightStationName`,`isManuallyStopped`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideHistory entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        statement.bindLong(3, entity.getBoardingTime());
        if (entity.getBoardingStationName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getBoardingStationName());
        }
        if (entity.getBusNumber() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBusNumber());
        }
        if (entity.getPlateNumber() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getPlateNumber());
        }
        if (entity.getAlightTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getAlightTime());
        }
        if (entity.getAlightStationName() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getAlightStationName());
        }
        final int _tmp = entity.isManuallyStopped() ? 1 : 0;
        statement.bindLong(9, _tmp);
      }
    };
    this.__insertionAdapterOfCachedRouteStation = new EntityInsertionAdapter<CachedRouteStation>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `route_station_cache` (`id`,`routeId`,`stationId`,`stationName`,`stationSeq`,`x`,`y`,`mobileNo`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedRouteStation entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getRouteId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getRouteId());
        }
        if (entity.getStationId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getStationId());
        }
        if (entity.getStationName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getStationName());
        }
        statement.bindLong(5, entity.getStationSeq());
        statement.bindDouble(6, entity.getX());
        statement.bindDouble(7, entity.getY());
        if (entity.getMobileNo() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getMobileNo());
        }
        statement.bindLong(9, entity.getTimestamp());
      }
    };
    this.__insertionAdapterOfSystemLog = new EntityInsertionAdapter<SystemLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `system_logs` (`id`,`timestamp`,`tag`,`message`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SystemLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        if (entity.getTag() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTag());
        }
        if (entity.getMessage() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMessage());
        }
      }
    };
    this.__deletionAdapterOfRideAlert = new EntityDeletionOrUpdateAdapter<RideAlert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `ride_alerts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideAlert entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfArrivalAlert = new EntityDeletionOrUpdateAdapter<ArrivalAlert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `arrival_alerts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArrivalAlert entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfRideHistory = new EntityDeletionOrUpdateAdapter<RideHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `ride_histories` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideHistory entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfRideAlert = new EntityDeletionOrUpdateAdapter<RideAlert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `ride_alerts` SET `id` = ?,`busNumber` = ?,`busRouteId` = ?,`destinationStationName` = ?,`destinationStationId` = ?,`destinationStationSeq` = ?,`shareEmails` = ?,`shareKakao` = ?,`shareMemo` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideAlert entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getBusNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getBusNumber());
        }
        if (entity.getBusRouteId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getBusRouteId());
        }
        if (entity.getDestinationStationName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDestinationStationName());
        }
        if (entity.getDestinationStationId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDestinationStationId());
        }
        statement.bindLong(6, entity.getDestinationStationSeq());
        final String _tmp = __converters.fromStringList(entity.getShareEmails());
        if (_tmp == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp);
        }
        final int _tmp_1 = entity.getShareKakao() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        if (entity.getShareMemo() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getShareMemo());
        }
        statement.bindLong(10, entity.getId());
      }
    };
    this.__updateAdapterOfArrivalAlert = new EntityDeletionOrUpdateAdapter<ArrivalAlert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `arrival_alerts` SET `id` = ?,`stationName` = ?,`stationId` = ?,`targetBusNumbers` = ?,`targetBusNames` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArrivalAlert entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getStationName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getStationName());
        }
        if (entity.getStationId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getStationId());
        }
        final String _tmp = __converters.fromStringList(entity.getTargetBusNumbers());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.fromStringList(entity.getTargetBusNames());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_1);
        }
        statement.bindLong(6, entity.getId());
      }
    };
    this.__updateAdapterOfRideHistory = new EntityDeletionOrUpdateAdapter<RideHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `ride_histories` SET `id` = ?,`date` = ?,`boardingTime` = ?,`boardingStationName` = ?,`busNumber` = ?,`plateNumber` = ?,`alightTime` = ?,`alightStationName` = ?,`isManuallyStopped` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideHistory entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        statement.bindLong(3, entity.getBoardingTime());
        if (entity.getBoardingStationName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getBoardingStationName());
        }
        if (entity.getBusNumber() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBusNumber());
        }
        if (entity.getPlateNumber() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getPlateNumber());
        }
        if (entity.getAlightTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getAlightTime());
        }
        if (entity.getAlightStationName() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getAlightStationName());
        }
        final int _tmp = entity.isManuallyStopped() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfClearOldCache = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM route_station_cache WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearSystemLogs = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM system_logs";
        return _query;
      }
    };
  }

  @Override
  public Object insertRideAlert(final RideAlert alert,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRideAlert.insertAndReturnId(alert);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertArrivalAlert(final ArrivalAlert alert,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfArrivalAlert.insertAndReturnId(alert);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRideHistory(final RideHistory history,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRideHistory.insertAndReturnId(history);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCachedStations(final List<CachedRouteStation> stations,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedRouteStation.insert(stations);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSystemLog(final SystemLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSystemLog.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRideAlert(final RideAlert alert,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRideAlert.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteArrivalAlert(final ArrivalAlert alert,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfArrivalAlert.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRideHistory(final RideHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRideHistory.handle(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRideAlert(final RideAlert alert,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRideAlert.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateArrivalAlert(final ArrivalAlert alert,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfArrivalAlert.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRideHistory(final RideHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRideHistory.handle(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearOldCache(final long expiryTime, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearOldCache.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, expiryTime);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearOldCache.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearSystemLogs(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearSystemLogs.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearSystemLogs.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RideAlert>> getAllRideAlerts() {
    final String _sql = "SELECT * FROM ride_alerts";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ride_alerts"}, new Callable<List<RideAlert>>() {
      @Override
      @NonNull
      public List<RideAlert> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBusNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "busNumber");
          final int _cursorIndexOfBusRouteId = CursorUtil.getColumnIndexOrThrow(_cursor, "busRouteId");
          final int _cursorIndexOfDestinationStationName = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationStationName");
          final int _cursorIndexOfDestinationStationId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationStationId");
          final int _cursorIndexOfDestinationStationSeq = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationStationSeq");
          final int _cursorIndexOfShareEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "shareEmails");
          final int _cursorIndexOfShareKakao = CursorUtil.getColumnIndexOrThrow(_cursor, "shareKakao");
          final int _cursorIndexOfShareMemo = CursorUtil.getColumnIndexOrThrow(_cursor, "shareMemo");
          final List<RideAlert> _result = new ArrayList<RideAlert>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RideAlert _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpBusNumber;
            if (_cursor.isNull(_cursorIndexOfBusNumber)) {
              _tmpBusNumber = null;
            } else {
              _tmpBusNumber = _cursor.getString(_cursorIndexOfBusNumber);
            }
            final String _tmpBusRouteId;
            if (_cursor.isNull(_cursorIndexOfBusRouteId)) {
              _tmpBusRouteId = null;
            } else {
              _tmpBusRouteId = _cursor.getString(_cursorIndexOfBusRouteId);
            }
            final String _tmpDestinationStationName;
            if (_cursor.isNull(_cursorIndexOfDestinationStationName)) {
              _tmpDestinationStationName = null;
            } else {
              _tmpDestinationStationName = _cursor.getString(_cursorIndexOfDestinationStationName);
            }
            final String _tmpDestinationStationId;
            if (_cursor.isNull(_cursorIndexOfDestinationStationId)) {
              _tmpDestinationStationId = null;
            } else {
              _tmpDestinationStationId = _cursor.getString(_cursorIndexOfDestinationStationId);
            }
            final int _tmpDestinationStationSeq;
            _tmpDestinationStationSeq = _cursor.getInt(_cursorIndexOfDestinationStationSeq);
            final List<String> _tmpShareEmails;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfShareEmails)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfShareEmails);
            }
            _tmpShareEmails = __converters.toStringList(_tmp);
            final boolean _tmpShareKakao;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfShareKakao);
            _tmpShareKakao = _tmp_1 != 0;
            final String _tmpShareMemo;
            if (_cursor.isNull(_cursorIndexOfShareMemo)) {
              _tmpShareMemo = null;
            } else {
              _tmpShareMemo = _cursor.getString(_cursorIndexOfShareMemo);
            }
            _item = new RideAlert(_tmpId,_tmpBusNumber,_tmpBusRouteId,_tmpDestinationStationName,_tmpDestinationStationId,_tmpDestinationStationSeq,_tmpShareEmails,_tmpShareKakao,_tmpShareMemo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ArrivalAlert>> getAllArrivalAlerts() {
    final String _sql = "SELECT * FROM arrival_alerts";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"arrival_alerts"}, new Callable<List<ArrivalAlert>>() {
      @Override
      @NonNull
      public List<ArrivalAlert> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStationName = CursorUtil.getColumnIndexOrThrow(_cursor, "stationName");
          final int _cursorIndexOfStationId = CursorUtil.getColumnIndexOrThrow(_cursor, "stationId");
          final int _cursorIndexOfTargetBusNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "targetBusNumbers");
          final int _cursorIndexOfTargetBusNames = CursorUtil.getColumnIndexOrThrow(_cursor, "targetBusNames");
          final List<ArrivalAlert> _result = new ArrayList<ArrivalAlert>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ArrivalAlert _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpStationName;
            if (_cursor.isNull(_cursorIndexOfStationName)) {
              _tmpStationName = null;
            } else {
              _tmpStationName = _cursor.getString(_cursorIndexOfStationName);
            }
            final String _tmpStationId;
            if (_cursor.isNull(_cursorIndexOfStationId)) {
              _tmpStationId = null;
            } else {
              _tmpStationId = _cursor.getString(_cursorIndexOfStationId);
            }
            final List<String> _tmpTargetBusNumbers;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfTargetBusNumbers)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfTargetBusNumbers);
            }
            _tmpTargetBusNumbers = __converters.toStringList(_tmp);
            final List<String> _tmpTargetBusNames;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfTargetBusNames)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfTargetBusNames);
            }
            _tmpTargetBusNames = __converters.toStringList(_tmp_1);
            _item = new ArrivalAlert(_tmpId,_tmpStationName,_tmpStationId,_tmpTargetBusNumbers,_tmpTargetBusNames);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<RideHistory>> getAllRideHistories() {
    final String _sql = "SELECT * FROM ride_histories ORDER BY boardingTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ride_histories"}, new Callable<List<RideHistory>>() {
      @Override
      @NonNull
      public List<RideHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfBoardingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "boardingTime");
          final int _cursorIndexOfBoardingStationName = CursorUtil.getColumnIndexOrThrow(_cursor, "boardingStationName");
          final int _cursorIndexOfBusNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "busNumber");
          final int _cursorIndexOfPlateNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "plateNumber");
          final int _cursorIndexOfAlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "alightTime");
          final int _cursorIndexOfAlightStationName = CursorUtil.getColumnIndexOrThrow(_cursor, "alightStationName");
          final int _cursorIndexOfIsManuallyStopped = CursorUtil.getColumnIndexOrThrow(_cursor, "isManuallyStopped");
          final List<RideHistory> _result = new ArrayList<RideHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RideHistory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final long _tmpBoardingTime;
            _tmpBoardingTime = _cursor.getLong(_cursorIndexOfBoardingTime);
            final String _tmpBoardingStationName;
            if (_cursor.isNull(_cursorIndexOfBoardingStationName)) {
              _tmpBoardingStationName = null;
            } else {
              _tmpBoardingStationName = _cursor.getString(_cursorIndexOfBoardingStationName);
            }
            final String _tmpBusNumber;
            if (_cursor.isNull(_cursorIndexOfBusNumber)) {
              _tmpBusNumber = null;
            } else {
              _tmpBusNumber = _cursor.getString(_cursorIndexOfBusNumber);
            }
            final String _tmpPlateNumber;
            if (_cursor.isNull(_cursorIndexOfPlateNumber)) {
              _tmpPlateNumber = null;
            } else {
              _tmpPlateNumber = _cursor.getString(_cursorIndexOfPlateNumber);
            }
            final Long _tmpAlightTime;
            if (_cursor.isNull(_cursorIndexOfAlightTime)) {
              _tmpAlightTime = null;
            } else {
              _tmpAlightTime = _cursor.getLong(_cursorIndexOfAlightTime);
            }
            final String _tmpAlightStationName;
            if (_cursor.isNull(_cursorIndexOfAlightStationName)) {
              _tmpAlightStationName = null;
            } else {
              _tmpAlightStationName = _cursor.getString(_cursorIndexOfAlightStationName);
            }
            final boolean _tmpIsManuallyStopped;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManuallyStopped);
            _tmpIsManuallyStopped = _tmp != 0;
            _item = new RideHistory(_tmpId,_tmpDate,_tmpBoardingTime,_tmpBoardingStationName,_tmpBusNumber,_tmpPlateNumber,_tmpAlightTime,_tmpAlightStationName,_tmpIsManuallyStopped);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCachedStations(final String routeId,
      final Continuation<? super List<CachedRouteStation>> $completion) {
    final String _sql = "SELECT * FROM route_station_cache WHERE routeId = ? ORDER BY stationSeq ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (routeId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, routeId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CachedRouteStation>>() {
      @Override
      @NonNull
      public List<CachedRouteStation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRouteId = CursorUtil.getColumnIndexOrThrow(_cursor, "routeId");
          final int _cursorIndexOfStationId = CursorUtil.getColumnIndexOrThrow(_cursor, "stationId");
          final int _cursorIndexOfStationName = CursorUtil.getColumnIndexOrThrow(_cursor, "stationName");
          final int _cursorIndexOfStationSeq = CursorUtil.getColumnIndexOrThrow(_cursor, "stationSeq");
          final int _cursorIndexOfX = CursorUtil.getColumnIndexOrThrow(_cursor, "x");
          final int _cursorIndexOfY = CursorUtil.getColumnIndexOrThrow(_cursor, "y");
          final int _cursorIndexOfMobileNo = CursorUtil.getColumnIndexOrThrow(_cursor, "mobileNo");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<CachedRouteStation> _result = new ArrayList<CachedRouteStation>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedRouteStation _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpRouteId;
            if (_cursor.isNull(_cursorIndexOfRouteId)) {
              _tmpRouteId = null;
            } else {
              _tmpRouteId = _cursor.getString(_cursorIndexOfRouteId);
            }
            final String _tmpStationId;
            if (_cursor.isNull(_cursorIndexOfStationId)) {
              _tmpStationId = null;
            } else {
              _tmpStationId = _cursor.getString(_cursorIndexOfStationId);
            }
            final String _tmpStationName;
            if (_cursor.isNull(_cursorIndexOfStationName)) {
              _tmpStationName = null;
            } else {
              _tmpStationName = _cursor.getString(_cursorIndexOfStationName);
            }
            final int _tmpStationSeq;
            _tmpStationSeq = _cursor.getInt(_cursorIndexOfStationSeq);
            final double _tmpX;
            _tmpX = _cursor.getDouble(_cursorIndexOfX);
            final double _tmpY;
            _tmpY = _cursor.getDouble(_cursorIndexOfY);
            final String _tmpMobileNo;
            if (_cursor.isNull(_cursorIndexOfMobileNo)) {
              _tmpMobileNo = null;
            } else {
              _tmpMobileNo = _cursor.getString(_cursorIndexOfMobileNo);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new CachedRouteStation(_tmpId,_tmpRouteId,_tmpStationId,_tmpStationName,_tmpStationSeq,_tmpX,_tmpY,_tmpMobileNo,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SystemLog>> getRecentSystemLogs() {
    final String _sql = "SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 200";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"system_logs"}, new Callable<List<SystemLog>>() {
      @Override
      @NonNull
      public List<SystemLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final List<SystemLog> _result = new ArrayList<SystemLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SystemLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpTag;
            if (_cursor.isNull(_cursorIndexOfTag)) {
              _tmpTag = null;
            } else {
              _tmpTag = _cursor.getString(_cursorIndexOfTag);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            _item = new SystemLog(_tmpId,_tmpTimestamp,_tmpTag,_tmpMessage);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
