# EstudioAudio Ω — Laboratorio A/B de reproducao, captura e comparacao relativa
Data: 2026-10-23
Owner: autor do repositorio; implementacao proposta por assistente
Estado inicial: SOURCE_ONLY; CI_EXACT_HEAD=NOT_OBSERVED; DEVICE_EXACT_HEAD=NOT_RUN
claim_allowed=false

## Problema fisico e hipotese falsificavel

Um som pode ser reproduzido pelo alto-falante, captado em seguida/pela mesma janela pelo microfone e comparado com o sinal de referencia. Isto testa a cadeia
PCM gerado -> AudioTrack -> ar/transdutores/sala -> AudioRecord -> sincronismo -> medicao -> recibo.
**Nao** pressupor identidade perfeita: o caminho acustico introduz atraso, ganho, reverberacao, ruido, resposta em frequencia e possiveis preprocessadores de Android.

Reutilizacao: `RelativeCalibrationEngine` ja gera sync e sweep de 20 Hz a ~20 kHz/2s pelo C freestanding, reproduz no AudioTrack a 0.25, grava PCM16 mono/48k no AudioRecord, alinha por correlacao C e produz CFR contendo sinal de referencia, captura, 16 energias por faixas da varredura e resposta de sala. O projeto ja mede lag e relacoes Q20.
Novo incremento: um botao unico em "More tools", estados da caixa industrial, duas visualizacoes de 16 janelas da varredura, ligacao real da resposta captada via JNI -> rfa_playback_observe_mono, reapresentacao de ultimo resultado sem refazer teste, e exportacao EXPLICITA de CFR+recibo em Downloads/RafaeliaAudio com hash de leitura e readback.

As 16 barras sao **janelas temporais de um sweep frequencial variavel**, NAO FFT de 16 bandas com janelas de frequencia fixa. A comparacao de energias e ratios Q20 mede comportamento relativo; nao certifica espectrometria, identidade vocal, causalidade isolada, curva de resposta de frequencia calibrada ou SPL absoluto.

## Protocolo em dispositivo

1. Selecione "More tools" -> "Orquestrar teste fisico / 16 faixas"; conceda RECORD_AUDIO somente no dialogo do Android. Nao conectar fones/Bluetooth neste teste inicial, deixar o telefone sobre superficie estavel, volume conservador, sem outras apps usando microfone/alto-falante. Pessoas ao redor devem consentir; nao gravar terceiros sem permissao.
2. 00_PRECHECK: verifica exclusao de sessoes (nenhum mastering, gravador, evidencia e calibracao paralelos).
3. 01_REFERENCE: gera sync/guarda/sweep do nucleo, 02_PLATFORM inicia captura e reproduz (o sistema pode aplicar ganho/AGC mesmo com configuracao desejada), 03_ALIGN correlaciona C, 04_COMPARE compara energias em 16 janelas, 05_OBSERVER roda freestanding C nos PCM do microfone atraves de JNI; 06_CFR preserva referencia/captura/metadados.
4. UI mostra duas barras, lag em samples/us, valid_bands 0..16, candidatos de silencio, glitch e alteracao acustica. Um candidato nao e conclusao sobre falha do aparelho; o sweep deliberadamente altera energia/frequencia.
5. Botao "Rever a ultima comparacao" **somente renderiza o resultado em memoria**, sem novo AudioRecord nem execucao fisica. Nao usar este click como replica independente.
6. Botao "Salvar CFR + SHA-256 nos Downloads" e explicito; copia de cache para MediaStore, reread hash, publica CFR e recibo de parametros em txt. Se falhar, o cache original nao e reescrito. O CFR com PCM inclui audio ambiente e exige cuidado de privacidade.
7. Botao ja existente "Generate ZIPRAF" registra `reference_capture_comparison` no bloco de metricas e referencia SHA do CFR; por contrato o ZIPRAF de assurance **nao embute** o CFR, salvo fluxo explicito de transferencia posterior. Fazer backup externo do CFR exportado para reproduzir depois.

## Grupo de verificacao industrial (papeis de revisao, NAO pessoas contratadas)

