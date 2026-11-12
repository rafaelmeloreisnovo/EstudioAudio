/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Foreground-only survey UI. Session statistics are observations, not a trained acoustic model. */
public final class InstrumentSurveyActivity extends Activity {
    private static final int PERMISSIONS = 742;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, CheckBox> choices = new LinkedHashMap<>();
    private final List<android.view.View> controls = new ArrayList<>();
    private InstrumentSurveyCollector collector;
    private InstrumentSurveyModel model;
    private TextView dashboard;
    private EditText window, rows, rotations, minimum, age, accuracy, rate, duration, site;
    private CheckBox sensitive;
    private Button start, stop, export;
    private boolean running, writing;
    private long deadline;
    private String sessionMeta = "", lastArchive = "Nenhuma sessão arquivada nesta abertura.";
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (SystemClock.elapsedRealtime() >= deadline) { finishSession("Duração concluída"); return; }
            collector.snapshot();
            render();
            handler.postDelayed(this, 2000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        collector = new InstrumentSurveyCollector(this, (id, epoch, now, source, value, quality, sector, status) -> {
            if (running && model != null) model.observe(id, epoch, now, source, value, quality, sector, status);
        });
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 16, 20, 24);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        label(root, "Varredura de instrumentos", 23);
        label(root, "Escolha família, grupo ou item. A sessão observa apenas enquanto esta tela está aberta. "
                + "GPS usa atualizações em primeiro plano; Wi-Fi e celular usam dados em cache. "
                + "O aplicativo não altera rádios nem acessa ServiceMode privado.", 14);
        label(root, "Aprendizado = resumos estatísticos locais por item e setor de orientação. "
                + "Não equivale a diagnóstico, previsão acústica ou certificação industrial. "
                + "Selecione vetor de rotação para classificar orientação; ausência fica sem setor.", 14);
        site = field(root, "Nome do ponto/ambiente (opcional, fica somente no relatório desta sessão)", "", false);
        window = field(root, "Janela de rotação dos dados (ms, 100–600000)", "10000", true);
        rows = field(root, "Máximo de linhas por janela", "500", true);
        rotations = field(root, "Janelas retidas (1–64; produto com linhas ≤ 20000)", "4", true);
        minimum = field(root, "Mínimo de amostras aceitas para resumo (2–20000)", "10", true);
        age = field(root, "Idade máxima da fonte (ms, 0–600000)", "10000", true);
        accuracy = field(root, "Incerteza horizontal máxima do GPS (metros)", "30", true);
        rate = field(root, "Taxa solicitada dos sensores (Hz, 1–50; efetiva depende do aparelho)", "20", true);
        duration = field(root, "Duração máxima da sessão (segundos, 1–600)", "60", true);
        label(root, "GPS: intervalo solicitado de 1 s. Contexto/caches: consulta a cada 2 s. "
                + "Releituras do mesmo instante não contam novamente como aprendizado. "
                + "Resumos sem coordenadas/rádios são guardados no aparelho; dados brutos ficam em memória.", 14);
        buildSelector(root, collector.inventory());
        Button permissions = button(root, "Permitir localização / Bluetooth dos itens selecionados");
        controls.add(permissions);
        permissions.setOnClickListener(v -> requestSelectedPermissions());
        start = button(root, "Iniciar nova sessão");
        start.setOnClickListener(v -> begin());
        stop = button(root, "Parar e guardar resumo");
        stop.setEnabled(false);
        stop.setOnClickListener(v -> finishSession("Parada pelo usuário"));
        sensitive = new CheckBox(this);
        sensitive.setText("Incluir localização e dados de rádio no arquivo exportado (pode identificar lugares)");
        root.addView(sensitive);
        export = button(root, "Exportar sessão parada · CSV + configuração em ZIP");
        export.setOnClickListener(v -> exportSession());
        Button history = button(root, "Ver resumos locais retidos");
        history.setOnClickListener(v -> showHistory());
        dashboard = label(root, "Escolha os instrumentos e inicie uma sessão.", 14);
    }

    private TextView label(LinearLayout root, String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setPadding(0, 10, 0, 10); root.addView(view); return view;
    }
    private Button button(LinearLayout root, String text) {
        Button view = new Button(this); view.setText(text); view.setAllCaps(false); root.addView(view); return view;
    }
    private EditText field(LinearLayout root, String title, String value, boolean numeric) {
        label(root, title, 14);
        EditText view = new EditText(this);
        view.setSingleLine(true);
        view.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        view.setText(value); root.addView(view); controls.add(view); return view;
    }
    private void buildSelector(LinearLayout root, List<InstrumentSurveyModel.Item> items) {
        Map<String, Map<String, List<InstrumentSurveyModel.Item>>> families = new LinkedHashMap<>();
        for (InstrumentSurveyModel.Item item : items)
            families.computeIfAbsent(item.family, key -> new LinkedHashMap<>())
                    .computeIfAbsent(item.group, key -> new ArrayList<>()).add(item);
        for (Map.Entry<String, Map<String, List<InstrumentSurveyModel.Item>>> family : families.entrySet()) {
            label(root, "Família: " + family.getKey(), 18);
            List<CheckBox> familyMembers = new ArrayList<>();
            selectionButtons(root, familyMembers);
            for (Map.Entry<String, List<InstrumentSurveyModel.Item>> group : family.getValue().entrySet()) {
                label(root, "Grupo: " + group.getKey(), 15);
                List<CheckBox> groupMembers = new ArrayList<>();
                selectionButtons(root, groupMembers);
                for (InstrumentSurveyModel.Item item : group.getValue()) {
                    CheckBox leaf = new CheckBox(this);
                    leaf.setText(item.label + " [" + item.unit + "]");
                    root.addView(leaf); choices.put(item.id, leaf); controls.add(leaf);
                    groupMembers.add(leaf); familyMembers.add(leaf);
                }
            }
        }
    }
    private void selectionButtons(LinearLayout root, List<CheckBox> members) {
        LinearLayout commands = new LinearLayout(this);
        root.addView(commands);
        Button all = button(commands, "Selecionar todos"), none = button(commands, "Limpar seleção");
        controls.add(all); controls.add(none);
        all.setOnClickListener(v -> { for (CheckBox member : members) member.setChecked(true); });
        none.setOnClickListener(v -> { for (CheckBox member : members) member.setChecked(false); });
    }
    private Set<String> selected() {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, CheckBox> item : choices.entrySet()) if (item.getValue().isChecked()) result.add(item.getKey());
        return result;
    }
    private void requestSelectedPermissions() {
        List<String> requested = new ArrayList<>();
        boolean loc = false, bt = false;
        for (String id : selected()) {
            loc |= id.startsWith("location.") || id.startsWith("gnss.") || id.startsWith("wifi.") || id.startsWith("cell.");
            bt |= id.startsWith("bluetooth.");
        }
        if (loc && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requested.add(Manifest.permission.ACCESS_FINE_LOCATION); requested.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (bt && Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            requested.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (requested.isEmpty()) dashboard.setText("Itens selecionados não exigem novas permissões, ou permissões já concedidas.");
        else requestPermissions(requested.toArray(new String[0]), PERMISSIONS);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == PERMISSIONS) dashboard.setText("Permissões atualizadas. Inicie a sessão quando desejar. "
                + "Recusa ou localização apenas aproximada serão registradas como indisponíveis para fontes precisas.");
    }
    private int integer(EditText view) { return Integer.parseInt(view.getText().toString().trim()); }
    private void begin() {
        if (running || writing) return;
        Set<String> selection = selected();
        if (selection.isEmpty()) { dashboard.setText("Selecione pelo menos um item."); return; }
        try {
            int hz = integer(rate), seconds = integer(duration);
            if (hz < 1 || hz > 50 || seconds < 1 || seconds > 600) throw new IllegalArgumentException("Taxa: 1–50 Hz. Duração: 1–600 s.");
            InstrumentSurveyModel.Config config = new InstrumentSurveyModel.Config(integer(window), integer(rows), integer(rotations),
                    integer(minimum), Long.parseLong(age.getText().toString().trim()), Double.parseDouble(accuracy.getText().toString().trim()));
            InstrumentSurveyModel next = new InstrumentSurveyModel(config);
            for (InstrumentSurveyModel.Item item : collector.inventory()) if (selection.contains(item.id)) next.register(item);
            model = next;
            String place = site.getText().toString().replace('\n', ' ').replace('\r', ' ');
            if (place.length() > 160) place = place.substring(0, 160);
            sessionMeta = "schema=rafaelia.instrument-survey/v1\nsource_sha=" + BuildConfig.SOURCE_SHA
                    + "\nstarted_epoch_ms=" + System.currentTimeMillis() + "\nsite_label=" + place
                    + "\nrequested_sensor_hz=" + hz + "\nduration_limit_s=" + seconds
                    + "\nselection=" + selection + "\nposition_join=TIMESTAMP_CONTEXT_NOT_TOWER_COORDINATES"
                    + "\nexternal_spatial_database=NOT_CONNECTED\nlearning=DESCRIPTIVE_NOT_PREDICTIVE\nphysical_validation=NOT_RUN\n";
            running = true; deadline = SystemClock.elapsedRealtime() + seconds * 1000L;
            setRunning(true);
            collector.start(selection, 1000000 / hz);
            handler.post(tick);
        } catch (RuntimeException error) {
            running = false; collector.stop(); setRunning(false);
            dashboard.setText("Configuração/sessão recusada: " + error.getMessage());
        }
    }
    private void setRunning(boolean active) {
        for (android.view.View control : controls) control.setEnabled(!active);
        start.setEnabled(!active && !writing); stop.setEnabled(active); export.setEnabled(!active && !writing);
    }
    private void render() {
        dashboard.setText((running ? "COLETANDO · " + Math.max(0, (deadline - SystemClock.elapsedRealtime()) / 1000) + " s restantes\n" : "SESSÃO PARADA\n")
                + collector.status() + "\n" + (model == null ? "" : model.dashboard()) + "\n" + lastArchive);
    }
    private void finishSession(String reason) {
        if (!running) return;
        running = false; handler.removeCallbacks(tick); collector.stop(); setRunning(false);
        sessionMeta += "stop_reason=" + reason + "\nstopped_epoch_ms=" + System.currentTimeMillis() + "\n";
        // Only redacted descriptive summaries survive process death. Never save raw/location/radio automatically.
        try {
            File dir = new File(getFilesDir(), "instrument_summaries");
            if (!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("mkdir");
            byte[] bytes = model.exportSummaryCsv(false).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 2 * 1024 * 1024) throw new java.io.IOException("summary_limit");
            File target = new File(dir, "summary_" + System.currentTimeMillis() + ".csv");
            File pending = new File(dir, "pending.tmp");
            File pendingMetadata = new File(dir, "pending.txt.tmp");
            Files.write(pending.toPath(), bytes);
            Files.write(pendingMetadata.toPath(), (sessionMeta.replaceAll("(?m)^site_label=.*$", "site_label=REDACTED")
                    + "saved_sensitive_fields=false\n" + model.configurationCsv()).getBytes(StandardCharsets.UTF_8));
            if (!pending.renameTo(target)) throw new java.io.IOException("atomic_rename");
            File provenance = new File(dir, target.getName() + ".txt");
            if (!pendingMetadata.renameTo(provenance)) {
                if (!target.delete()) throw new java.io.IOException("metadata_rollback_delete");
                throw new java.io.IOException("metadata_atomic_rename");
            }
            File[] entries = dir.listFiles((d, name) -> name.startsWith("summary_") && name.endsWith(".csv"));
            if (entries != null) {
                Arrays.sort(entries, (a, b) -> a.getName().compareTo(b.getName()));
                int keep = Math.min(8, model.config().rotationCount);
                for (int i = 0; i < entries.length - keep; i++) {
                    if (!entries[i].delete()) throw new java.io.IOException("retention_delete");
                    File metadata = new File(dir, entries[i].getName() + ".txt");
                    if (metadata.exists() && !metadata.delete()) throw new java.io.IOException("retention_metadata_delete");
                }
            }
            lastArchive = "Resumo sem localização/rádios guardado. Retenção: até " + Math.min(8, model.config().rotationCount) + " sessões. Dados brutos permanecem só nesta sessão em memória.";
        } catch (Exception error) { lastArchive = "Falha ao guardar resumo: " + error.getClass().getSimpleName(); }
        render();
    }
    private File[] historyFiles() {
        File[] files = new File(getFilesDir(), "instrument_summaries").listFiles((d, name) -> name.startsWith("summary_") && name.endsWith(".csv"));
        if (files == null) return new File[0];
        Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName()));
        return files;
    }
    private void showHistory() {
        File[] files = historyFiles();
        String[] names = new String[Math.min(8, files.length)];
        for (int i = 0; i < names.length; i++) names[i] = files[i].getName();
        if (names.length == 0) { dashboard.setText("Nenhum resumo local retido."); return; }
        new AlertDialog.Builder(this).setTitle("Sessões anteriores · resumos descritivos").setItems(names, (d, which) -> {
            try {
                if (files[which].length() > 2 * 1024 * 1024) throw new java.io.IOException("limit");
                String csv = new String(Files.readAllBytes(files[which].toPath()), StandardCharsets.UTF_8);
                TextView text = new TextView(this); text.setText(csv); text.setTextIsSelectable(true);
                ScrollView scroll = new ScrollView(this); scroll.addView(text);
                new AlertDialog.Builder(this).setTitle(names[which]).setView(scroll).setPositiveButton("Fechar", null).show();
            } catch (Exception error) { dashboard.setText("Resumo indisponível: " + error.getClass().getSimpleName()); }
        }).setNegativeButton("Fechar", null).show();
    }
    private void exportSession() {
        if (running || writing || model == null) { dashboard.setText("Pare uma sessão antes de exportar."); return; }
        final InstrumentSurveyModel frozen = model;
        final boolean include = sensitive.isChecked();
        final String meta = include ? sessionMeta : sessionMeta.replaceAll("(?m)^site_label=.*$", "site_label=REDACTED");
        writing = true; setRunning(false);
        new Thread(() -> {
            Uri uri = null;
            String result;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "EstudioAudio_instrumentos_" + System.currentTimeMillis() + ".zip");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RafaeliaAudio/Instrumentos");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("insert");
                try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                    if (stream == null) throw new java.io.IOException("stream");
                    RfaStoredZip zip = new RfaStoredZip(stream);
                    zip.add("00_manifest.txt", (meta + "sensitive_location_radio=" + include
                            + "\nraw_retention=BOUNDED_RING_OLDER_ROWS_EVICTED\nsummary_scope=SESSION_ACCEPTED_SAMPLES"
                            + "\nintegrity=CRC32_NOT_AUTHENTICITY\nclaim_allowed=false\n").getBytes(StandardCharsets.UTF_8));
                    zip.add("01_configuration.csv", frozen.configurationCsv().getBytes(StandardCharsets.UTF_8));
                    zip.add("10_samples.csv", frozen.exportCsv(include).getBytes(StandardCharsets.UTF_8));
                    zip.add("20_learning.csv", frozen.exportSummaryCsv(include).getBytes(StandardCharsets.UTF_8));
                    File[] past = historyFiles();
                    for (int i = 0; i < Math.min(8, past.length); i++) {
                        if (past[i].length() > 2 * 1024 * 1024) throw new java.io.IOException("history_limit");
                        zip.add("history_" + i + ".csv", Files.readAllBytes(past[i].toPath()));
                        File metadata = new File(past[i].getParentFile(), past[i].getName() + ".txt");
                        if (!metadata.isFile() || metadata.length() > 65536) throw new java.io.IOException("history_provenance_missing_or_limit");
                        zip.add("history_" + i + ".txt", Files.readAllBytes(metadata.toPath()));
                    }
                    zip.finish();
                }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                if (getContentResolver().update(uri, values, null, null) != 1) throw new java.io.IOException("publish");
                result = "Exportado em Downloads/RafaeliaAudio/Instrumentos. Localização e rádio: " + (include ? "incluídos" : "omitidos") + ".";
            } catch (Exception error) {
                if (uri != null) try { getContentResolver().delete(uri, null, null); } catch (RuntimeException ignored) { }
                result = "Exportação falhou: " + error.getClass().getSimpleName() + " · " + error.getMessage();
            }
            final String message = result;
            runOnUiThread(() -> { writing = false; setRunning(running); dashboard.setText(message); });
        }, "instrument-survey-export").start();
    }
    @Override protected void onPause() {
        finishSession("Tela saiu do primeiro plano"); collector.stop(); handler.removeCallbacks(tick);
        super.onPause();
    }
    @Override protected void onDestroy() { collector.stop(); handler.removeCallbacks(tick); super.onDestroy(); }
}
