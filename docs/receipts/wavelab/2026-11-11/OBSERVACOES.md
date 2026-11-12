# WaveLab — observações de cinco exportações (2026-11-11)

**Estado:** `AUDIT / PASS_SCOPED_ARCHIVE_CRC_AND_SHA / OBSERVED_UNPROMOTED`  
**claim_allowed:** `false`  
**Repositório produtor:** `rafaelmeloreisnovo/EstudioAudio`  
**Commit da captura declarado e identificado no GitHub:** `6be178da533d22564ae67d4e359668d8fc4f99d3`  
**CI declarado pelo bundle Assurance:** `43117415889` (não verificado aqui como execução independente).

## Como interpretar
1. **5/5 ZIPs** passam na verificação local de CRC, com cinco SHA-256 distintos; possuem o mesmo manifesto e cinco observações distintas. Hash identifica bytes, não assinatura/autoria.
2. Entre a primeira e última exportação, **3,985 s** transcorreram; contador cumulativo do acelerômetro aumentou **500** eventos. **125,47 eventos/s** é uma taxa estimada do incremento, não certificação do sensor.
3. O instante de observação de áudio e os seis valores espectrais relativos são **idênticos** nas cinco exportações: **não equivalem a cinco novas capturas PCM**. O primeiro registro declara `PCM16_MONO_48K`, `n=4096`, janela de `85,333 ms`.
4. Nenhum ZIP inclui as amostras PCM brutas; não há reprodução DSP independente. Somente valores relativos em dBFS; **sem SPL absoluto calibrado, DOA, distância de eco ou ambisonia FOA comprovados**.
5. A referência ao SHA do commit do repositório produtor é um vínculo documental. Não comprova assinatura do APK, reprodução binária ou teste físico do equipamento.
6. O ramo foi criado para **revisão de documentação apenas**; não constitui autorização para mesclar, lançar APK, alterar medições ou promover `PASS` global.

## Arquivos deste diretório
- `RAFAELIA_WaveLab_5ZIP_Audit_Report.md`: relato completo da inspeção original, inclusive o escopo read-only então vigente.
- `RAFAELIA_WaveLab_5ZIP_Audit_Receipt.json`: recibo integral por arquivo, contendo SHA-256, contadores, métricas e ausências tipadas.
- `OBSERVACOES.md` (este documento): interpretação, próximos falsificadores e custódia.

Os cinco ZIPs originais e a gravação PCM **não são publicados neste repositório**; os SHA-256 dos ZIPs permitem confrontar cópias autorizadas sem expor os registros brutos.

## Gates para eventual promoção
- `G0` — ZIP CRC/sha256 e JSON coerentes: **PASS_SCOPED_LOCAL**.
- `G1` — uma nova observação PCM possui capture_id + timestamp + hash do PCM, e diferencia cache de aquisição: **NOT_RUN**.
- `G2` — calibração de referência SPL e resposta acústica com repetição independente: **NOT_RUN**.
- `G3` — pacote instalado e certificado de assinatura autoral verificados: **TOKEN_VAZIO**.
- `G4` — CI exact-head, revisão e enforcement de merge no provedor: **NOT_VERIFIED**.

**Falsificador prioritário:** exportar o WaveLab duas vezes sem nova aquisição e verificar que o mesmo `audio_capture_id` permaneça constante, com `audio_age_ms` crescente. Após nova aquisição, exigir identificador e hash diferentes **somente quando os bytes forem diferentes**, sem inferir diversidade pelo nome do ZIP.

**Rollback:** não mesclar a branch; correções por commit sucessor append-only. Sem exclusão de arquivos originais.

**R3:** F_ok=integridade e comparação documental; F_gap=PCM, assinatura, calibração e CI não confirmados; F_next=instrumentar capture_id/PCM hash e testar cache versus nova gravação em dispositivo, com consentimento.
