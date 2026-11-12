# RAFAELIA Ω — Auditoria read-only: cinco WaveLab ZIPs

## Escopo
Verificação dos cinco pacotes recebidos, sem executar código, interagir com Android, alterar os arquivos de origem ou escrever em repositórios. Os SHA-256 abaixo foram recalculados sobre os bytes carregados localmente.

## Síntese
- CRC ZIP: **5/5 PASS_SCOPED**.
- Arquivos ZIP distintos por SHA-256: **5**; manifestos idênticos: **1 versão de bytes**; observações distintas: **5**.
- Intervalo entre primeira e última captura: **3.985 s**; incremento cumulativo do contador de acelerômetro: **500** (estimativa de frequência incremental: **125.47 Hz**).
- **Todos os cinco pacotes compartilham a mesma marca temporal de observação de áudio e os mesmos seis valores espectrais relativos.** Isto NÃO evidencia cinco novas medições de áudio.
- Amostras PCM brutas: **não incluídas** nestes cinco pacotes. Não é possível recalcular DSP independentemente a partir deles.
- ZIP STORED/CRC32 informa integridade do transporte, mas não fornece autenticidade de origem. Os hashes SHA-256 deste relatório fornecem recibos locais, não assinatura digital.
- As medidas acústicas absolutas, localização/pose, direção do eco, alcance, GNSS/CN0 e ambisonia FOA não estão comprovadas.
- claim_allowed = false em todos os pacotes.

## Capturas
| Arquivo (sufixo) | Horário local (UTC-3) | Eventos totais | Incremento | RMS ΔX (m/s²) | Idade do áudio (s) | SHA-256 do ZIP |
|---|---|---:|---:|---:|---:|---|
| `1794448585493.zip` | 22:56:25.586 | 2246 | — | 0.2703 | 22.094 | `1e5bba17f363c36b26e81bac2af35a69dd0e24e4699fda1890694d5fbd689a3b` |
| `1794448587014.zip` | 22:56:27.068 | 2434 | 188 | 0.2619 | 23.576 | `fec0726da8577cc63ee2be3792db6e636e60f3ceea32a90597155df32a126041` |
| `1794448588048.zip` | 22:56:28.111 | 2562 | 128 | 0.2632 | 24.619 | `32e3ee0e7b5cdbbf516213b4c819b3d7297cfed9b8c15d9017058d0812f964d5` |
| `1794448588820.zip` | 22:56:28.852 | 2658 | 96 | 0.2624 | 25.360 | `8fb6ffd36856a74aebcd7694670e6c1b16efad05fa40d5bbd635db02ff9868e6` |
| `1794448589536.zip` | 22:56:29.571 | 2746 | 88 | 0.2625 | 26.079 | `73b145612db8cd9e593a4ef8e7ceea1a35bba09c23617e11d29dcd2f56490db9` |

## Evidências e limites
1. `00_manifest.txt` tem os mesmos bytes em todos os cinco ZIPs. Declara `integrity=ZIP_STORED_CRC32_NOT_CRYPTOGRAPHIC_AUTHENTICITY`, `sample_stream=NOT_EMBEDDED_PRIVACY_BY_DEFAULT`, `no_radio_mutation=true` e `claim_allowed=false`.
2. `10_observations.txt` é único em cada ZIP, com alterações em tempo, contador e direção/aceleração; por outro lado o espectro e `audio_observation_epoch_ms` são idênticos.
3. A primeira observação registra `audio=OBSERVED_PCM16_MONO_48K n=4096 window_ms=85.333`, as quatro restantes registram `audio=ZIPRAF_SAVED_IN_DOWNLOADS`. Essa alteração de rótulo não representa nova aquisição PCM.
4. O teste matemático `D_POSITIVE` é sintético e a via `D_NEGATIVE` declara `STOP_MAGNITUDE_BOUND`. Essas condições são limites da simulação, não prova física.
5. Poincaré registra apenas contagens de cruzamentos, sem evidência de caos. Sem rotation vector e com áudio mono, direção e espacialização FOA permanecem indeterminadas.

## Proveniência da ligação com pacote assurance anterior
- SHA de origem idêntico ao manifesto do ZIP assurance: `6be178da533d22564ae67d4e359668d8fc4f99d3`.
- Run CI declarada no assurance: `43117415889`. Igualdade do SHA textual não prova execução reproduzível nem assinatura de instalação.

## R3
- F_ok: integridade ZIP 5/5, diferenças de captura identificadas, referências de áudio repetidas demonstradas.
- F_gap: PCM ausente, ausência de assinatura, calibração e validação física independente; não reivindicar aprovação acústica.
- F_next: anexar hashes dos cinco ZIPs e origem da captura PCM ao recibo canônico; coletar novo quadro PCM somente mediante autorização e com consentimento para gravação; validar assinatura e calibração em etapa separada.

Nenhum arquivo de origem foi alterado. Nenhuma escrita remota foi realizada.
