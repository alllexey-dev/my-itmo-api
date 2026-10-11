#!/bin/bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
MODE=${1:-all}
START=$SECONDS

summary() {
    local result=$?
    local outcome=FAIL
    local revision
    trap - EXIT
    case "$result" in
        0) outcome=PASS ;;
        2) ;;
        *) result=1 ;;
    esac
    revision=$(git rev-parse --short=7 HEAD 2>/dev/null) || revision=unknown
    if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
        revision="${revision}+dirty"
    fi
    printf 'VERIFY M %s %s %ss %s\n' "$MODE" "$outcome" "$((SECONDS - START))" "$revision"
    exit "$result"
}
trap summary EXIT

unavailable() {
    printf '%s\n' "$*" >&2
    exit 2
}

case "$MODE" in
    all|maven|kmp|kmp-jvm|kmp-ios) ;;
    *) unavailable 'Usage: scripts/verify.sh [all|maven|kmp|kmp-jvm|kmp-ios]' ;;
esac
[ "$#" -le 1 ] || unavailable 'Expected at most one verification mode'

require_kmp() {
    [ -f kmp/settings.gradle.kts ] || unavailable 'KMP: kmp/ is missing'
    [ -x ./gradlew ] || unavailable 'KMP: root Gradle wrapper is unavailable'
}

has_xcode() {
    command -v xcrun >/dev/null 2>&1 && xcrun --sdk iphonesimulator --show-sdk-path >/dev/null 2>&1
}

case "$MODE" in
    kmp|kmp-jvm|kmp-ios) require_kmp ;;
esac
if [ "$MODE" = kmp-ios ] && ! has_xcode; then
    unavailable 'iOS tests: SKIP (no Xcode)'
fi

if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 17) || unavailable 'JDK 17 is unavailable'
    export JAVA_HOME
fi

verify_maven() {
    [ -x ./mvnw ] || unavailable 'Maven wrapper is unavailable'
    ./mvnw -B verify -Dgpg.skip=true "-Dmaven.repo.local=$ROOT/target/verify-maven-repository"
}

verify_kmp_jvm() {
    require_kmp
    ./gradlew -p kmp jvmTest
}

verify_kmp() {
    verify_kmp_jvm
    ./gradlew -p kmp compileKotlinIosArm64 compileKotlinIosSimulatorArm64
    if has_xcode; then
        ./gradlew -p kmp iosSimulatorArm64Test
    else
        printf 'iOS tests: SKIP (no Xcode)\n'
    fi
}

case "$MODE" in
    all)
        verify_maven
        if [ -f kmp/settings.gradle.kts ]; then
            verify_kmp
        fi
        ;;
    maven) verify_maven ;;
    kmp) verify_kmp ;;
    kmp-jvm) verify_kmp_jvm ;;
    kmp-ios) ./gradlew -p kmp iosSimulatorArm64Test ;;
esac
