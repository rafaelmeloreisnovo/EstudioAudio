# ZIP32 / CRC32 — Falsificador sucessor de fonte, 2026-10-14

**Rota canônica:** START HERE Ω V2.2 → CURRENT_STATE → EstudioAudio producer → `ci/core-byte-parity-gate.sh` → `ci/zip32-adversarial-gate.py` → CI exact HEAD → receipt → gate de promoção.

**Intenção:** reduzir incerteza sem duplicar o algoritmo. O core produtor já contém `rfa_crc32_internal.h`, `rfa_zip32_core.c` e `rfa_digest_core.c`. O pacote externo histórico `rfa_zip_crc32.c` não deve ser inserido como segundo produtor.

## Componentes e proveniência

| Nó | Papel | Autoridade / evidência |
| --- | --- | --- |
| A0 | Fonte C ZIP32/CRC32 | GitHub EstudioAudio; `app/src/main/cpp/rfa_zip32_core.c` @ blob `305095ba8422d695319eb7cd6818f431f36c0f71` |
| A1 | CRC32 primitivo | `rfa_crc32_internal.h` @ blob `0f984184228f04098cf5c0064a5b74ef7863ad05` |
| A2 | Oracle de bytes C↔Java existente | `ci/core-byte-parity-gate.sh`, `ci/CoreByteOracle.java` |
| A3 | Adversarial/rejeições/bounds | `ci/zip32-adversarial-gate.py` (este sucessor) |
| A4 | Execução exata de CI | GitHub PR branch/HEAD + workflow ID + log terminal; `TOKEN_VAZIO` até readback |
| A5 | APK/dispositivo/provedor | gates independentes; não promovidos por A3/A4 |

Relações tipadas: A0 **uses** A1; A2 **compares** A0 com Java; A3 **falsifies** A0/A1; A4 **executes** A2/A3; A5 **requires independent evidence**.

## Falsificadores

- **96 amostras determinísticas**, seed `20261014`, arquivos de 1–32 entradas, além de exatamente 32 entradas e arquivo ZIP vazio.
- Oráculo independente por `struct`, `zlib.crc32` e `zipfile` da biblioteca padrão **apenas no teste hospedado**.
- Limites positivos: nome de 255 bytes e entrada de 8 MiB.
- Negativos: nomes inválidos/traversal, duplicação, capacidade insuficiente, comprimento acima de 8 MiB, count 33 / 2³²−1, entradas nulas; falha preserva bytes e `written`.
- Mutação `0xedb88320 → 0xedb88321` **numa cópia temporária** do header, nunca na fonte do repositório; leitor ZIP independente deve rejeitar o CRC.
- Compilação do C em `-ffreestanding -nostdinc -nostdlib -nodefaultlibs`, com `nm -u` vazio e nenhum `DT_NEEDED`. Shared object hospedado é somente uma ponte de teste `ctypes`, não um APK nem boot físico.
- `python -O` deve **falhar fechado**, porque assert sem execução não é prova.

## Estado e decisão

`LOCAL_RECONSTRUCTION=PASS` para os blobs C conferidos com Git hash; `LOCAL_ADVERSARIAL=PASS` para o script desta branch. `CI_EXACT_HEAD=NOT_OBSERVED_AT_AUTHORING`; `FULL_APK=NOT_RUN_BY_THIS_TEST`; `ARM32/ARM64_DEVICE=NOT_RUN`; `PROVIDER_ENFORCEMENT=TOKEN_VAZIO_UNCONFIRMED`; `CLAIM_ALLOWED=false`.

Mudança proposta = apenas testes, roteamento de gate e documentação. Sem alteração de C/Java/JNI/NDK/Gradle, assinador, permissão Android ou arquivo de produção. O teste Python é dependência **do oráculo hospedado**, nunca do core freestanding.

Rollback: `git revert <commit>` do ramo experimental, ou fechar PR sem merge; não force-push no main. Reavaliar promoção só após checks terminais no SHA exato, revisão independente e prova de políticas efetivas do provedor.

`R3=<F_ok:redundância evitada e falsificadores locais reais,F_gap:CI remota e provider/physical independentes,F_next:verificar execução exata de PR e recibo sucessor; não promover automaticamente>`.
