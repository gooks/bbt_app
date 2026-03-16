package com.czt.bbt.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b!\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001Bi\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u0012\u0006\u0010\b\u001a\u00020\u0006\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0006\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\u0006\u0012\b\u0010\f\u001a\u0004\u0018\u00010\r\u0012\b\u0010\u000e\u001a\u0004\u0018\u00010\r\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\u0010J\t\u0010!\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\"\u001a\u0004\u0018\u00010\rH\u00c6\u0003J\u0010\u0010#\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0014J\t\u0010$\u001a\u00020\u0003H\u00c6\u0003J\t\u0010%\u001a\u00020\u0006H\u00c6\u0003J\u0010\u0010&\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0014J\t\u0010\'\u001a\u00020\u0006H\u00c6\u0003J\u0010\u0010(\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0014J\t\u0010)\u001a\u00020\u0006H\u00c6\u0003J\u0010\u0010*\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0014J\u000b\u0010+\u001a\u0004\u0018\u00010\rH\u00c6\u0003J\u0088\u0001\u0010,\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\b\u001a\u00020\u00062\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\n\u001a\u00020\u00062\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\r2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\r2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0006H\u00c6\u0001\u00a2\u0006\u0002\u0010-J\u0013\u0010.\u001a\u00020/2\b\u00100\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00101\u001a\u00020\u0006H\u00d6\u0001J\t\u00102\u001a\u00020\rH\u00d6\u0001R\u0011\u0010\b\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0015\u0010\t\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u0015\u001a\u0004\b\u0013\u0010\u0014R\u0013\u0010\f\u001a\u0004\u0018\u00010\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0017R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0012R\u0015\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u0015\u001a\u0004\b\u001a\u0010\u0014R\u0011\u0010\n\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0012R\u0015\u0010\u000b\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u0015\u001a\u0004\b\u001c\u0010\u0014R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0015\u0010\u000f\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u0015\u001a\u0004\b\u001f\u0010\u0014R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001e\u00a8\u00063"}, d2 = {"Lcom/czt/bbt/api/GBusArrivalItem;", "", "routeId", "", "stationId", "predictTime1", "", "predictTime2", "locationNo1", "locationNo2", "remainSeatCnt1", "remainSeatCnt2", "plateNo1", "", "plateNo2", "staOrder", "(JJILjava/lang/Integer;ILjava/lang/Integer;ILjava/lang/Integer;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)V", "getLocationNo1", "()I", "getLocationNo2", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "getPlateNo1", "()Ljava/lang/String;", "getPlateNo2", "getPredictTime1", "getPredictTime2", "getRemainSeatCnt1", "getRemainSeatCnt2", "getRouteId", "()J", "getStaOrder", "getStationId", "component1", "component10", "component11", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(JJILjava/lang/Integer;ILjava/lang/Integer;ILjava/lang/Integer;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Lcom/czt/bbt/api/GBusArrivalItem;", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class GBusArrivalItem {
    private final long routeId = 0L;
    private final long stationId = 0L;
    private final int predictTime1 = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer predictTime2 = null;
    private final int locationNo1 = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer locationNo2 = null;
    private final int remainSeatCnt1 = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer remainSeatCnt2 = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String plateNo1 = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String plateNo2 = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer staOrder = null;
    
    public GBusArrivalItem(long routeId, long stationId, int predictTime1, @org.jetbrains.annotations.Nullable()
    java.lang.Integer predictTime2, int locationNo1, @org.jetbrains.annotations.Nullable()
    java.lang.Integer locationNo2, int remainSeatCnt1, @org.jetbrains.annotations.Nullable()
    java.lang.Integer remainSeatCnt2, @org.jetbrains.annotations.Nullable()
    java.lang.String plateNo1, @org.jetbrains.annotations.Nullable()
    java.lang.String plateNo2, @org.jetbrains.annotations.Nullable()
    java.lang.Integer staOrder) {
        super();
    }
    
    public final long getRouteId() {
        return 0L;
    }
    
    public final long getStationId() {
        return 0L;
    }
    
    public final int getPredictTime1() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getPredictTime2() {
        return null;
    }
    
    public final int getLocationNo1() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getLocationNo2() {
        return null;
    }
    
    public final int getRemainSeatCnt1() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getRemainSeatCnt2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPlateNo1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPlateNo2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getStaOrder() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component11() {
        return null;
    }
    
    public final long component2() {
        return 0L;
    }
    
    public final int component3() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component4() {
        return null;
    }
    
    public final int component5() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component6() {
        return null;
    }
    
    public final int component7() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.czt.bbt.api.GBusArrivalItem copy(long routeId, long stationId, int predictTime1, @org.jetbrains.annotations.Nullable()
    java.lang.Integer predictTime2, int locationNo1, @org.jetbrains.annotations.Nullable()
    java.lang.Integer locationNo2, int remainSeatCnt1, @org.jetbrains.annotations.Nullable()
    java.lang.Integer remainSeatCnt2, @org.jetbrains.annotations.Nullable()
    java.lang.String plateNo1, @org.jetbrains.annotations.Nullable()
    java.lang.String plateNo2, @org.jetbrains.annotations.Nullable()
    java.lang.Integer staOrder) {
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