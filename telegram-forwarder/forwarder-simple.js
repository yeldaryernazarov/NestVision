import { Telegraf } from 'telegraf';
import https from 'https';
import http from 'http';

// Токен бота (из переменных окружения)
const BOT_TOKEN = process.env.BOT_TOKEN || '8504010525:AAGy12ITz9T2P5BhPYtjt99vf2EWmOjy9NA';

// ID канала (из переменных окружения)
const CHANNEL_ID = process.env.CHANNEL_ID || '-1003264139245';

// Ваш User ID (узнайте у @userinfobot) - куда пересылать сообщения
const YOUR_USER_ID = process.env.USER_ID || '1956288165'; // Ваш ID

// URL Java backend для автоматической обработки
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080/api/telegram/process-video';

// ID бота (для пересылки напрямую боту)
// Боты не могут писать самим себе, поэтому пересылаем в ваш личный чат
// Оттуда вы можете переслать боту, или настроить автоматическую пересылку
const BOT_USER_ID = process.env.BOT_USER_ID || null; // Опционально

const bot = new Telegraf(BOT_TOKEN);

console.log('🤖 Telegram Channel Forwarder - Слушатель канала');
console.log('═══════════════════════════════════════════════════');
console.log('📡 Слушаю канал: nest-pre (ID: ' + CHANNEL_ID + ')');
console.log('⚡ АВТОМАТИЧЕСКАЯ ОБРАБОТКА:');
console.log('   1. Получаю видео из канала');
console.log('   2. Отправляю в Java backend через HTTP API');
console.log('   3. Backend скачивает и добавляет в базу данных');
console.log('   4. Видео появляется на сайте автоматически');
console.log('═══════════════════════════════════════════════════');

