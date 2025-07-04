import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Main {

    private static final String API_KEY = "5eaca587fd2cb84d1686dcdd7de05039";
    private static final Map<String, WeatherData> savedWeather = new HashMap<>();

    static class WeatherData {
        String city;
        String description;
        double temperature;
        double feelsLike;
        int humidity;
        double windSpeed;

        public WeatherData(String city, String description, double temperature,
                           double feelsLike, int humidity, double windSpeed) {
            this.city = city;
            this.description = description;
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
        }

        public String toFormattedString() {
            return String.format(
                    "🌆 Погода в %s: %s\n🌡 Температура: %.1f°C\n🥶 Ощущается как: %.1f°C\n💧 Влажность: %d%%\n💨 Ветер: %.1f м/с",
                    city, description, temperature, feelsLike, humidity, windSpeed
            );
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Меню ===");
            System.out.println("1. Узнать погоду");
            System.out.println("2. Показать сохранённые города");
            System.out.println("3. Удалить город из истории");
            System.out.println("4. Выйти");
            System.out.print("Выберите действие: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> fetchAndDisplayWeather(scanner);
                case "2" -> showSaved();
                case "3" -> deleteCity(scanner);
                case "4" -> {
                    System.out.println("Выход из программы.");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    private static void fetchAndDisplayWeather(Scanner scanner) {
        System.out.println("Введите город (или нажмите Enter для выбора популярных): ");
        String city = scanner.nextLine().trim();

        if (city.isEmpty()) {
            System.out.println("Популярные города России:");
            List<String> popularCities = List.of("Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань");
            for (int i = 0; i < popularCities.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, popularCities.get(i));
            }
            System.out.print("Выберите номер города: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= popularCities.size()) {
                    city = popularCities.get(choice - 1);
                } else {
                    System.out.println("❌ Неверный выбор.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Неверный ввод.");
                return;
            }
        }

        WeatherData data = fetchWeather(city);
        if (data != null) {
            savedWeather.put(city.toLowerCase(), data);
            System.out.println(data.toFormattedString());
        } else {
            System.out.println("❌ Ошибка: Город не найден или данные не получены.");
        }
    }

    private static WeatherData fetchWeather(String city) {
        try {
            String encodedCity = city.replace(" ", "%20");
            String urlStr = String.format(
                    "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=ru",
                    encodedCity, API_KEY
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) return null;

            InputStream input = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            reader.close();

            return parseJsonManually(json.toString(), city);

        } catch (Exception e) {
            System.out.println("⚠ Ошибка подключения: " + e.getMessage());
            return null;
        }
    }

    private static WeatherData parseJsonManually(String json, String city) {
        try {
            String description = extractValue(json, "\"description\":\"", "\"");
            String tempStr = extractValue(json, "\"temp\":", ",}");
            String feelsLikeStr = extractValue(json, "\"feels_like\":", ",}");
            String humidityStr = extractValue(json, "\"humidity\":", ",}");
            String windSpeedStr = extractValue(json, "\"speed\":", ",}");

            double temp = Double.parseDouble(tempStr);
            double feelsLike = Double.parseDouble(feelsLikeStr);
            int humidity = Integer.parseInt(humidityStr);
            double windSpeed = Double.parseDouble(windSpeedStr);

            return new WeatherData(
                    capitalize(city),
                    capitalize(description),
                    temp,
                    feelsLike,
                    humidity,
                    windSpeed
            );

        } catch (Exception e) {
            System.out.println("⚠ Ошибка парсинга JSON: " + e.getMessage());
            return null;
        }
    }

    private static String extractValue(String json, String key, String endChars) {
        int start = json.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = json.length();
        for (char c : endChars.toCharArray()) {
            int tempEnd = json.indexOf(c, start);
            if (tempEnd != -1 && tempEnd < end) {
                end = tempEnd;
            }
        }
        return json.substring(start, end).replaceAll("[^\\d.-]", "");
    }

    private static void showSaved() {
        if (savedWeather.isEmpty()) {
            System.out.println("Нет сохранённых данных.");
        } else {
            for (WeatherData data : savedWeather.values()) {
                System.out.println("\n" + data.toFormattedString());
            }
        }
    }

    private static void deleteCity(Scanner scanner) {
        System.out.print("Введите город для удаления: ");
        String city = scanner.nextLine().trim().toLowerCase();
        if (savedWeather.remove(city) != null) {
            System.out.println("✅ Успешно удалено.");
        } else {
            System.out.println("❌ Город не найден.");
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
