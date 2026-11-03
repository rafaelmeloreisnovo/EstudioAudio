# EstudioAudio · ###_AUDIO_CUSTODY70_CONTRACT_V1_2

SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**Estado:** CANDIDATE_DEMO / NODE_PASS_SCOPED / DRAFT_HOLD / `claim_allowed=false` / `release_allowed=false`.

## 1. Fonte, revisão e fronteira

- **SOURCE:** upload do usuário `AUDIO_CUSTODY70_V1_2_KIT.zip`; HTML de entrada preservado como `###_AUDIO_CUSTODY70_V1_2_UPLOADED_ORIGINAL.html`, SHA-256 registrado no README.
- **ARTIFACT:** versão corrigida `###_AUDIO_CUSTODY70_V1_2.html`, testes autocontidos, amostra **sintética** e contrato presente.
- **EXECUTION:** Node.js 22, sem navegador real, microfone físico, Android/JNI/APK ou serviço de tempo externo.
- **EVIDENCE:** logs dos scripts vinculados ao hash dos arquivos; o resultado de cada gate pertence apenas à execução correspondente.
- **CLAIM:** `claim_allowed=false`. Proteção criptográfica incide no `original.enc` mediante AES-GCM e senha; os ruídos/cortes no `demo.wav` não são confidencialidade ou DRM.

## 2. Perfil binário exato (V1.2, sem migração implícita)

- **ZIP STORE:** máximo 71.680 bytes; entradas em ordem fixa `manifest.json`, `demo.wav`, `original.enc`, `seal.hmac`. Cabeçalhos locais/centrais, datas DOS zeradas, flags, offsets, tamanhos, atributos, EOCD e CRC32 exigidos em perfil canônico; não é um parser ZIP universal.
- **WAV:** PCM 8 bits, mono, 8.000 amostras/s, até 2 segundos; o áudio-fonte protegido é WAV **normalizado**, não fluxo bruto do microfone/ADC.
- **Senha:** pelo menos 10 caracteres; PBKDF2-HMAC-SHA256, 150.000 iterações e salt aleatório de 16 bytes; deriva **288 bits (36 bytes)**: AES-128-GCM = 16 bytes, HMAC-SHA256 = 16 bytes, semente = 4 bytes. IV GCM aleatório de 12 bytes. Não se usam 320 bits: o código de entrada calculava 320, mas descartava os últimos 32 bits; a versão reconciliada solicita os 288 bits consumidos sem alterar os primeiros 36 bytes.
- **Semente:** extraída dos últimos 4 bytes; o código define o bit menos significativo a 1, reduzindo o espaço de estados a **até 2³¹**. O gerador XorShift32 **não é gerador criptográfico**; a semente não é proteção de confidencialidade do áudio público. O algoritmo exige parâmetro explícito ao derivar áudio.
- **Derivação:** demo de ruído com `rain`, `falls` ou `crackle` e picotes de 550 amostras/5200. Uma prévia temporária usa semente aleatória local; na exportação o derivado é **recalculado** pela semente KDF e vinculado ao manifesto. O proprietário confere `DERIVATION_MATCH_WITH_PASSWORD_SEED` após autenticar HMAC, GCM e SHA da fonte.
- **Domínios V1.2:** `schema=rafe.audio-custody70.v1.2`; GCM AAD prefixado `C70-V1.2:`; hashchain prefixada `RAFCODE-CUSTODY70-V1.2|`. Pacotes V1.1 têm outro perfil e são **rejeitados**, não migrados silenciosamente; o leitor V1.1 deve permanecer disponível em separado para evidências antigas.
- **Verificação pública:** CRC32, SHA-256 e hashchain conferem valores **declarados e recalculáveis por qualquer um**. `PUBLIC_DIGESTS_MATCH_NOT_AUTHENTICATED` **não prova autoria/origem**. HMAC protege manifesto somente para quem tem senha; senha compartilhada permite forjas por outro portador. AES-GCM verifica tag e vincula ciphertext ao SHA de `demo.wav` por AAD.
- **Validade:** 15 min/1 h/24 h baseados em `SYSTEM_CLOCK_UNVERIFIED`; a política bloqueia botões de reprodução no visualizador, mas **não revoga o segredo**, não impede decriptar externamente nem oferece prova cronológica confiável. Sem NTP/TSA.

