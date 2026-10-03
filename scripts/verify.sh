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
    [ -f kmp/settings.gradle.kts ] || unavailable 'KMP: unavailable (ML-01a has not landed)'
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

in_slot() {
    local kind=$1
    local slot_script=${ITMO_SLOT_SH:-$HOME/proj/.wt/bin/slot.sh}
    shift
    if [ "${CI:-false}" = true ]; then
        "$@"
    elif [ -x "$slot_script" ]; then
        "$slot_script" "$kind" -- "$@"
    else
        [ -x /usr/bin/lockf ] || unavailable 'Build slots require lockf or ITMO_SLOT_SH'
        [ "$kind" != kn ] || kind=android
        mkdir -p "$HOME/.cache/itmo-agents/slots"
        /usr/bin/lockf -k "$HOME/.cache/itmo-agents/slots/${kind}.1.lock" "$@"
    fi
}

verify_maven() {
    local maven
    if [ -x ./mvnw ]; then
        maven=./mvnw
    elif command -v mvn >/dev/null 2>&1; then
        maven=$(command -v mvn)
    else
        maven='/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn'
        [ -x "$maven" ] || unavailable 'Maven is unavailable'
    fi
    in_slot jvm "$maven" -B verify -Dgpg.skip=true "-Dmaven.repo.local=$ROOT/target/verify-maven-repository"
}

verify_kmp_jvm() {
    require_kmp
    in_slot jvm ./gradlew -p kmp jvmTest
}

verify_kmp() {
    verify_kmp_jvm
    in_slot kn ./gradlew -p kmp compileKotlinIosArm64 compileKotlinIosSimulatorArm64
    if has_xcode; then
        in_slot kn ./gradlew -p kmp iosSimulatorArm64Test
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
    kmp-ios) in_slot kn ./gradlew -p kmp iosSimulatorArm64Test ;;
esac
