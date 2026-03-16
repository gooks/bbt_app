package com.czt.bbt.service;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00f0\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0017\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\u0005\b\u0007\u0018\u0000 \u0085\u00012\u00020\u00012\u00020\u00022\u00020\u0003:\u0006\u0084\u0001\u0085\u0001\u0086\u0001B\u0005\u00a2\u0006\u0002\u0010\u0004J(\u0010=\u001a\u0004\u0018\u00010>2\u0006\u0010?\u001a\u00020\u00102\u0006\u0010@\u001a\u00020\u00102\u0006\u0010A\u001a\u00020\u0010H\u0082@\u00a2\u0006\u0002\u0010BJ\u0018\u0010C\u001a\u0004\u0018\u00010\u00142\u0006\u0010D\u001a\u00020\tH\u0082@\u00a2\u0006\u0002\u0010EJ\b\u0010F\u001a\u00020GH\u0002J\b\u0010H\u001a\u00020GH\u0002J\b\u0010I\u001a\u00020GH\u0002J\u0010\u0010J\u001a\u00020K2\u0006\u0010L\u001a\u00020\u0010H\u0002J\b\u0010M\u001a\u00020GH\u0002J\b\u0010N\u001a\u00020GH\u0002J\b\u0010O\u001a\u00020GH\u0002J\b\u0010P\u001a\u00020GH\u0002J\u001a\u0010Q\u001a\u00020G2\b\u0010R\u001a\u0004\u0018\u00010\u00062\u0006\u0010S\u001a\u00020\u0014H\u0016J\u0014\u0010T\u001a\u0004\u0018\u00010U2\b\u0010V\u001a\u0004\u0018\u00010WH\u0016J\b\u0010X\u001a\u00020GH\u0016J\b\u0010Y\u001a\u00020GH\u0016J\u0010\u0010Z\u001a\u00020G2\u0006\u0010[\u001a\u00020\u0014H\u0016J\u0012\u0010\\\u001a\u00020G2\b\u0010]\u001a\u0004\u0018\u00010^H\u0016J\"\u0010_\u001a\u00020\u00142\b\u0010V\u001a\u0004\u0018\u00010W2\u0006\u0010`\u001a\u00020\u00142\u0006\u0010a\u001a\u00020\u0014H\u0016J\u0018\u0010b\u001a\b\u0012\u0004\u0012\u00020c032\b\u0010d\u001a\u0004\u0018\u00010eH\u0002J\u0018\u0010f\u001a\b\u0012\u0004\u0012\u00020g032\b\u0010d\u001a\u0004\u0018\u00010eH\u0002J\u0018\u0010h\u001a\b\u0012\u0004\u0012\u00020i032\b\u0010d\u001a\u0004\u0018\u00010eH\u0002J\b\u0010j\u001a\u00020GH\u0002J:\u0010k\u001a\u00020G2\u0006\u0010l\u001a\u00020\u00102\u0006\u0010m\u001a\u00020\u00102\u0006\u0010n\u001a\u00020\u00102\u0006\u0010o\u001a\u00020\t2\u0006\u0010p\u001a\u00020\u00102\b\b\u0002\u0010q\u001a\u00020\u0010H\u0002J\u0010\u0010r\u001a\u00020G2\u0006\u0010s\u001a\u00020\u0010H\u0002J\u0010\u0010t\u001a\u00020G2\u0006\u0010D\u001a\u00020\tH\u0002J\b\u0010u\u001a\u00020GH\u0003J\u0010\u0010v\u001a\u00020G2\u0006\u0010D\u001a\u00020\tH\u0002J\b\u0010w\u001a\u00020GH\u0002J\u0010\u0010x\u001a\u00020G2\u0006\u0010D\u001a\u00020\tH\u0002J\b\u0010y\u001a\u00020GH\u0002J\b\u0010z\u001a\u00020GH\u0002J \u0010{\u001a\u00020G2\u0006\u0010D\u001a\u00020\t2\u0006\u0010|\u001a\u00020\u00102\u0006\u0010L\u001a\u00020\u0010H\u0002J\u0010\u0010}\u001a\u00020G2\u0006\u0010L\u001a\u00020\u0010H\u0002J;\u0010~\u001a\u0004\u0018\u00010\u0014\"\b\b\u0000\u0010\u007f*\u00020e*\b\u0012\u0004\u0012\u0002H\u007f032\u0015\u0010\u0080\u0001\u001a\u0010\u0012\u0004\u0012\u0002H\u007f\u0012\u0005\u0012\u00030\u0082\u00010\u0081\u0001H\u0002\u00a2\u0006\u0003\u0010\u0083\u0001R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\n0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\f0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0015\u001a\u00020\u00168\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001aR\u000e\u0010\u001b\u001a\u00020\u001cX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010 \u001a\u00020\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\"\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u00140\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010#\u001a\u0004\u0018\u00010$X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010%\u001a\u00020&X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\'\u001a\u00020(X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020*X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010,\u001a\u00020-8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b.\u0010/\"\u0004\b0\u00101R \u00102\u001a\u0014\u0012\u0004\u0012\u00020\u0010\u0012\n\u0012\b\u0012\u0004\u0012\u000204030\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u00105\u001a\u000206X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u00107\u001a\u000208X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u00109\u001a\u0004\u0018\u00010:X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010;\u001a\u00020<X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0087\u0001"}, d2 = {"Lcom/czt/bbt/service/BusAlertService;", "Landroid/app/Service;", "Landroid/hardware/SensorEventListener;", "Landroid/speech/tts/TextToSpeech$OnInitListener;", "()V", "accelerometer", "Landroid/hardware/Sensor;", "activeArrivalAlerts", "", "", "Lcom/czt/bbt/model/ArrivalAlert;", "activeArrivalJobs", "Lkotlinx/coroutines/Job;", "activeRideAlert", "Lcom/czt/bbt/model/RideAlert;", "boardingStationName", "", "boardingTime", "currentBusPlate", "destStationIndex", "", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "getFusedLocationClient", "()Lcom/google/android/gms/location/FusedLocationProviderClient;", "setFusedLocationClient", "(Lcom/google/android/gms/location/FusedLocationProviderClient;)V", "gson", "Lcom/google/gson/Gson;", "isAlightingDetected", "", "isBoardingDetected", "isTtsReady", "lastAlertStops", "lastArrivalAlertStops", "lastLocation", "Landroid/location/Location;", "locationCallback", "Lcom/google/android/gms/location/LocationCallback;", "mode", "Lcom/czt/bbt/service/BusAlertService$Mode;", "notificationManager", "Landroid/app/NotificationManager;", "potentialBoardingTime", "repository", "Lcom/czt/bbt/data/BusRepository;", "getRepository", "()Lcom/czt/bbt/data/BusRepository;", "setRepository", "(Lcom/czt/bbt/data/BusRepository;)V", "routeStationsCache", "", "Lcom/czt/bbt/model/CachedRouteStation;", "sensorManager", "Landroid/hardware/SensorManager;", "serviceScope", "Lkotlinx/coroutines/CoroutineScope;", "tts", "Landroid/speech/tts/TextToSpeech;", "vibrator", "Landroid/os/Vibrator;", "calculateArrivalFromLocation", "Lcom/czt/bbt/service/BusAlertService$ArrivalEstimate;", "routeId", "targetStationId", "busName", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkArrivalStatus", "alertId", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkBoardingStatusWithTimeout", "", "checkRideStatus", "confirmBoarding", "createNotification", "Landroid/app/Notification;", "content", "createNotificationChannel", "handleAlight", "handleManualStop", "notifyWidgetUpdate", "onAccuracyChanged", "sensor", "accuracy", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onInit", "status", "onSensorChanged", "event", "Landroid/hardware/SensorEvent;", "onStartCommand", "flags", "startId", "parseArrivalList", "Lcom/czt/bbt/api/GBusArrivalItem;", "data", "", "parseLocationList", "Lcom/czt/bbt/api/GBusLocationItem;", "parseRouteStations", "Lcom/czt/bbt/api/GBusRouteStationItem;", "revertToWaitingStatus", "shareStatus", "type", "busNo", "plateNo", "time", "station", "summary", "speak", "text", "startArrivalMode", "startLocationUpdates", "startRideMode", "stopArrivalAll", "stopIndividualAlert", "stopRideOnly", "triggerAlertEffects", "updateArrivalNotification", "title", "updateNotification", "indexOfMinByOrNull", "T", "selector", "Lkotlin/Function1;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;)Ljava/lang/Integer;", "ArrivalEstimate", "Companion", "Mode", "app_debug"})
public final class BusAlertService extends android.app.Service implements android.hardware.SensorEventListener, android.speech.tts.TextToSpeech.OnInitListener {
    @javax.inject.Inject()
    public com.czt.bbt.data.BusRepository repository;
    @javax.inject.Inject()
    public com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope serviceScope = null;
    private android.hardware.SensorManager sensorManager;
    @org.jetbrains.annotations.Nullable()
    private android.hardware.Sensor accelerometer;
    private android.app.NotificationManager notificationManager;
    private android.os.Vibrator vibrator;
    @org.jetbrains.annotations.Nullable()
    private android.speech.tts.TextToSpeech tts;
    private boolean isTtsReady = false;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private com.czt.bbt.service.BusAlertService.Mode mode = com.czt.bbt.service.BusAlertService.Mode.IDLE;
    @org.jetbrains.annotations.Nullable()
    private com.czt.bbt.model.RideAlert activeRideAlert;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.Long, kotlinx.coroutines.Job> activeArrivalJobs = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.Long, com.czt.bbt.model.ArrivalAlert> activeArrivalAlerts = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<com.czt.bbt.model.CachedRouteStation>> routeStationsCache = null;
    private int lastAlertStops = -1;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.Long, java.lang.Integer> lastArrivalAlertStops = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String boardingStationName;
    private boolean isBoardingDetected = false;
    private boolean isAlightingDetected = false;
    private long potentialBoardingTime = 0L;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentBusPlate;
    private long boardingTime = 0L;
    @org.jetbrains.annotations.Nullable()
    private android.location.Location lastLocation;
    private int destStationIndex = -1;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_START_RIDE = "ACTION_START_RIDE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_START_ARRIVAL = "ACTION_START_ARRIVAL";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_STOP = "ACTION_STOP";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_STOP_RIDE = "ACTION_STOP_RIDE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_STOP_ARRIVAL_ALL = "ACTION_STOP_ARRIVAL_ALL";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_STOP_ALERT = "ACTION_STOP_ALERT";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_REFRESH = "ACTION_REFRESH";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_ALERT_ID = "EXTRA_ALERT_ID";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_ID = "bus_alert_channel";
    public static final int NOTIFICATION_ID = 1001;
    @org.jetbrains.annotations.NotNull()
    private final com.google.android.gms.location.LocationCallback locationCallback = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.czt.bbt.service.BusAlertService.Companion Companion = null;
    
