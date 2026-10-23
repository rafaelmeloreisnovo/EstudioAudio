# Auditoria de continuidade — EstudioAudio (22/10/2026)

**Intenção:** prosseguir do relatório fornecido na sessão por três camadas: medidor/BS.1770, ZIP32/ZIPRAF, PT-BR/voz/timbre. **Autor da fonte do projeto:** Rafael Melo Reis. **Revisão editorial:** assistente. **Status:** DOC_REVIEW_ONLY; claim_allowed=false. **Base indexada observada:** 8d79e91e4d4f6a0a5daa304baa430e0796489546; HEAD deve ser reconfirmado para recibos de CI.

## Fonte, evidência e limites

O relatório fornecido pelo usuário afirma execução local do fast gate, vetores SHA, testes DSP e 301 round-trips RAC1. Esses números são **USER_REPORTED_EXECUTION**, pois este processo não clonou nem executou o repo. Inspecionou-se diretamente a fonte conectada no GitHub:

| Camada | Fonte e blob SHA | Observação fundamentada | Sem claim |
|---|---|---|---|
| Meter | app/src/main/cpp/meter_core.c · 1e58962e6d4235c4c7113404364e78f352723223 | K-weighting em duas seções, janelas de 4.800 frames 100ms x4 = 400ms, 75% overlap a 48k, gate em 2 passes e FIR true peak 4 fases | BS1770/EBU sem vetores normativos |
| Entrada | AudioPipeline.java · 10e8b87e3de38b17fd3ef3eb9ba5ddb993277b1c | masterAndNormalize recusa amostragens diferentes de 48 kHz antes do meter; AudioRecorderEngine também captura 48 kHz | Não alegar bug 44,1k no fluxo filtrado, nem suporte genérico multirate |
| CFR | rfa_measure_core.c · 62a7d9400e6337bc0391f66835290f271ba3ebd9 | varredura sintética Q31, 16 janelas relativas de energia, busca de atraso e estimadores EDT/T20/T30 | Não é FFT calibrada, SPL absoluto nem ISO3382 |
| ZIP32 | rfa_zip32_core.c · 305095ba8422d695319eb7cd6818f431f36c0f71 | STORED, até 32 entradas, 8 MiB cada, 64 MiB total, valida caminhos, duplicados e capacidade | Sem ZIP64/compressão ou autenticidade criptográfica |
| ZIP testes | ci/zip32-adversarial-gate.py · 7e372c76d7b478219b2a0469130cacec856c5e68 | teste escrito contém 96 casos seedados, oráculo zlib/zipfile, mutante CRC e negativos | Sem alegar PASS desta leitura documental |
| PTBR | rfa_ptbr_core.c e docs/PTBR_AUTHORIAL_SPEECH_V1.md · 71660215eb05d4065d961054a93462608e8763a1 | eventos grafema/fonema limitados, até 2.048 bytes, PCM com buffers limitados | Inteligibilidade e TTS natural NOT_PROVEN |
| Timbre | rfa_timbre_core.c · f141d828d7d74c921bdfe42530faec18c8577ff8 | perfil de amplitude/diferença/cruzamento e filtro de suavização | Sem identificação de locutor, formantes ou speaker cloning |
| RAC1 | rfa_rac1_core.c · 704923c97722cf9cdbe8fa692985b88d12da6853 | diferença + zigzag + varint, até 3 bytes/amostra | Pode expandir dados PCM16 até 50% no pior caso, não universal |
| JNI concurrency | AudioProcessingGate.java · f98407216feaff7d34f39d9fbc46a43c078e955a | AtomicBoolean por instância usado na UI, JNI contém estados globais mutáveis | Exclusão global entre todos chamadores sem teste/revisão |
| Escopo de CI | AGENTS.md, ci/rafaelia-fast-gate.sh · 789ef4cd6329c3307f2a979cf43316bb60eef636 | fast gate declara PASS_SOURCE_HOST_ONLY, APK/ARM/physical NOT_RUN | Objeto freestanding != APK freestanding |

