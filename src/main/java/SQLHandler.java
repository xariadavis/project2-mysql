import com.google.common.collect.Lists;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class SQLHandler {
    private String errorString;
    private int numColumns;
    private int numRows;
    private List<String> columnNames;
    private List<String> cellData;
    private List<List<String>> rowData;
    private int rowsAffectedUpdate;

    // operations log
    private int numQueries = 0;
    private int numUpdates = 0;
    private MysqlDataSource operationDataSource;
    private Connection operationsConnection;

    // connect to the database
    public void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded");
            System.out.println("SUCCESSFUL CONNECTION");

            // connect to operations log database
            readOperationProperties();
            setOperationsConnection(getOperationDataSource().getConnection());

        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Cannot issue statements that do not produce result sets.");
            System.out.println("Issue in connectToDatabase");
            e.printStackTrace();
        }
    }

    public void executeCommand(Connection connection, String query) {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = null;

            Statement operationsStatement = getOperationsConnection().createStatement();

            // if we have to use DML -- update, insert, delete then we need to use executeUpdate
            if(query.toLowerCase(Locale.ROOT).contains("select")) {
                resultSet = statement.executeQuery(query);
                setNumQueries(getNumQueries() + 1);
                operationsStatement.executeUpdate("update operationscount set num_queries = " + getNumQueries());
            } else {
                setRowsAffectedUpdate(statement.executeUpdate(query)); // if 0 nothing updated

                // update numUpdates in operations log
                setNumUpdates(getNumUpdates() + 1);     // increment count
                // perform execute Update operation
                operationsStatement.executeUpdate("update operationscount set num_updates = " + getNumUpdates());

            }

            ResultSetMetaData metaData;
            if (resultSet != null) {
                metaData = resultSet.getMetaData();
                setNumColumns(metaData.getColumnCount());

                int columnCount = metaData.getColumnCount();
                setNumColumns(columnCount);

                columnNames = new ArrayList<>();
                for(int i = 1; i <= columnCount; i++) {
                    System.out.println("In the for loop: " + metaData.getColumnName(i));
                    columnNames.add(metaData.getColumnName(i));
                }

                cellData = new ArrayList<>();
                while (resultSet.next()) { // jdbcRowSet.next()
                    for(int i = 1; i <= columnCount; i++) {
                        cellData.add(resultSet.getString(i));
                    }
                }
            }


            System.out.println("Tables affected: " + getRowsAffectedUpdate());
            setNumRows(cellData.size() / getNumColumns());
            setRowData(Lists.partition(getCellData(), getNumColumns()));
            setErrorString(null);

        } catch (SQLException e) {
            System.out.println("Issue in executeCommand");
            e.printStackTrace();
            setErrorString(e.getMessage());
        }
    }

    private void readOperationProperties() {
        InputStream inputStream;
        Properties properties = new Properties();

        try {
            inputStream = new FileInputStream("properties/operationslog.properties");
            properties.load(inputStream);
            setOperationDataSource(new MysqlDataSource());
            getOperationDataSource().setURL(properties.getProperty("MYSQL_DB_URL"));
            getOperationDataSource().setUser(properties.getProperty("MYSQL_DB_USERNAME"));
            getOperationDataSource().setPassword(properties.getProperty("MYSQL_DB_PASSWORD"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ======= Getters/Setters ======= //

    public String getErrorString() {
        return errorString;
    }

    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    public List<String> getCellData() {
        return cellData;
    }

    public void setCellData(List<String> cellData) {
        this.cellData = cellData;
    }

    public List<List<String>> getRowData() {
        return rowData;
    }

    public void setRowData(List<List<String>> rowData) {
        this.rowData = rowData;
    }

    public int getRowsAffectedUpdate() {
        return rowsAffectedUpdate;
    }

    public void setRowsAffectedUpdate(int rowsAffectedUpdate) {
        this.rowsAffectedUpdate = rowsAffectedUpdate;
    }

    public int getNumQueries() {
        return numQueries;
    }

    public void setNumQueries(int numQueries) {
        this.numQueries = numQueries;
    }

    public int getNumUpdates() {
        return numUpdates;
    }

    public void setNumUpdates(int numUpdates) {
        this.numUpdates = numUpdates;
    }

    public MysqlDataSource getOperationDataSource() {
        return operationDataSource;
    }

    public void setOperationDataSource(MysqlDataSource operationDataSource) {
        this.operationDataSource = operationDataSource;
    }

    public Connection getOperationsConnection() {
        return operationsConnection;
    }

    public void setOperationsConnection(Connection operationsConnection) {
        this.operationsConnection = operationsConnection;
    }
}