    public BusAlertService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.czt.bbt.data.BusRepository getRepository() {
        return null;
    }
    
    public final void setRepository(@org.jetbrains.annotations.NotNull()
    com.czt.bbt.data.BusRepository p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.google.android.gms.location.FusedLocationProviderClient getFusedLocationClient() {
        return null;
    }
    
    public final void setFusedLocationClient(@org.jetbrains.annotations.NotNull()
    com.google.android.gms.location.FusedLocationProviderClient p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @java.lang.Override()
    public void onInit(int status) {
    }
    
    private final void speak(java.lang.String text) {
    }
    
    private final void notifyWidgetUpdate() {
    }
    
    @java.lang.Override()
    public int onStartCommand(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent, int flags, int startId) {
        return 0;
    }
    
    private final void stopRideOnly() {
    }
    
    private final void stopArrivalAll() {
    }
    
    private final void handleManualStop() {
    }
    
    private final void stopIndividualAlert(long alertId) {
    }
    
    private final void startRideMode(long alertId) {
    }
    
    private final void checkBoardingStatusWithTimeout() {
    }
    
    private final void revertToWaitingStatus() {
    }
    
    private final void startArrivalMode(long alertId) {
    }
    
    private final java.lang.Object checkArrivalStatus(long alertId, kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    private final void updateArrivalNotification(long alertId, java.lang.String title, java.lang.String content) {
    }
    
    private final java.lang.Object calculateArrivalFromLocation(java.lang.String routeId, java.lang.String targetStationId, java.lang.String busName, kotlin.coroutines.Continuation<? super com.czt.bbt.service.BusAlertService.ArrivalEstimate> $completion) {
        return null;
    }
    
    @java.lang.Override()
    public void onSensorChanged(@org.jetbrains.annotations.Nullable()
    android.hardware.SensorEvent event) {
    }
    
    private final void confirmBoarding() {
    }
    
    private final void shareStatus(java.lang.String type, java.lang.String busNo, java.lang.String plateNo, long time, java.lang.String station, java.lang.String summary) {
    }
    
    private final void checkRideStatus() {
    }
    
    private final void handleAlight() {
    }
    
    private final void triggerAlertEffects() {
    }
    
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    private final void startLocationUpdates() {
    }
    
    private final void createNotificationChannel() {
    }
    
    private final android.app.Notification createNotification(java.lang.String content) {
        return null;
    }
    
    private final void updateNotification(java.lang.String content) {
    }
    
    private final java.util.List<com.czt.bbt.api.GBusArrivalItem> parseArrivalList(java.lang.Object data) {
        return null;
    }
    
    private final java.util.List<com.czt.bbt.api.GBusRouteStationItem> parseRouteStations(java.lang.Object data) {
        return null;
    }
    
    private final java.util.List<com.czt.bbt.api.GBusLocationItem> parseLocationList(java.lang.Object data) {
        return null;
    }
    
    private final <T extends java.lang.Object>java.lang.Integer indexOfMinByOrNull(java.util.List<? extends T> $this$indexOfMinByOrNull, kotlin.jvm.functions.Function1<? super T, java.lang.Float> selector) {
        return null;
    }
    
    @java.lang.Override()
    public void onAccuracyChanged(@org.jetbrains.annotations.Nullable()
    android.hardware.Sensor sensor, int accuracy) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public android.os.IBinder onBind(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent) {
        return null;
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u000f\b\u0086\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0007H\u00c6\u0003J\'\u0010\u0011\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0012\u001a\u00020\u00072\b\u0010\u0013\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\u0015\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0016"}, d2 = {"Lcom/czt/bbt/service/BusAlertService$ArrivalEstimate;", "", "busName", "", "remainStops", "", "isBeforeGarage", "", "(Ljava/lang/String;IZ)V", "getBusName", "()Ljava/lang/String;", "()Z", "getRemainStops", "()I", "component1", "component2", "component3", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
    public static final class ArrivalEstimate {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String busName = null;
        private final int remainStops = 0;
        private final boolean isBeforeGarage = false;
        
        public ArrivalEstimate(@org.jetbrains.annotations.NotNull()
        java.lang.String busName, int remainStops, boolean isBeforeGarage) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getBusName() {
            return null;
        }
        
        public final int getRemainStops() {
            return 0;
        }
        
        public final boolean isBeforeGarage() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        public final int component2() {
            return 0;
        }
        
        public final boolean component3() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.czt.bbt.service.BusAlertService.ArrivalEstimate copy(@org.jetbrains.annotations.NotNull()
        java.lang.String busName, int remainStops, boolean isBeforeGarage) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/czt/bbt/service/BusAlertService$Companion;", "", "()V", "ACTION_REFRESH", "", "ACTION_START_ARRIVAL", "ACTION_START_RIDE", "ACTION_STOP", "ACTION_STOP_ALERT", "ACTION_STOP_ARRIVAL_ALL", "ACTION_STOP_RIDE", "CHANNEL_ID", "EXTRA_ALERT_ID", "NOTIFICATION_ID", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/czt/bbt/service/BusAlertService$Mode;", "", "(Ljava/lang/String;I)V", "IDLE", "RIDE", "ARRIVAL", "app_debug"})
    public static enum Mode {
        /*public static final*/ IDLE /* = new IDLE() */,
        /*public static final*/ RIDE /* = new RIDE() */,
        /*public static final*/ ARRIVAL /* = new ARRIVAL() */;
        
        Mode() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.czt.bbt.service.BusAlertService.Mode> getEntries() {
            return null;
        }
    }
}