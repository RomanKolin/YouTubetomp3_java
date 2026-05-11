module com.example.youtubetomp3
{
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.youtubetomp3 to javafx.fxml;
    exports com.example.youtubetomp3;
}