## P0 / P1 / P2 / P3

**P0 — Prova e claim:** manter FAST_HOST_TEST=USER_REPORTED (do relatório externo), BS1770_COMPLIANT=NOT_PROVEN, ISO3382_COMPLIANT=NOT_PROVEN, APK_CURRENT_PHYSICAL=TOKEN_VAZIO_NOT_REATTESTED. Nunca copiar resultados entre SHAs sem receipt.

**P1 — Medidor:** validar contra sinais oficiais e tolerâncias de ITU-R BS.1770-5/EBU Tech 3341, com silêncio, bloco 399/400ms, 100ms overlap, 48k mono/estéreo, gate absoluto -70 LUFS, relativo -10 LU, true-peak intersample e comparação numérica. O guard sampleRate!=48000 já existe no pipeline. Não mudar filtros sem falha reproduzida.

**P1 — JNI:** conferir se todas as entradas que tocam g_dsp_state, g_meter_state e g_sweep_state são serializadas. O AudioProcessingGate é por instância; a proteção absoluta entre instâncias/threads/atividades ainda requer prova, não alegar race como observado.

**P1 — Formatos:** ZIP32 CRC32 e ZRF/CFR FNV-1a-32 validam corrupção, mas não autenticidade; SHA256 vincula bytes ao hash conhecido mas requer fonte confiável/assinatura para atestar origem. RAC1 deve declarar worst-case de 3 bytes versus PCM16 2 bytes (+50%, excluídos metadados).

**P2 — CFR:** busca de transferência por correlação bruta e análise em 16 janelas temporais relativas, não FFT/STFT nem deconvolução calibrada. Testar ruído, varredura truncada, atraso negativo não pesquisado e amplitude zero antes de qualquer inferência acústica.

**P2 — Voz:** testar chunk-invariance, UTF-8 inválido, nasalidade/acentos, digrafos, pontuação, saturação, contorno temporal e avaliação auditiva cega antes de claim inteligibilidade. Perfil timbral estatístico não é biometria nem clonagem.

**P3 — Rastreabilidade:** o relatório cita nome histórico Est-dio-de-udio, atualmente repo observado EstudioAudio. Sem consultar eventos históricos de rename, origem permanece TOKEN_VAZIO. O ator fixo em START.yml merece plano de continuidade, sem retirar controle do dono.

## Sequência mínima executável e não regressiva

1. Current HEAD exato e lista de PRs/CI checks; não reusar o HEAD de 04/10 citado em START HERE como atual.
2. Medidor: vetores normativos + negativos, com relatório de tolerância e hash dos inputs. Adicionar só o menor teste que reproduza divergência, antes de hotfix DSP.
3. ZIP32: preservar produtor autoral, usar oráculo independente e mutante existente; conferir relação C↔Java e envelope ZIPRAF, sem inventar nova compressão.
4. Voz: verificar eventos PTBR↔síntese com testes de fragmentação, depois escuta cega em dispositivo somente com aprovação/consentimento.
5. Registro de qualquer execução: SOURCE_SHA | TOOLCHAIN | INPUT_SHA | COMMAND | EXIT | MEASURES | GAPS | ROLLBACK. Classe de evidence separada para dispositivo.
6. Qualquer correção posterior só em PR DRAFT com reversão por commit e zero alterações de licença/arquivos legados.

## Fontes normativas externas (apenas referência, nenhum teste executado)

- ITU-R BS.1770-5, 11/2023: https://www.itu.int/rec/R-REC-BS.1770-5-202311-I
- EBU Tech 3341 (EBU Mode): https://tech.ebu.ch/publications/tech3341
- EBU Loudness Test Signals: https://tech.ebu.ch/loudness/

**R3:** F_ok=código e limites de medidor, ZIP e voz inspecionados e classificados; F_gap=recibo de execução neste HEAD, vetores normativos, revisão JNI e evidência física; F_next=gate normativo reproduzível e eventuais hotfixes mínimos com recibos. SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM; TOKEN_VAZIO!=0; claim_allowed=false.
