package org.example;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        try {
            // Создаем экземпляр CrptApi с ограничением 10 запросов в секунду
            CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);

            // Вызываем метод createIntroduceGoodsDocument для создания документа
            api.createIntroduceGoodsDocument(new CrptApi.CreateGoodsDocumentRequest());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
