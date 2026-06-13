pipeline {
    agent { label 'built-in' }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'BUILD_TYPE', choices: ['Debug', 'Release'], description: '选择打包类型；Debug 调用 pgyerBuildDebug，Release 调用 pgyerBuildRelease')
        string(name: 'JDK_HOME', defaultValue: '/Users/dian/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home', description: 'JDK 17 路径')
        string(name: 'ANDROID_SDK_ROOT_OVERRIDE', defaultValue: '/Users/dian/Library/Android/sdk', description: 'Android SDK 路径；本机默认一般为 /Users/dian/Library/Android/sdk')
        string(name: 'VERSION_DESCRIPTION', defaultValue: 'Jenkins 自动打包上传蒲公英', description: '本次构建说明，写入蒲公英更新文案')
        password(name: 'PGYER_API_KEY', defaultValue: '', description: '蒲公英 _api_key')
        password(name: 'PGYER_USER_KEY', defaultValue: '', description: '蒲公英 userKey，保留给 pgyer-upload.gradle 读取')
        password(name: 'NOTIFY_DING_WEBHOOK', defaultValue: '', description: '钉钉机器人 Webhook；pgyer-upload.gradle 当前固定发送钉钉通知')
        string(name: 'SIGNING_RELEASE_STORE_FILE', defaultValue: '', description: 'Release 签名文件路径；只在 Release 打包时需要')
        string(name: 'SIGNING_RELEASE_KEY_ALIAS', defaultValue: '', description: 'Release 签名 alias；只在 Release 打包时需要')
        password(name: 'SIGNING_RELEASE_STORE_PASSWORD', defaultValue: '', description: 'Release keystore 密码；只在 Release 打包时需要')
        password(name: 'SIGNING_RELEASE_KEY_PASSWORD', defaultValue: '', description: 'Release key 密码；只在 Release 打包时需要')
    }

    environment {
        JAVA_HOME = "${params.JDK_HOME}"
        ANDROID_HOME = "${params.ANDROID_SDK_ROOT_OVERRIDE}"
        ANDROID_SDK_ROOT = "${params.ANDROID_SDK_ROOT_OVERRIDE}"
        PATH = "${params.JDK_HOME}/bin:${params.ANDROID_SDK_ROOT_OVERRIDE}/platform-tools:${params.ANDROID_SDK_ROOT_OVERRIDE}/cmdline-tools/latest/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                sh '''
                    set -e
                    fetched=0
                    for attempt in 1 2 3; do
                      if git fetch origin master; then
                        fetched=1
                        break
                      fi
                      echo "git fetch origin master failed, retry ${attempt}/3" >&2
                      sleep 3
                    done
                    git checkout master
                    if [ "$fetched" = "1" ]; then
                      git merge --ff-only origin/master
                    else
                      echo "Warning: unable to reach origin/master; continue with current local master checkout." >&2
                    fi
                    git rev-parse --abbrev-ref HEAD
                    git rev-parse --short HEAD
                '''
            }
        }

        stage('Validate Environment') {
            steps {
                sh '''
                    set -e
                    has_local_property() {
                      key="$1"
                      [ -f local.properties ] && grep -Eq "^[[:space:]]*${key}[[:space:]]*=[[:space:]]*[^[:space:]]+" local.properties
                    }
                    required_config() {
                      env_name="$1"
                      local_key="$2"
                      eval "env_value=\\${$env_name:-}"
                      if [ -z "$env_value" ] && ! has_local_property "$local_key"; then
                        echo "Missing required config: set Jenkins parameter/env $env_name or local.properties key $local_key" >&2
                        exit 2
                      fi
                    }
                    test -x "$JAVA_HOME/bin/java"
                    "$JAVA_HOME/bin/java" -version
                    test -d "$ANDROID_SDK_ROOT"
                    required_config PGYER_API_KEY "pgyer\\.apiKey"
                    required_config PGYER_USER_KEY "pgyer\\.userKey"
                    required_config NOTIFY_DING_WEBHOOK "notify\\.dingWebhook"
                    if [ "$BUILD_TYPE" = "Release" ]; then
                      required_config SIGNING_RELEASE_STORE_FILE "signing\\.release\\.storeFile"
                      required_config SIGNING_RELEASE_KEY_ALIAS "signing\\.release\\.keyAlias"
                      required_config SIGNING_RELEASE_STORE_PASSWORD "signing\\.release\\.storePassword"
                      required_config SIGNING_RELEASE_KEY_PASSWORD "signing\\.release\\.keyPassword"
                      if [ -n "$SIGNING_RELEASE_STORE_FILE" ]; then
                        test -f "$SIGNING_RELEASE_STORE_FILE"
                      fi
                    fi
                '''
            }
        }

        stage('Build And Upload To Pgyer') {
            steps {
                sh '''
                    set -e
                    chmod +x ./gradlew
                    if [ "$BUILD_TYPE" = "Release" ]; then
                      ./gradlew --no-daemon clean pgyerBuildRelease
                    else
                      ./gradlew --no-daemon clean pgyerBuildDebug
                    fi
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'demo/build/outputs/apk/**/*.apk', allowEmptyArchive: true, fingerprint: true
        }
    }
}
