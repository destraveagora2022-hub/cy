#include <jni.h>
#include <sys/uio.h>
#include <unistd.h>
#include <dirent.h>
#include <cstring>
#include <cstdio>
#include <android/log.h>

#define TAG "cryX"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static pid_t g_pid = -1;

static pid_t find_pid(const char* pkg) {
    DIR* dir = opendir("/proc");
    if (!dir) return -1;
    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        if (entry->d_type != DT_DIR) continue;
        pid_t pid = (pid_t)atoi(entry->d_name);
        if (pid <= 0) continue;
        char path[64];
        snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
        FILE* f = fopen(path, "r");
        if (!f) continue;
        char cmd[256] = {};
        fread(cmd, 1, sizeof(cmd) - 1, f);
        fclose(f);
        if (strstr(cmd, pkg)) { closedir(dir); return pid; }
    }
    closedir(dir);
    return -1;
}

static bool mem_read(pid_t pid, uintptr_t addr, void* buf, size_t size) {
    struct iovec local  = { buf,         size };
    struct iovec remote = { (void*)addr, size };
    return process_vm_readv(pid, &local, 1, &remote, 1, 0) == (ssize_t)size;
}

static bool mem_write(pid_t pid, uintptr_t addr, const void* buf, size_t size) {
    struct iovec local  = { (void*)buf,  size };
    struct iovec remote = { (void*)addr, size };
    return process_vm_writev(pid, &local, 1, &remote, 1, 0) == (ssize_t)size;
}

static uintptr_t get_module_base(pid_t pid, const char* mod) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE* f = fopen(path, "r");
    if (!f) return 0;
    char line[512]; uintptr_t base = 0;
    while (fgets(line, sizeof(line), f))
        if (strstr(line, mod)) { sscanf(line, "%lx", &base); break; }
    fclose(f);
    return base;
}

static uintptr_t walk_chain(pid_t pid, uintptr_t base,
                             const uintptr_t* offs, size_t count) {
    uintptr_t addr = base;
    for (size_t i = 0; i < count; i++) {
        uintptr_t next = 0;
        if (!mem_read(pid, addr, &next, sizeof(next))) return 0;
        addr = next + offs[i];
    }
    return addr;
}

extern "C" {

JNIEXPORT jint JNICALL
Java_com_cryx_MemoryReader_findPid(JNIEnv* env, jobject, jstring jpkg) {
    const char* pkg = env->GetStringUTFChars(jpkg, nullptr);
    pid_t pid = find_pid(pkg);
    env->ReleaseStringUTFChars(jpkg, pkg);
    return (jint)pid;
}

JNIEXPORT jboolean JNICALL
Java_com_cryx_MemoryReader_attachPid(JNIEnv*, jobject, jint pid) {
    g_pid = (pid_t)pid;
    LOGI("Attached PID %d", g_pid);
    return g_pid > 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_cryx_MemoryReader_getModuleBase(JNIEnv* env, jobject, jstring jmod) {
    const char* mod = env->GetStringUTFChars(jmod, nullptr);
    uintptr_t base = get_module_base(g_pid, mod);
    env->ReleaseStringUTFChars(jmod, mod);
    return (jlong)base;
}

JNIEXPORT jfloat JNICALL
Java_com_cryx_MemoryReader_readFloat(JNIEnv*, jobject, jlong addr) {
    float v = 0.f;
    mem_read(g_pid, (uintptr_t)addr, &v, sizeof(v));
    return v;
}

JNIEXPORT jint JNICALL
Java_com_cryx_MemoryReader_readInt(JNIEnv*, jobject, jlong addr) {
    int v = 0;
    mem_read(g_pid, (uintptr_t)addr, &v, sizeof(v));
    return v;
}

JNIEXPORT jlong JNICALL
Java_com_cryx_MemoryReader_readLong(JNIEnv*, jobject, jlong addr) {
    uintptr_t v = 0;
    mem_read(g_pid, (uintptr_t)addr, &v, sizeof(v));
    return (jlong)v;
}

JNIEXPORT jboolean JNICALL
Java_com_cryx_MemoryReader_writeFloat(JNIEnv*, jobject, jlong addr, jfloat val) {
    return mem_write(g_pid, (uintptr_t)addr, &val, sizeof(val)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_cryx_MemoryReader_writeInt(JNIEnv*, jobject, jlong addr, jint val) {
    return mem_write(g_pid, (uintptr_t)addr, &val, sizeof(val)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_cryx_MemoryReader_walkChain(JNIEnv* env, jobject,
                                      jlong base, jlongArray joffs) {
    jsize len = env->GetArrayLength(joffs);
    jlong* raw = env->GetLongArrayElements(joffs, nullptr);
    auto* offs = new uintptr_t[len];
    for (int i = 0; i < len; i++) offs[i] = (uintptr_t)raw[i];
    uintptr_t result = walk_chain(g_pid, (uintptr_t)base, offs, (size_t)len);
    delete[] offs;
    env->ReleaseLongArrayElements(joffs, raw, JNI_ABORT);
    return (jlong)result;
}

} // extern "C"
