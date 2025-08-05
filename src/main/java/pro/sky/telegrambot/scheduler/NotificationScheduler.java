package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationTaskRepository repository;
    private final TelegramBot telegramBot;

    public NotificationScheduler(
            NotificationTaskRepository repository,
            TelegramBot telegramBot
    ) {
        this.repository = repository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkNotifications() {
        try {
            // Исправление: точное сравнение времени
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            processNotificationsForTime(now);
        } catch (Exception e) {
            logger.error("Ошибка в шедулере", e);
        }
    }

    private void processNotificationsForTime(LocalDateTime now) {
        logger.info("Проверка напоминаний для времени: {}", now);
        List<NotificationTask> tasks = repository.findByNotificationDateTime(now);
        logger.info("Найдено задач для отправки: {}", tasks.size());

        for (NotificationTask task : tasks) {
            try {
                sendNotification(task);
                repository.delete(task);
            } catch (Exception e) {
                handleNotificationError(task, now, e);
            }
        }
    }

    private void sendNotification(NotificationTask task) throws Exception {
        telegramBot.execute(new SendMessage(
                task.getChatId(),
                "🔔 <b>Напоминание!</b>\n\n" + task.getMessage()
        ).parseMode(com.pengrad.telegrambot.model.request.ParseMode.HTML));

        logger.info("Успешно отправлено напоминание для chatId: {}", task.getChatId());
    }

    private void handleNotificationError(NotificationTask task, LocalDateTime now, Exception e) {
        logger.error("Ошибка отправки напоминания chatId: {}", task.getChatId(), e);
        task.setNotificationDateTime(now.plusMinutes(1));
        repository.save(task);
        logger.info("Напоминание перенесено на следующую минуту для chatId: {}", task.getChatId());
    }
}
