package screens;

import DataAccessLayer.PlayerDTO;
import DataAccessLayer.RequestDTO;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import tic.toe.game.Mynav;

public class AvailabilityScreen extends AnchorPane implements Runnable {

    private final AnchorPane anchorPane;
    private final TableView<PlayerDTO> _tableView;
    private final TableColumn<PlayerDTO, String> col_playerName;
    private final TableColumn<PlayerDTO, String> col_available;
    private final Button _backBtn;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ArrayList<PlayerDTO> players;
    PlayerDTO pto;
    private String currentUsername;
    Button _refreshBtn;

    public AvailabilityScreen() {
        anchorPane = new AnchorPane();
        _tableView = new TableView<>();
        col_playerName = new TableColumn<>("Player name");
        col_available = new TableColumn<>("Available");
        _backBtn = new Button("Sign Out");
        players = new ArrayList<>();
        this.currentUsername = LoginScreen.pto.getUsername();
        _refreshBtn = new Button("Refresh");

        setId("AnchorPane");
        setPrefHeight(800.0);
        setPrefWidth(1100.0);
        setStyle("-fx-background-color: #008080;");

        anchorPane.setLayoutX(25.0);
        anchorPane.setLayoutY(20.0);
        anchorPane.setPrefHeight(760.0);
        anchorPane.setPrefWidth(1050.0);
        anchorPane.setStyle("-fx-background-color: #FFA500;");

        _tableView.setLayoutX(31.0);
        _tableView.setLayoutY(112.0);
        _tableView.setPrefHeight(634.0);
        _tableView.setPrefWidth(988.0);
        _tableView.setStyle("-fx-background-color: #008080;");

        col_playerName.setPrefWidth(494.0);
        col_playerName.setCellValueFactory(new PropertyValueFactory<>("username"));

        col_available.setPrefWidth(493.0);
        col_available.setCellValueFactory(new PropertyValueFactory<>("status"));

        _backBtn.setLayoutX(31.0);
        _backBtn.setLayoutY(28.0);
        _backBtn.setOnAction(this::handleOnBtnSignOut);
        _backBtn.setStyle("-fx-background-color: #008080; -fx-background-radius: 50;");
        _backBtn.setTextFill(javafx.scene.paint.Color.ORANGE);
        _backBtn.setFont(new Font("System Bold Italic", 21.0));
        
        
        _refreshBtn.setLayoutX(500.0);
        _refreshBtn.setLayoutY(28.0);
        _refreshBtn.setOnAction(this::handleOnRefreshBtn);
        _refreshBtn.setStyle("-fx-background-color: #008080; -fx-background-radius: 50;");
        _refreshBtn.setTextFill(javafx.scene.paint.Color.ORANGE);
        _refreshBtn.setFont(new Font("System Bold Italic", 21.0));

        _tableView.getColumns().addAll(col_playerName, col_available);
        anchorPane.getChildren().addAll(_tableView, _backBtn,_refreshBtn);
        getChildren().add(anchorPane);

        initializeConnection(LoginScreen.pto);

        setupTableCellFactory();
    }
    
   

