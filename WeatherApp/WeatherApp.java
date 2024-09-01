import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application class for the WeatherApp.
 * This application fetches and displays weather information using the WeatherAPI.
 */
public class WeatherApp extends Application {

    /**
     * API key for WeatherAPI.
     */
    private static final String API_KEY = "fbcfbbd97e6f42f09f984240241108";

    /**
     * URL for the current weather API endpoint.
     */
    private static final String WEATHER_URL =
            "https://api.weatherapi.com/v1/current.json";

    /**
     * URL for the weather forecast API endpoint.
     */
    private static final String FORECAST_URL =
            "https://api.weatherapi.com/v1/forecast.json";

    /**
     * Logger for the application.
     */
    private static final Logger LOGGER =
            Logger.getLogger(WeatherApp.class.getName());

    // Background images for different times of the day.
    private Background morningBg;
    private Background afternoonBg;
    private Background eveningBg;
    private Background noonBg;

    // UI components for selecting units.
    private final ComboBox<String> windSpeedUnitComboBox = new ComboBox<>();
    private static final ComboBox<String> temperatureUnitComboBox =
            new ComboBox<>();

    // ObservableList and ListView to display search history.
    private final ObservableList<SearchHistory> searchHistoryList =
            FXCollections.observableArrayList();
    private final ListView<SearchHistory> historyListView =
            new ListView<>(searchHistoryList);

    /**
     * Main method to launch the JavaFX application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {

        launch(args);
    }

    /**
     * Class representing weather data fetched from the API.
     */
    public static class WeatherData {
        private final double temperature;
        private final double humidity;
        private final double windSpeed;
        private final String weatherIcon;
        private String date;
        private String condition;

        /**
         * Constructs a WeatherData object.
         *
         * @param temperature the temperature value
         * @param humidity the humidity percentage
         * @param windSpeed the wind speed value
         * @param weatherIcon the icon URL for the weather condition
         */
        public WeatherData(double temperature, double humidity,
                           double windSpeed, String weatherIcon) {

            this.temperature = temperature;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.weatherIcon = weatherIcon;
        }

        @Override
        public String toString() {

            return "Date: " + date + " - Temperature: " + temperature +
                    getTemperatureSymbol() + " - Condition: " + condition;
        }

        // Getter and Setter methods for the WeatherData fields.
        public double getTemperature() {

            return temperature;
        }

        public double getHumidity() {

            return humidity;
        }

        public double getWindSpeed() {

            return windSpeed;
        }

        public String getWeatherIconURL() {

            return "https:" + weatherIcon;
        }

        public void setDate(String date) {

            this.date = date;
        }

        public void setCondition(String condition) {

            this.condition = condition;
        }
    }

    /**
     * Converts the unit type selected by the user to the appropriate API query
     * parameter.
     *
     * @param unitType the type of unit to convert (e.g., "temp", "wind")
     * @return the corresponding API query parameter
     */
    private String convertUnit(String unitType) {

        if (unitType.equalsIgnoreCase("temp")) {
            String unit = temperatureUnitComboBox.getValue();
            return unit.equalsIgnoreCase("celsius") ? "temp_c" : "temp_f";
        } else if (unitType.equalsIgnoreCase("wind")) {
            String unit = windSpeedUnitComboBox.getValue();
            return unit.equalsIgnoreCase("kph") ? "wind_kph" : "wind_mph";
        }
        return unitType;
    }

    /**
     * Retrieves the temperature symbol based on the selected unit.
     *
     * @return the temperature symbol (°C or °F)
     */
    private static String getTemperatureSymbol() {

        String unit = temperatureUnitComboBox.getValue();
        return unit.equalsIgnoreCase("celsius") ? "°C" : "°F";
    }

    /**
     * Starts the JavaFX application and initializes the UI components.
     *
     * @param primaryStage the primary stage for the application
     */
    @Override
    public void start(Stage primaryStage) {

        BackgroundFill bgFill =
                new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY);
        Background bg = new Background(bgFill);
        ImageView weatherIcon = new ImageView();