| Papel / quadrante | Check e evidencia | Gate |
|---|---|---|
| Arquitetura L0 | C sem libc/heap/sistema, smoke adversarial longo/96k, nm -u = 0 | P0 / CI C |
| Android I/O | AudioTrack <-> AudioRecord, RECORD_AUDIO, close/watchdog, one-flight | P0 / CI e device |
| Metrologia | 16 janelas ref/resposta, atraso, bins, unidade, ausencia de ref SPL | P1 / medicao REL |
| QA / falsificador | silencio, clipping, sinal baixo, ruido alto, 2 execucoes iguais/diferentes; diferenciar falhas de captura | P1 / device+replica |
| Custodia/proveniencia | SHA do CFR e do APK, source SHA, CI run, atestacao do dispositivo, cert assinatura | P0/P1 / receipt |
| Revisao humana independente | Reproducao em outro aparelho ou mesma configuracao com controle e testemunha | P1 / REVIEW |

Ninguem deste grupo foi nomeado/contratado; trata-se de matriz de responsabilidades para revisores humanos efetivos.

## Contra-hipoteses e falsificadores

- Ausencia de permissao -> BLOCK sem gravacao. AudioRecord indisponivel -> FAIL_CLOSED.
- Falha de correlacao, transfer, numero de amostras ou JNI -> nao publicar resultado como medicao PASS.
- Antigo arquivo de naracao V2/protecao por hash permanece intacto, assim como o unico workflow START.
- Dispositivo ausente em CI -> DEVICE_NOT_RUN (CI nao finge prova fisica).
- O baseline observado em main antes desta alteracao tem APK ~333947 bytes; 1 MiB e banda de referencia, nao teto irrevogavel. Medir o delta com o mesmo pipeline e SHA, reverter aumentos relevantes sem justificativa.
- Fonte do sistema Android/JNI nao e freestanding puro; C de calculo permanece separado; Python nao compila APK.
- Comparacao de 16 janelas nao e FFT, nem padrao internacional aferido.
- Certificado debug nao se transforma em assinatura de autoria por declaracao.
- Prov main estava sem protecao no ultimo readback; CI success nao corrige provider authority.
- O ZIPRAF enviado ao chat tem SHA-256 `fef84f56e7e42e6984de1d73a3f6654fde2d5ca11c627dd1b95351b1615a1a5a`, prova de bytes/autodeclaracao no artefato anterior, nao execucao de novo codigo.

## TOKEN_VAZIO tipado (P0→P3)
P0: `EXACT_HEAD_CI=NOT_RUN` ate run terminal (identificar SHA).
P0: `ANDROID_OBSERVER_END_TO_END=NOT_RUN` ate dispositivo efetivamente reproduzir+gravar+analisar+registrar SHA.
P0: `PROVIDER_ENFORCEMENT=AUTHORITY_UNRESOLVED` ate ruleset/branch protected true + negative merge test.
P1: `INDEPENDENT_REPRODUCTION=NOT_RUN` ate replay independente hash-bound.
P1: `AUTHORIAL_SIGNING=TOKEN_VAZIO_AUTHORIAL_SIGNATURE` ate cert instalado e hash esperado.
P1: `CFR_EXPORT_IN_CI=NOT_RUN_BY_DESIGN` (testar dispositivo com MediaStore).
P2: `ABSOLUTE_SPL=TOKEN_VAZIO_PHYSICAL_REFERENCE`, exige referencia calibrada/rastreavel.
P2: `SPECTRAL_FFT=NOT_IMPLEMENTED` (este teste usa 16 janelas do sweep).
P2: `MIC_PATH_PROCESSING=UNKNOWN_PLATFORM_EFFECTS`, requer testes comparativos independentes.
P3: `EXTERNAL_STANDARD_AUDIT=NOT_AUDITED`.

## Rollback e R3
Base: main@3b6b495c4892c37bda097b3972232a1605d32c98; nao alterar main diretamente.
Rollback: reverter este PR draft sem renomear ABI; manter a matriz de falhas e o antigo pacote como custodia. Nao apagar historicos do Drive/GitHub.
`F_ok` = CI original do sweep/CFR, codigo JNI/proposta UI/CFR receipt. `F_gap` = CI novo/physical replay/provedor. `F_next` = executar CI no HEAD do draft, corrigir falhas sem relaxar gates, medir bytes, obter recibo fisico por Android; review antes da promocao.