    private void setupTableCellFactory() {
        col_playerName.setCellFactory(col -> {
            TableCell<PlayerDTO, String> cell = new TableCell<PlayerDTO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                    }
                }
            };
            // two clicks on the targeted player to send a rquest to 
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.getClickCount() == 2) {
                    PlayerDTO selectedPlayer = cell.getTableView().getItems().get(cell.getIndex());
                    if (selectedPlayer != null) {
                        sendRequestToServer(selectedPlayer.getUsername());
                    }
                }
            });

            return cell;
        });
    }
    
    

    private void sendRequestToServer(String selectedUsername) {
        try {
            RequestDTO rdo = new RequestDTO();
            rdo.setSender_username(LoginScreen.pto.getUsername());
            rdo.setReciver_username(selectedUsername);
            rdo.setScreenIndicator(5); // send rquest to play to the server  
            oos.writeObject(rdo);  
            oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(AvailabilityScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void handleOnBtnSignOut(javafx.event.ActionEvent actionEvent) {
        pto = new PlayerDTO();
        pto.setUsername(LoginScreen.pto.getUsername());
        pto.setScreenIndicator(4);  // send a request to the server to signOut 
        try {
            oos.writeObject(pto);
            oos.flush();
            players.remove(pto);
            Parent root = new LoginScreen();
            Mynav.navigateTo(root, actionEvent);
        } catch (IOException ex) {
            Logger.getLogger(AvailabilityScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    private void handleOnRefreshBtn(ActionEvent event) {
    try {
        // Create a new PlayerDTO object to send a refresh request
        PlayerDTO refreshRequest = new PlayerDTO();
        refreshRequest.setUsername(LoginScreen.pto.getUsername());
        refreshRequest.setScreenIndicator(3); // refresh online player list in the table in this screen 

        
        oos.writeObject(refreshRequest);
        oos.flush();

        // Clear the current list of players
        players.clear();

        // Update the TableView
        updateTableView();

        
        
        _refreshBtn.setText("Refreshing...");

        // Re-enable the button after a short delay until the data is restord 
        new Thread(() -> {
            try {
                Thread.sleep(2000); 
                Platform.runLater(() -> {
                    _refreshBtn.setDisable(false);
                    _refreshBtn.setText("Refresh");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

    } catch (IOException ex) {
        Logger.getLogger(AvailabilityScreen.class.getName()).log(Level.SEVERE, "Error refreshing player list", ex);
        showAlert("Failed to refresh the player list. Please try again.");
    }
}
    
    @Override
    public void run() {
        try {
            while (true) {
                Object obj = ois.readObject();
                System.out.println("Received object: " + obj.getClass().getName());

                if (obj instanceof PlayerDTO) {
                    PlayerDTO player = (PlayerDTO) obj;
                    Platform.runLater(() -> updatePlayerStatus(player));
                } else if (obj instanceof RequestDTO) {
                    RequestDTO request = (RequestDTO) obj;
                    Platform.runLater(() -> handleRequest(request));
                } else {
                    System.out.println("Received unexpected object type: " + obj.getClass().getName());
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Error in AvailabilityScreen run method: " + ex.getMessage());
            ex.printStackTrace();
            resetConnection();
        }
        
    }
    
        private void handleRequest(RequestDTO request) {
        System.out.println("Handling RequestDTO with screenIndicator: " + request.getScreenIndicator());
        switch (request.getScreenIndicator()) {
            case 5:
            case 8:
                showGameRequestAlert(request);
                break;
            case 6:
                System.out.println("Game request accepted by: " + request.getSender_username());
                // suppose to put the game begins 
                break;
            case 7:
                showAlert("Game request declined by: " + request.getSender_username());
                break;
            case 20:
                Platform.runLater(() -> {
                    showAlert("Player " + request.getSender_username() + " has accepted your game request!");
                });
                break;
            default:
                System.out.println("Unhandled screenIndicator: " + request.getScreenIndicator());
                break;
        }
    }

    private void resetConnection() {
        try {
            if (ois != null) {
                ois.close();
            }
            if (oos != null) {
                oos.close();
            }
            if (socket != null) {
                socket.close();
            }
            initializeConnection(LoginScreen.pto);
        } catch (IOException ex) {
            Logger.getLogger(AvailabilityScreen.class.getName()).log(Level.SEVERE, "Failed to reset connection", ex);
        }
    }

    private void showGameRequestAlert(RequestDTO request) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to play a game with " + request.getSender_username() + "?", new ButtonType("Cancel"), new ButtonType("Okay"));
        alert.setTitle("Game Request");
        alert.setHeaderText("Game Request from " + request.getSender_username());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sendGameResponse(request, true);
        } else {
            sendGameResponse(request, false);
        }
    }

    private void sendGameResponse(RequestDTO request, boolean accepted) {
        try {
            RequestDTO response = new RequestDTO();
            response.setSender_username(LoginScreen.pto.getUsername());
            response.setReciver_username(request.getSender_username());
            response.setScreenIndicator(accepted ? 6 : 7);
            oos.writeObject(response);
            oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(AvailabilityScreen.class.getName()).log(Level.SEVERE, "Error sending game response", ex);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    private void updatePlayerStatus(PlayerDTO player) {
        if (player.getUsername().equals(currentUsername)) {
            // This is the current user, don't add to the TableView
            return;
        }

        if (!players.contains(player)) {
            players.add(player);
            updateTableView();
        } else {
            int index = players.indexOf(player);
            players.set(index, player);
            _tableView.refresh();
        }
    }

    private void updateTableView() {
        Platform.runLater(() -> {
            _tableView.getItems().clear();
            for (PlayerDTO player : players) {
                if (!player.getUsername().equals(currentUsername)) {
                    _tableView.getItems().add(player);
                }
            }
        });
    }

    public void initializeConnection(PlayerDTO playerDTO) {
        try {
            socket = new Socket("10.178.240.32", 6007);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            playerDTO.setScreenIndicator(3);
            oos.writeObject(playerDTO);
            oos.flush();

            Thread receiveThread = new Thread(this);
            receiveThread.start();

            this.currentUsername = playerDTO.getUsername();
            updateTableView();
        } catch (IOException ex) {
            Logger.getLogger(AvailabilityScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
        
                