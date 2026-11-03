# ### RAFCODEΦ / EstudioAudio — AUDIO CUSTODY70 V1

SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**Estado:** DEMO_BROWSER / TESTED_LOCAL_SCOPED; **não integrado ao APK Android**; `claim_allowed=false`.

## Objetivo

Uma única tomada de microfone de até 2 s, transformada num áudio intencionalmente degradado com chuva, cachoeira ou estalos e cortes perceptíveis. A evidência é o áudio: origem e derivado, sem documentos anexados ao ZIP exceto manifesto técnico e autenticação.

## Limites e formato

- `MAX_PACKAGE_BYTES=71680` (70 KiB); na dúvida, recusar antes de exportar ou importar.
- Captura: navegador (`getUserMedia`, com consentimento); perfil de evidência: WAV PCM mono, 8 kHz, 8 bits, no máximo 16000 amostras (16.044 B incluindo header). Isso **não** preserva os bits originais do ADC: a origem de custódia V1 é o **WAV normalizado**.
- Demonstração: derivado da origem normalizada; adicionar ruído sintético, cortes de 550 amostras a cada 5200, e marcação na waveform. O efeito é registrado no manifesto. `RAW_SOURCE != DERIVED_DEMO`.
- ZIP STORE UTF-8 com quatro entradas fixas: `manifest.json`, `demo.wav`, `original.enc`, `seal.hmac`. CRC32 valida contêiner; SHA-256 de entrada, saída e ciphertext valida bytes; hashchain inclui SHA da origem e derivado e tempos declarados.
- Senha de 10+ caracteres; PBKDF2-HMAC-SHA256 (150.000 iterações, salt aleatório 16 B) deriva 256 bits: AES-128-GCM para original + HMAC-SHA256 para manifesto. AES IV 12 B aleatório; GCM AAD inclui SHA do derivado. Sem credenciais no ZIP.
- Papéis: `guest` pode ouvir `demo.wav` depois de conferir digests **não autenticados**; `owner` confirma HMAC com senha e verifica AES-GCM + SHA da origem antes de ouvir a fonte.
- Expiração: 15 min / 1 h / 24 h, por `expires_utc`, com `SYSTEM_CLOCK_UNVERIFIED`. Rejeição na interface após expirar **não é revogação criptográfica**. Browser não dispõe de NTP bruto; horário local, consultas HTTP e busca na web **não constituem timestamp confiável**.

## Regras de custódia

1. `source_sha256` identifica o WAV normalizado, não os dados de sensor/ADC nem autoria.
2. `demo_sha256` identifica exclusivamente a cópia degradada com picotes.
3. `chain_sha256` é hashchain local e só é autenticado pelo HMAC verificado pelo dono; não há âncora externa ou assinatura pública.
4. ZIP tem CRC de 4 entradas e rejeita métodos, caminhos, duplicatas, tamanhos ou campos fora do perfil.
5. Não enviar áudio automaticamente. Exportação via ação humana; importação explícita.
6. Nenhuma alegação de DRM, impossibilidade de regravação, retenção legal, guarda judicial ou superioridade criptográfica.
7. Quaisquer edições posteriores geram novo recibo `supersedes` (não sobrescrever fonte sem registro).

## Teste e portas

- HOST: `node --check` e testes isolados de WAV, três efeitos, SHA, AES-GCM ida/volta, ZIP STORE ida/volta e falha de CRC após tamper.
- Browser real, microfone Android, permissão, OGG/OPUS, envio por WhatsApp, leitura independente, NTP/TSA, APK, CI remoto: `NOT_RUN`.
- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.
- `R3`: F_ok = fluxo browser autossuficiente e teste local do núcleo; F_gap = teste físico e ponte ao Android, assinatura externa e relógio confiável; F_next = testar envio/retorno no smartphone, fotografar recibo exato e então decidir integração nativa.

**Rollback:** remover somente o HTML/contrato do laboratório; nenhum módulo Android/CI existente é alterado.

**Origem documental:** `rmr/crypto/CUSTODY.md`, `rmr/crypto/tools/forensic_time_attest.py`, `rmr/crypto/ZIP_BITSTACK_CUSTODY_PROFILE.md` em `rafaelmeloreisnovo/BLAKE3`; `docs/ZRF_CFR_FORMAT_V1.md` do EstudioAudio.
## Sucessor V1.1 — inversão de bits, ZIP canônico e correções

**Precedência:** o V1.1 fecha a aceitação de metadados ignorados. É um perfil ZIP restrito, não leitor ZIP universal.

- **CRC original:** bit alterado em payload é rejeitado pelo CRC. **CRC refeito:** alterações em demo.wav/original.enc com manifesto intacto falham no SHA-256.
- **Autenticação:** um atacante pode reconstruir CRC, SHA público e hashchain; visitante ainda pode receber PUBLIC_DIGESTS_MATCH_NOT_AUTHENTICATED. Isso não prova origem. HMAC do proprietário rejeita reconstrução sem a chave.
- **ZIP canônico:** quatro entradas em ordem fixa; cabeçalhos locais/centrais exigem versões definidas, DOS timestamp zerado, flags e método exatos, discos/atributos zero, offsets contíguos, EOCD sem comentário, sem extras ou gaps. Divergência falha mesmo com CRC corrigido.
- **HMAC:** seal.hmac adulterado com CRC refeito pode passar na checagem pública não autenticada; abertura do proprietário deve falhar.
- **Manifesto:** schema de campos fechado; digests hex minúsculos, tipos/claims e papéis exatos, Base64 canônico e comprimentos de fonte/derivado/cifra coerentes.
- **Interface:** importação repetida do mesmo ZIP; expiração reavaliada ao clicar nos dois players mesmo após exportação/desbloqueio; ObjectURL revogado se play() rejeitar; comprimento de wave divergente exposto.
- **Validade:** SYSTEM_CLOCK_UNVERIFIED significa política visual, não revogação criptográfica ou timestamp confiável.

### Gate de regressão

node test.mjs && node regression.mjs --strict

Execução local com amostra sintética 2 s: 31/31 verificações PASS_SCOPED; 208/208 bits de metadados ZIP e 64/64 bits dos campos CRC rejeitados; CRC refeito vs SHA, reconstrução de hashes públicos vs HMAC, expiração, duplo import, schema, Base64, reprodução, senha errada, limite de 70 KiB. ZIP amostral 33.593 bytes. Isso NÃO é varredura exaustiva de cada bit do ZIP.

CI remoto específico da nova suíte, navegador físico e microfone Android permanecem NOT_RUN até recibo do HEAD. claim_allowed=false.

**Rollback:** restaurar somente labs/custody70/ no commit anterior do PR; main, APK, JNI e Android intocados.
