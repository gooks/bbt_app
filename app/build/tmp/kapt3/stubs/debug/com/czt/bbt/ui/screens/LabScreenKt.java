package com.czt.bbt.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u001e\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001aT\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\u0010\u0007\u001a\u0004\u0018\u00010\b2\u001a\u0010\t\u001a\u0016\u0012\u0012\u0012\u0010\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f\u0018\u00010\u000b0\n2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u000e\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u0011H\u0007\u001aZ\u0010\u0012\u001a\u00020\u00012\b\u0010\u0007\u001a\u0004\u0018\u00010\b2\u001a\u0010\t\u001a\u0016\u0012\u0012\u0012\u0010\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f\u0018\u00010\u000b0\n2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u000e\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u00112\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u00a8\u0006\u0013"}, d2 = {"ApiUsageScreen", "", "viewModel", "Lcom/czt/bbt/ui/BusViewModel;", "onBack", "Lkotlin/Function0;", "LabScreen", "tts", "Landroid/speech/tts/TextToSpeech;", "wordRange", "Landroidx/compose/runtime/State;", "Lkotlin/Pair;", "", "availableVoices", "", "Landroid/speech/tts/Voice;", "selectedVoice", "Landroidx/compose/runtime/MutableState;", "TtsTestScreen", "app_debug"})
public final class LabScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void LabScreen(@org.jetbrains.annotations.NotNull()
    com.czt.bbt.ui.BusViewModel viewModel, @org.jetbrains.annotations.Nullable()
    android.speech.tts.TextToSpeech tts, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.State<kotlin.Pair<java.lang.Integer, java.lang.Integer>> wordRange, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends android.speech.tts.Voice> availableVoices, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.MutableState<android.speech.tts.Voice> selectedVoice) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void ApiUsageScreen(@org.jetbrains.annotations.NotNull()
    com.czt.bbt.ui.BusViewModel viewModel, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void TtsTestScreen(@org.jetbrains.annotations.Nullable()
    android.speech.tts.TextToSpeech tts, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.State<kotlin.Pair<java.lang.Integer, java.lang.Integer>> wordRange, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends android.speech.tts.Voice> availableVoices, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.MutableState<android.speech.tts.Voice> selectedVoice, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
}