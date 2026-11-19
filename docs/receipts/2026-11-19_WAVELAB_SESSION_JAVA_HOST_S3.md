# μRECEIPT S3 — WaveLab sessão Android / host gate — 2026-11-19

**event_id:** MU-ESTUDIOAUDIO-WAVELAB-SESSION-20261119-S3  
**kind:** SOURCE+CI_SCOPED / append-only / no claim promotion  
**producer:** rafaelmeloreisnovo/EstudioAudio  
**branch:** feature/wavelab-trial-matrix-20261112 (não mesclada)  
**PR:** https://github.com/rafaelmeloreisnovo/EstudioAudio/pull/83 (draft em leitura confirmada)  
**parent/supersedes:** docs/receipts/2026-11-12_WAVELAB_DIGITAL_REFERENCE_FIXTURES_S2.md e docs/receipts/2026-11-12_WAVELAB_UNIFIED_TRIAL_MATRIX_S1.md; os originais permanecem preservados.  
**source_head_before_receipt:** e51e8d65b95c1c384fbb6dd0f61dcba77172af2b  
**base_main:** 58d45fe2c8b17575ea13fafa8cf4bde54b691851  
**authority:** START HERE Ω V2.2 → AGENTS.md → app/src/main/java/io/rafaelia/audiostudio/AGENTS.md → produtor.

## Implementação — fonte + leitura GitHub

- WaveLabSessionActivity.java — 25.677 caracteres, Git blob \`801e2b046dddbd9605594a15b1844875031d690c\`
- WaveLabReferencePcm.java — 4.672 caracteres, blob \`02df40058ce0fb046d6533decabb66c880c28411\`
- WaveLabWelchCore.java — 3.990 caracteres, blob \`e7364eb7f4187e8b8abdc568441e397398292bb2\`
- ci/WaveLabReferenceSpectrumTest.java — 4.623 caracteres, blob \`91ad85e45191a69b8074bb0995e167924188ea63\`
- AndroidManifest.xml, WavePhysicsLabActivity.java, ci/wave-lab-gate.sh e docs/WAVE_LAB_SESSION_ANDROID_IMPLEMENTATION_20261119_V2.md — readback por arquivo, sem modificação de manifest permission list; nova activity interna \`exported=false\`.

**CONTRACT:** cada captura exige toque expresso, referencia WAV PCM16 mono 48kHz de 3 a 30s; grava PCM raw privado + SHA256; 25 bandas de potência relativa por 8192-point Hann 50%-overlap Welch (não SPL), acelerômetro CSV + SHA256; três capturas independentes com carimbo de tempo e deduplicação; nenhum arquivo de áudio do autor foi escrito no repositório.

## Falsificação documentada: a fonte inicialmente falhou e foi corrigida

**FAIL_COMMIT:** 4f7091009cce48f0906bc978e2db0d68e3d82fd9  
**RUN_FAIL:** https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/runs/44623107202  
**CAUSA:** gate \`ZIPRAF_LOWLEVEL_GATE\` detectou \`ByteArrayOutputStream\` como buffer de expansão não autorizado em \`WaveLabReferencePcm.java\`; \`HOSTED_ZIP_OR_UNBOUNDED_BYTE_STREAM_REINTRODUCED\`. Esta falha é real e não foi apagada.

**HOTFIX:** commit e51e8d65b95c1c384fbb6dd0f61dcba77172af2b substituiu a classe expansível por arena fixa com \`MAX_WAV_BYTES\`, rejeição explícita de excesso e leitura limitada; sem alterar a autoridade do gate.

**PASS_EXACT_HEAD:** https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/runs/44623450644  
- run \`completed/success\`; \`head_sha=e51e8d65b95c1c384fbb6dd0f61dcba77172af2b\`
- \`WAVELAB_TRIAL_MATRIX_HOST_PASS_SCOPED assertions=37\`
- \`WAVELAB_REFERENCE_SPECTRUM_HOST_PASS_SCOPED assertions=45\`
- \`WAVE_LAB_PLATFORM_FREE_MATH=PASS_HOST\`
- \`ROOM_GUARD_HOST_ADVERSARIAL=PASS_SCOPED\`
- \`RAFAELIA_FAST_GATE=PASS_SOURCE_HOST_ONLY\`
- \`WAVELAB_ANDROID_PLAYBACK_CAPTURE=NOT_RUN\`
- \`WAVELAB_ABSOLUTE_SPL=TOKEN_VAZIO\`
- \`gate / build=SKIPPED\` (draft PR). Não inferir APK compilado.

## Governança e privacidade

A PR #83 foi criada em draft, apareceu transitoriamente como ready_for_review durante os eventos da integração e foi **explicitamente reconvertida para draft**. Readback de GitHub PR confirmou \`open=true\`, \`draft=true\`, \`merged=false\`. Isto revela que a flag/título HOLD precisa de enforcement externo e vigilância; não há prova de proteção do provedor. Sem merge, release, permissões, conteúdo bruto, credenciais ou alteração de serviços conectados neste delta.

**G0_SOURCE_READBACK:** PASS_SCOPED.  
**G1_EXACT_HOST_GATE:** PASS_SCOPED.  
**G2_ANDROID_BUILD:** NOT_RUN.  
**G3_ANDROID_DEVICE_CAPTURE:** NOT_RUN.  
**G4_PHYSICAL_AB_SPL/THD/ROOM:** TOKEN_VAZIO_NOT_CALIBRATED.  
**G5_PRODUCER_CONSUMER_CORRELATION:** NOT_RUN.  
**CLAIM_ALLOWED:** false para acústica real, qualidade/superioridade de som ou conformidade industrial.

## R3

F_ok = implementação em branch + leitura verificada + falha real corrigida e host PASS por SHA exato.  
F_gap = Java/Android build binário, instalação, áudio real, controle de volume/rota física, latência e calibração.  
F_next = revisão da fonte + full Android CI sob gate protegido; após APK e dispositivo, 3 tomadas com WAV de hash real, comparação A/B cega e controle de erro sistemático.
