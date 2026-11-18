#!/bin/bash
# Runs the checks CI runs. Usage: scripts/verify.sh [all|jvm|ios]
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
    printf 'VERIFY %s %s %ss %s\n' "$MODE" "$outcome" "$((SECONDS - START))" "$revision"
    exit "$result"
}
trap summary EXIT

unavailable() {
    printf '%s\n' "$*" >&2
    exit 2
}

case "$MODE" in
    all|jvm|ios) ;;
    *) unavailable 'Usage: scripts/verify.sh [all|jvm|ios]' ;;
esac
[ "$#" -le 1 ] || unavailable 'Expected at most one verification mode'
[ -x ./gradlew ] || unavailable 'Gradle wrapper is unavailable'

has_xcode() {
    command -v xcrun >/dev/null 2>&1 && xcrun --sdk iphonesimulator --show-sdk-path >/dev/null 2>&1
}

if [ "$MODE" = ios ] && ! has_xcode; then
    unavailable 'iOS tests: SKIP (no Xcode)'
fi

if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 17) || unavailable 'JDK 17 is unavailable'
    export JAVA_HOME
fi

case "$MODE" in
    jvm) ./gradlew jvmTest ;;
    ios) ./gradlew iosSimulatorArm64Test ;;
    all)
        ./gradlew jvmTest compileKotlinIosArm64 compileKotlinIosSimulatorArm64
        if has_xcode; then
            ./gradlew iosSimulatorArm64Test
        else
            printf 'iOS tests: SKIP (no Xcode)\n'
        fi
        ;;
esac
