-- Создание базы данных, если не существует
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'notification_db') THEN
    CREATE DATABASE notification_db;
END IF;
END $$;

-- Переключение на созданную БД
\c notification_db

-- Создание пользователя, если не существует
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mkorovaynyy') THEN
    CREATE USER mkorovaynyy WITH PASSWORD 'qwerty123';
END IF;
END $$;

-- Выдача прав пользователю
GRANT ALL PRIVILEGES ON DATABASE notification_db TO mkorovaynyy;

-- Создание таблицы (для случая, если Liquibase не используется)
CREATE TABLE IF NOT EXISTS notification_task (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 chat_id BIGINT NOT NULL,
                                                 message TEXT NOT NULL,
                                                 notification_date_time TIMESTAMP NOT NULL
);

-- Выдача прав на таблицу
GRANT ALL PRIVILEGES ON TABLE notification_task TO mkorovaynyy;
GRANT USAGE, SELECT ON SEQUENCE notification_task_id_seq TO mkorovaynyy;