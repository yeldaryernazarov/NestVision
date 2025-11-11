#!/bin/bash

# Скрипт для запуска forwarder в фоновом режиме

cd "$(dirname "$0")"

echo "🚀 Запуск Telegram Channel Forwarder..."

# Проверяем, установлен ли Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js не установлен!"
    echo "   Установите Node.js: https://nodejs.org/"
    exit 1
fi

# Проверяем, установлены ли зависимости
if [ ! -d "node_modules" ]; then
    echo "📦 Устанавливаю зависимости..."
    npm install
fi

# Запускаем скрипт
echo "✅ Запускаю слушатель..."
node forwarder-simple.js

