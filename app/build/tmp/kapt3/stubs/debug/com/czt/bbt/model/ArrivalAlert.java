package com.czt.bbt.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0011\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B?\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u000e\b\u0001\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00050\b\u0012\u000e\b\u0003\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00050\b\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0005H\u00c6\u0003J\u000f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00050\bH\u00c6\u0003J\u000f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00050\bH\u00c6\u0003JG\u0010\u0018\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\u000e\b\u0003\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00050\b2\u000e\b\u0003\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00050\bH\u00c6\u0001J\u0013\u0010\u0019\u001a\u00020\u001a2\b\u0010\u001b\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001c\u001a\u00020\u001dH\u00d6\u0001J\t\u0010\u001e\u001a\u00020\u0005H\u00d6\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00050\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00050\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0011\u00a8\u0006\u001f"}, d2 = {"Lcom/czt/bbt/model/ArrivalAlert;", "", "id", "", "stationName", "", "stationId", "targetBusNumbers", "", "targetBusNames", "(JLjava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/util/List;)V", "getId", "()J", "getStationId", "()Ljava/lang/String;", "getStationName", "getTargetBusNames", "()Ljava/util/List;", "getTargetBusNumbers", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
@androidx.room.Entity(tableName = "arrival_alerts")
public final class ArrivalAlert {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final long id = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String stationName = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String stationId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> targetBusNumbers = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> targetBusNames = null;
    
    public ArrivalAlert(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String stationName, @org.jetbrains.annotations.NotNull()
    java.lang.String stationId, @androidx.room.TypeConverters(value = {com.czt.bbt.model.Converters.class})
    @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> targetBusNumbers, @androidx.room.TypeConverters(value = {com.czt.bbt.model.Converters.class})
    @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> targetBusNames) {
        super();
    }
    
    public final long getId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStationName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStationId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getTargetBusNumbers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getTargetBusNames() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.czt.bbt.model.ArrivalAlert copy(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String stationName, @org.jetbrains.annotations.NotNull()
    java.lang.String stationId, @androidx.room.TypeConverters(value = {com.czt.bbt.model.Converters.class})
    @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> targetBusNumbers, @androidx.room.TypeConverters(value = {com.czt.bbt.model.Converters.class})
    @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> targetBusNames) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}