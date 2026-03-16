package com.czt.bbt.widget;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000b\u001a\u00020\fH\u0007J\u0012\u0010\r\u001a\u00020\f2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0014J \u0010\u0010\u001a\u00020\f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0014H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0005\u001a\u00020\u00068\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\n\u00a8\u0006\u0016"}, d2 = {"Lcom/czt/bbt/widget/BusAlertWidgetConfigActivity;", "Landroidx/activity/ComponentActivity;", "()V", "appWidgetId", "", "database", "Lcom/czt/bbt/data/BusDatabase;", "getDatabase", "()Lcom/czt/bbt/data/BusDatabase;", "setDatabase", "(Lcom/czt/bbt/data/BusDatabase;)V", "WidgetConfigScreen", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "selectAlert", "id", "", "type", "", "title", "app_debug"})
public final class BusAlertWidgetConfigActivity extends androidx.activity.ComponentActivity {
    @javax.inject.Inject()
    public com.czt.bbt.data.BusDatabase database;
    private int appWidgetId = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
    
    public BusAlertWidgetConfigActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.czt.bbt.data.BusDatabase getDatabase() {
        return null;
    }
    
    public final void setDatabase(@org.jetbrains.annotations.NotNull()
    com.czt.bbt.data.BusDatabase p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @androidx.compose.runtime.Composable()
    public final void WidgetConfigScreen() {
    }
    
    private final void selectAlert(long id, java.lang.String type, java.lang.String title) {
    }
}