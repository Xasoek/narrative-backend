import json
import urllib.request

BASE_URL = "http://localhost:8082/projects/d101059d-ec15-429a-a29d-fcd0ee47cefc/narrative/nodes"
LAYER_ID = "d101059d-ec15-429a-a29d-fcd0ee47cef2"

topics = [
    "Docker",
    "PostgreSQL",
    "Spring Security",
    "JWT Authentication",
    "Apache Kafka",
    "Redis",
    "Microservices",
    "OpenSearch",
    "CI/CD",
    "Kubernetes",
]

base_sentences = {
    "Docker": [
        "Docker позволяет запускать приложения внутри изолированных контейнеров.",
        "Контейнер содержит приложение и все необходимые зависимости.",
        "Docker image используется как шаблон для создания контейнера.",
        "Dockerfile описывает процесс сборки image.",
        "Docker Compose позволяет запускать несколько сервисов вместе.",
        "Volumes используются для сохранения данных.",
        "Docker network позволяет контейнерам общаться друг с другом.",
        "Контейнеризация упрощает deployment приложений.",
    ],

    "PostgreSQL": [
        "PostgreSQL является реляционной системой управления базами данных.",
        "Данные хранятся в таблицах и строках.",
        "SQL используется для чтения и изменения данных.",
        "Primary key уникально идентифицирует запись.",
        "Foreign key связывает таблицы между собой.",
        "Index может ускорять выполнение запросов.",
        "Transaction позволяет выполнять несколько операций атомарно.",
        "PostgreSQL поддерживает тип JSONB.",
    ],

    "Spring Security": [
        "Spring Security отвечает за безопасность backend приложения.",
        "Authentication определяет личность пользователя.",
        "Authorization определяет права пользователя.",
        "Security Filter обрабатывает HTTP запрос до Controller.",
        "BCrypt используется для безопасного хранения паролей.",
        "JWT часто применяется для stateless authentication.",
        "Spring Security может ограничивать доступ по ролям.",
        "SecurityContext содержит информацию об authenticated user.",
    ],

    "JWT Authentication": [
        "JWT используется для передачи информации об авторизованном пользователе.",
        "Access token обычно имеет короткий срок жизни.",
        "Refresh token используется для получения нового access token.",
        "JWT содержит header payload и signature.",
        "Signature позволяет проверить целостность токена.",
        "Клиент отправляет JWT через Authorization header.",
        "Bearer token часто используется в REST API.",
        "Expired token не должен давать доступ к защищенным ресурсам.",
    ],

    "Apache Kafka": [
        "Apache Kafka используется для передачи событий между сервисами.",
        "Producer отправляет сообщения в topic.",
        "Consumer читает сообщения из topic.",
        "Topic может быть разделен на partitions.",
        "Kafka хорошо подходит для больших потоков данных.",
        "Consumer group позволяет распределять обработку сообщений.",
        "Kafka часто используется в event driven architecture.",
        "Broker хранит сообщения Kafka.",
    ],

    "Redis": [
        "Redis является быстрым in-memory хранилищем.",
        "Redis часто используется как cache.",
        "Cache уменьшает количество запросов к основной базе данных.",
        "Redis поддерживает expiration для ключей.",
        "Redis может хранить строки списки множества и hash структуры.",
        "Redis используется для rate limiting.",
        "Redis может использоваться для хранения временных session данных.",
        "In-memory операции обычно выполняются очень быстро.",
    ],

    "Microservices": [
        "Microservice architecture разделяет приложение на небольшие сервисы.",
        "Каждый сервис отвечает за отдельную бизнес область.",
        "Сервисы могут общаться через HTTP.",
        "Асинхронная коммуникация может выполняться через Kafka.",
        "Каждый микросервис может иметь собственную базу данных.",
        "Microservices позволяют независимо масштабировать части системы.",
        "Distributed systems требуют обработки сетевых ошибок.",
        "Monitoring особенно важен для микросервисной архитектуры.",
    ],

    "OpenSearch": [
        "OpenSearch используется для полнотекстового поиска.",
        "Документы сохраняются внутри индексов.",
        "Text поля анализируются перед поиском.",
        "Keyword поля подходят для точного сравнения.",
        "Multi match позволяет искать сразу по нескольким полям.",
        "Bool query объединяет несколько условий поиска.",
        "Term query используется для точного значения.",
        "OpenSearch построен поверх Apache Lucene.",
    ],

    "CI/CD": [
        "Continuous Integration автоматически проверяет изменения кода.",
        "Pipeline может запускать unit tests.",
        "Pipeline может выполнять сборку приложения.",
        "Docker image можно создавать автоматически.",
        "Continuous Deployment автоматизирует доставку приложения.",
        "GitLab CI используется для автоматизации pipeline.",
        "Jenkins также используется для CI CD.",
        "Автоматизация уменьшает количество ручных deployment операций.",
    ],

    "Kubernetes": [
        "Kubernetes используется для оркестрации контейнеров.",
        "Pod является базовой единицей запуска приложения.",
        "Deployment управляет replica экземплярами приложения.",
        "Service предоставляет стабильный сетевой доступ к pod.",
        "Kubernetes может автоматически перезапускать упавшие контейнеры.",
        "ConfigMap используется для конфигурации приложения.",
        "Secret используется для чувствительных конфигурационных данных.",
        "Kubernetes поддерживает rolling update.",
    ],
}


def build_text(topic):
    sentences = base_sentences[topic]
    parts = []

    for i in range(240):
        sentence = sentences[i % len(sentences)]
        parts.append(
            f"{sentence} Этот текст относится к теме {topic} и используется для проверки полнотекстового поиска."
        )

    return " ".join(parts)


def create_node(topic, index):
    data = {
        "layerId": LAYER_ID,
        "title": topic,
        "content": {
            "text": build_text(topic)
        },
        "position": {
            "x": index * 150,
            "y": 100
        },
        "linkedNodeIds": []
    }

    body = json.dumps(data).encode("utf-8")

    request = urllib.request.Request(
        BASE_URL,
        data=body,
        headers={
            "Content-Type": "application/json"
        },
        method="POST"
    )

    with urllib.request.urlopen(request) as response:
        print(f"{topic}: {response.status}")


for index, topic in enumerate(topics, start=1):
    create_node(topic, index)