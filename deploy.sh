#!/bin/bash
set -e

echo "▶ Build..."
./gradlew clean bootJar

echo "▶ Upload JAR..."
scp build/libs/mangilik-el-0.0.1-SNAPSHOT.jar \
  root@104.248.39.82:/apps/mangilik-el.jar

echo "▶ Restart app..."
ssh root@104.248.39.82 << 'EOF'
cd /apps
docker rm -f mangilikel-el || true
docker-compose up -d app
EOF

echo "✅ Deploy done"
