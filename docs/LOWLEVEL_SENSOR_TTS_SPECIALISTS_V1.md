# EstudioAudio · S01 / T01 / W01 — especialistas low-level V1

**STATUS:** branch DRAFT/HOLD; `claim_allowed=false`; preserva o TTS PT-BR e a captura física atuais sem alterar comportamento padrão.

## Código real e autoridade

- **S01** `rfa_sensor_delta_core.{h,c}`: energia das diferenças de três eixos inteiros Q15, saturação sem desvios no cálculo; não captura o acelerômetro nem lê registradores físicos.
- **T01** `rfa_tts_pcm_guard_core.{h,c}`: ganho Q15 e mute binário de PCM16, por bloco, sem divisão de 64 bits, sem estado global, sem heap e sem ramificações explícitas por amostra. O percurso de PCM requer um loop, que é parte do trabalho e não um defeito. Para garantir branchless na **máquina**, observar o assembly das duas ABIs; o código-fonte sozinho não prova ausência de branches.
- **W01** `rfa_dual_watchdog_core.{h,c}`: duas fontes de frescor independentes, áudio e sensor, estados/timestamps pertencentes ao chamador, modo voz sem sensor (fallback), exigência de sensor habilitada só por decisão externa, bloqueio por relógio regressivo ou dados obsoletos. É watchdog de lógica determinística; **não** é watchdog do sistema operacional, supervisor de processo, nem interrupt controller.

`rfa_core_types.h` permanece autoridade dos inteiros. Nenhum arquivo do núcleo contém headers Android, libc/libm, C++ ou malloc. Compiladores são externos; `-ffreestanding -nostdinc -fno-builtin -Wshadow -Werror` e `nm -u`/ausência de B/D são gates, não garantias automáticas de tempo real.

**Hosted boundaries separados:** `jni_bridge.c` e `NativeDsp.java` expõem entradas opcionais com comprimento validado. O aplicativo continua usando as implementações prévias `AuthorialGuidePlayer`, `VoiceBodyMotionProbe` e `SystemAccessSnapshot` para saída `AudioTrack` e callbacks `SensorManager`. Nenhum registro de sensor, TTS já existente, permissão ou encaminhamento da UI foi removido.

## Build e perfis

| Escopo | Fonte de verdade | O que pode ser comprovado |
| --- | --- | --- |
| Core host | `Makefile.core` + `ci/sensor-tts-specialists-gate.sh` | C11, O0/O3, teste negativo, sem símbolos indefinidos/globais mutáveis |
| ARM32 / ARM64 | `ci/freestanding-runtime-gate.sh` + NDK fixo | Cross-compile, ABI, ELF `ET_REL`, ausência de helpers em objetos |
| APK | `app/src/main/cpp/CMakeLists.txt` + `NativeDsp` | JNI ligado ao .so e DEX do Android compilados no CI full |
| Físico | Android 10+ e sensores reais | TOKEN_VAZIO sem recibo físico de latência/deriva/potência |
| GPU/NEON | Capacidade suportada pelo SoC e driver | TOKEN_VAZIO até prova de aceleração vantajosa |
| NUMA / registradores / GPIO | kernel/driver/SELinux/hardware proprietário | Não manipular registradores/endereços nem burlar HAL/permissões |

`-O3` é o perfil de compilação padrão do núcleo integrado. `-O0` e `-O3` exercitam a mesma semântica em teste host. **Não** habilitar `-march=native`, `-mfpu=neon` global nem flags de CPU sem detecção e fallback; flags que eliminem validações não são otimização válida. LLVM LTO e afinidade de CPU requerem recibos comparativos de latência e energia antes de adoção.

## Matriz de 3 especialistas

- S01: leitura por *snapshot já autorizado* → diferenciais X/Y/Z → saída limitada. Se dados brutos inválidos/serviço indisponível → `TOKEN_VAZIO_SENSOR`; não inventar aceleração.
- T01: amostras PCM já renderizadas → ganho Q15/mute. Se falhar um gate externo → mutação não é iniciada, saída da cadeia original preservada; modo explícito mute é amostra zero, não substitui erro.
- W01: carimbos de tempo monotônicos atribuídos pelo chamador → dois prazos → flags 1/2/4. Falha sensor permite **voz isolada** somente quando `require_sensor=0`. Clock regressivo é `FAILSAFE`; reiniciar manualmente/revalidar fonte, não rollback mágico.
- XOR já existe no sintetizador de ruído experimental `rfa_tele_tts_core.c`; não se deve usar XOR como criptografia/validação de integridade.

## Direitos autorais e fronteira de código

Os novos módulos preservam `Copyright (c) 2026 Rafael Melo Reis` e `LicenseRef-RAFCODE-Research-Commercial-0.1` existentes. **Autoria aqui cobre somente os módulos escritos pelo projeto.** Android SDK, ART, DEX, AudioTrack, SensorManager, NDK, CMake e compiler backend são de terceiros e continuam sob seus próprios direitos. Não reatribuir seus códigos ou alegar autoria do Android.

## Testes falsificadores, stop e rollback

`native/tests/sensor_tts_guard_smoke.c` testa saturação, arredondamento, estados inválidos, ausência de mutação, no-heap, voz isolada, relógio regressivo, decaimento de duas fontes e mute; chamado de `ci/sensor-tts-specialists-gate.sh` pela CI única `START.yml`. `ci/freestanding-runtime-gate.sh` inclui os três núcleos nos dois ABIs.

**STOP_PASS_SCOPED:** core compila e testes host passam no SHA exato. **STOP_BLOCKED:** falha de CI full/ELF/JNI ou Android real não provado. **FAILBACK:** manter TTS, sensores e transcrições existentes, sem acoplar guard ao caminho padrão. **ROLLBACK:** fechar PR em rascunho; nenhum usuário precisa instalar APK, nenhuma alteração de permissão, output ou HAL.

Para execução física em produção: exigir teste de áudio audível, sensor calibrado, endpoints TTS/JNI e failover real em ARM32/ARM64. Um único APK não se torna freestanding por conter ELF.

R3=<fontes novas isoladas; falta física/bench; validar CI e Android produtor>.