// Функция для обработки видео из channel_post
async function processVideo(post, chatId) {
    if (!post.video) return false;
    
    const video = post.video;
    const fileName = video.file_name || `video_${post.message_id}.mp4`;
    const fileId = video.file_id;
    const fileSize = (video.file_size / 1024 / 1024).toFixed(2);
    
    console.log(`\n   📹 ДЕТАЛЬНАЯ ИНФОРМАЦИЯ О ВИДЕО:`);
    console.log(`   - File Name: ${fileName}`);
    console.log(`   - File ID: ${fileId}`);
    console.log(`   - File Size: ${fileSize} MB (${video.file_size} bytes)`);
    console.log(`   - File Unique ID: ${video.file_unique_id || 'N/A'}`);
    console.log(`   - Duration: ${video.duration || 'N/A'} сек`);
    console.log(`   - Width: ${video.width || 'N/A'}`);
    console.log(`   - Height: ${video.height || 'N/A'}`);
    console.log(`   - MIME Type: ${video.mime_type || 'N/A'}`);
    console.log(`   - Message ID: ${post.message_id}`);
    console.log(`   - Chat ID: ${chatId}`);
    console.log(`   - Chat Title: ${post.chat?.title || 'N/A'}`);
    
    // АВТОМАТИЧЕСКАЯ ОБРАБОТКА: Отправляем в Java backend через HTTP
    try {
        const formData = new URLSearchParams();
        formData.append('fileId', fileId);
        formData.append('fileName', fileName);
        formData.append('messageId', post.message_id.toString());
        
        // Определяем категорию по хэштегу из подписи
        let category = 'SUDDEN_EVENT'; // По умолчанию
        let recordedDateTime = null; // Дата и время записи
        
        if (post.caption) {
            const caption = post.caption;
            const captionLower = caption.toLowerCase();
            
            // Ищем хэштеги для определения категории
            const hashtagRegex = /#(\w+)/g;
            const hashtags = caption.match(hashtagRegex) || [];
            
            for (const hashtag of hashtags) {
                const tag = hashtag.toLowerCase().replace('#', '');
                if (tag === 'aggression_children' || tag === 'aggressionchildren') {
                    category = 'AGGRESSION_BETWEEN_CHILDREN';
                    break;
                } else if (tag === 'aggression_teacher' || tag === 'aggressionteacher') {
                    category = 'AGGRESSION_TEACHER';
                    break;
                } else if (tag === 'children_unattended' || tag === 'childrenunattended') {
                    category = 'CHILDREN_UNATTENDED';
                    break;
                } else if (tag === 'sudden_event' || tag === 'suddenevent') {
                    category = 'SUDDEN_EVENT';
                    break;
                }
            }
            
            // Парсим дату и время из формата: DD-MM-YYYY_HH-MM-SS
            // Пример: "07-07-2025_12-12-12"
            const dateTimeRegex = /(\d{2}-\d{2}-\d{4}_\d{2}-\d{2}-\d{2})/;
            const dateTimeMatch = caption.match(dateTimeRegex);
            
            if (dateTimeMatch) {
                recordedDateTime = dateTimeMatch[1];
                console.log(`   📅 Найдена дата записи: ${recordedDateTime}`);
            }
        }
        
        formData.append('category', category);
        if (recordedDateTime) {
            formData.append('recordedDateTime', recordedDateTime);
        }
        
        const url = new URL(BACKEND_URL);
        const options = {
            hostname: url.hostname,
            port: url.port || (url.protocol === 'https:' ? 443 : 80),
            path: url.pathname,
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Content-Length': formData.toString().length
            }
        };
        
        return new Promise((resolve, reject) => {
            console.log(`\n   📤 ОТПРАВКА В BACKEND:`);
            console.log(`   - URL: ${BACKEND_URL}`);
            console.log(`   - File ID: ${fileId}`);
            console.log(`   - File Name: ${fileName}`);
            console.log(`   - Message ID: ${post.message_id}`);
            console.log(`   - Category: ${category}`);
            if (recordedDateTime) {
                console.log(`   - Recorded DateTime: ${recordedDateTime}`);
            }
            
            const req = (url.protocol === 'https:' ? https : http).request(options, (res) => {
                let data = '';
                console.log(`   - HTTP Status: ${res.statusCode}`);
                console.log(`   - Headers:`, res.headers);
                
                res.on('data', (chunk) => { data += chunk; });
                res.on('end', () => {
                    try {
                        console.log(`   - Response Body: ${data}`);
                        const response = JSON.parse(data);
                        
                        if (response.success) {
                            console.log('\n✅ Видео автоматически обработано Java backend!');
                            console.log(`   📁 Файл: ${fileName}`);
                            console.log(`   📂 Категория: ${category}`);
                            if (recordedDateTime) {
                                console.log(`   📅 Дата записи: ${recordedDateTime}`);
                            }
                            console.log('   💾 Добавлено в базу данных\n');
                            resolve(true);
                        } else {
                            console.log('\n⚠️  Видео не обработано:');
                            console.log(`   - Message: ${response.message || 'N/A'}`);
                            console.log(`   - Timestamp: ${response.timestamp || 'N/A'}`);
                            console.log('   Возможно, оно уже существует в базе или произошла ошибка\n');
                            resolve(false);
                        }
                    } catch (e) {
                        console.error('\n❌ Ошибка при парсинге ответа от backend:');
                        console.error(`   - Error: ${e.message}`);
                        console.error(`   - Response: ${data}\n`);
                        resolve(false);
                    }
                });
            });
            
            req.on('error', (error) => {
                console.error('\n❌ Ошибка при отправке в Java backend:');
                console.error(`   - Error: ${error.message}`);
                console.error(`   - Code: ${error.code}`);
                console.error(`   - URL: ${BACKEND_URL}\n`);
                resolve(false);
            });
            
            const formDataString = formData.toString();
            console.log(`   - Request Body: ${formDataString}`);
            req.write(formDataString);
            req.end();
        });
    } catch (error) {
        console.error('❌ Ошибка при автоматической обработке:', error.message);
        return false;
    }
}

