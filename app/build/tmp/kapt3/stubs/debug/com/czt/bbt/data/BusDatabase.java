package com.czt.bbt.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&\u00a8\u0006\u0005"}, d2 = {"Lcom/czt/bbt/data/BusDatabase;", "Landroidx/room/RoomDatabase;", "()V", "busDao", "Lcom/czt/bbt/data/BusDao;", "app_debug"})
@androidx.room.Database(entities = {com.czt.bbt.model.RideAlert.class, com.czt.bbt.model.ArrivalAlert.class, com.czt.bbt.model.RideHistory.class, com.czt.bbt.model.SystemLog.class, com.czt.bbt.model.CachedRouteStation.class}, version = 6, exportSchema = false)
@androidx.room.TypeConverters(value = {com.czt.bbt.model.Converters.class})
public abstract class BusDatabase extends androidx.room.RoomDatabase {
    
    public BusDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.czt.bbt.data.BusDao busDao();
}