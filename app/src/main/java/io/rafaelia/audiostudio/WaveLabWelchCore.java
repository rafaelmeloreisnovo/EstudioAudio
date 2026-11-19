/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

/**
 * Platform-free bounded single-channel FFT/Welch relative band powers.
 * Hosted Java implementation pending native-core equivalence tests.
 * FFT != calibrated RTA; no Pa, dB SPL or physical loudspeaker correction.
 */
final class WaveLabWelchCore {
    static final int SAMPLE_RATE = 48000;
    static final int WINDOW = 8192;
    static final int HOP = WINDOW / 2;
    static final String ESTIMATOR = "WELCH_HANN_8192_HOP4096_BANDPOWER_V1";
    static final int[] CENTERS_HZ = {
        63, 80, 100, 125, 160, 200, 250, 315, 400, 500,
        630, 800, 1000, 1250, 1600, 2000, 2500, 3150,
        4000, 5000, 6300, 8000, 10000, 12500, 16000
    };
    private WaveLabWelchCore() {}

    static final class Result {
        final double[] frequenciesHz;
        final double[] bandPowers;
        final int windowCount;
        Result(double[] hz, double[] powers, int windows) {
            frequenciesHz = hz;
            bandPowers = powers;
            windowCount = windows;
        }
    }
    private static void fft(double[] re, double[] im) {
        final int n = re.length;
        int j = 0;
        for (int i=1;i<n;i++) {
            int bit=n>>1;
            while ((j&bit)!=0) {
                j^=bit;
                bit>>=1;
            }
            j^=bit;
            if (i<j) {
                double t=re[i];re[i]=re[j];re[j]=t;
                t=im[i];im[i]=im[j];im[j]=t;
            }
        }
        for(int span=2;span<=n;span<<=1) {
            int half=span/2;
            double ang=-2*Math.PI/span;
            double wrStep=Math.cos(ang), wiStep=Math.sin(ang);
            for(int start=0;start<n;start+=span) {
                double wr=1,wi=0;
                for(int k=0;k<half;k++) {
                    int u=start+k, v=u+half;
                    double vr=re[v]*wr-im[v]*wi;
                    double vi=re[v]*wi+im[v]*wr;
                    double ur=re[u],ui=im[u];
                    re[u]=ur+vr;im[u]=ui+vi;
                    re[v]=ur-vr;im[v]=ui-vi;
                    double next=wr*wrStep-wi*wiStep;
                    wi=wr*wiStep+wi*wrStep;
                    wr=next;
                }
            }
        }
    }

    static Result analyze(short[] pcm, int count) {
        if (pcm==null || count<WINDOW || count>pcm.length ||
                count>SAMPLE_RATE*31) return null;
        final double[] window=new double[WINDOW];
        double w2=0;
        for(int n=0;n<WINDOW;n++) {
            double w=0.5*(1-Math.cos(2*Math.PI*n/(WINDOW-1)));
            window[n]=w;
            w2+=w*w;
        }
        double[] hz=new double[CENTERS_HZ.length];
        double[] sum=new double[CENTERS_HZ.length];
        for(int b=0;b<hz.length;b++)hz[b]=CENTERS_HZ[b];
        double[] real=new double[WINDOW], imag=new double[WINDOW];
        final double lower=Math.pow(2, -1.0/6.0);
        final double upper=Math.pow(2, 1.0/6.0);
        int frames=0;
        for (int offset=0;offset+WINDOW<=count;offset+=HOP) {
            for (int n=0;n<WINDOW;n++){
                real[n]=(pcm[offset+n]/32768.0)*window[n];
                imag[n]=0;
            }
            fft(real,imag);
            for(int bin=1;bin<WINDOW/2;bin++) {
                double frequency=(double)bin*SAMPLE_RATE/WINDOW;
                double power=2*(real[bin]*real[bin]+imag[bin]*imag[bin])
                        /(WINDOW*w2);
                for (int b=0;b<CENTERS_HZ.length;b++) {
                    if (frequency>=hz[b]*lower && frequency<hz[b]*upper)
                        sum[b]+=power;
                }
            }
            ++frames;
        }
        if(frames==0)return null;
        for(int b=0;b<sum.length;b++) {
            sum[b]/=frames;
            if (!Double.isFinite(sum[b]) || sum[b]<0) return null;
        }
        return new Result(hz,sum,frames);
    }
}
