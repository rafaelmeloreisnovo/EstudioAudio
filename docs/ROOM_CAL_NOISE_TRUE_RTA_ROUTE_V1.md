# EstudioAudio — TrueRTA / ROOM / CAL / NOISE / fórmulas — rota V1

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**Estado:** SOURCE_IMPLEMENTED_UNTESTED (novo módulo) / CAL_DEVICE=NOT_RUN / ROOM_APPLIED=NOT_IMPLEMENTED / claim_allowed=false.  
**Data:** 2026-11-11. **Destino:** somente `feature/room-guard-multipoint-20261111`; não muda main, distribuição, permissões, microfone nem segredos.  
**Autoridade:** [START HERE Ω V2.2 do Drive](https://docs.google.com/document/d/1T4OIN2RC1I-KFsdaQTCqieeZUGYBXissWrdrZMG0DN8/edit) → [EstudioAudio START HERE](START_HERE.md) → [AGENTS.md](../AGENTS.md) → fonte de C/JNI. Repo produtor governa o código. Recibos são separados.

## 1. Definição sem sobreposição

- **TrueRTA** é software externo de analisador espectral em tempo real, gerador, osciloscópio e medidores; referência funcional, **não** dependência importada nem código copiado. Fonte: https://www.trueaudio.com/
- **REW** é referência de fluxo de medição do sistema alto-falante+sala+microfone e calibração do microfone. Não implica que a implementação própria seja equivalente. Fontes: https://roomeqwizard.com/ e https://www.roomeqwizard.com/help/help_en-GB/html/calfiles.html
- **Room correction** aplica ao caminho de reprodução *medido*, não à gravação de voz; **noise reduction** limpa sinal de captura com limites; **calibration** documenta a relação do sensor com a realidade; **RTA** observa o espectro; **Ambisonics** requer canais espaciais genuínos, não mono rebatizado.
- **ZRF/CFR** preserva PCM real e relações relativas; 16 janelas temporais log-sweep != FFT/STFT calibrada, nem SPL físico. Consultar [CFR_RELATIVE_ANALYSIS_V1](CFR_RELATIVE_ANALYSIS_V1.md).

## 2. Estado de implementação/contrato

| Função | Base existente ou delta | Estado honesto |
| --- | --- | --- |
| Captação 48 kHz, PCM bruto, permissões, clipping | AudioRecorderEngine, RelativeCalibrationEngine | SOURCE_EXISTS; DEVICE_EXACT_HEAD=NOT_RUN |
| Sweep + sincronismo, perfil relativo Q20 de 16 janelas, cauda | rfa_measure_core | SOURCE_EXISTS; ABS_SPL=NOT_CLAIMED |
| FIR/IIR EQ | rfa_fir_core, rfa_biquad_core | SOURCE_EXISTS; AUTOMATIC_ROOM_APPLY=NOT_IMPLEMENTED |
| NLMS com referência | rfa_lms_core e NLMS_REFERENCE_GATE_V1 | SOURCE_EXISTS; NO_REFERENCE => não alegar remoção de ruído |
| RTA, coerência/cross-spectrum, HRTF/Ambisonics | Wave Lab e fontes de pesquisa | SCOPED_MODEL; REAL_SPATIAL_MEASUREMENT=TOKEN_VAZIO |
| Planejador multiposição de atenuação | **rfa_room_guard_core.c/h** (este delta) | IMPLEMENTED_UNTESTED antes dos gates |
| SPL, RT60 conforme ISO3382, BS1770, certificação de ruído | referências físicas/externas | NOT_CLAIMED; normas exigem vetores/protocolos próprios |

## 3. Menor delta: RFA_ROOM_GUARD_V1

Módulo freestanding, inteiro, C11, sem libc/libm/heap, sem acesso físico. Entrada: no máximo 16 faixas × 8 posições, relativas em **Q8 dB** (1 dB = 256), alvo relativo e flags por observação. Dados devem ser **previamente** alinhados e normalizados sob mesma referência; uma leitura sem unidade de resposta válida não satisfaz este contrato.

- Aceita posições apenas com VALID + SYNC_OK + UNCLIPPED. Flags são DECLARAÇÕES do chamador, não verificação intrínseca do hardware.
- Exige número mínimo de posições [2,8] definido pelo chamador. A mediana inferior, para N par, evita média de uma posição aberrante; spread é máximo-mínimo.
- Se número de posições insuficiente, `INSUFFICIENT` e ganho 0. Se dispersão > limite, `UNSTABLE` e ganho 0.
- Se mediana <= alvo, `NO_CUT` e ganho 0 — **não** aumenta graves, cavidades/nulos nem potência da caixa.
- Se mediana > alvo, produz apenas `gain_q8 = -min(mediana-alvo,max_cut)`, com máximo de corte configurável (1..12 dB). Estes são **valores de proposta**, NÃO coeficientes biquad nem aplicação no fluxo da reprodução.
- Rejeita dados fora de ±96 dB e políticas inválidas **sem escrever saída parcial**. Ausente != zero; nos casos inseguros o motivo tipado acompanha o ganho neutro.
- O ganho 0 com razão `UNSTABLE` não implica sala ideal: informa ausência de atuação proposta.

**Interface exata:** `rfa_room_guard_plan_q8(...)` em `app/src/main/cpp/rfa_room_guard_core.h`. ABI registrado em `native/abi/public_symbols_v1.txt`, objeto ARMv7/AArch64 no gate, teste host em `native/tests/room_guard_smoke.c` e `ci/room-guard-gate.sh`. Sem novas permissões, bibliotecas ou rádio.

## 4. Fórmulas e limites de interpretação

| Relação | Uso permitido | Falsificador/gap |
| --- | --- | --- |
| `R_k=E_resp,k/E_ref,k` em Q20 | perfil relativo de 16 janelas de sweep | janela != verdadeiro banco FFT de frequências |
| `L_rel,k=10 log10 R_k` | conversão **fora deste módulo**, somente se escala e janela compatíveis | não converter PCM bruto em pascal sem referência |
| `gain_k=min(0,target_k-median(measures_k))` com corte limitado | candidato de redução de picos, sem correção de fase | nulo profundo, spread, clipping, movimentação do ouvinte |
| `gamma²(f)=|S_xy(f)|²/(S_xx(f) S_yy(f))` | coerência espectral para medições sincronizadas | sem sinais co-amostrados não computar |
| `Lp=20log10(p_rms/20 µPa)` | apenas com transdutor, ganho e referência acústica calibrados | PCM digital não fornece Pa diretamente |
| `DeltaP≈0.18` | **RMRCTI**: diferença de probabilidades condicionais de estabilidade entre peak/nonpeak | não é dB, pascal, constante universal nem parâmetro acústico |
| `r≈70` | símbolo histórico ainda sem identidade/unidade confirmadas | `TOKEN_VAZIO_INDEX_AROUND_70`; não confundir com `CUSTODY70`, raio ou 70 dB |

**Fonte canônica ΔP e r:** https://github.com/instituto-Rafael/relativity-living-light/blob/main/receipts/2026-09-22_RLL_DYNAMIC_STRUCTURAL_GEOMETRY_CONTEXT_V1.md; código-fonte RMRCTI indicado pelo recibo: `rafaelmeloreisnovo/llamaRafaelia/rmrCti/rmrCti/gbs3_color.c`. Verificar commit de origem antes de usar em experimento.

A amostra do Supabase `raf_private.novoexport_formula_observations_v1` consultada para `0.18` e `70` não retornou correspondências textuais nesta consulta; **não** significa inexistência global.

## 5. Engenharia para NOISE / CAL / ROOM sem promessas excessivas

**NOISE:** distinguir ruído estacionário/impulsivo, reverberação, eco acústico, saturação e som ambiente intencional. Aplicar NLMS apenas quando existe referência apropriada e alinhada; congelar aprendizagem em fala/ausência de confiabilidade; comparar inteligibilidade e artefatos com bypass. Não chamar supressão de captura de 'active noise cancellation' em alto-falante externo.

**CAL:** perfil de microfone por dispositivo+orientação+taxa de amostragem+arquivo de calibração; checar integridade; capturar ruído de fundo, gain, peak, clipping, latência, jitter e referência SPL. Se faltam Pa/ref físico, exibir **relativo** e `ABS_SPL=TOKEN_VAZIO`. Não gravar áudio automaticamente em serviços externos.

**ROOM:** preferir várias posições estáveis, volumes conservadores e checks de ruído antes/depois. Equalização predominantemente por cortes; sem corrigir física por boost em nulo; não confundir atenuação spectral e reverberação temporal. O gerador deve exigir usuário iniciar a medição, hardware apropriado e consentimento de microfone. Referência: https://professionalsupport.dolby.com/s/article/Music-Room-Calibration-FAQs

**RTA:** interface futuro baseada em FFT/STFT verdadeira e ENBW/janelamento explicitados; 16 janelas existentes não se tornam analisador TrueRTA por nomenclatura. Sem copiar bibliotecas de terceiros sob licença autoral.

**Espacial:** não inferir 3D Ambisonics de PCM mono. HRTF/beamforming e redução multicanal são rotas separadas; a literatura pode informar hipóteses, não provar implementação. Estudo: https://www.alphaxiv.org/abs/2609.18532

## 6. Gates, falsificadores e promoção

G0: fonte/AGENTS/referência Git válidos e sem licença externa incorporada.  
G1: `room_guard_smoke` rejeita falta de sincronismo, N insuficiente, spread excessivo, entrada >96 dB, política inválida e ponteiro nulo; positivo 6 dB de corte limitado, negativo **zero boost**.  
G2: ABI manifesto e objeto no runtime gate ARMv7/AArch64, **zero undefined**, nenhuma variável global mutável.  
G3: full CI no exact HEAD com APK+hash e instalação estática; hosted fast gate não substitui G3.  
G4: APK exato instalado em Android físico; capturas raw/loopback e comparações A/B com bypass.  
G5: microfone calibrado e amostragem multiposição repetida; antes/depois **medidos** no mesmo protocolo, sem claims ISO/BS1770 sem conformidade.  
G6: aprovação humana de aplicar qualquer EQ automático e avaliação perceptiva, sem volume inesperado.

**Controles negativos:** sinal silencioso, clip, clock desalinhado, amostra ausente, pico/nulo estreito em uma posição, mudança de cadeira, noise-floor elevado, bypass invertido, troca de microfone e referência cortada.  
**Rollback:** abandonar/fechar a branch/PR em revisão ou reverter somente arquivos desta integração após revisão; nenhum PCM bruto/autorizações são alterados.  
**Não implementar agora:** escrita automática de filtro no Android, reclassificar `DeltaP` como áudio, inferir `r=70`, fazer medição física sem dispositivo conectado.

## 7. Evidência acadêmica de rota (não validação da implementação)

- Lingvall & Brännmark (2009), multi-point MMSE room correction: https://scispace.com/papers/multiple-point-statistical-room-correction-for-audio-1hh8q6u88w
- Jungmann et al. (2013), robust listening room compensation: referência recuperada no Undermind por título; DOI ainda `TOKEN_VAZIO`.
- Carini, Cecchi & Terenzi (2021), RIR robusto a não linearidades: https://scispace.com/papers/a-room-impulse-response-measurement-method-robust-towards-3oyqvjnnzv
- Speech spatial multichannel enhancement, alphaXiv 2609.18532: https://www.alphaxiv.org/abs/2609.18532

### R3
`F_ok`: arquitetura existente preservada; fonte nova dos cortes multiposição e gates proposta/registrada na branch.  
`F_gap`: execução de CI e dispositivo, calibração Pa, FFT/IR verdadeiras, correção aplicada, inteligibilidade e identificação de r70.  
`F_next`: readback dos arquivos + quick host CI exact HEAD; depois full APK ABI e experimento físico com microfone calibrado, sem auto-merge.
