/* Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Educational script, not a research simulation or a claim of speech quality.
 */
package io.rafaelia.audiostudio;

/* Hosted Java text boundary only; the PCM/PT-BR synthesis stays in the existing
 * freestanding C module. No external SDK, service, data or researcher voice. */
final class ComplexNetworkNarrationPreset {
    private ComplexNetworkNarrationPreset() {}

    static String ptBrScript() {
        return "Uma rede complexa possui agentes e ligações que podem mudar com o tempo. "
                + "Uma mensagem pode alterar a reação de um agente. "
                + "A reação muda a ligação e essa ligação muda a próxima mensagem. "
                + "Quando várias condições se encontram, pode surgir uma nova ação. "
                + "Um evento observado não comprova sozinho a sua causa. "
                + "Uma lacuna não é zero. "
                + "Antes de promover qualquer mudança, precisamos conferir fonte, "
                + "revisão, controle, efeito posterior e reversão.";
    }

    static String physicalPlaybackObserverScript() {
        return "Cole aqui a resposta que será lida por um TTS externo. "
                + "Reproduza o áudio em outro aplicativo, como GPT, DeepSeek ou Claude, "
                + "e grave pelo botão físico do EstudioAudio. "
                + "O observador mede pausas, silêncios, glitches e mudanças acústicas candidatas. "
                + "Ele não identifica pessoa, sexo ou voz humana. "
                + "Qualquer mudança de tom ou leitor fica como TOKEN_VAZIO até existir evidência física revisável.";
    }
}
