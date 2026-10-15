# EstudioAudio — Observador, ruído, latência e reconstrução temporal (V1)

**Data:** 2026-10-15  
**Estado:** `DESIGN_RECEIVER / SOURCE_ONLY / NO_PROMOTE / claim_allowed=false`  
**Autoridade:** áudio PCM/DSP/captura e telemetria do EstudioAudio, não entropia de NAND nem percepção humana universal.

## Núcleo de significado contextual

Duas pessoas podem dizer "vermelho" ou "timbre limpo" e não estarem descrevendo a mesma experiência. Para áudio, um rótulo de timbre/ruído não substitui observação: declarar microfone, ambiente, posição, canal, PCM SHA, sample rate, ganho, unidades, calibrador e clock.

`sample_zero != missing_sample != silent_room != zero_noise`.
`noise_cancelled != noise_source_explained`.
`SRC_PCM_HASH != PLAYBACK_HASH != MICROPHONE_CAPTURE_HASH`.

## Ponte limitada ao AUDIO_MANIFOLD_CONTRACT_V1

Cada janela candidata: `(source_sha,device,observer,clock,sample_range,capture_mode,reference,baseline,transform,latency_samples,latency_variance,noise_floor,clip_count,signal_rms,context,uncertainty,evidence_id)`. Sem referência física: `ABS_SPL=PENDING_PHYSICAL_REFERENCE`. Sem acesso ao relógio do sistema: `TOKEN_VAZIO_CLOCK`.

Projeções e hipóteses:
- L: tempo entre onset e resposta, cross-correlation, drift, variação por sessão e rollback.
- O: PCM de referência, loopback, gravação de microfone independente, humana escuta cega — fontes distintas não equivalentes.
- T: acústica, frequência, termodinâmica, microeletrônica, memória e latência; ligação somente por medida e causalidade testada.
- Direta: estímulo conhecido -> sinal esperado. Inversa: possível origem de resposta, geralmente não única. Derivativa: variação de nível/fase ao longo de amostras comparáveis. Antiderivativa: acumulação de deltas com condição inicial. Recursiva: adaptação LMS, limitada pela estabilidade declarada. Integrativa: comparação entre canais e critérios de erro tipados.

## Latência pode ser útil ou prejudicial

Buffers 128/512/4096 do contrato atual definem perfis *candidatos*, não ganhos garantidos. Uma janela maior pode melhorar estimativa espectral/controle de ruído, mas aumenta atraso e pode inviabilizar teleprompter ou feedback em tempo real. Registrar `p95/p99`, underruns, variabilidade e deadline por perfil, em vez de promover um único valor médio.

## Gates sem tocar o áudio de produção

1. PCM original preservado e hash vinculado.
2. Verificar ruído sintético vs ruído captado, sem classificar residual como física nova.
3. Impedir `TOKEN_VAZIO` convertido em zero PCM.
4. A/B de cada perfil de bloco no mesmo hardware/condições, com repetição e incerteza.
5. Identidade de canal e unidade antes de combinar observações; escuta humana como avaliação, não prova matemática.
6. Teste offline antes de qualquer JNI/Android; não reabrir PR/ci existente nem fundir branch por conveniência.

**F_ok:** ponte documental de sinais e latência; **F_gap:** nenhum algoritmo/rotina DSP alterado, Android e escuta física ainda exigem receipt; **F_next:** vetor offline contextual+latência comparando perfis e restauração do PCM.