// Функция для загрузки всех видео из канала при старте
async function loadAllVideosFromChannel() {
    console.log('\n📥 Загружаю все существующие видео из канала...\n');
    
    let offset = 0;
    let totalProcessed = 0;
    let videosFound = 0;
    
    // Функция для выполнения HTTP запроса
    function makeRequest(url) {
        return new Promise((resolve, reject) => {
            const urlObj = new URL(url);
            const options = {
                hostname: urlObj.hostname,
                path: urlObj.pathname + urlObj.search,
                method: 'GET'
            };
            
            const req = https.request(options, (res) => {
                let data = '';
                res.on('data', (chunk) => { data += chunk; });
                res.on('end', () => {
                    try {
                        resolve(JSON.parse(data));
                    } catch (e) {
                        reject(e);
                    }
                });
            });
            
            req.on('error', reject);
            req.end();
        });
    }
    
    try {
        while (true) {
            // ИСПРАВЛЕНО: правильный формат allowed_updates как JSON массив
            const allowedUpdates = encodeURIComponent(JSON.stringify(['channel_post']));
            const url = `https://api.telegram.org/bot${BOT_TOKEN}/getUpdates?offset=${offset}&limit=100&allowed_updates=${allowedUpdates}`;
            const data = await makeRequest(url);
            
            if (!data.ok || !data.result || data.result.length === 0) {
                console.log('Больше обновлений нет');
                break;
            }
            
            const updates = data.result;
            console.log(`Получено обновлений: ${updates.length}, offset: ${offset}`);
            
            for (const update of updates) {
                if (update.channel_post) {
                    const post = update.channel_post;
                    const chatId = post.chat?.id;
                    
                    // Обрабатываем ВСЕ видео из ЛЮБОГО канала
                    if (post.video) {
                        videosFound++;
                        console.log(`\n📨 Найдено видео #${videosFound}:`);
                        console.log(`   Message ID: ${post.message_id}`);
                        console.log(`   Chat ID: ${chatId}`);
                        console.log(`   Chat Title: ${post.chat?.title || 'N/A'}`);
                        await processVideo(post, chatId);
                    }
                }
                
                offset = update.update_id + 1;
                totalProcessed++;
            }
            
            // Если получили меньше 100 обновлений, значит это последняя партия
            if (updates.length < 100) {
                break;
            }
            
            // Небольшая задержка, чтобы не перегружать API
            await new Promise(resolve => setTimeout(resolve, 100));
        }
        
        console.log(`\n✅ Загрузка завершена:`);
        console.log(`   Всего обработано обновлений: ${totalProcessed}`);
        console.log(`   Найдено видео: ${videosFound}`);
        console.log(`\n⏳ Теперь слушаю канал постоянно... (для остановки нажмите Ctrl+C)\n`);
    } catch (error) {
        console.error('❌ Ошибка при загрузке видео:', error.message);
        console.log('\n⏳ Продолжаю слушать канал...\n');
    }
}

// Обработчик для пересланных сообщений (когда пользователь пересылает из канала в бот)
bot.on('message', async (ctx) => {
    try {
        const message = ctx.message;
        
        // ИСПРАВЛЕНО: правильное сравнение для пересланных сообщений
        const targetChannelId = CHANNEL_ID.startsWith('-') 
            ? parseInt(CHANNEL_ID) 
            : parseInt('-' + CHANNEL_ID);
        const forwardedChatId = message.forward_from_chat?.id 
            ? parseInt(message.forward_from_chat.id.toString()) 
            : null;
        
        // Проверяем, что это пересланное сообщение из канала
        //if (message.forward_from_chat && forwardedChatId === targetChannelId) {
            console.log('\n📨 Получено пересланное сообщение из канала:');
            console.log(`   Message ID: ${message.message_id}`);
            console.log(`   Forwarded from: ${message.forward_from_chat.title}`);
            console.log(`   Тип: ${message.video ? 'Видео' : message.photo ? 'Фото' : message.document ? 'Документ' : 'Текст'}`);
            
            if (message.video) {
                // Создаем объект, похожий на channel_post для processVideo
                const fakePost = {
                    video: message.video,
                    message_id: message.message_id,
                    caption: message.caption
                };
                await processVideo(fakePost, message.forward_from_chat.id);
            }
       // }
    } catch (error) {
        // Игнорируем ошибки для обычных сообщений
    }
});

