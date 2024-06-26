package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Класс предоставляет методы для взаимодействия с API Честного знака.
 * Поддерживает ограничение на количество запросов в заданное время.
 */
public class CrptApi {

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final BlockingQueue<Long> requestTimes;
    private final int requestLimit;
    private final Duration duration;

    /**
     * @param timeUnit     Единица измерения времени для ограничения запросов (секунды, минуты и т.д.).
     * @param requestLimit Максимальное количество запросов в заданном временном интервале.
     * @throws IllegalArgumentException если requestLimit меньше или равен нулю, либо неподдерживаемый TimeUnit.
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Invalid requestLimit, must be a positive number");
        }

        this.requestTimes = new LinkedBlockingQueue<>(requestLimit);
        this.requestLimit = requestLimit;
        this.duration = switch (timeUnit) {
            case SECONDS -> Duration.ofSeconds(1);
            case MINUTES -> Duration.ofMinutes(1);
            case HOURS -> Duration.ofHours(1);
            default -> throw new IllegalArgumentException("Unsupported TimeUnit: " + timeUnit);
        };
    }

    /**
     * Метод createIntroduceGoodsDocument выполняет запрос к API для создания документа ввода в оборот товара, произведенного в РФ.
     *
     * @param request Данные документа в виде объекта CreateGoodsDocumentRequest.
     * @throws InterruptedException если поток был прерван во время ожидания доступа к API.
     */
    public void createIntroduceGoodsDocument(CreateGoodsDocumentRequest request) throws InterruptedException {
        long now = System.nanoTime();
        long startWindow = now - duration.toNanos();

        // Очистка старых запросов
        requestTimes.removeIf(time -> time < startWindow);

        // Блокировка при достижении лимита запросов
        while (requestTimes.size() >= requestLimit) {
            TimeUnit.MILLISECONDS.sleep(100); // Установка времени сна
            requestTimes.removeIf(time -> time < startWindow); // Повторная проверка и очистка старых запросов
        }

        // JSON сериализация
        String documentJson;
        try {
            documentJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Выполнение HTTP POST запроса
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(documentJson))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            requestTimes.offer(System.nanoTime());
        }
    }

    /**
     * Класс CreateGoodsDocumentRequest представляет данные для создания документа ввода в оборот товара, произведенного в РФ.
     */
    public static class CreateGoodsDocumentRequest {
        private String description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;
    }

    /**
     * Класс Product представляет данные о продукте для документа ввода в оборот товара, произведенного в РФ.
     */
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