        Label temperatureLabel = new Label();
        Label humidityLabel = new Label();
        Label windLabel = new Label();
        Label historyLabel = new Label("Search History:");
        Label forecastLabel = new Label("Short-term Forecast:");

        VBox weatherInfoBox = new VBox();
        HBox SearchAreaBox = new HBox(5);
        HBox UnitsAreaBox = new HBox(5);
        ListView<String> forecastListView = new ListView<>();
        TextField locationInput = new TextField();
        Button searchButton = new Button();
        searchButton.setTooltip(new Tooltip("Search Weather"));
        locationInput.setTooltip(new Tooltip("Enter location..."));

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        primaryStage.setTitle("Weather Information App");

        // Load images
        morningBg = createBackground("bg/morning.jpg");
        afternoonBg = createBackground("bg/afternoon.jpg");
        eveningBg = createBackground("bg/evening.jpg");
        noonBg = createBackground("bg/noon.jpg");

        locationInput.setEditable(true);
        locationInput.setPrefWidth(200);
        locationInput.setMaxWidth(400);
        SearchAreaBox.getChildren().add(locationInput);
        SearchAreaBox.getChildren().add(searchButton);
        SearchAreaBox.setAlignment(Pos.CENTER);

        temperatureUnitComboBox.getItems().addAll("Celsius", "Fahrenheit");
        temperatureUnitComboBox.setValue("Celsius");
        windSpeedUnitComboBox.getItems().addAll("Kph", "Mph");
        windSpeedUnitComboBox.setValue("Kph");

        UnitsAreaBox.getChildren().add(temperatureUnitComboBox);
        UnitsAreaBox.getChildren().add(windSpeedUnitComboBox);
        UnitsAreaBox.setAlignment(Pos.CENTER);

        Font font = Font.font
                ("Calibri", FontWeight.BOLD, FontPosture.REGULAR, 14);
        historyLabel.setFont(font);
        historyLabel.setBackground(bg);
        forecastLabel.setFont(font);
        forecastLabel.setBackground(bg);


        weatherInfoBox.setAlignment(Pos.CENTER);
        ImageView searchIcon = new ImageView("searchIcon.png");
        searchIcon.setFitHeight(20);
        searchIcon.setPreserveRatio(true);

        searchButton.setGraphic(searchIcon);
        searchButton.setMaxSize(9, 9);
        searchButton.setOnAction(e -> {
            weatherInfoBox.getChildren().clear();

            String location = locationInput.getText();

            if (!isLocationValid(location)) {
                showError(weatherInfoBox, "Invalid location. " +
                        "Please enter a valid location name.");
                return;
            }

            WeatherData weatherData = getWeatherData(location);
            if (weatherData != null) {
                updateWeatherDisplay(weatherData, temperatureLabel,
                        humidityLabel, windLabel, weatherIcon);
                updateForecastDisplay(location, forecastListView);
            } else {
                showError(weatherInfoBox, "Could not fetch weather data.");
            }

            addToHistory(location, weatherData);
        });

        forecastListView.setPrefWidth(300);
        forecastListView.setMaxWidth(600);

        historyListView.setPrefWidth(300);
        historyListView.setMaxWidth(600);

        VBox forecastBox = new VBox(10);
        forecastBox.setAlignment(Pos.CENTER);
        forecastBox.getChildren().addAll(forecastListView);

        VBox weatherPanel = new VBox(10);
        weatherPanel.setAlignment(Pos.CENTER);
        weatherPanel.setPadding(new Insets(20));
        weatherPanel.setMaxWidth(600);
        weatherPanel.setBackground(new Background(new BackgroundFill(Color.rgb
                (255, 255, 255, 0.7),
                CornerRadii.EMPTY, Insets.EMPTY)));
        weatherPanel.getChildren().addAll(weatherInfoBox, weatherIcon,
                progressIndicator, temperatureLabel, humidityLabel, windLabel);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setMinWidth(500);
        layout.setMaxWidth(700);
        layout.setMaxHeight(600);
        layout.setBackground(getBackgroundForCurrentTime());

