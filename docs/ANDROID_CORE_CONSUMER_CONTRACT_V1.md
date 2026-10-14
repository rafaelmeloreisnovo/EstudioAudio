# Android consumer boundary for standalone byte cores V1

SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

Status: SHA-256/CRC32 adapter source added; no APK, physical-device or
production-route promotion claim.

## Purpose

The standalone C modules `rfa_digest_core` and `rfa_zip32_core` are already
freestanding and independently tested. This contract defines the smallest safe
Android/JNI consumer that can be added without changing the existing Java
serializer or its byte contract. The current PR now implements only the first
slice of that consumer: SHA-256 and CRC32 native entrypoints plus an opt-in Java
adapter that falls back to Java unless native output is exact.

## A/B rule

The Java path remains the reference producer until every fixture below matches:

1. SHA-256 of empty input, ASCII `abc`, 255 bytes, 4096 bytes, and a 65536-byte
   deterministic vector.
2. CRC-32 of the empty vector and `123456789`.
3. ZIP32 STORED output for an empty archive, one empty entry, and the current
   multi-entry evidence fixture.
4. Rejection behavior for duplicate names, traversal names, capacity overflow,
   and entry-count overflow.

The native consumer must be opt-in at first. A failed native result falls back to
the Java producer in the same operation and records the route
`NATIVE_REJECTED_JAVA_FALLBACK`; it must never silently emit a partial archive.

## Boundary

The JNI edge may use Java arrays and Android memory management. The C modules
may not gain JNI, ART, libc, filesystem, network, threads, global mutable state,
or hidden allocation. The edge owns all buffers and passes disjoint bounded
regions.
The app's existing `LowSha256` and `RfaStoredZip` remain the rollback baseline.

## Required implementation shape

The consumer is a narrow adapter with:

- caller-owned digest state and output bytes;
- explicit capacity and overflow checks before writing;
- no persistent native pointer into a Java array;
- one native operation per complete artifact, with an explicit status code;
- byte-for-byte comparison gate against the Java oracle in CI;
- an Android instrumentation receipt recording route, artifact SHA-256, ABI and
  source/CI coordinates.

No UI or codec path may switch to native merely because the symbol links.

## Fail-closed criteria

The adapter is not eligible for merge if any native result differs from the Java
oracle, if a rejection path writes bytes, if an exception leaves a partial
artifact, or if either ARM ABI introduces an undefined symbol outside the
existing JNI platform boundary.

## Rollback

Rollback is a one-commit revert of the adapter and declarations. Removing the
adapter must leave the existing Java producer and all historical receipts
unchanged.

## Remaining gaps

- IMPLEMENTED_SOURCE_ONLY: SHA-256/CRC32 JNI adapter and Java consumer; CI quick
  source gate passed, but no APK/instrumentation artifact was produced.
- TOKEN_VAZIO: ZIP32 native adapter and exact Android A/B instrumentation.
- TOKEN_VAZIO: physical device receipt for the native route.
- TOKEN_VAZIO: provider/reviewer enforcement.
- NOT_APPLICABLE to this contract: replacing Gradle, SDK, NDK, ART or Android I/O
  with freestanding code.

R3=<F_ok: standalone C byte contracts, Java baseline and SHA/CRC native adapter
source are defined,
F_gap: ZIP32 adapter, cross-ABI APK bytes, instrumentation and physical
native-route receipt,
F_next: add exact A/B instrumentation gate before switching any production call
site>
