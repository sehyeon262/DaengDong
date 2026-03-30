
FROM jenkins/jenkins:lts

USER root

# ============================================
# 기본 패키지 설치
# ============================================
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    curl \
    && rm -rf /var/lib/apt/lists/*

# ============================================
# Android SDK 설치
# ============================================
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# Android command-line tools 다운로드
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && cd ${ANDROID_HOME}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip \
    && unzip -q cmdline-tools.zip \
    && mv cmdline-tools latest \
    && rm cmdline-tools.zip

# Android SDK 라이센스 동의 + 필수 패키지 설치
RUN yes | sdkmanager --licenses > /dev/null 2>&1 \
    && sdkmanager --install \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0"

# ============================================
# Gradle (프로젝트 gradlew 사용 권장, 이건 fallback)
# ============================================
ENV GRADLE_VERSION=8.7
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

# ============================================
# Firebase CLI 설치 (App Distribution 배포용)
# ============================================
RUN curl -sL https://firebase.tools | bash

# root 유저 유지 (기존 설정과 동일)
USER root