        layout.getChildren().addAll(SearchAreaBox, UnitsAreaBox, weatherPanel,
                forecastLabel, forecastBox, historyLabel, historyListView);

        Scene scene = new Scene(layout);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Validates the location entered by the user by sending a request to the
     * WeatherAPI.
     *
     * @param location the location to validate
     * @return true if the location is valid, false otherwise
     */
    private boolean isLocationValid(String location) {

        try {
            String formattedLocation = location.replace(" ", "_");
            String apiUrl = WEATHER_URL + "?key=" + API_KEY + "&q=" +
                    formattedLocation + "&days=1";
            HttpURLConnection connection = createConnection(apiUrl);

            if (connection.getResponseCode() == 200) {
                JSONObject jsonObject = new JSONObject(readResponse(connection));
                return !jsonObject.has("error");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating location", e);
        }
        return false;
    }

    /**
     * Updates the weather display with the given WeatherData.
     *
     * @param weatherData the weather data to display
     * @param temperatureLabel the label to display the temperature
     * @param humidityLabel the label to display the humidity
     * @param windLabel the label to display the wind speed
     * @param weatherIcon the ImageView to display the weather icon
     */
    private void updateWeatherDisplay(WeatherData weatherData,
                                      Label temperatureLabel,
                                      Label humidityLabel, Label windLabel,
                                      ImageView weatherIcon) {

        temperatureLabel.setText("Temperature: " + weatherData.getTemperature()
                + getTemperatureSymbol());
        humidityLabel.setText("Humidity: " + weatherData.getHumidity() + "%");
        windLabel.setText("Wind Speed: " + weatherData.getWindSpeed() + " "
                + windSpeedUnitComboBox.getValue());
        weatherIcon.setImage(new Image(weatherData.getWeatherIconURL()));
    }

    /**
     * Updates the forecast display with the hourly forecast for the specified
     * location.
     *
     * @param location the location to fetch the forecast for
     * @param forecastListView the ListView to display the forecast
     */
    private void updateForecastDisplay(String location,
                                       ListView<String> forecastListView) {

        ObservableList<WeatherData> forecast = fetchHourlyForecast(location);
        forecastListView.getItems().clear();
        if (!forecast.isEmpty()) {
            forecast.forEach(forecastData -> forecastListView.getItems().add
                    (forecastData.toString()));
        } else {
            forecastListView.getItems().add("Could not fetch forecast data.");
        }
    }

    /**
     * Displays an error message in the given VBox.
     *
     * @param weatherInfoBox the VBox to display the error message in
     * @param message the error message to display
     */
    private void showError(VBox weatherInfoBox, String message) {

        weatherInfoBox.getChildren().clear();
        weatherInfoBox.getChildren().add(new Label(message));
    }

    /**
     * Adds the specified location and weather data to the search history.
     *
     * @param location the location to add to the history
     * @param weatherData the weather data associated with the location
     */
    private void addToHistory(String location, WeatherData weatherData) {

        SearchHistory entry = new SearchHistory(location, weatherData);
        searchHistoryList.add(entry);
    }

    /**
     * Retrieves the current weather data for the specified location.
     *
     * @param location the location to retrieve weather data for
     * @return a WeatherData object containing the weather information
     */
    private WeatherData getWeatherData(String location) {

        try {
            String formattedlocation = location.replace(" ", "_");
            String apiUrl = WEATHER_URL + "?key=" + API_KEY + "&q="
                    + formattedlocation;
            HttpURLConnection connection = createConnection(apiUrl);

            if (connection.getResponseCode() == 200) {
                JSONObject current = new JSONObject(readResponse(connection))
                        .getJSONObject("current");

                return new WeatherData(
                        current.getDouble(convertUnit("temp")),
                        current.getDouble("humidity"),
                        current.getDouble(convertUnit("wind")),
                        current.getJSONObject("condition").getString("icon"));
            } else {
                LOGGER.log(Level.WARNING, "Failed to fetch weather data:" +
                        " Response code " + connection.getResponseCode());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching weather data", e);
        }
        return null;
    }

    /**
     * Fetches the hourly weather forecast for the specified location.
     *
     * @param location the location to fetch the forecast for
     * @return an ObservableList of WeatherData representing the hourly forecast
     */
    private ObservableList<WeatherData> fetchHourlyForecast(String location) {

        try {
            String formattedLocation = location.replace(" ", "_");
            String apiUrl = FORECAST_URL + "?key=" + API_KEY + "&q="
                    + formattedLocation + "&days=1";
            HttpURLConnection connection = createConnection(apiUrl);

            if (connection.getResponseCode() == 200) {
                JSONArray hourlyForecast =
                        new JSONObject(readResponse(connection))
                        .getJSONObject("forecast")
                        .getJSONArray("forecastday")
                        .getJSONObject(0)
                        .getJSONArray("hour");

                ObservableList<WeatherData> forecast =
                        FXCollections.observableArrayList();

                for (int i = 0; i < hourlyForecast.length(); i++) {
                    JSONObject hourData = hourlyForecast.getJSONObject(i);
                    WeatherData forecastData = new WeatherData(
                            hourData.getDouble("temp_c"),
                            0.0,
                            0.0,
                            hourData.getJSONObject("condition")
                                    .getString("icon"));
                    forecastData.setDate(hourData.getString("time"));
                    forecastData.setCondition(hourData
                            .getJSONObject("condition").getString("text"));
                    forecast.add(forecastData);
                }
                return forecast;
            } else {
                LOGGER.log(Level.WARNING, "Failed to fetch forecast data:" +
                        " Response code " + connection.getResponseCode());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching forecast data", e);
        }
        return FXCollections.observableArrayList();
    }

    /**
     * Creates an HttpURLConnection for the given API URL.
     *
     * @param apiUrl the API URL to connect to
     * @return the created HttpURLConnection
     * @throws Exception if an error occurs while opening the connection
     */
    private HttpURLConnection createConnection(String apiUrl) throws Exception {

        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    /**
     * Reads the response from the given HttpURLConnection.
     *
     * @param connection the HttpURLConnection to read from
     * @return the response as a String
     * @throws Exception if an I/O error occurs while reading the response
     */
    private String readResponse(HttpURLConnection connection) throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader
                (connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    /**
     * Create background based from the image path.
     *
     * @param imagePath the path of the image
     * @return background image
     */
    private Background createBackground(String imagePath) {

        Image image = new Image(getClass().getResourceAsStream(imagePath));
        BackgroundImage backgroundImage = new BackgroundImage
                (image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
        return new Background(backgroundImage);
    }

    /**
     * Get the background for the current time.
     *
     * @return the corresponding background based on localTime
     */
    private Background getBackgroundForCurrentTime() {

        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(5, 0)) && now.isBefore
                (LocalTime.of(12, 0))) {
            return morningBg;
        } else if (now.isAfter(LocalTime.of(12, 0)) && now.isBefore
                (LocalTime.of(16, 0))) {
            return noonBg;
        } else if (now.isAfter(LocalTime.of(16, 0)) && now.isBefore
                (LocalTime.of(18, 0))) {
            return afternoonBg;
        } else {
            return eveningBg;
        }
    }

    /**
     * Class representing search history from recent searches of the user.
     */
    public static class SearchHistory {
        private final String location;
        private final String timestamp;
        private final WeatherData weatherData;

        /**
         * Constructs a SearchHistory object.
         *
         * @param location the location searched by user
         * @param weatherData the recent weather temperature
         */
        public SearchHistory(String location, WeatherData weatherData) {

            this.location = location;
            this.timestamp = java.time.LocalDateTime.now().toString();
            this.weatherData = weatherData;
        }

        @Override
        public String toString() {

            return timestamp + ": " + location + " - "
                    + weatherData.getTemperature() + getTemperatureSymbol();
        }
    }
}
