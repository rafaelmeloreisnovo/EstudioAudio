/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Pure-host adversarial test. No microphone, loudspeaker, room or SPL test.
 */
package io.rafaelia.audiostudio;

import java.util.Arrays;

public final class WaveLabTrialMatrixTest {
    private static int assertions;
    private static void check(boolean ok, String context) {
        ++assertions;
        if (!ok) throw new AssertionError(context);
    }
    private static String hex(char c) {
        char[] a = new char[64];
        Arrays.fill(a,c);
        return new String(a);
    }
    private static WaveLabTrialMatrix.Take take(WaveLabTrialMatrix.Role role, char raw,
                                                 long start, double p) {
        return make(role, hex(raw), hex('f'), "mic-A", "speaker-A",
                "position-1", "gain-20", "FFT-4096-HANN-POWER-V1",
                "RELATIVE_UNCALIBRATED", "boot-1",
                48000, 480000, 0, start, start + 10000000000L,
                new double[]{125,250,500,1000,2000,4000},
                new double[]{p,p,p,p,p,p});
    }
    private static WaveLabTrialMatrix.Take make(WaveLabTrialMatrix.Role role,
            String raw, String stimulus, String mic, String speaker,
            String position, String gain, String estimator, String calibration,
            String clock, int fs, int samples, int clipped,
            long start, long end, double[] bands, double[] power) {
        return new WaveLabTrialMatrix.Take(role,"PINK_PERIODIC",stimulus,raw,
                mic,speaker,position,gain,estimator,calibration,clock,
                fs,samples,clipped,start,end,bands,power);
    }
    private static void acceptedAndStats() {
        WaveLabTrialMatrix m = new WaveLabTrialMatrix();
        check(m.count()==0, "empty");
        check(m.report().acceptedTakes==0,"no synthetic baseline");
        WaveLabTrialMatrix.Take a=take(WaveLabTrialMatrix.Role.PRIMARY,'1',10,1.0);
        check(m.append(a)==WaveLabTrialMatrix.Decision.ACCEPTED_PRIMARY,"primary");
        check(Double.isNaN(m.report().spanDb[0]),"one take cannot have variance");
        WaveLabTrialMatrix.Take b=take(WaveLabTrialMatrix.Role.SECONDARY,'2',
                20000000000L,2.0);
        check(m.append(b)==WaveLabTrialMatrix.Decision.ACCEPTED_REPEAT,"secondary");
        WaveLabTrialMatrix.Take c=take(WaveLabTrialMatrix.Role.TERTIARY,'3',
                40000000000L,3.0);
        check(m.append(c)==WaveLabTrialMatrix.Decision.ACCEPTED_REPEAT,"tertiary");
        WaveLabTrialMatrix.Report report=m.report();
        check(report.acceptedTakes==3 && m.count()==3,"three only");
        check(report.medianLinearPower.length==6,"band count");
        check(Math.abs(report.medianLinearPower[0]-2.0)<1e-12,"median in linear power");
        check(Math.abs(report.medianRelativeToPrimaryDb[0]-10.0*Math.log10(2.0))<1e-12,"gain dB");
        check(Math.abs(report.spanDb[0]-10.0*Math.log10(3.0))<1e-12,"variation dB");
        check(!report.absoluteSplClaimAllowed && !report.independentCalibrationProven,
                "no SPL or independent calibration promoted");
        check("THREE_TAKES_RELATIVE_ONLY".equals(report.state),"typed scope");
        check(m.append(take(WaveLabTrialMatrix.Role.PRIMARY,'4',70000000000L,4.0))
                ==WaveLabTrialMatrix.Decision.REJECTED_COMPLETE,"cap 3");
        check(m.count()==3,"no extra entry");
    }

    private static void falsifiers() {
        WaveLabTrialMatrix m=new WaveLabTrialMatrix();
        WaveLabTrialMatrix.Take a=take(WaveLabTrialMatrix.Role.PRIMARY,'1',10,1.0);
        WaveLabTrialMatrix.Take b=take(WaveLabTrialMatrix.Role.SECONDARY,'2',
                20000000000L,2.0);
        check(m.append(b)==WaveLabTrialMatrix.Decision.REJECTED_ORDER,"no second without first");
        check(m.append(null)==WaveLabTrialMatrix.Decision.REJECTED_INPUT,"no null");
        check(m.append(a)==WaveLabTrialMatrix.Decision.ACCEPTED_PRIMARY,"seed");
        check(m.append(take(WaveLabTrialMatrix.Role.SECONDARY,'1',
                20000000000L,2.0))==WaveLabTrialMatrix.Decision.REJECTED_UNBOUND_SOURCE,
                "same PCM digest is NOT another measurement");
        check(m.append(take(WaveLabTrialMatrix.Role.SECONDARY,'2',
                9000000000L,2.0))==WaveLabTrialMatrix.Decision.REJECTED_CLOCK,"overlap");
        check(m.append(make(b.role,b.rawSha256,hex('a'),b.micChain,b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_CONTEXT,"source changed");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,"mic-B",b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_CONTEXT,"mic changed");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                "position-2",b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_CONTEXT,"position changed");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,"gain-30",b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_CONTEXT,"gain changed");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,b.gain,"GOERTZEL-POINT",b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_CONTEXT,"estimator changed");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,1000,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_SHORT,"too short");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,1,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_CLIPPING,"clipping");
        check(m.append(make(b.role,"TOKEN_VAZIO",b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,b.linearPower))==WaveLabTrialMatrix.Decision.REJECTED_UNBOUND_SOURCE,"unknown bytes");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,new double[]{1,1,Double.NaN,1,1,1}))
                ==WaveLabTrialMatrix.Decision.REJECTED_INPUT,"NaN");
        check(m.append(make(b.role,b.rawSha256,b.stimulusSha256,b.micChain,b.speakerChain,
                b.position,b.gain,b.estimator,b.calibrationRef,b.clockDomain,
                b.sampleRate,b.samples,0,b.startMonotonicNs,b.endMonotonicNs,
                b.bandHz,new double[]{1,1,0,1,1,1}))
                ==WaveLabTrialMatrix.Decision.REJECTED_INPUT,"silent band no fake dB");
        check(m.count()==1,"rejections leave first source untouched");
        check(m.append(b)==WaveLabTrialMatrix.Decision.ACCEPTED_REPEAT,"valid after rejects");
        check(m.count()==2,"exact 2");
    }
    public static void main(String[] args) {
        acceptedAndStats();
        falsifiers();
        System.out.println("WAVELAB_TRIAL_MATRIX_HOST_PASS_SCOPED assertions=" + assertions);
        System.out.println("WAVELAB_APPLIED_CALIBRATION=NOT_RUN");
        System.out.println("WAVELAB_REAL_RTA=NOT_IMPLEMENTED");
    }
}
