package com.ricardo.notasvozlocal;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CaptureActivity extends android.app.Activity implements RecognitionListener {
    private static final int REQ_AUDIO = 7;
    private static final float SAMPLE_RATE = 16000.0f;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<String> confirmedSegments = new ArrayList<>();

    private AppDatabase db;
    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;
    private BroadcastReceiver screenOffReceiver;
    private long openedAt;
    private boolean closing;
    private boolean listening;
    private boolean modelReady;
    private boolean loadingModel;
    private String partialText = "";

    private TextView status;
    private TextView liveText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openedAt = System.currentTimeMillis();
        Log.i(ModelRepository.TAG, "Apertura CaptureActivity");
        Window window = getWindow();
        window.setStatusBarColor(Color.WHITE);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        db = AppDatabase.get(this);
        buildUi();
        registerScreenOffAutosave();
        prepareModel();
    }

    @Override
    protected void onDestroy() {
        unregisterScreenOffAutosave();
        stopRecognition(false);
        io.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(250, 250, 250));

        TextView title = new TextView(this);
        title.setText("Nota de voz");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(20, 20, 20));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("Preparando micrófono...");
        status.setTextSize(15);
        status.setTextColor(Color.rgb(76, 76, 76));
        status.setPadding(0, dp(10), 0, dp(12));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        liveText = new TextView(this);
        liveText.setText("...");
        liveText.setTextSize(22);
        liveText.setTextColor(Color.rgb(25, 25, 25));
        liveText.setGravity(Gravity.TOP);
        liveText.setMinLines(8);
        liveText.setPadding(dp(12), dp(12), dp(12), dp(12));
        liveText.setBackgroundColor(Color.WHITE);
        root.addView(liveText, new LinearLayout.LayoutParams(-1, 0, 1));

        saveButton = new Button(this);
        saveButton.setText("Guardar");
        saveButton.setAllCaps(false);
        saveButton.setTextSize(24);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> saveAndClose());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(76));
        saveParams.setMargins(0, dp(14), 0, dp(8));
        root.addView(saveButton, saveParams);

        Button cancel = new Button(this);
        cancel.setText("Cancelar");
        cancel.setAllCaps(false);
        cancel.setTextSize(18);
        cancel.setOnClickListener(v -> {
            stopRecognition(false);
            finish();
        });
        root.addView(cancel, new LinearLayout.LayoutParams(-1, dp(56)));
        setContentView(root);
    }

    private void prepareModel() {
        if (loadingModel || modelReady) {
            return;
        }
        loadingModel = true;
        status.setText("Preparando micrófono...");
        saveButton.setEnabled(false);
        io.execute(() -> {
            try {
                model = ModelRepository.load(this);
                runOnUiThread(() -> {
                    loadingModel = false;
                    modelReady = true;
                    status.setText("Escuchando...");
                    Log.i(ModelRepository.TAG, "Tiempo apertura hasta modelo listo ms="
                            + (System.currentTimeMillis() - openedAt));
                    ensurePermissionAndStart();
                });
            } catch (Exception e) {
                Log.e(ModelRepository.TAG, "No se pudo cargar el reconocimiento local", e);
                runOnUiThread(() -> {
                    loadingModel = false;
                    status.setText("No se pudo cargar el reconocimiento local: " + e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void ensurePermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        startRecognition();
    }

    private void startRecognition() {
        if (!modelReady || model == null || listening || speechService != null) {
            return;
        }
        try {
            confirmedSegments.clear();
            partialText = "";
            recognizer = new Recognizer(model, SAMPLE_RATE);
            speechService = new SpeechService(recognizer, SAMPLE_RATE);
            boolean started = speechService.startListening(this);
            if (!started) {
                throw new IllegalStateException("No se pudo iniciar SpeechService");
            }
            listening = true;
            saveButton.setEnabled(true);
            liveText.setText("");
            status.setText("Escuchando...");
            Log.i(ModelRepository.TAG, "Inicio de escucha");
            Log.i(ModelRepository.TAG, "Tiempo hasta comenzar a escuchar ms="
                    + (System.currentTimeMillis() - openedAt));
            ModelRepository.logMemory("memoria después de comenzar escucha");
        } catch (Exception e) {
            Log.e(ModelRepository.TAG, "Error al iniciar escucha", e);
            status.setText("No se pudo iniciar el micrófono: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveAndClose() {
        if (closing) {
            return;
        }
        closing = true;
        Log.i(ModelRepository.TAG, "Guardando nota y cerrando captura");
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        appendFinalRecognizerText();
        closeRecognizer();
        listening = false;
        String text = currentText().trim();
        if (!text.isEmpty()) {
            long now = System.currentTimeMillis();
            db.notes().insert(new NoteEntity(text, now, now));
        }
        finish();
    }

    private void registerScreenOffAutosave() {
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    Log.i(ModelRepository.TAG, "Pantalla apagada: guardado automático");
                    saveAndClose();
                }
            }
        };
        registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void unregisterScreenOffAutosave() {
        if (screenOffReceiver == null) {
            return;
        }
        try {
            unregisterReceiver(screenOffReceiver);
        } catch (IllegalArgumentException ignored) {
            Log.w(ModelRepository.TAG, "Receiver de pantalla ya estaba desregistrado");
        }
        screenOffReceiver = null;
    }

    private void stopRecognition(boolean cancel) {
        if (speechService != null) {
            if (cancel) {
                speechService.cancel();
            } else {
                speechService.stop();
            }
            speechService.shutdown();
            speechService = null;
        }
        closeRecognizer();
        listening = false;
    }

    private void closeRecognizer() {
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
    }

    private void appendFinalRecognizerText() {
        if (recognizer == null) {
            return;
        }
        String finalText = extract(recognizer.getFinalResult(), "text");
        addConfirmed(finalText);
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String text = extract(hypothesis, "partial");
        runOnUiThread(() -> {
            partialText = text;
            liveText.setText(currentText());
        });
    }

    @Override
    public void onResult(String hypothesis) {
        String text = extract(hypothesis, "text");
        runOnUiThread(() -> {
            addConfirmed(text);
            partialText = "";
            liveText.setText(currentText());
        });
    }

    @Override
    public void onFinalResult(String hypothesis) {
        String text = extract(hypothesis, "text");
        runOnUiThread(() -> {
            addConfirmed(text);
            partialText = "";
            liveText.setText(currentText());
        });
    }

    @Override
    public void onError(Exception exception) {
        Log.e(ModelRepository.TAG, "Error de reconocimiento", exception);
        runOnUiThread(() -> status.setText("Error de reconocimiento: " + exception.getMessage()));
    }

    @Override
    public void onTimeout() {
        runOnUiThread(() -> stopRecognition(false));
    }

    private String currentText() {
        String confirmed = String.join(" ", confirmedSegments).trim();
        if (partialText == null || partialText.trim().isEmpty()) {
            return confirmed;
        }
        if (confirmed.isEmpty()) {
            return partialText.trim();
        }
        return (confirmed + " " + partialText.trim()).trim();
    }

    private void addConfirmed(String text) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) {
            return;
        }
        if (!confirmedSegments.isEmpty()
                && confirmedSegments.get(confirmedSegments.size() - 1).equals(clean)) {
            return;
        }
        confirmedSegments.add(clean);
    }

    private String extract(String json, String key) {
        try {
            return new JSONObject(json).optString(key, "").trim();
        } catch (Exception e) {
            Log.e(ModelRepository.TAG, "JSON de Vosk no parseable: " + json, e);
            return "";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecognition();
        } else {
            status.setText("Permiso de micrófono necesario.");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
