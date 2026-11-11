cmake_minimum_required(VERSION 3.22.1)
project(rafaelia_audio C)

add_library(dsp_core STATIC dsp_core.c)
add_library(meter_core STATIC meter_core.c)
add_library(rfa_manifold_core STATIC
    rfa_zip32_core.c
    rfa_digest_core.c
    rfa_wave_core.c
    rfa_matrix_core.c
    rfa_bit_stack_core.c
    rfa_block_core.c
    rfa_container_core.c
    rfa_fir_core.c
    rfa_time_core.c
    rfa_lms_core.c
    rfa_biquad_core.c
    rfa_voice_core.c
    rfa_playback_observer_core.c
    rfa_tele_tts_core.c
    rfa_sensor_delta_core.c
    rfa_tts_pcm_guard_core.c
    rfa_dual_watchdog_core.c
    rfa_ptbr_core.c
    rfa_timbre_core.c
    rfa_measure_core.c
    rfa_room_guard_core.c
    rfa_rac1_core.c
    rfa_audio_format_core.c
)

add_library(rfa_verbo_core STATIC rfa_verbo_core.c)

set_target_properties(dsp_core meter_core rfa_manifold_core rfa_verbo_core PROPERTIES
    POSITION_INDEPENDENT_CODE ON
)

set(RFA_CORE_FLAGS
    -O3
    -ffreestanding
    -fno-builtin
    -nostdinc
    -fno-stack-protector
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -fno-common
    -fno-optimize-sibling-calls
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

target_compile_options(dsp_core PRIVATE ${RFA_CORE_FLAGS})
target_compile_options(meter_core PRIVATE ${RFA_CORE_FLAGS})
target_compile_options(rfa_manifold_core PRIVATE ${RFA_CORE_FLAGS})
target_compile_options(rfa_verbo_core PRIVATE ${RFA_CORE_FLAGS})

add_library(rafaelia_audio SHARED jni_bridge.c)

target_compile_options(rafaelia_audio PRIVATE
    -O3
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

target_link_libraries(rafaelia_audio PRIVATE dsp_core meter_core rfa_manifold_core rfa_verbo_core)

target_link_options(rafaelia_audio PRIVATE
    -Wl,--as-needed
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL
)

# RAFAELIA ASM PCM: two independently compiled leaf modules per Android ABI.
# Staged/no JNI calls until exact-head dual-device differential evidence.
enable_language(ASM)
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(RFA_PCM_ABS_SOURCE arch/armv7/rfa_pcm_abs_u16.S)
    set(RFA_PCM_SAT_SOURCE arch/armv7/rfa_pcm_sat_add_s16.S)
    set(RFA_PCM_MODULE_FLAGS -marm)
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    set(RFA_PCM_ABS_SOURCE arch/aarch64/rfa_pcm_abs_u16.S)
    set(RFA_PCM_SAT_SOURCE arch/aarch64/rfa_pcm_sat_add_s16.S)
    set(RFA_PCM_MODULE_FLAGS "")
else()
    message(FATAL_ERROR "RAFAELIA_PCM_UNSUPPORTED_ANDROID_ABI=${ANDROID_ABI}")
endif()
add_library(rfa_pcm_abs_u16 STATIC ${RFA_PCM_ABS_SOURCE})
add_library(rfa_pcm_sat_add_s16 STATIC ${RFA_PCM_SAT_SOURCE})
foreach(rfa_pcm_target rfa_pcm_abs_u16 rfa_pcm_sat_add_s16)
    set_target_properties(${rfa_pcm_target} PROPERTIES POSITION_INDEPENDENT_CODE ON)
    target_compile_options(${rfa_pcm_target} PRIVATE ${RFA_PCM_MODULE_FLAGS})
endforeach()
