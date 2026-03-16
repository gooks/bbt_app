package com.czt.bbt.receiver;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\t\u001a\u00020\nH\u0007J\u0012\u0010\u000b\u001a\u00020\n2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u0014J\u0018\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0002R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\u0013"}, d2 = {"Lcom/czt/bbt/receiver/BusAlertRoutineConfigActivity;", "Landroidx/activity/ComponentActivity;", "()V", "database", "Lcom/czt/bbt/data/BusDatabase;", "getDatabase", "()Lcom/czt/bbt/data/BusDatabase;", "setDatabase", "(Lcom/czt/bbt/data/BusDatabase;)V", "RoutineConfigScreen", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "selectForRoutine", "id", "", "type", "", "app_debug"})
public final class BusAlertRoutineConfigActivity extends androidx.activity.ComponentActivity {
    @javax.inject.Inject()
    public com.czt.bbt.data.BusDatabase database;
    
    public BusAlertRoutineConfigActivity() {
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
    public final void RoutineConfigScreen() {
    }
    
    private final void selectForRoutine(long id, java.lang.String type) {
    }
}