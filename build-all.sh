#!/usr/bin/env bash
# Builds every Minecraft version of the mod on this machine and collects the jars in dist/.
#
#   ./build-all.sh                  build every version
#   ./build-all.sh 1.20.1 26.3      build only these folders
#   ./build-all.sh --check          also start a server with each jar to catch load crashes (slow)
#   ./build-all.sh --version 1.2.0  build as mod version 1.2.0 instead of the one in gradle.properties
#
# Each version needs its own Java. Point the matching variable at a JDK of that version:
#   JAVA_HOME_8, JAVA_HOME_17, JAVA_HOME_21, JAVA_HOME_25
# A version whose variable is unset uses the default JAVA_HOME (or java on the PATH).
set -u
cd "$(dirname "$0")"

# folder:Java version to build it with. Matches the release workflow.
ALL_VERSIONS="1.7.10:21 1.12.2:8 1.16.5:8 1.18.2:17 1.20.1:17 1.20.1-fabric:21 26.3:25"

check=false
mod_version=""
wanted=()
while [ $# -gt 0 ]; do
    case "$1" in
        --check) check=true ;;
        --version) mod_version="${2:?--version needs a value}"; shift ;;
        -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
        *) wanted+=("${1%/}") ;;
    esac
    shift
done

mkdir -p dist
# A full build starts from an empty dist/, so it never holds jars of an older mod version.
[ ${#wanted[@]} -eq 0 ] && rm -f dist/*.jar
passed=()
failed=()

for entry in $ALL_VERSIONS; do
    dir="${entry%%:*}"
    java="${entry##*:}"
    if [ ${#wanted[@]} -gt 0 ] && [[ ! " ${wanted[*]} " == *" $dir "* ]]; then
        continue
    fi

    echo "=== Minecraft $dir (Java $java) ==="
    java_var="JAVA_HOME_$java"
    java_home="${!java_var:-${JAVA_HOME:-}}"
    if [ -z "${!java_var:-}" ]; then
        echo "note: $java_var is not set, using ${java_home:-java on the PATH}"
    fi

    gradle_args=(build)
    [ -n "$mod_version" ] && gradle_args+=(-Pmod_version="$mod_version")

    if (
        cd "$dir" || exit 1
        [ -n "$java_home" ] && export JAVA_HOME="$java_home"
        # The 1.7.10 build (GTNH buildscript) reads VERSION instead of mod_version.
        [ -n "$mod_version" ] && export VERSION="$mod_version"
        ./gradlew "${gradle_args[@]}" || exit 1
        if $check; then
            ./scripts/check-server-starts.sh || exit 1
        fi
        # Keep only the jar players install (not the -dev, -sources or -api jars).
        find build/libs -maxdepth 1 -name '*.jar' \
            ! -name '*-dev.jar' ! -name '*-sources.jar' ! -name '*-api.jar' \
            -exec cp {} ../dist/ \;
    ); then
        passed+=("$dir")
    else
        failed+=("$dir")
    fi
done

echo
echo "Built: ${passed[*]:-none}"
[ ${#failed[@]} -gt 0 ] && echo "FAILED: ${failed[*]}"
echo "Jars are in dist/:"
ls -1 dist
[ ${#failed[@]} -eq 0 ]
