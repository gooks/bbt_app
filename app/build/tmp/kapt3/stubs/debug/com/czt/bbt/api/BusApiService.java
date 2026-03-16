package com.czt.bbt.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J2\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\tJ2\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\f\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\tJ2\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u000f\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\tJ2\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\f\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\tJ<\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0014\u001a\u00020\u00062\b\b\u0001\u0010\u0015\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0016J2\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\tJ2\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u000f\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\tJ2\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001c0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u00062\b\b\u0003\u0010\b\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\t\u00a8\u0006\u001d"}, d2 = {"Lcom/czt/bbt/api/BusApiService;", "", "getBusArrivalList", "Lretrofit2/Response;", "Lcom/czt/bbt/api/GBusArrivalResponse;", "serviceKey", "", "stationId", "format", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getBusLocationList", "Lcom/czt/bbt/api/GBusLocationResponse;", "routeId", "getBusRouteList", "Lcom/czt/bbt/api/GBusRouteResponse;", "keyword", "getBusRouteStationList", "Lcom/czt/bbt/api/GBusRouteStationResponse;", "getBusStationAroundList", "Lcom/czt/bbt/api/GBusStationAroundResponse;", "x", "y", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getBusStationInfo", "Lcom/czt/bbt/api/GBusStationDetailResponse;", "getBusStationList", "Lcom/czt/bbt/api/GBusStationListResponse;", "getBusStationViaRouteList", "Lcom/czt/bbt/api/GBusStationViaRouteResponse;", "app_debug"})
public abstract interface BusApiService {
    
    @retrofit2.http.GET(value = "6410000/busrouteservice/v2/getBusRouteListv2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusRouteList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "keyword")
    @org.jetbrains.annotations.NotNull()
    java.lang.String keyword, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusRouteResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/buslocationservice/v2/getBusLocationListv2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusLocationList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "routeId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String routeId, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusLocationResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/busstationservice/v2/busStationInfov2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusStationInfo(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "stationId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String stationId, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusStationDetailResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/busstationservice/v2/getBusStationAroundListv2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusStationAroundList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "x")
    @org.jetbrains.annotations.NotNull()
    java.lang.String x, @retrofit2.http.Query(value = "y")
    @org.jetbrains.annotations.NotNull()
    java.lang.String y, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusStationAroundResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/busrouteservice/v2/getBusRouteStationListv2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusRouteStationList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "routeId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String routeId, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusRouteStationResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/busarrivalservice/getBusArrivalList")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusArrivalList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "stationId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String stationId, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusArrivalResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/busstationservice/v2/getBusStationListv2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusStationList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "keyword")
    @org.jetbrains.annotations.NotNull()
    java.lang.String keyword, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusStationListResponse>> $completion);
    
    @retrofit2.http.GET(value = "6410000/busstationservice/v2/getBusStationViaRouteListv2")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBusStationViaRouteList(@retrofit2.http.Query(value = "serviceKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String serviceKey, @retrofit2.http.Query(value = "stationId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String stationId, @retrofit2.http.Query(value = "format")
    @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.czt.bbt.api.GBusStationViaRouteResponse>> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}