// Функция для пересылки в личный чат (резервный вариант)
async function forwardToPersonalChat(ctx, chatId, messageId) {
    try {
        await ctx.telegram.forwardMessage(
            YOUR_USER_ID,
            chatId,
            messageId
        );
        console.log('✅ Сообщение переслано в ваш личный чат (резервный вариант)');
        console.log('   📝 Переслайте его боту @NestVesionVideoDownloaderBot для обработки\n');
    } catch (error) {
        console.error('❌ Ошибка при пересылке в личный чат:', error.message);
        if (error.message.includes('chat not found')) {
            console.log('   ⚠️  Проверьте, что USER_ID указан правильно\n');
        }
    }
}

// Обработчик для сообщений из канала (обрабатывает ВСЕ каналы)
bot.on('channel_post', async (ctx) => {
    try {
        const post = ctx.channelPost;
        const chatId = ctx.chat?.id;
        
        console.log(`\n📨 Получено сообщение из канала:`);
        console.log(`   Chat ID: ${chatId}`);
        console.log(`   Chat Title: ${ctx.chat?.title || 'N/A'}`);
        console.log(`   Message ID: ${post.message_id}`);
        console.log(`   Тип: ${post.video ? 'Видео' : post.photo ? 'Фото' : post.document ? 'Документ' : 'Текст'}`);
        
        // Обрабатываем ВСЕ сообщения с видео из ЛЮБОГО канала
        if (post.video) {
            console.log(`   ✅ Обрабатываем видео из канала ${chatId}`);
            await processVideo(post, chatId);
        } else {
            // Если не видео, просто пересылаем в личный чат
            forwardToPersonalChat(ctx, chatId, post.message_id);
        }
        
    } catch (error) {
        console.error('❌ Ошибка в обработчике channel_post:', error.message);
        console.error('   Stack:', error.stack);
    }
});

// ИСПРАВЛЕНО: Запускаем бота с явным указанием типов обновлений
// Сначала регистрируем обработчики, потом запускаем бота, потом загружаем историю
bot.launch({
    allowedUpdates: ['channel_post', 'message'], // Явно указываем, что хотим получать channel_post
    dropPendingUpdates: false // Не пропускаем обновления при старте
})
    .then(async () => {
        console.log('✅ Слушатель запущен и работает!');
        console.log('📨 Все новые сообщения из канала будут автоматически обработаны');
        console.log('📡 Слушаю обновления типа: channel_post, message');
        console.log('⏳ Загружаю историю канала...\n');
        
        // Загружаем все существующие видео ПОСЛЕ запуска бота
        await loadAllVideosFromChannel();
    })
    .catch((error) => {
        console.error('❌ Ошибка при запуске:', error);
        console.error('   Детали:', error.message);
        console.log('\n💡 Проверьте:');
        console.log('   1. Токен бота указан правильно');
        console.log('   2. Бот добавлен в канал как администратор');
        console.log('   3. У бота есть право "Читать сообщения"');
        console.log('   4. Интернет соединение работает');
        console.log('   5. Telegram API доступен');
        process.exit(1);
    });

// Graceful shutdown
process.once('SIGINT', () => {
    console.log('\n\n🛑 Остановка слушателя...');
    bot.stop('SIGINT');
    setTimeout(() => process.exit(0), 1000);
});

process.once('SIGTERM', () => {
    console.log('\n\n🛑 Остановка слушателя...');
    bot.stop('SIGTERM');
    setTimeout(() => process.exit(0), 1000);
});
