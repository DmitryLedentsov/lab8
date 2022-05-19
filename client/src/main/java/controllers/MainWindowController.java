package controllers;


import common.connection.CommandMsg;
import common.connection.Request;
import common.data.*;
import common.exceptions.ConnectionException;
import common.exceptions.InvalidDataException;
import common.utils.DateConverter;
import controllers.tools.TableFilter;
import controllers.tools.ZoomOperator;
import javafx.animation.ScaleTransition;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;
import main.App;
import client.Client;
import controllers.tools.ObservableResourceFactory;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.google.jhsheets.filtered.FilteredTableView;
import org.google.jhsheets.filtered.tablecolumn.*;
//import org.controlsfx.control.table.TableFilter;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

/**
 * Main window controller.
 */
public class MainWindowController {
    public static final String LOGIN_COMMAND_NAME = "login";
    public static final String REGISTER_COMMAND_NAME = "register";
    public static final String REFRESH_COMMAND_NAME = "refresh";
    public static final String INFO_COMMAND_NAME = "info";
    public static final String ADD_COMMAND_NAME = "add";
    public static final String UPDATE_COMMAND_NAME = "update";
    public static final String REMOVE_COMMAND_NAME = "remove_by_id";
    public static final String CLEAR_COMMAND_NAME = "clear";
    public static final String EXIT_COMMAND_NAME = "exit";
    public static final String ADD_IF_MIN_COMMAND_NAME = "add_if_min";
    public static final String REMOVE_GREATER_COMMAND_NAME = "remove_greater";
    public static final String HISTORY_COMMAND_NAME = "history";
    public static final String SUM_OF_HEALTH_COMMAND_NAME = "sum_of_health";

    private final long RANDOM_SEED = 1821L;
    private final Duration ANIMATION_DURATION = Duration.millis(800);
    private final double MAX_SIZE = 250;

    @FXML
    private TableView<Worker> workerTable;
    @FXML
    private TableColumn<Worker, Integer> idColumn;
    @FXML
    private TableColumn<Worker, String> ownerColumn;
    @FXML
    private TableColumn<Worker, Date> creationDateColumn;
    @FXML
    private TableColumn<Worker, LocalDate> endDateColumn;
    @FXML
    private TableColumn<Worker, String> nameColumn;
    @FXML
    private TableColumn<Worker, Long> salaryColumn;
    @FXML
    private TableColumn<Worker, Float> coordinatesXColumn;
    @FXML
    private TableColumn<Worker, Long> coordinatesYColumn;
    @FXML
    private TableColumn<Worker, Position> positionColumn;
    @FXML
    private TableColumn<Worker, Status> statusColumn;
    @FXML
    private TableColumn<Worker, String> organizationNameColumn;
    @FXML
    private TableColumn<Worker, OrganizationType> organizationTypeColumn;
    @FXML
    private AnchorPane canvasPane;
    @FXML
    private Tab tableTab;
    @FXML
    private Tab canvasTab;
    @FXML
    private Button infoButton;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button executeScriptButton;
    @FXML
    private Button addIfMinButton;
    @FXML
    private Button addIfMaxButton;
    @FXML
    private Button filterStartsWithNameButton;
    @FXML
    private Button groupByEndDateButton;
    @FXML
    private Button printUniqueSalariesButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button helpButton;
    @FXML
    private Tooltip infoButtonTooltip;
    @FXML
    private Tooltip addButtonTooltip;
    @FXML
    private Tooltip updateButtonTooltip;
    @FXML
    private Tooltip removeButtonTooltip;
    @FXML
    private Tooltip clearButtonTooltip;
    @FXML
    private Tooltip executeScriptButtonTooltip;
    @FXML
    private Tooltip addIfMinButtonTooltip;
    @FXML
    private Tooltip removeGreaterButtonTooltip;
    @FXML
    private Tooltip historyButtonTooltip;
    @FXML
    private Tooltip sumOfHealthButtonTooltip;
    @FXML
    private Tooltip refreshButtonTooltip;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private Label usernameLabel;

