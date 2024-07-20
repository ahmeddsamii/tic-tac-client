package tic.toe.game;


import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import screens.LoginScreen;



public class TicToeMain extends Application {    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = new LoginScreen();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
        
    }
    
}
