#!/usr/bin/env python3
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
"""Negative tests for the immutable audio-first V2 script, host-only stdlib."""
from pathlib import Path
import hashlib
import subprocess

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "app/src/main/java/io/rafaelia/audiostudio/ComplexNetworkNarrationPreset.java"
text = source.read_text(encoding="utf-8")
original = source.read_bytes()
EXPECTED_GIT_BLOB = "0f48f96e5d6512282fa859c122bbf96887aadb67"


def git_blob(payload: bytes) -> str:
    # Same Git blob bytes contract as git hash-object; no third-party dependency.
    header = b"blob " + str(len(payload)).encode("ascii") + b"\x00"
    return hashlib.sha1(header + payload).hexdigest()


def semantic_guard(value: str) -> bool:
    anchors = [
        "Pensa numa máquina feita de peças.",
        "Isso é retroalimentação.",
        "Três peças podem funcionar bem separadas",
        "ordem, no tempo ou na combinação das ações.",
        "Um evento observado não comprova sozinho a sua causa.",
        "Uma lacuna não é zero.",
        "O desconhecido precisa ser marcado e investigado",
        "a revisão, as permissões, os outros projetos atingidos",
        "e aprender com o que ainda não sabíamos.",
    ]
    last = -1
    for anchor in anchors:
        at = value.find(anchor)
        if at <= last:
            return False
        last = at
    return True


if git_blob(original) != EXPECTED_GIT_BLOB:
    raise SystemExit("NARRATION_SCRIPT_SOURCE_IDENTITY_MISMATCH")
git_sha = subprocess.check_output(
    ["git", "hash-object", str(source)], cwd=ROOT, text=True
).strip()
if git_sha != EXPECTED_GIT_BLOB:
    raise SystemExit("NARRATION_SCRIPT_GIT_HASH_MISMATCH")
if not semantic_guard(text):
    raise SystemExit("NARRATION_SCRIPT_SEMANTIC_ORDER_MISSING")
# Falsify accidental reversion, loss of typed void, inverted causal claim,
# missing rollback boundary and a one-byte unauthorized modification.
negative_edits = [
    ("Pensa numa máquina feita de peças.", "Isto é só um resumo."),
    ("Uma lacuna não é zero.", "Uma lacuna é zero."),
    ("não comprova sozinho a sua causa.", "comprova sua causa."),
    ("a revisão, as permissões, os outros projetos atingidos",
     "a revisão e as permissões"),
    ("Três peças podem funcionar bem separadas",
     "Duas peças funcionam"),
]
for before, after in negative_edits:
    if before not in text:
        raise SystemExit("NEGATIVE_FIXTURE_ANCHOR_ABSENT")
    altered = text.replace(before, after, 1)
    if git_blob(altered.encode("utf-8")) == EXPECTED_GIT_BLOB:
        raise SystemExit("NEGATIVE_FIXTURE_FALSE_ACCEPTANCE")
    if semantic_guard(altered):
        raise SystemExit("NEGATIVE_FIXTURE_SEMANTIC_FALSE_ACCEPTANCE")
altered = original.replace(b"Uma mensagem muda", b"uma mensagem muda", 1)
if altered == original or git_blob(altered) == EXPECTED_GIT_BLOB:
    raise SystemExit("ONE_BYTE_CHANGE_NOT_REJECTED")
print("AUDIO_FIRST_SCRIPT_V2_NEGATIVE_MUTATIONS=6_REJECTED")
print("SOURCE_GIT_BLOB_LOCK_IS_NOT_PROVIDER_ENFORCEMENT=TRUE")
