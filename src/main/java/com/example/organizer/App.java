package com.example.organizer;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
public class App extends Application{
    @override
    public void start(Stage stage){
        Label label = new Label("Hello Organizer!");
        Scene scene = new Scene(label,400,200);
        stage.setScene(scene);
        stage.setTitle("TodoProgram");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
