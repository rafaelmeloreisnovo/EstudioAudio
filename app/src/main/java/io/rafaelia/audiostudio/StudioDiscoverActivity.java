/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation: LICENSE_RESEARCH_COMMERCIAL.md.
 */
package io.rafaelia.audiostudio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Read-only discovery surface. No microphone request, DSP invocation, export
 * or evidence production takes place here. Android is the hosted UI edge.
 */
public final class StudioDiscoverActivity extends Activity {
    private static final int BG = Color.rgb(8, 13, 20);
    private static final int PANEL = Color.rgb(16, 27, 39);
    private static final int EDGE = Color.rgb(42, 66, 83);
    private static final int TEXT = Color.rgb(238, 246, 251);
    private static final int MUTED = Color.rgb(173, 193, 207);
    private static final int AQUA = Color.rgb(95, 220, 241);
    private static final int GREEN = Color.rgb(145, 220, 189);
    private static final int AMBER = Color.rgb(250, 207, 145);

    private static final String[] PAGES = {
            "Visão geral", "Recursos", "Confiança", "Links"
    };
    // Fixed, reviewed destinations only. No dynamic URL/deep-link input.
    private static final String[] ROUTES = {
            "https://github.com/rafaelmeloreisnovo/EstudioAudio",
            "https://github.com/rafaelmeloreisnovo/EstudioAudio/blob/main/docs/START_HERE.md",
            "https://github.com/rafaelmeloreisnovo/EstudioAudio/blob/main/docs/CAPABILITY_MATRIX.md",
            "https://github.com/rafaelmeloreisnovo/EstudioAudio/blob/main/docs/RELEASE_READINESS.md",
            "https://github.com/rafaelmeloreisnovo/EstudioAudio/issues",
            "https://github.com/rafaelmeloreisnovo/EstudioAudio/blob/main/LICENSE_RESEARCH_COMMERCIAL.md"
    };
    private final Button[] navigation = new Button[PAGES.length];
    private LinearLayout body;
    private ScrollView pageScroll;
    private int page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) page = savedInstanceState.getInt("page", 0);
        if (page < 0 || page >= PAGES.length) page = 0;
        buildScreen();
        showPage(page);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("page", page);
        super.onSaveInstanceState(state);
    }

    private void buildScreen() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(18), dp(16), dp(12));

        TextView brand = text("◈  ESTUDIOAUDIO   /   RAFCODE", 12, AQUA, true);
        brand.setLetterSpacing(0.08f);
        root.addView(brand);

        TextView title = text("Um estúdio. Todas as rotas.", 28, TEXT, true);
        title.setPadding(0, dp(12), 0, dp(8));
        root.addView(title);
        root.addView(text(
                "Interface de descoberta para gravação, análise e evidências. " +
                "Controles operacionais continuam no console original.",
                14, MUTED, false));

        LinearLayout chips = row();
        chips.setPadding(0, dp(16), 0, dp(14));
        chips.addView(chip("ANDROID 10+", AQUA));
        chips.addView(chip("CORE C", GREEN));
        root.addView(chips);

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = row();
        for (int i = 0; i < PAGES.length; ++i) {
            final int index = i;
            Button tab = action(PAGES[i], () -> showPage(index));
            tab.setContentDescription("Ir para a tela " + PAGES[i]);
            navigation[i] = tab;
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(52));
            tabParams.setMargins(0, 0, dp(8), 0);
            tabs.addView(tab, tabParams);
        }
        tabScroll.addView(tabs);
        root.addView(tabScroll);

        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(false);
        body = column();
        body.setPadding(0, dp(16), 0, dp(24));
        pageScroll.addView(body);
        root.addView(pageScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button console = action("↩  Voltar ao console de áudio", this::finish);
        console.setContentDescription("Voltar ao console principal de gravação");
        console.setBackground(shape(Color.rgb(25, 60, 75), AQUA, 14));
        root.addView(console, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        setContentView(root);
    }

    private void showPage(int selected) {
        page = selected;
        if (body == null) return;
        for (int i = 0; i < navigation.length; ++i) {
            navigation[i].setBackground(shape(
                    i == selected ? Color.rgb(28, 67, 83) : PANEL,
                    i == selected ? AQUA : EDGE, 14));
            navigation[i].setTextColor(i == selected ? TEXT : MUTED);
        }
        body.removeAllViews();
        if (selected == 0) overview();
        else if (selected == 1) features();
        else if (selected == 2) trust();
        else links();
        pageScroll.post(() -> pageScroll.scrollTo(0, 0));
    }

    private void overview() {
        heading("O que você pode fazer");
        card("Capturar e acompanhar", "CÓDIGO PRESENTE", GREEN,
                "Gravação PCM16 mono, waveform e telemetria de pico, RMS e clipping.",
                "A captura real depende de microfone, permissão e dispositivo.");
        card("Analisar com contexto", "RELATIVO", AQUA,
                "Medições e calibração relativa no workspace. " +
                "Nenhum valor de SPL absoluto é presumido.", null);
        card("Organizar a evidência", "AÇÃO EXPLÍCITA", AQUA,
                "Fluxos de recibo, origem e ZIPRAF pertencem ao console; " +
                "um arquivo gerado não substitui prova física.", null);
        heading("Acesso rápido");
        buttonRow("Conhecer os recursos", () -> showPage(1));
        buttonRow("Ver limites e validações", () -> showPage(2));
    }

    private void features() {
        heading("Ferramentas do estúdio");
        card("Gravação e mastering", "INTEGRADO", GREEN,
                "REC, STOP/MASTER e PLAY permanecem no transporte original.",
                "Preserva o fluxo e os controles existentes.");
        card("Narração e teleprompter", "EXPERIMENTAL", AMBER,
                "Roteiro, velocidade e voz-guia fonética opcional.",
                "A inteligibilidade da voz sintética não foi validada.");
        card("Medidor e espectro", "CÓDIGO PRESENTE", GREEN,
                "Pico, RMS, clipping e observação Goertzel de 16 bandas.",
                "Não equivale a certificação BS.1770/EBU.");
        card("Laboratório de ondas", "SOMENTE LEITURA", AQUA,
                "Rotas analíticas e observações condicionadas às permissões.",
                "Não altera configurações de rádio.");
        card("Exportação e custódia", "AÇÃO EXPLÍCITA", AQUA,
                "Recursos de compartilhamento e recibos seguem no painel avançado.",
                "Nenhum dado de microfone é enviado por esta tela.");
    }

    private void trust() {
        heading("Confiança é um estado verificável");
        card("Núcleo DSP", "C FREESTANDING", GREEN,
                "Algoritmos autorais de baixo nível têm fronteira própria.",
                "A interface Android, o JNI e o APK não são bare-metal.");
        card("Áudio físico no HEAD atual", "EVIDÊNCIA PENDENTE", AMBER,
                "A compilação ou o CI não comprovam microfone, alto-falante " +
                "ou inteligibilidade em um aparelho específico.", null);
        card("Metrologia absoluta", "NÃO VALIDADA", AMBER,
                "Calibração relativa não deve ser rotulada como SPL absoluto.",
                "Referência acústica física e auditoria normativa são gates separados.");
        card("Privacidade e proveniência", "ESCOPO LOCAL", AQUA,
                "Esta central apenas apresenta informações e abre links externos " +
                "por ação direta. Não solicita microfone.", null);
        buttonRow("Examinar prontidão de release", () -> openRoute(3));
    }

    private void links() {
        heading("Navegação e documentação");
        link("Código-fonte", "Repositório produtor · GitHub", 0);
        link("Comece por aqui", "Arquitetura, caminhos e contratos", 1);
        link("Matriz de capacidades", "Recursos, limites e estágios", 2);
        link("Prontidão de publicação", "Gates e evidências necessárias", 3);
        link("Relatar problema", "Issues e acompanhamento técnico", 4);
        link("Termos de licença", "Autoria e regras de uso do projeto", 5);
        card("Links externos", "SOMENTE POR TOQUE", AQUA,
                "Os destinos abrem em um aplicativo HTTPS disponível " +
                "no dispositivo. Não há navegação automática.", null);
    }

    private void link(String label, String description, int route) {
        LinearLayout panel = panel();
        panel.addView(text(label + "  ↗", 17, TEXT, true));
        panel.addView(text(description, 13, MUTED, false));
        panel.setContentDescription(label + ". " + description + ". Abrir link externo");
        panel.setOnClickListener(v -> openRoute(route));
        panel.setFocusable(true);
        panel.setClickable(true);
        panel.setMinimumHeight(dp(72));
        body.addView(panel, cardParams());
    }

    private void openRoute(int route) {
        if (route < 0 || route >= ROUTES.length) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ROUTES[route])));
        } catch (ActivityNotFoundException ex) {
            new android.app.AlertDialog.Builder(this)
                    .setMessage("Nenhum navegador disponível para abrir este link.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void card(String title, String badge, int tone, String detail, String note) {
        LinearLayout panel = panel();
        panel.addView(chip(badge, tone));
        TextView name = text(title, 19, TEXT, true);
        name.setPadding(0, dp(10), 0, dp(6));
        panel.addView(name);
        panel.addView(text(detail, 14, MUTED, false));
        if (note != null) {
            TextView limitation = text(note, 12, tone, false);
            limitation.setPadding(0, dp(10), 0, 0);
            panel.addView(limitation);
        }
        body.addView(panel, cardParams());
    }

    private void heading(String title) {
        TextView heading = text(title, 20, TEXT, true);
        heading.setPadding(0, 0, 0, dp(12));
        body.addView(heading);
    }

    private void buttonRow(String label, Runnable call) {
        Button button = action(label + "  ›", call);
        LinearLayout.LayoutParams params = cardParams();
        params.height = dp(52);
        body.addView(button, params);
    }

    private Button action(String label, Runnable call) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(TEXT);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(48));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setBackground(shape(PANEL, EDGE, 14));
        button.setOnClickListener(v -> call.run());
        return button;
    }

    private LinearLayout panel() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(shape(PANEL, EDGE, 18));
        return panel;
    }

    private TextView chip(String label, int tone) {
        TextView labelView = text(label, 11, tone, true);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(dp(10), dp(7), dp(10), dp(7));
        labelView.setBackground(shape(Color.rgb(19, 37, 48), tone, 30));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, dp(7), 0);
        labelView.setLayoutParams(p);
        return labelView;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(3), 1.06f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        return box;
    }

    private LinearLayout row() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        return box;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(12));
        return p;
    }

    private GradientDrawable shape(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
