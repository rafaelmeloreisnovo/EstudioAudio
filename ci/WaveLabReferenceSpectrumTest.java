/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Strict host-only deterministic tests; NOT device or physical acoustic validation.
 */
package io.rafaelia.audiostudio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public final class WaveLabReferenceSpectrumTest {
    private static int assertions=0;
    private static void check(boolean yes,String name){
        assertions++;
        if(!yes)throw new AssertionError(name);
    }
    private static void put16(byte[] b,int p,int value){
        b[p]=(byte)value;
        b[p+1]=(byte)(value>>8);
    }
    private static void put32(byte[] b,int p,int value){
        put16(b,p,value);put16(b,p+2,value>>16);
    }
    private static void tag(byte[] b,int pos,String word){
        for(int j=0;j<4;j++)b[pos+j]=(byte)word.charAt(j);
    }
    private static byte[] wav(int seconds) {
        int samples=48000*seconds;
        byte[] b=new byte[44+samples*2];
        tag(b,0,"RIFF");put32(b,4,b.length-8);
        tag(b,8,"WAVE");
        tag(b,12,"fmt ");put32(b,16,16);
        put16(b,20,1);put16(b,22,1);
        put32(b,24,48000);put32(b,28,96000);
        put16(b,32,2);put16(b,34,16);
        tag(b,36,"data");put32(b,40,samples*2);
        for(int n=0;n<samples;n++)put16(b,44+n*2,
                (int)(4000*Math.sin(2*Math.PI*1000*n/48000)));
        return b;
    }
    private static void invalid(byte[] b,String title){
        boolean refused=false;
        try{ WaveLabReferencePcm.parse(b); }
        catch(IOException expected){refused=true;}
        check(refused,title);
    }
    private static void wavTests() throws Exception {
        byte[] raw=wav(3);
        WaveLabReferencePcm.Parsed p=WaveLabReferencePcm.parse(raw);
        check(p.pcm.length==144000,"3s sample count");
        check(p.sampleRate==48000,"48k rate");
        check(p.pcm[0]==0,"sample0");
        check(WaveLabReferencePcm.readBounded(new ByteArrayInputStream(raw)).length==raw.length,
                "byte-read identity");
        byte[] different=raw.clone();
        put32(different,4,raw.length);
        invalid(different,"false RIFF length");
        different=raw.clone();put16(different,22,2);
        invalid(different,"stereo refused");
        different=raw.clone();put32(different,24,44100);
        invalid(different,"different sample rate");
        different=raw.clone();put16(different,34,8);
        invalid(different,"not pcm16");
        different=Arrays.copyOf(raw,raw.length-1);
        invalid(different,"truncated");
        different=raw.clone();tag(different,36,"garb");
        invalid(different,"missing data chunk");
        different=raw.clone();put32(different,40,0x7fffffff);
        invalid(different,"out of range chunk");
        different=raw.clone();tag(different,0,"RIFX");
        invalid(different,"big endian refused");
        check(WaveLabReferencePcm.parse(wav(30)).pcm.length==1440000,
                "max 30 sec");
    }
    private static void spectrumTests() {
        int n=48000*3;
        short[] pcm=new short[n],silence=new short[n];
        for(int i=0;i<n;i++)pcm[i]=(short)(8000*Math.sin(2*Math.PI*1000*i/48000));
        WaveLabWelchCore.Result one=WaveLabWelchCore.analyze(pcm,n);
        check(one!=null && one.windowCount>2,"windowing");
        check(one.bandPowers.length==25,"25 bands");
        int peak=0;
        for(int b=0;b<one.bandPowers.length;b++) {
            check(Double.isFinite(one.bandPowers[b]) && one.bandPowers[b]>=0.0,
                    "finite nonnegative "+b);
            if(one.bandPowers[b]>one.bandPowers[peak])peak=b;
        }
        check(one.frequenciesHz[peak]==1000.0,"1kHz band dominates");
        WaveLabWelchCore.Result again=WaveLabWelchCore.analyze(pcm,n);
        check(again!=null && again.bandPowers[peak]==one.bandPowers[peak],
                "deterministic repeat");
        WaveLabWelchCore.Result zero=WaveLabWelchCore.analyze(silence,n);
        check(zero!=null && zero.bandPowers[peak]==0.0,
                "zero is zero not invented floor");
        check(WaveLabWelchCore.analyze(pcm,100)==null,"short rejected");
        check(WaveLabWelchCore.analyze(null,n)==null,"null rejected");
    }
    public static void main(String[] args) throws Exception {
        wavTests();
        spectrumTests();
        System.out.println("WAVELAB_REFERENCE_SPECTRUM_HOST_PASS_SCOPED assertions="+assertions);
        System.out.println("WAVELAB_ANDROID_PLAYBACK_CAPTURE=NOT_RUN");
        System.out.println("WAVELAB_ABSOLUTE_SPL=TOKEN_VAZIO");
    }
}
