import com.jfoenix.controls.JFXButton;
import com.mysql.cj.jdbc.MysqlDataSource;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class Controller {
    private MysqlDataSource dataSource = null;
    SQLHandler sqlHandler = new SQLHandler();
    private Connection connection = null;

    String username;
    String password;
    String fileName;
    private String url;
    @FXML Pane loginPane;
    @FXML Pane mainPane;
    @FXML ComboBox<String> filesComboBox;
    @FXML TextField usernameField;
    @FXML TextField passwordField;
    @FXML Alert alert= new Alert(Alert.AlertType.NONE);

    @FXML TextField userTypeTF;
    @FXML TextField urlTF;
    @FXML TextArea SQLCommand_txtA;
    @FXML JFXButton executeCommandbtn;
    @FXML JFXButton clearCommandbtn;
    @FXML JFXButton clearResultsbtn;
    @FXML TableView<ObservableList<String>> resultsTable;

    public void initialize() {
        addFileOptions();
        resultsTable.getColumns().clear();
        displayURL();
        clearResultsbtn.setVisible(false);
    }

    // close the application window
    public void exitWindow(ActionEvent actionEvent) {
        try {
            if(getConnection() != null) {
                getConnection().close();
                System.out.println("Connection closed");
            } else {
                System.out.println("No connection to close");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // grab the stage window
        Stage stage = (Stage) ((Button) actionEvent.getSource()).getScene().getWindow();
        // close the stage window
        stage.close();
    }

    // Adds items to choice box
    public void addFileOptions() {
        ObservableList<String> fileList = FXCollections.observableArrayList("root.properties", "client.properties");
        filesComboBox.getItems().addAll(fileList);
    }

    private void showErrorMessage(String errorMessage) {
        alert.setAlertType(Alert.AlertType.ERROR);
        alert.setContentText(errorMessage);
        alert.setResizable(true);
        alert.show();
    }

    // get the file chosen and sets the correct username and password
    private void getFileChosen(String pathname) {
        InputStream inputStream;
        Properties properties = new Properties();

        try {
            if(getFileName() != null) {
                inputStream = new FileInputStream(pathname);
                properties.load(inputStream);
                setDataSource(new MysqlDataSource());
                getDataSource().setURL(properties.getProperty("MYSQL_DB_URL"));
                getDataSource().setUser(properties.getProperty("MYSQL_DB_USERNAME"));
                getDataSource().setPassword(properties.getProperty("MYSQL_DB_PASSWORD"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        setUrl(properties.getProperty("MYSQL_DB_URL"));
        setUsername(properties.getProperty("MYSQL_DB_USERNAME"));
        setPassword(properties.getProperty("MYSQL_DB_PASSWORD"));
    }

    // used check if the login info the user inputted is correct
    private boolean checkInfo(String expected, String actual) {
        return actual.equals(expected);
    }

    public void login() {
        setFileName(filesComboBox.getValue());
        String filepath = "properties/" + getFileName();
        getFileChosen(filepath);

        // get the user input and compare
        boolean validUser = checkInfo(getUsername(), usernameField.getText());
        boolean validPass = checkInfo(getPassword(), passwordField.getText());

        if((validUser && validPass) && fileName != null) {
            // go to the next window
            System.out.println("Connected to Database!!!");
            try {
                sqlHandler.connectToDatabase();
                setConnection(getDataSource().getConnection());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            loginPane.setVisible(false);
            mainPane.setVisible(true);
            SQLCommand_txtA.requestFocus();

            displayUserTypeTF();
            displayURL();
        } else if (getFileName() == null ){
            // show error message
            System.out.println("No filetype was chosen");
            showErrorMessage("Please choose a file type.");
        } else {
            // show error message
            System.out.println("Your username or password is incorrect :?");
            showErrorMessage("Your username or password is incorrect.");
        }
    }

    public void onEnter() {
        // if the login page is visible then attempt to login
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                login();
            }
        });
    }

    public void displayUserTypeTF() {
        userTypeTF.setText("User: " + getUsername());
    }

    public void displayURL() {
        urlTF.setText("Connected to: " + getUrl());
    }

    public void getRequestedCommand() {
        sqlHandler.executeCommand(getConnection(), SQLCommand_txtA.getText());
        setResultsColumnNames();

        if(sqlHandler.getErrorString() != null) {
            showErrorMessage(sqlHandler.getErrorString());
            resultsTable.getColumns().clear();
            resultsTable.getItems().clear();
            resultsTable.refresh();
            clearResultsbtn.setVisible(false);
        }
    }

    public void clearResults() {
        resultsTable.getColumns().clear();
        clearResultsbtn.setVisible(false);
    }

    public void clearCommand() {
        SQLCommand_txtA.clear();
    }

    private void setResultsColumnNames() {
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        for (int i = 0; i < sqlHandler.getNumColumns(); i++) {
            final int finalIdx = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(
                    sqlHandler.getColumnNames().get(i)
            );
            column.setCellValueFactory(param ->
                    new ReadOnlyObjectWrapper<>(param.getValue().get(finalIdx))
            );
            resultsTable.getColumns().add(column);
        }

        // for each row
        // get num columns
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        for(int i = 0; i < sqlHandler.getNumRows(); i++) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for(int j = 0; j < sqlHandler.getNumColumns(); j++) {
                row.add(sqlHandler.getRowData().get(i).get(j));
            }
            data.add(row);
        }

        resultsTable.refresh();
        resultsTable.setItems(data);
        clearResultsbtn.setVisible(true);
        System.out.println();
        System.out.println();
        System.out.println("The number of rows is: " + sqlHandler.getNumRows());
    }

    public void backToLogin() {
        try {
            getConnection().close();
            mainPane.setVisible(false);
            loginPane.setVisible(true);
        } catch (SQLException e) {
            System.out.println("Failed to go back to login page.");
            e.printStackTrace();
        }
    }

    // ====== Getters/Setters ======= //
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public MysqlDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(MysqlDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
