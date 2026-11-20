# EstudioAudio — Sucessor V2 · Sessão experimental de três tomadas + espectro relativo + acelerômetro

Data de registro: 2026-11-19. Autor do projeto: Rafael Melo Reis.  
Destino: branch \`feature/wavelab-trial-matrix-20261112\`. **Não mesclar, publicar, treinar nem enviar dados sem autorização explícita**.
Predecessor: \`docs/WAVE_LAB_UNIFIED_RTA_REPEATABILITY_EXPERIMENT_V1.md\` (preservado).
Fonte de autoridade: START HERE Ω V2.2 → AGENTS.md → Java AGENTS.md → WavePhysicsLabActivity + WaveLabCore + CFR_RELATIVE_ANALYSIS_V1.
Categoria: \`SOURCE_IMPLEMENTED\` / \`HOST_TEST_PENDING\` / \`ANDROID_BUILD_NOT_RUN\` / \`PHYSICAL_NOT_RUN\`.

## O que mudou concretamente

- Rota opcional na interface do WavePhysicsLabActivity: **"Aferição conjunta: WAV + 3 tomadas + espectro por janela"**, abrindo \`WaveLabSessionActivity\` privada.
- Um único experimento com até três tomadas explicitamente iniciadas pelo usuário: PRIMARY, SECONDARY, TERTIARY. O mesmo WAV PCM16 mono de 48 kHz (3–30 s), escolhido via seletor SAF, é reproduzido somente depois de cada toque explícito. O WAV original permanece com o usuário; não é incorporado ao APK nem publicado.
- O aplicativo grava PCM16LE mono a 48 kHz por duração do WAV + 1 s (cauda); arquivos sob \`getFilesDir()/wavelab_sessions/session-<id>/\`, **privados do aplicativo**. Cada tentativa recebe um identificador próprio; falhas deixam recibo com motivo tipado e dados parciais não são promovidos. Dados não são exportados nem enviados.
- SHA-256 real calculado sobre o WAV selecionado e sobre cada PCM efetivamente escrito. A comparação exige três hashes distintos e mesmo estímulo, sample rate, cadeia/rota, volume observado, posição declarada e estimador. O app não gera hash fictício.
- Estatística por banda: \`WaveLabWelchCore\` calcula FFT de 8192 pontos com Hann e hop 4096 em janelas sobre **toda a captura**, somando potência linear relativa em 25 bandas nominais de terço de oitava de 63 a 16000 Hz. O algoritmo é uma análise relativa do microfone, **não** RTA calibrado segundo IEC nem compensação da resposta do microfone.
- Por tentativa, são registradas observações do acelerômetro (se disponível), com timestamp do sensor, ax/ay/az em CSV privado e SHA-256. São fontes distintas do áudio; são correlacionáveis por relógio monotônico, mas **não** têm alinhamento físico amostra a amostra comprovado.
- Nenhuma mudança nas 16 janelas de sweep, no CFR, no Room Guard, no TrueRTA externo, no modo solo de gravação, nas permissões ou na política de rádio.
- O gerador WaveLabRef V1 está no **kit de WAV privado da conversa**, não no repositório. Os hashes foram verificados por leitura dos bytes do ZIP: pink 10,24 s \`d2a7f569be26004cece109b933ccba953bf59844dffd4b6eaf65040bd86d503e\`; sweep 10 s \`f1b42dc1527eac7d4ff0e19010ebf0d1a4a99f5dc0927182ae7422a8b0c42139\`; senos escalonados 7,5 s \`23e3f2012315d8929c9750ed8562d4b18000645624f3e7c382e86100f5cf135e\`. **Nunca interpretar esses hashes como hashes de gravação do telefone.**

## Limites experimentais e segurança

O WAV pode ter cerca de -24 dBFS de pico digital, mas nenhum limite em Pa foi comprovado. O \`AudioTrack\` utiliza fator 0,10 do ganho interno além do volume do sistema; isso não garante intensidade sonora segura. O usuário deve baixar o volume e não usar fones, material médico, testes de potência elevada ou contato com ouvido.

Os efeitos AGC/NS/AEC são solicitados desativados quando a API oferece controle; o retorno de cada efeito é registrado. Desativação solicitada \`≠\` desativação de todo DSP de hardware, e AudioSource UNPROCESSED depende de suporte do aparelho. O caminho elétrico/latência real e o roteamento podem ser desconhecidos. Mudanças de volume entre começo/fim são recusadas; route IDs são observações de API, não provas metrológicas.

O \`AudioRecord.getTimestamp\` pode não fornecer amostras suficientemente cedo; o receipt usa token/sentinela de indisponibilidade e guarda outros timestamps do sistema. A base monotônica do acelerômetro oferece alinhamento aproximado de relógio, não prova offset, drift ou compensação de latência de saída.

**Comparações válidas:** mesmo WAV e processamento relativo, três capturas independentes; medianas de potência linear, variação max/min em dB, repetibilidade de seis segundos a dez minutos de janela? **Não**: cada tomada máxima é 30 s WAV + 1 s áudio. Nenhum resultado demonstra "melhor que telefone" sem A/B cego de mesma reprodução, um caminho de referência e medição calibrada de SPL/THD/SNR.

**Não suportado neste incremento:**
- SPL absoluto, RT60 ISO 3382, resposta de alto-falante independente da sala, curva de fabricante de peça real não identificada.
- Deconvolução de sweep; THD/THD+N calibrada; coerência multicanal; ambisonics; compensação HRTF; equalização aplicada.
- Exportação de PCM sem ação explícita, coleta de localização/WiFi/celular/Bluetooth durante a sessão, geração de ruído de alta potência.
- Teste físico, APK instalado e comparação auditiva cega.

## Falsificadores prévios à promoção

1. WAV inválido: RIFF corrupto, fmt ausente/duplicado, PCM 8 bit/estéreo, 44,1 kHz, data >30 s, truncado → recusar.
2. 3 s de seno em 1 kHz → potência dominante na banda 1 kHz, densidades não negativas e resultado determinístico; silêncio → zero físico relativo e NÃO valor inventado.
3. Mesmo PCM/hash reutilizado em SECONDARY → recusar; entrada clipping/mudança de ganho/posição/estimador → recusar.
4. Falta de permissão, microfone inicialização falha, ciclo app onPause, troca rota, volume alterado → não promover tomada; recibo de falha.
5. 48kHz PCM e acelerômetro com timestamp monotônico: verificar tempos, tamanho dos arquivos, SHA-256, origin identity, sem assumir sincronismo amostral.
6. CI host e Android exact HEAD, ABI e APK determinístico; **draft/hold não é controle de proteção de branch**.
7. Teste de campo A/B do usuário, com referência de microfone conhecida, relatório sem SPL absoluto quando não disponível.

## Distribuição e privacidade

O repositório é público; **nenhum arquivo de áudio do usuário, ID pessoal, sensor bruto, credencial ou URL privada** é enviado nesta branch. Dados capturados ficarão somente em armazenamento privado do aplicativo, com retenção local que ainda necessita de política e controle de exportação/permanência. Os usuários devem ter controle sobre a exclusão de dados privados no dispositivo; esta entrega não adiciona ação automática de limpeza.

A origem de aparelho/speaker pode não ser identificada. A ficha de componentes da DigiKey é tratada como hipótese de datasheet, não prova sobre os componentes concretos do telefone.

## R3

F_ok: software opt-in definido em arquivos de código e testes de host preparados, corpus original/CFR preservado.  
F_gap: gate host, build Android, rota real, alinhamento áudio/sensor, calibração física, datasheet de peça e A/B controlado pendentes.  
F_next: rodar host no exact SHA → corrigir falhas → full Android CI em revisão quando permitido → ensaio físico seguro com 3 WAV+takes e readback de recibos; nada é promovido a PASS global.
