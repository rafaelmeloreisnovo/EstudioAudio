# RAFAELIA / EstudioAudio — Experimento unificado RTA, room, ruído e repetibilidade (V1)

**Data:** 2026-11-12. **Autor do projeto:** Rafael Melo Reis · RAFCODEΦ.  
**Categoria:** protocolo experimental + delta de software em branch de revisão, não resultado de aferição.  
**Baseline GitHub:** main@58d45fe2c8b17575ea13fafa8cf4bde54b691851 (após PR #82).  
**Branch de trabalho:** feature/wavelab-trial-matrix-20261112. **Não mesclar/publicar** sem autorização.  
**Autoridade:** START HERE Ω V2.2 Drive → AGENTS.md do EstudioAudio → docs/START_HERE.md → código produtor.  
**Claim:** SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM; claim_allowed=false para fidelidade física, SPL, superioridade sonora ou RTA calibrado.

## 1. O problema verdadeiro da captura atual

No arquivo WavePhysicsLabActivity.java, botão “Amostrar espectro local (microfone, 4096 amostras)”, a entrada é PCM16 mono a 48.000 Hz e usa 4096 samples: **85,333 ms**, NÃO um segundo. Seis valores pontuais (125, 250, 500, 1000, 2000 e 4000 Hz) vêm do método Goertzel finito WaveLabCore.relativeDbfs. Eles não são um espectro de energia contínuo de 1/3 de oitava, nem FFT completa, nem medição SPL absoluta.

O acelerômetro/rotação tem relógio próprio; o método readSnapshot lê GPS/rede/cache em ação separada. Juntar strings no ZIPRAF atual NÃO sincroniza fisicamente as amostras.

O conjunto enviado pelo autor em 2026-11-12 fornece áudio Opus mono de 21,84 s e PCM16 mono de 5,46 s com durações diferentes; não presumir que sejam a mesma tomada. Os dois documentos WaveLab no ZIP fornecido compartilham um mesmo instante de observação acústica e mesmas seis bandas: são exportações repetidas da mesma leitura, não duas medições independentes. Há relatos locais de 16 janelas log-sweep e estimativa de decaimento, mas não equivalem a banda FFT física calibrada.

## 2. Um experimento com relógio, referência e tomadas separadas

Uma sessão contém **três tomadas realmente novas** de um mesmo estímulo, com arquivos brutos distintos, hashes reais e condições reprodutíveis:

- **Primária**: aquisição do conjunto referência controlado; mic/caixa e volume fixos, posição P1, no mínimo 3 segundos (recomendação inicial: 10 s para ruído/uma música recortada) e de preferência sinal de referência de hash conhecido.
- **Secundária**: repetição com mesmo estímulo, mesmo sistema, mesma posição, sem mexer no ganho; nova aquisição e novo hash PCM.
- **Terciária**: outra repetição independente, mesmas condições, com clock auditável.

Um movimento de microfone ou troca de volume não é mais uma repetição da condição original: cria uma **nova célula experimental** (P2/ganho B). A referência de entrada digital (WAV de teste) e a referência física capturada são distintas. Um microfone diferente exige seu próprio perfil/curva e, para comparação de dB SPL, referência de sensibilidade calibrada.

Manter, por tomada, o envelope mínimo:
  session_id, take_role, raw_content_sha256, digital_stimulus_sha256,
  stimulus_kind, mic_chain_id, speaker_chain_id, position_id, playback_gain_id,
  microphone_calibration_ref, spectral_estimator_id, sample_rate_hz,
  frame_count, clock_domain, capture_start/end_monotonic_ns,
  clipping_count, accepted_window_count, rejected_window_reasons,
  FFT_window_type, overlap, FFT_len, bands_hz, linear_band_power,
  temperature/humidity_if_measured, environmental_noise_ref,
  source/commit/ref, consent, claims.

Ausente ≠ zero; identidade de equipamento informada pelo usuário não significa descoberta automática de modelo/peças.

**Relógios:** timestamp Android de conclusão de leitura e timestamp do sensor são origens com diferentes atrasos. Sincronização amostral de áudio exige getTimestamp/AudioTimestamp, offset e análise de drift ou loopback. Nunca atribuir precisão temporal ao simples uso do relógio monotônico comum.

**Armazenamento:** PCM bruto preservado no espaço autorizado do usuário, retenção limitada por tamanho e tempo, exportação explícita; no mínimo SHA-256 do áudio bruto antes de afirmar uma repetição; CRC32 de ZIPRAF não substitui SHA-256 nem assinatura.

## 3. Uma sessão integrada, não leituras dispersas

Proposta para uma futura ação explícita **“Iniciar aferição unificada”** no Lab:

    Checar permissões/temperatura/volume/segurança
      → preparar ID de sessão + perfil de fonte
      → janela de ruído de fundo e referência de ganho
      → reproduzir estímulo padronizado com volume inicial conservador
      → capturar PCM e sensores continuamente, cada um com timestamp da fonte
      → janelar PCM (STFT: Hann ou escolha declarada)
      → estimar PSD/cross-spectrum/coerência apenas com canais verdadeiros
      → guardar séries temporal e espectral / eventos de pose
      → gravar fonte PCM + metadados + hashes + motivo de rejeição
      → capturar nova tomada sob mesmas condições
      → comparar 1/2/3 com falsificadores
      → criar proposta de EQ, nunca aplicá-la sem validação e aprovação.

A coleta de GPS/WiFi/rádio da PR #82 permanece OPCIONAL e a parte sensível não deve ser habilitada implicitamente em uma sessão de áudio. A coleta deve respeitar foreground, consentimento e as políticas já existentes do InstrumentSurveyActivity.

**Fases de integração, ainda NÃO implementadas:** PCM de longa duração contínua; reprodutor de sinais testes; sincronismo real AudioTimestamp; PSD/STFT com calibração e bandas IEC; persistência dos três brutos; correção automática com A/B físico. O código adicionado V1 implementa somente contrato e agregação tipada sobre bandas *fornecidas* por uma futura medição válida.

## 4. Estímulos normativos / experimentais

| Estímulo | Para que serve | Não demonstra |
| --- | --- | --- |
| Silêncio/ambiente | piso de ruído e interferências; um controle negativo | resposta de frequência da caixa |
| Ruído rosa PERIÓDICO | RTA por bandas de oitava com energia aproximadamente igual por oitava, sob sincronismo / janelas corretos | THD/harmônicos isolados |
| Varredura log-senoidal controlada | impulso/resposta de frequência e separação de distorções por análise apropriada | SPL absoluto sem calibrador |
| Senoides isoladas / multitone | harmônicos H2/H3, THD e intermodulação sob protocolo específico | cobertura espacial completa da sala |
| Voz padronizada / música de fonte licenciada | teste de reprodução *de um mesmo arquivo* e comparação perceptiva | medição objetiva de fidelidade geral sem métricas e controles |

O pink noise é descrito pelo espectro de densidade proporcional a 1/f sobre uma banda definida; não é necessário utilizar ruído branco de banda larga em alto volume. Ruído branco contém mais energia acumulada nas oitavas superiores e eleva o risco aos tweeters. Teste de microfone/alto-falante inicia com nível baixo, medição de clipping e parada de segurança. Sem execução ou geração sonora automática neste delta.

**Referências públicas:**
- REW, gerador rosa periódico, sweep, níveis RMS e cuidados com ruído branco: https://www.roomeqwizard.com/help/help_en-GB/html/siggen.html
- REW, modos RTA, diferença espectro/RTA, FFT e médias: https://www.roomeqwizard.com/betahelp/help/html/spectrum.html
- REW, medições e necessidade de alinhamento de múltiplos sweeps: https://www.roomeqwizard.com/help/help_en-GB/html/makingmeasurements.html
- Audio Precision, estratégias de medição de resposta e multitone: https://www.audioprecision.com/blog/how-many-ways-can-we-measure-frequency-response

## 5. Matriz de análise espectral, repetições e incerteza

Sob estimador, janela, ganho e escala realmente iguais, tomar potência linear por banda P(k,j) da aquisição j.

    median_P(k) = median(P(k,1),P(k,2),P(k,3))
    relative_to_primary_db(k) = 10 log10(median_P(k)/P(k,1))
    span_db(k) = 10 log10(max_j P(k,j)/min_j P(k,j))

**Usar potências lineares para agregar**; não calcular média aritmética de dB para inferir potência física. Rejeitar potências ausentes, nulas ou não finitas para impedir log(0)/NaN como valor legítimo. Reportar banda/estimador/unidade.

Span entre três repetições **não é intervalo de confiança**; amostras de mesma sala são correlacionadas. Mesmo com variação estreita, erros sistemáticos de mic/captação e resposta de sala podem persistir. Para afirmar “melhor do que o telefone”, comparar nas mesmas posições, volumes e arquivo, com teste cego A/B quando possível, e métricas previamente registradas (sinal-ruído, distorção, ruído, resposta de frequência, inteligibilidade, preferência).

Condições NEGATIVAS: mesma gravação exportada duas vezes; raw SHA duplicado; 85 ms da leitura atual; fontes diferentes; música diferente; ganho não controlado; cadeia de microfone alterada; posições trocadas; clipe; timestamps sobrepostos; bandas em frequência/estimador diferentes; ausência de calibração.

**Critério de repetibilidade** (a registrar antes de medir): banda/frequência de interesse, valor máximo de spread tolerável e número de repetições. Não declarar valor estatístico de seis sigma sem distribuição, baseline e amostras suficientes.

## 6. Datasheet de componentes, DigiKey e carcaça

Datasheets de microfone, amplificador e alto-falante podem conter sensibilidade, curvas e tolerâncias, impedância, faixa de frequência, distorção, máximo SPL, resposta de fase, ruído próprio e esquema de medição **sob condições do fabricante**. Não assumir que só a marca do celular revela o part number de cada peça.

O produto montado depende de **carcaça, vedação, dutos, orientação, codec, DSP, gain, temperatura, tolerância, posição de medida e sala**. A curva típica de um componente é um PRIOR/REFERÊNCIA, não a calibração do conjunto instalado. Comparações exigem unidade de eixo, distância, ângulo, tensão/potência, configuração de baffle/cavidade e revisão do fabricante.

Exemplos de fabricante/distribuidor:
- DigiKey, impacto de enclosure e montagem: https://www.digikey.com/en/articles/mounting-speakers-important-tips-and-considerations
- DigiKey, resposta muda do componente solto para montado: https://www.digikey.com/en/articles/how-to-use-an-audio-codec-to-optimize-audio-performance
- Datasheets de exemplos NÃO IDENTIFICADOS como peças do telefone: https://media.digikey.com/pdf/Data%20Sheets/CUI%20Inc%20All%20Brands%20PDFs/CDS-16098A.pdf

O registro de metadados de componente deve incluir fabricante, part number, revision, fonte URL, fonte SHA-256 se os bytes forem lidos, condições de teste e curva com incerteza (se publicada). Não preencher curvas a partir de foto genérica ou diagrama sem escala.

## 7. Delta implementado sem trocar Android/captura

Novo núcleo Java hosted (não freestanding) **WaveLabTrialMatrix.java**, com:
- sequência estrita PRIMARY → SECONDARY → TERTIARY;
- máximo 3 capturas, intervalo mínimo de 3 segundos, faixa 8k..192k Hz;
- mesmo hash de estímulo, mic, caixa, ganho, posição, clock, estimador e calibração;
- SHA-256 de estímulo/PCM preenchidos pelo chamador; recusa hash não vinculado, recusa o mesmo PCM como segunda medição;
- flag de clipping recusada, dados inválidos/curtos e timestamps sobrepostos recusados;
- medianas de potência linear, razão relativa em dB e variação máximo/mínimo;
- explicitamente NÃO promove SPL, independência física ou certificado.

**Gate:** ci/WaveLabTrialMatrixTest.java é chamado pelo gate host existente ci/wave-lab-gate.sh. Status de execução: SOURCE_IMPLEMENTED_UNTESTED até CI ou test host executado com o commit exato. A integração com AudioRecord/Android, AudioTrack, ferramentas de geração de ruído, FFT real e sensores coordenados ainda não foi implementada.

**Rollback:** branch separada; não alterar main nem a PR #82 sem revisão. Código não envia dados, não grava nem reproduz áudio, não solicita permissões nem chama serviços externos.

## R3

★ F_ok: restrição dos 4096 samples localizada em fonte, protocolo matemático/external de aferição definido, módulo de repetição com falsificadores no produtor, source readback após escrita.  
† F_gap: fonte real da mesma música/sweep em três tomadas ainda não existente; curva/datasheet do equipamento não identificados; FFT/RTA contínuo, som gerado, captura sincronizada e SPL físico NOT_RUN.  
‡ F_next: executar host gate no commit da branch; depois adicionar sessão com PCM 10–30 s e streaming de timestamps em UI, testar 3 capturas independentes reais no mesmo aparelho, sem tocar em distâncias/volumes entre repetições.  
