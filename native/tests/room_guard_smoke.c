/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Host-only falsifiers; does not prove physical room correction.
 */
#include "rfa_room_guard_core.h"

static void position(rfa_room_band_q8 *b, int index, int db, rfa_u8 flags) {
    b->positions[index].relative_db_q8 = db * 256;
    b->positions[index].flags = flags;
}
int main(void) {
    rfa_room_band_q8 bands[2] = {{{{0, 0}}, 0, 0}, {{{0, 0}}, 0, 0}};
    rfa_room_decision_q8 decisions[2];
    rfa_room_guard_policy_q8 policy = {3, 8 * 256, 6 * 256};
    int i;

    for (i=0;i<2;++i) {
        bands[i].count=3;
        bands[i].target_relative_db_q8=0;
    }
    position(&bands[0],0,5,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],1,6,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],2,7,RFA_ROOM_OBS_REQUIRED);
    position(&bands[1],0,-8,RFA_ROOM_OBS_REQUIRED);
    position(&bands[1],1,-7,RFA_ROOM_OBS_REQUIRED);
    position(&bands[1],2,-9,RFA_ROOM_OBS_REQUIRED);
    if (rfa_room_guard_plan_q8(bands,2,&policy,decisions)!=2) return 10;
    if (decisions[0].gain_db_q8!=-6*256 ||
        decisions[0].reason!=RFA_ROOM_DECISION_CUT ||
        decisions[1].gain_db_q8!=0 ||
        decisions[1].reason!=RFA_ROOM_DECISION_NO_CUT) return 11;
    position(&bands[0],0,5,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],1,6,RFA_ROOM_OBS_VALID); /* no sync/clipping evidence */
    position(&bands[0],2,7,RFA_ROOM_OBS_REQUIRED);
    if (rfa_room_guard_plan_q8(bands,1,&policy,decisions)!=1 ||
        decisions[0].reason!=RFA_ROOM_DECISION_INSUFFICIENT ||
        decisions[0].gain_db_q8!=0) return 12;
    position(&bands[0],0,0,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],1,8,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],2,22,RFA_ROOM_OBS_REQUIRED);
    if (rfa_room_guard_plan_q8(bands,1,&policy,decisions)!=1 ||
        decisions[0].reason!=RFA_ROOM_DECISION_UNSTABLE ||
        decisions[0].gain_db_q8!=0) return 13;
    decisions[0].gain_db_q8=123;
    bands[0].count=9;
    if (rfa_room_guard_plan_q8(bands,1,&policy,decisions)!=0 ||
        decisions[0].gain_db_q8!=123) return 14;
    bands[0].count=3;
    policy.max_cut_db_q8=-1;
    if (rfa_room_guard_plan_q8(bands,1,&policy,decisions)!=0) return 15;
    policy.max_cut_db_q8=6*256;
    if (rfa_room_guard_plan_q8((const rfa_room_band_q8 *)0,1,&policy,decisions)!=0) return 16;
    position(&bands[0],0,100,RFA_ROOM_OBS_REQUIRED);
    if (rfa_room_guard_plan_q8(bands,1,&policy,decisions)!=0) return 17;
    position(&bands[0],0,4,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],1,4,RFA_ROOM_OBS_REQUIRED);
    position(&bands[0],2,4,RFA_ROOM_OBS_REQUIRED);
    if (rfa_room_guard_plan_q8(bands,1,&policy,decisions)!=1 ||
        decisions[0].gain_db_q8!=-4*256) return 18;
    return 0;
}
