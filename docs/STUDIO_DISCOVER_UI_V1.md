# EstudioAudio — Discovery UI Ω V1 (Android platform edge)

**Classificação:** implementação de interface, SEM promoção de release.
**Estado inicial:** `IMPLEMENTED_UNTESTED` até CI do head exato; aparelho físico `NOT_RUN`.

## Produto e navegação
Nova central nativa Android, opcional, aberta pelo botão **Visão, recursos e links** no console principal. Não substitui REC/STOP/PLAY/CAL/PROOF, nem executa medição, calibração ou exportação.

| Tela | Conteúdo | Ação |
|---|---|---|
| Visão geral | captura, análise relativa, custódia | atalhos internos |
| Recursos | gravação/mastering, narração experimental, 16 bandas, laboratório, exportação | leitura/retorno ao console |
| Confiança | fronteira C/Android, claims, estado físico, privacidade | link de gates |
| Links | repositório, START HERE, capacidades, release, issues e licença | `ACTION_VIEW` após toque |

### Wireframe mobile

```text
┌───────────────────────────────────┐
│ ◈ ESTUDIOAUDIO  /  RAFCODE       │
│ Um estúdio. Todas as rotas.      │
│ Interface de descoberta...       │
│ [ANDROID 10+] [CORE C]          │
│ [Visão] [Recursos] [Confiança] > │
│                                   │
│  [CÓDIGO PRESENTE]               │
│  Capturar e acompanhar           │
│  PCM16 / waveform / peak / RMS   │
│                                   │
│  [RELATIVO]                      │
│  Analisar com contexto           │
│  Sem alegar SPL absoluto         │
│                                   │
│ [↩ Voltar ao console de áudio]    │
└───────────────────────────────────┘
```

## Regras de usabilidade
- Mesmo componente nativo suporta Android API 29+, sem dependências AndroidX/Material/externas adicionais.
- Áreas acionáveis >= 48dp; link externo apenas por toque e URL HTTPS fixa; nenhuma solicitação adicional de permissão.
- Texto legível, contraste, estado textual além da cor, rolagem vertical e abas horizontais para 360dp.
- A central é informativa e não interfere na sessão: entrada bloqueada durante REC, DSP, calibração ou evidência.
- Dados não são copiados, alterados nem enviados pela central.
- Uso de badges somente para capacidade, escopo ou limite real; não para conferir prestígio ou status fictício.

## Fonte / artefato / evidência
SOURCE = `StudioDiscoverActivity.java` + ligação `MainActivity` + `AndroidManifest.xml`.
ARTIFACT = classe Android compilada em APK, somente quando o pipeline compilar.
EXECUTION = lançamento/rotas no aparelho, ainda pendente.
EVIDENCE = CI de source+APK e recibo físico por hash, ainda pendentes no momento da escrita.
CLAIM = apenas tela informativa implementada, `claim_allowed_end_to_end=false`.

## Critérios de aceitação
1. `python3 ci/studio-discover-ui-gate.py` verifica a rota, URL/permite/claim estático.
2. Compile Java/Gradle do HEAD exato e confira Manifest + ausência de nova dependência.
3. Android 10: abrir as quatro telas e os links, voltar com hardware/CTA, testar rotação, texto ampliado e toque.
4. Com REC/MASTER ativo: bloqueio de entrada mantém o áudio inalterado.
5. Confrontar bytes/SHA de APK com recibo; teste físico não substituído por CI.

**Rollback:** revert apenas deste delta; console e DSP originais intactos.
