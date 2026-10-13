/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Text validation is NOT PT-BR intelligibility or Android speaker evidence.
 */
package io.rafaelia.audiostudio;

import java.nio.charset.StandardCharsets;

public final class ComplexNetworkNarrationPresetSmoke {
    private ComplexNetworkNarrationPresetSmoke() {}

    public static void main(String[] args) {
        String script = ComplexNetworkNarrationPreset.ptBrScript();
        byte[] utf8 = script.getBytes(StandardCharsets.UTF_8);
        if (utf8.length == 0 || utf8.length > 2048) {
            throw new AssertionError("PTBR_NATIVE_BOUNDS");
        }
        if (!script.equals(ComplexNetworkNarrationPreset.ptBrScript())) {
            throw new AssertionError("NONDETERMINISTIC_SCRIPT");
        }
        if (!script.contains("Uma lacuna não é zero.")
                || !script.contains("não comprova sozinho a sua causa.")
                || !script.contains("efeito posterior e reversão.")) {
            throw new AssertionError("UNKNOWN_OR_CAUSAL_GUARDS_MISSING");
        }
        for (int i = 0; i < script.length(); ++i) {
            char c = script.charAt(i);
            boolean ascii = c >= 'A' && c <= 'Z'
                    || c >= 'a' && c <= 'z'
                    || c == ' ' || c == '.' || c == ',';
            boolean latin = "áàâãÁÀÂÃéêÉÊíÍóôõÓÔÕúÚçÇ".indexOf(c) >= 0;
            if (!ascii && !latin) {
                throw new AssertionError("UNSUPPORTED_PTBR_CHARACTER_" + i);
            }
        }
        System.out.println("NETWORK_NARRATION_TEXT_BOUNDS_AND_SEMANTICS=PASS_SCOPED");
        System.out.println("PTBR_LISTENER_INTELLIGIBILITY=NOT_RUN");
        System.out.println("ANDROID_SPEAKER_PHYSICAL=NOT_RUN");
    }
}