## 3. Ataques reproduzíveis e interpretação

Os números abaixo pertencem a **uma amostra sintética senoidal de 2 s** e a **um estimador específico** que subtrai ruído de XorShift32 usando uma semente candidata e interpola lacunas. Precisão = fração das 16.000 amostras com diferença ≤1 LSB, comparada à fonte somente pelo avaliador de teste.

| Probe | Medição reproduzida | Conclusão autorizada |
|---|---:|---|
| Semente pública antiga | 6,1% | Falha deste estimador particular |
| Semente errada `1` | 6,0% | Falha deste estimador particular |
| Semente real | 87,0% total / 100% nas amostras retidas | Dependência funcional do seed no algoritmo |
| **Reprodução pública do tom sintético** | **100,0% (16.000/16.000)** | **Refuta** tratar 6% como limite geral para ataque sem senha |

**Regra:** `6% ≠ segurança`, `87% ≠ limite universal`, `100% da amostra sintética ≠ recuperação garantida de microfone real`. Dados públicos/sinais previsíveis, reconstrução perceptual, denoise e busca de espaço de estados são ameaças fora do estimador simples. A semente efetiva de ≤31 bits é enumerável em princípio; este protocolo não foi criptanalisado independentemente. A demonstração não justifica afirmar que alguém sem senha *não consegue* remover ruído.

A bateria de 10 cenários demonstra expressamente: um portador da senha pode forjar pacote e HMAC (`A2`), o prazo não impede descriptografia externa (`A1`), hashes públicos aceitam manifesto reconstruído sem autenticação (`A3`), alteração sem hashes falha, conteúdo alterado ou HMAC errado rejeitados no desbloqueio e backdating com senha é bloqueado apenas na interface.

**Assinatura Ed25519** com chave pública confiável/pinada seria uma medida de autenticação assimétrica, não uma solução isolada para prova de gravação original, identidade, timestamp, ou impedimento de forja pelo próprio assinante. Necessitaria identidade/chave custodiada e atestação temporal quando relevantes.

## 4. Gate reprodutível

Execute na pasta do kit com Node.js mantido:

```bash
node --check test.mjs
node test.mjs
node regression.mjs --strict
node make_sample_v12.mjs
node reconstruction_probe.mjs
node known_plaintext_probe.mjs
node forgery_battery.mjs
```

Os scripts usam explicitamente `###_AUDIO_CUSTODY70_V1_2.html`. `harness.mjs` e `sample_v12.zip` integram o kit; `make_sample_v12.mjs` pode gerar uma amostra nova e substitui apenas sua própria fixture sintética. A amostra usa a **senha deliberadamente pública** `DemoPass!2026`, proibida para material real. Testes que escrevem resultados geram `results.json` localmente.

**Resultado local em 2026-11-03:** `test.mjs` PASS, `regression.mjs --strict` **32/32 (31 anteriores + gate de versão)**, `reconstruction_probe` 6.1/6.0/87.0, `known_plaintext_probe` 100% e `forgery_battery` **10/10** conforme esperado. Esses são testes simulados com Node, não medições físicas.

## 5. Publicação, rollback e próximo gate

- **DRAFT/HOLD**, sem merge em `main`, sem publicação jurídica ou DRM, sem substituição do HTML V1.1. Preservar as duas versões em caminhos distintos.
- **F_gap:** navegador real e Android com permissão do microfone; teste de envio/recebimento em outro app; criptanálise externa, proteção contra brute force e fixação de identidade; NTP/TSA, assinatura pública; APK `NOT_RUN`.
- **Rollback:** remover apenas novos arquivos versionados V1.2 do laboratório e o passo V1.2 de CI, mantendo V1.1 e seu histórico; nunca apagar evidências anteriores.
- **R3:** `<PASS_SCOPED, PHYSICAL_NOT_RUN + SECURITY_LIMITS, RUN_BROWSER_AND_REVIEW>`.
