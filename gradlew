#!/usr/bin/env sh

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)
JAVA_CMD=${JAVA_HOME:+$JAVA_HOME/bin/java}
JAVA_CMD=${JAVA_CMD:-java}

if [ -n "${ANDROID_AAPT2_FROM_MAVEN_OVERRIDE:-}" ]; then
    set -- "-Pandroid.aapt2FromMavenOverride=$ANDROID_AAPT2_FROM_MAVEN_OVERRIDE" "$@"
fi

exec "$JAVA_CMD" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
