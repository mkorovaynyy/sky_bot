package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    private final NotificationTaskRepository repository;

    private static final Pattern TASK_PATTERN =
            Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4} \\d{1,2}:\\d{2})\\s+(.+)");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    public TelegramBotUpdatesListener(
            TelegramBot telegramBot,
            NotificationTaskRepository repository
    ) {
        this.telegramBot = telegramBot;
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() != null && update.message().text() != null) {
                String text = update.message().text();
                Long chatId = update.message().chat().id();

                if ("/start".equalsIgnoreCase(text)) {
                    sendResponse(chatId, "👋 Привет! Я бот-напоминалка.\n\n" +
                            "📝 Отправь задачу в формате:\n" +
                            "01.01.2025 12:00 Сделать домашку");
                } else {
                    Matcher matcher = TASK_PATTERN.matcher(text);
                    if (matcher.matches()) {
                        processTask(matcher, chatId);
                    } else {
                        sendResponse(chatId, "❌ Неверный формат сообщения.\n\n" +
                                "✅ Используй:\n" +
                                "<b>01.01.2025 12:00 Текст напоминания</b>\n\n" +
                                "Где:\n" +
                                "• 01.01.2025 - дата\n" +
                                "• 12:00 - время\n" +
                                "• Текст напоминания - ваше сообщение", true);
                    }
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processTask(Matcher matcher, Long chatId) {
        String dateTimeStr = matcher.group(1);
        String taskText = matcher.group(2);

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, FORMATTER);

            // Проверка что дата в будущем
            if (dateTime.isBefore(LocalDateTime.now())) {
                sendResponse(chatId, "⏰ Ошибка: указанная дата уже прошла");
                return;
            }

            NotificationTask task = new NotificationTask();
            task.setChatId(chatId);
            task.setMessage(taskText);
            task.setNotificationDateTime(dateTime);
            repository.save(task);

            sendResponse(chatId, "✅ <b>Напоминание создано!</b>\n\n" +
                    "📝 Текст: " + taskText + "\n" +
                    "⏰ Дата: " + dateTimeStr, true);
        } catch (DateTimeParseException e) {
            logger.error("Ошибка парсинга даты: {}", dateTimeStr, e);
            sendResponse(chatId, "❌ Ошибка формата даты.\n\n" +
                    "✅ Используй формат: <b>дд.мм.гггг чч:мм</b>\n" +
                    "Например: 01.01.2025 12:00", true);
        }
    }

    private void sendResponse(Long chatId, String text) {
        sendResponse(chatId, text, false);
    }

    private void sendResponse(Long chatId, String text, boolean html) {
        try {
            SendMessage message = new SendMessage(chatId, text);
            if (html) {
                message.parseMode(com.pengrad.telegrambot.model.request.ParseMode.HTML);
            }
            telegramBot.execute(message);
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в чат {}", chatId, e);
        }
    }
}