    private Tooltip shapeTooltip;
    private TableFilter<Worker> tableFilter;
    private Client client;
    private Stage askStage;
    private Stage primaryStage;
    private FileChooser fileChooser;
    private AskWindowController askWindowController;
    private Map<String, Color> userColorMap;
    private Map<Shape, Integer> shapeMap;
    private Map<Integer, Text> textMap;
    private Shape prevClicked;
    private Color prevColor;
    private Random randomGenerator;
    private ObservableResourceFactory resourceFactory;
    private Map<String, Locale> localeMap;

    /**
     * Initialize main window.
     */
    public void initialize() {
        initializeTable();
        initCanvas();
        fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("."));
        userColorMap = new HashMap<>();
        shapeMap = new HashMap<>();
        textMap = new HashMap<>();
        randomGenerator = new Random(RANDOM_SEED);
        localeMap = new HashMap<>();
        localeMap.put("English", new Locale("en", "NZ"));
        localeMap.put("Русский", new Locale("ru", "RU"));
        localeMap.put("Deutsche", new Locale("de", "DE"));
        localeMap.put("Dansk", new Locale("da", "DK"));
        languageComboBox.setItems(FXCollections.observableArrayList(localeMap.keySet()));
    }

    /**
     * Initialize table.
     */
    private void initializeTable() {

        idColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getId()));
        ownerColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getUserLogin()));
        creationDateColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getCreationDate()));
        nameColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getName()));
        salaryColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getSalary()));
        coordinatesXColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getCoordinates().getX()));
        coordinatesYColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getCoordinates().getY()));
        statusColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getStatus()));
        positionColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getPosition()));
        endDateColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getEndDate()));
        organizationNameColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getOrganization().getFullName()));
        organizationTypeColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().getOrganization().getType()));
        //workerTable.setItems(FXCollections.observableArrayList());
       //s TableFilter<Worker> tableFilter = TableFilter.forTableView(workerTable).apply();
        /*workerTable.setOnSort((e)->{
            workerTable.getSortOrder().stream().sorted().collect();
        });*/
        //FilteredTableView y;

    }

    public void initFilter(){
        tableFilter = new TableFilter<Worker>(workerTable,client.getWorkerManager().getCollection(),resourceFactory)
                .addFilter(idColumn, (w)->Integer.toString(w.getId()))
                .addFilter(nameColumn, (w)->w.getName())
                .addFilter(coordinatesXColumn, (w)->Float.toString(w.getCoordinates().getX()))
                .addFilter(coordinatesYColumn, (w)->Long.toString(w.getCoordinates().getY()))
                .addFilter(creationDateColumn, (w)-> DateConverter.dateToString(w.getCreationDate()))
                .addFilter(endDateColumn, (w)->DateConverter.dateToString(w.getEndDate()))
                .addFilter(positionColumn, (w)->w.getPosition().toString())
                .addFilter(statusColumn, (w)->w.getStatus().toString())
                .addFilter(salaryColumn, (w)->Long.toString(w.getSalary()))
                .addFilter(organizationNameColumn, (w)->w.getOrganization().getFullName())
                .addFilter(organizationTypeColumn, (w)->w.getOrganization().getType().toString())
                .addFilter(ownerColumn, (w)->w.getUserLogin());
    }

    public TableFilter<Worker> getFilter(){
        return tableFilter;
    }
    public TableColumn<Worker,?> getNameColumn(){
        return nameColumn;
    }

    private void initCanvas(){
        ZoomOperator zoomOperator = new ZoomOperator();

// Listen to scroll events (similarly you could listen to a button click, slider, ...)
        canvasPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double zoomFactor = 1.5;
                if (event.getDeltaY() <= 0) {
                    // zoom out
                    zoomFactor = 1 / zoomFactor;

                }

                double x = event.getSceneX();
                double y = event.getSceneY();

                //if(!(event.getDeltaY()<=0 && (zoomOperator.getBounds().getHeight()<=1000||zoomOperator.getBounds().getWidth()<=1000))){
                /*if((event.getDeltaY()<=0 && (zoomOperator.getBounds().getMinX()>=1000||
                        zoomOperator.getBounds().getMinY()>=1000||
                        zoomOperator.getBounds().getMaxX()<=2000-1000||
                        zoomOperator.getBounds().getMaxY()<=2000-1000))) return;*/
                if((event.getDeltaY()<=0 && (zoomOperator.getBounds().getHeight()<=500||zoomOperator.getBounds().getWidth()<=500))) return;
                zoomOperator.zoom(canvasPane, zoomFactor, x, y);


            }
        });
        /*canvasPane.setOnMouseDragged(event -> {
            canvasPane.setManaged(false);
            canvasPane.setTranslateX(event.getX() + canvasPane.getTranslateX());
            canvasPane.setTranslateY(event.getY() + canvasPane.getTranslateY());
            event.consume();
        });*/

        zoomOperator.draggable(canvasPane);
        canvasPane.setMinWidth(2000);
        canvasPane.setMinHeight(2000);
    }
    /**
     * Bind gui language.
     */
    private void bindGuiLanguage() {
        resourceFactory.setResources(ResourceBundle.getBundle
                (App.BUNDLE, localeMap.get(languageComboBox.getSelectionModel().getSelectedItem())));

        idColumn.textProperty().bind(resourceFactory.getStringBinding("IdColumn"));
        ownerColumn.textProperty().bind(resourceFactory.getStringBinding("OwnerColumn"));
        creationDateColumn.textProperty().bind(resourceFactory.getStringBinding("CreationDateColumn"));
        nameColumn.textProperty().bind(resourceFactory.getStringBinding("NameColumn"));
        coordinatesXColumn.textProperty().bind(resourceFactory.getStringBinding("CoordinatesXColumn"));
        coordinatesYColumn.textProperty().bind(resourceFactory.getStringBinding("CoordinatesYColumn"));
        salaryColumn.textProperty().bind(resourceFactory.getStringBinding("SalaryColumn"));
        endDateColumn.textProperty().bind(resourceFactory.getStringBinding("EndDateColumn"));
        positionColumn.textProperty().bind(resourceFactory.getStringBinding("PositionColumn"));
        statusColumn.textProperty().bind(resourceFactory.getStringBinding("StatusColumn"));
        organizationNameColumn.textProperty().bind(resourceFactory.getStringBinding("OrganizationNameColumn"));
        organizationTypeColumn.textProperty().bind(resourceFactory.getStringBinding("OrganizationTypeColumn"));

        tableTab.textProperty().bind(resourceFactory.getStringBinding("TableTab"));
        canvasTab.textProperty().bind(resourceFactory.getStringBinding("CanvasTab"));

        infoButton.textProperty().bind(resourceFactory.getStringBinding("InfoButton"));
        addButton.textProperty().bind(resourceFactory.getStringBinding("AddButton"));
        updateButton.textProperty().bind(resourceFactory.getStringBinding("UpdateButton"));
        removeButton.textProperty().bind(resourceFactory.getStringBinding("RemoveButton"));
        clearButton.textProperty().bind(resourceFactory.getStringBinding("ClearButton"));
        executeScriptButton.textProperty().bind(resourceFactory.getStringBinding("ExecuteScriptButton"));
        addIfMinButton.textProperty().bind(resourceFactory.getStringBinding("AddIfMinButton"));
        addIfMaxButton.textProperty().bind(resourceFactory.getStringBinding("AddIfMaxButton"));
        groupByEndDateButton.textProperty().bind(resourceFactory.getStringBinding("GroupByEndDateButton"));
        filterStartsWithNameButton.textProperty().bind(resourceFactory.getStringBinding("FilterStartsWithNameButton"));
        printUniqueSalariesButton.textProperty().bind(resourceFactory.getStringBinding("PrintUniqueSalariesButton"));
        refreshButton.textProperty().bind(resourceFactory.getStringBinding("RefreshButton"));
        helpButton.textProperty().bind(resourceFactory.getStringBinding("HelpButton"));

        infoButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("InfoButtonTooltip"));
        addButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("AddButtonTooltip"));
        updateButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("UpdateButtonTooltip"));
        removeButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("RemoveButtonTooltip"));
        clearButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("ClearButtonTooltip"));
        executeScriptButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("ExecuteScriptButtonTooltip"));
        addIfMinButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("AddIfMinButtonTooltip"));
        removeGreaterButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("RemoveGreaterButtonTooltip"));
        historyButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("HistoryButtonTooltip"));
        sumOfHealthButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("SumOfHealthButtonTooltip"));
        refreshButtonTooltip.textProperty().bind(resourceFactory.getStringBinding("RefreshButtonTooltip"));
    }

    /**
     * Refresh button on action.
     */
    @FXML
    public void refreshButtonOnAction() {
        requestAction(REFRESH_COMMAND_NAME);
    }

    /**
     * Info button on action.
     */
    @FXML
    private void infoButtonOnAction() {
        requestAction(INFO_COMMAND_NAME);
    }


    /**
     * Update button on action.
     */
    @FXML
    private void updateButtonOnAction() {
       /* Worker worker = null;
        try {
            worker = new DefaultWorker("a",new Coordinates(1,2L),1000L, DateConverter.parseLocalDate("2000-01-01"),
                    Position.BAKER,Status.PROBATION,new Organization("XXX",OrganizationType.GOVERNMENT));
        } catch (InvalidDateFormatException e) {
            e.printStackTrace();
        }
        workerTable.getItems().add(worker);
*/
        Worker worker = workerTable.getSelectionModel().getSelectedItem();
       // int idx = workerTable.getSelectionModel().getSelectedIndex() + 1;
        //int i = workerTable.getSelectionModel().getSelectedIndex();
        if(worker!=null) {
            askWindowController.setWorker(worker);
            try {
                client.getCommandManager().runCommand(new CommandMsg("update").setArgument(Integer.toString(worker.getId())).setWorker(askWindowController.readWorker()));
            } catch (InvalidDataException e) {
                //e.printStackTrace();
            }
        }


        /*if (!spaceMarineTable.getSelectionModel().isEmpty()) {
            long id = spaceMarineTable.getSelectionModel().getSelectedItem().getId();
            askWindowController.setMarine(spaceMarineTable.getSelectionModel().getSelectedItem());
            askStage.showAndWait();
            MarineRaw marineRaw = askWindowController.getAndClear();
            if (marineRaw != null) requestAction(UPDATE_COMMAND_NAME, id + "", marineRaw);
        } else OutputterUI.error("UpdateButtonSelectionException");*/

    }

    /**
     * Remove button on action.
     */
    @FXML
    private void removeButtonOnAction() {
        Worker worker = workerTable.getSelectionModel().getSelectedItem();
        if(worker!=null) client.getCommandManager().runCommand(new CommandMsg("remove_by_id").setArgument(Integer.toString(worker.getId())));
        /*if (!spaceMarineTable.getSelectionModel().isEmpty())
            requestAction(REMOVE_COMMAND_NAME,
                    spaceMarineTable.getSelectionModel().getSelectedItem().getId().toString(), null);
        else OutputterUI.error("RemoveButtonSelectionException");*/
    }

    /**
     * Clear button on action.
     */
    @FXML
    private void clearButtonOnAction() {
        client.getCommandManager().runCommand(new CommandMsg("clear"));
    }

    /**
     * Execute script button on action.
     */
    @FXML
    private void executeScriptButtonOnAction() {
        /*File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) return;
        if (client.processScriptToServer(selectedFile)) Platform.exit();
        else refreshButtonOnAction();*/
    }

    /**
     * Add button on action.
     */
    @FXML
    private void addButtonOnAction() {
        //askWindowController.clearMarine();

        try {
            client.getCommandManager().runCommand(new CommandMsg("add").setWorker(askWindowController.readWorker()));
        } catch (InvalidDataException e) {

        }
        /*workerTable.refresh();
        refreshCanvas();*/
        //if (marineRaw != null) requestAction(ADD_COMMAND_NAME, "", marineRaw);*/
    }
    /**
     * Add if min button on action.
     */
    @FXML
    private void addIfMinButtonOnAction() {
        try {
            Worker worker = askWindowController.readWorker();
            if(worker!=null) {
                client.getCommandManager().runCommand(new CommandMsg("add_if_min").setWorker(worker));
                /*workerTable.refresh();
                refreshCanvas();*/
            }
        } catch (InvalidDataException e) {

        }

    }

    @FXML
    private void addIfMaxButtonOnAction() {
        try {
            Worker worker = askWindowController.readWorker();
            if(worker!=null) {
                client.getCommandManager().runCommand(new CommandMsg("add_if_max").setWorker(worker));
                /*workerTable.refresh();
                refreshCanvas();*/
            }
        } catch (InvalidDataException e) {

        }
    }


    /**
     * Remove greater button on action.
     */
    @FXML
    private void groupByEndDateButtonOnAction() {
        client.getCommandManager().runCommand(new CommandMsg("group_counting_by_end_date"));
    }

    /**
     * History button on action.
     */
    @FXML
    private void printUniqueSalariesButtonOnAction() {
        client.getCommandManager().runCommand(new CommandMsg("print_unique_salary"));

    }

    /**
     * Sum of health button on action.
     */
    @FXML
    private void filterStartsWithNameButtonOnAction() {
        Label startsWithLabel = new Label();
        Stage stage = new Stage();
        Label nameLabel = new Label();
        TextField textField = new TextField();
        Button button = new Button();
        button.textProperty().bind(resourceFactory.getStringBinding("EnterButton"));
        button.setOnAction((e)->{
            String arg = textField.getText();
            if(arg!=null && !arg.equals("")) {
                client.getCommandManager().runCommand(new CommandMsg("filter_starts_with_name").setArgument(arg));
                stage.close();
            }

        });
        nameLabel.textProperty().bind(resourceFactory.getStringBinding("NameColumn"));
        button.setAlignment(Pos.CENTER);

        HBox hBox = new HBox(nameLabel,textField,button);
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(10);
        Scene scene = new Scene(hBox);
        stage.setScene(scene);
        stage.setWidth(300);
        stage.setHeight(100);
        stage.setResizable(false);
        stage.showAndWait();
    }
    @FXML
    private void helpButtonOnAction(){
        client.getCommandManager().runCommand(new CommandMsg("help"));
    }

    /**
     * Request action.
     */
    private void requestAction(String commandName, String commandStringArgument, Serializable commandObjectArgument) {
        /*NavigableSet<SpaceMarine> responsedMarines = client.processRequestToServer(commandName, commandStringArgument,
                commandObjectArgument);
        if (responsedMarines != null) {
            ObservableList<SpaceMarine> marinesList = FXCollections.observableArrayList(responsedMarines);
            spaceMarineTable.setItems(marinesList);
            TableFilter.forTableView(spaceMarineTable).apply();
            spaceMarineTable.getSelectionModel().clearSelection();
            refreshCanvas();
        }*/
    }

    /**
     * Binds request action.
     */
    private void requestAction(String commandName) {
        requestAction(commandName, "", null);
    }


    public void refreshTable(){
        workerTable.refresh();
    }
    /**
     * Refreshes canvas.
     */

    public void refreshCanvas() {
        shapeMap.keySet().forEach(s -> canvasPane.getChildren().remove(s));
        shapeMap.clear();
        textMap.values().forEach(s -> canvasPane.getChildren().remove(s));
        textMap.clear();
        SortedList<Worker> list = workerTable.getItems().sorted((w1,w2)->w1.getSalary()>w2.getSalary()?0:1);
        for (Worker worker : list) {
            if (!userColorMap.containsKey(worker.getUserLogin()))
                userColorMap.put(worker.getUserLogin(),
                        Color.color(randomGenerator.nextDouble(), randomGenerator.nextDouble(), randomGenerator.nextDouble()));

            double size = Math.min(worker.getSalary()/1000, MAX_SIZE);

            Shape circleObject = new Circle(size, userColorMap.get(worker.getUserLogin()));
            circleObject.setOnMouseClicked(this::shapeOnMouseClicked);
            circleObject.translateXProperty().bind(canvasPane.widthProperty().divide(2).add(worker.getCoordinates().getX()));
            circleObject.translateYProperty().bind(canvasPane.heightProperty().divide(2).subtract(worker.getCoordinates().getY()));

            circleObject.setOpacity(0.5);

            Text textObject = new Text(Integer.toString(worker.getId()));
            textObject.setOnMouseClicked(circleObject::fireEvent);
            textObject.setFont(Font.font(size / 3));
            textObject.setFill(userColorMap.get(worker.getUserLogin()).darker());
            textObject.translateXProperty().bind(circleObject.translateXProperty().subtract(textObject.getLayoutBounds().getWidth() / 2));
            textObject.translateYProperty().bind(circleObject.translateYProperty().add(textObject.getLayoutBounds().getHeight() / 4));

            canvasPane.getChildren().add(circleObject);
            canvasPane.getChildren().add(textObject);
            shapeMap.put(circleObject, worker.getId());
            textMap.put(worker.getId(), textObject);

            ScaleTransition circleAnimation = new ScaleTransition(ANIMATION_DURATION, circleObject);
            ScaleTransition textAnimation = new ScaleTransition(ANIMATION_DURATION, textObject);
            circleAnimation.setFromX(0);
            circleAnimation.setToX(1);
            circleAnimation.setFromY(0);
            circleAnimation.setToY(1);
            textAnimation.setFromX(0);
            textAnimation.setToX(1);
            textAnimation.setFromY(0);
            textAnimation.setToY(1);
            circleAnimation.play();
            textAnimation.play();
        }

    }

    /**
     * Shape on mouse clicked.
     */
    private void shapeOnMouseClicked(MouseEvent event) {
        Shape shape = (Shape) event.getSource();
        //Tooltip.install(shape,shapeTooltip);
        long id = shapeMap.get(shape);
        for (Worker worker : workerTable.getItems()) {
            if (worker.getId() == id) {
                if(shapeTooltip!=null && shapeTooltip.isShowing()) shapeTooltip.hide();
                shapeTooltip = new Tooltip(worker.toString());
                shapeTooltip.setAutoHide(true);
                shapeTooltip.show(shape,event.getScreenX(),event.getScreenY());
                workerTable.getSelectionModel().select(worker);
                //shapeTooltip.setText(worker.toString());
                //shapeTooltip.show(primaryStage);
                break;
            }
        }
        if (prevClicked != null) {
            prevClicked.setFill(prevColor);
        }
        prevClicked = shape;
        prevColor = (Color) shape.getFill();
        shape.setFill(prevColor.brighter());
    }

    public void setClient(Client client) {
        this.client = client;
        workerTable.setItems(client.getWorkerManager().getCollection());
        client.getWorkerManager().setController(this);
        client.setResourceFactory(resourceFactory);
    }

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    public void setAskStage(Stage askStage) {
        this.askStage = askStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setAskWindowController(AskWindowController askWindowController) {
        this.askWindowController = askWindowController;
    }

    public void initLangs(ObservableResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
        for (String localeName : localeMap.keySet()) {
            if (localeMap.get(localeName).equals(resourceFactory.getResources().getLocale()))
                languageComboBox.getSelectionModel().select(localeName);
        }
        if (languageComboBox.getSelectionModel().getSelectedItem().isEmpty())
            languageComboBox.getSelectionModel().selectFirst();
        languageComboBox.setOnAction((event) ->
                resourceFactory.setResources(ResourceBundle.getBundle
                        (App.BUNDLE, localeMap.get(languageComboBox.getValue()))));
        bindGuiLanguage();
    }
}
