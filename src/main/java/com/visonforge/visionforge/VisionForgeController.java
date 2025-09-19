package com.visonforge.visionforge;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class VisionForgeController implements Initializable {
    CategoryAxis xAxis = new CategoryAxis();
    NumberAxis yAxis = new NumberAxis();

    @FXML private ImageView trainingImage;
    @FXML private ImageView iconImage = new  ImageView();
    @FXML private StackPane drawingSurface;
    @FXML private TableView<BBox> bboxTable;
    @FXML private TableView<File> imageTable;
    @FXML private Slider zoomSlider;
    @FXML private BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
    @FXML private TableColumn<File, String> imageColumn;
    @FXML private TableColumn<BBox, String> typeColumn;
    @FXML private TableColumn<BBox, String> x1Column;
    @FXML private TableColumn<BBox, String> y1Column;
    @FXML private TableColumn<BBox, String> x2Column;
    @FXML private TableColumn<BBox, String> y2Column;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button updateButton;
    @FXML private Button loadTrainingFolderButton;

    private final ObservableList<File> imageFiles = FXCollections.observableArrayList();
    private final ObservableList<BBox> bboxList = FXCollections.observableArrayList();
    private final ObservableList<BBox> filteredBBoxList = FXCollections.observableArrayList();

    //Holds ratio for image pane.
    private double imageRatioWidth;
    private double imageRatioHeight;
    private double zoomValue = 0.05;

    //Object for use with drawing.
    Rectangle rectNewObject = new Rectangle();
    Circle circleStart = new Circle(.5);
    Circle circleEnd = new Circle(.5);
    List<Rectangle> rectList = new ArrayList<>();
    List<String> types = new ArrayList<>();

    //Directory where images and files are saved.
    File json_file;
    File training_folder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Setting up the table of images and boxes.
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        imageTable.setItems(imageFiles);

        //Creating and displaying loading dialog.
        Alert alert = new Alert(Alert.AlertType.NONE);
        URL icon = getClass().getResource("/com/visonforge/visionforge/icon_transparent.png");
        URL style = getClass().getResource("/com/visonforge/visionforge/style.css");

        if(style != null)
            alert.getDialogPane().getStylesheets().add(style.toString());

        if(icon != null) {
            Image image = new Image(icon.toString());
            iconImage.setFitHeight(300);
            iconImage.setFitWidth(300);
            iconImage.setImage(image);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(image);
        }
        alert.setTitle("VisionForge");
        alert.setGraphic(iconImage);
        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.setContentText("VisionForge is an open source program providing a means to rapidly develop and customize datasets for training computer vision programs.");
        alert.showAndWait();

        //Setting table values for use in the bounding box table.
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("stringType"));
        x1Column.setCellValueFactory(new PropertyValueFactory<>("x1"));
        y1Column.setCellValueFactory(new PropertyValueFactory<>("y1"));
        x2Column.setCellValueFactory(new PropertyValueFactory<>("x2"));
        y2Column.setCellValueFactory(new PropertyValueFactory<>("y2"));
        bboxTable.setItems(filteredBBoxList);

        //Setting new object rectangle parameters and adding all the drawing tools.
        rectNewObject.setStroke(Color.YELLOW);
        rectNewObject.setFill(Color.TRANSPARENT);
        rectNewObject.setStrokeWidth(2);
        drawingSurface.getChildren().add(rectNewObject);
        drawingSurface.getChildren().add(circleStart);
        drawingSurface.getChildren().add(circleEnd);

        typeComboBox.setDisable(true);
        saveButton.setDisable(true);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        zoomSlider.setDisable(true);
        bboxTable.setDisable(true);
        imageTable.setDisable(true);

        // Load folder button is provided training folder and loads all training data.
        loadTrainingFolderButton.setOnAction(e -> {
            try {
                loadTrainingFolder();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            updateGraph();
            loadImage();
        });

        // Zoom slider zooms in and out of the image.
        zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<?extends Number> observable, Number oldValue, Number newValue){
                zoomValue = newValue.doubleValue();
                loadImage();
            }
        });

        trainingImage.setOnScroll(event -> {
            if(event.getDeltaY() > 0){
                zoomValue += zoomSlider.getBlockIncrement();
            }
            else{
                zoomValue -= zoomSlider.getBlockIncrement();
            }
            if(zoomValue < zoomSlider.getMin()){
                zoomValue = zoomSlider.getMin();
            }
            if(zoomValue > zoomSlider.getMax()){
                zoomValue = zoomSlider.getMax();
            }
            zoomSlider.setValue(zoomValue);

        });

        // Loads a new image when an index of the image table is selected.
        imageTable.setOnMousePressed(event -> {
            loadImage();
        });

        // Draws an expanding rectangle when mouse is pressed.
        drawingSurface.setOnMousePressed(event -> {
            bboxTable.getSelectionModel().clearSelection();
            double x = event.getX();
            double y = event.getY();
            circleStart.setTranslateX(x);
            circleStart.setTranslateY(y);
            rectNewObject.setTranslateX(x);
            rectNewObject.setTranslateY(y);
        });

        // Draws an expanding rectangle when mouse is pressed.
        drawingSurface.setOnMouseReleased(event -> {
            double x = event.getX();
            double y = event.getY();
            bboxTable.getSelectionModel().clearSelection();
            rectNewObject.setHeight(Math.abs(y - circleStart.getTranslateY()));
            rectNewObject.setWidth(Math.abs(x - circleStart.getTranslateX()));
            circleEnd.setTranslateX(x);
            circleEnd.setTranslateY(y);
        });

        // Draws an expanding rectangle when mouse is pressed.
        drawingSurface.setOnMouseDragged(event -> {
            bboxTable.getSelectionModel().clearSelection();
            double x = event.getX();
            double y = event.getY();
            circleEnd.setTranslateX(x);
            circleEnd.setTranslateY(y);
            rectNewObject.setHeight(Math.abs((y - circleStart.getTranslateY())));
            rectNewObject.setWidth(Math.abs((x - circleStart.getTranslateX())));
        });

        // Adds a bounding box when save button is pressed.
        saveButton.setOnMousePressed(event -> {
            addBBox();
            loadImage();
            updateGraph();
            exportJSON();
        });

        // Loads a new image when an index of the image table is selected.
        bboxTable.setOnMousePressed(event -> {
            BBox selectedBBox = (BBox) bboxTable.getSelectionModel().getSelectedItem();

            rectNewObject.setTranslateX(selectedBBox.x1 * (1/imageRatioWidth));
            rectNewObject.setTranslateY(selectedBBox.y1 * (1/imageRatioHeight));
            rectNewObject.setWidth(Math.abs((selectedBBox.x2 * (1/imageRatioWidth)) - (selectedBBox.x1 * (1/imageRatioWidth))));
            rectNewObject.setHeight(Math.abs((selectedBBox.y2 * (1/imageRatioHeight)) - (selectedBBox.y1 * (1/imageRatioHeight))));
            typeComboBox.getSelectionModel().select(selectedBBox.getIntType());
        });

        // Adds a bounding box when save button is pressed.
        updateButton.setOnMousePressed(event -> {
            BBox selectedBBox = (BBox) bboxTable.getSelectionModel().getSelectedItem();
            if (selectedBBox == null)
                return;
            int i=bboxList.indexOf(selectedBBox);

            bboxList.get(i).type_int = typeComboBox.getSelectionModel().getSelectedIndex();
            bboxList.get(i).type_string = types.get(typeComboBox.getSelectionModel().getSelectedIndex());
            loadImage();
            updateGraph();
        });

        // Deletes selected bounding box.
        deleteButton.setOnMousePressed(event -> {
            BBox selectedBBox = (BBox) bboxTable.getSelectionModel().getSelectedItem();
            if (selectedBBox == null)
                return;
            bboxList.remove(selectedBBox);
            loadImage();
            updateGraph();
        });

    }

    /**
     * Takes a folder as an input and loads all files from that folder.
     */
    protected void loadTrainingFolder() throws IOException {

        //Opens a directory selector for user to select desired folder.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("training_data_folder"));
        fileChooser.setTitle("Open Training JSON");
        json_file = fileChooser.showOpenDialog(null);
        training_folder = new File(json_file.getParent() + "/images/");

        if (!json_file.exists()) {
            sendAlert("That is not a valid json. Please see help for more information.");
            return;
        }

        if (!training_folder.exists()) {
            sendAlert("Could not find a training folder labeled images in the same directory. Please see help for more information.");
            return;
        }

        imageFiles.clear();
        imageFiles.addAll(training_folder.listFiles((directory, fileName) -> fileName.endsWith(".jpg") || fileName.endsWith(".JPG")|| fileName.endsWith(".jpeg") || fileName.endsWith(".JPEG")));

        //Parses labels file from folder.
        try {
            JSONParser parser = new JSONParser();
            Reader reader = new FileReader(json_file.getAbsoluteFile());

            JSONObject labels_json = (JSONObject) parser.parse(reader);
            JSONArray labels = (JSONArray) labels_json.get("LABELS");

            if (labels==null) {
                sendAlert("The file does not have an object LABELS. Please see help for more information.");
                return;
            }

            for (Object label : labels) {
                if(label instanceof String){
                    types.add(String.valueOf(label));
                    typeComboBox.getItems().add(String.valueOf(label));
                }
            }
        } catch (FileNotFoundException e) {
            sendAlert("Failed to load types from JSON. Please see help for more information.");
            throw new RuntimeException(e);
        }
        catch (IOException | ParseException e) {
            sendAlert("Failed to parse types from JSON. Please see help for more information.");
            throw new RuntimeException(e);
        }

        //Parses bounding boxes from folder.
        try {
            JSONParser parser = new JSONParser();
            Reader reader = new FileReader(json_file.getAbsoluteFile());

            JSONObject labels_json = (JSONObject) parser.parse(reader);
            JSONArray features = (JSONArray) labels_json.get("FEATURES");

            if (features==null) {
                sendAlert("The json file does not have an object FEATURES.");
                return;
            }

            for (Object box: features) {
                if(box instanceof JSONObject feature){
                    String image_id = String.valueOf(feature.get("IMAGE_ID"));
                    int type_id = Integer.parseInt(String.valueOf(feature.get("TYPE_ID")))-1;
                    File imageFile = new File(training_folder.getAbsoluteFile() + "\\" + image_id);
                    if (!imageFile.exists()){
                        sendAlert("Could not find image file: " + imageFile.getAbsolutePath() +" removing it from training list.");
                    }
                    else if(type_id < types.size() && type_id >= 0) {
                        JSONArray bbox = (JSONArray) feature.get("BBOX");
                        int x1 = Integer.parseInt(String.valueOf(bbox.get(0)));
                        int y1 = Integer.parseInt(String.valueOf(bbox.get(1)));
                        int x2 = Integer.parseInt(String.valueOf(bbox.get(2)));
                        int y2 = Integer.parseInt(String.valueOf(bbox.get(3)));
                        bboxList.add(new BBox(image_id, type_id, types.get((int) type_id), x1, y1, x2, y2));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            sendAlert("Failed to load bounding boxes from JSON. Labels JSON should be save in sub-folder JSONs and saved as training.json. See help for more information.");
            throw new RuntimeException(e);
        }catch (IndexOutOfBoundsException e){
            sendAlert("Program has encountered an out of index out of bounds. Please check type labels and bounding boxes for formatting.");
            throw new RuntimeException(e);
        } catch (IOException | ParseException e) {
            sendAlert("During the loading process VisionForge encountered a bounding box in training.json that was not formatted correctly. Please reference the help manual for formatting.");
            throw new RuntimeException(e);
        }

        typeComboBox.getSelectionModel().select(0);
        imageTable.getSelectionModel().select(0);
        typeComboBox.setDisable(false);
        saveButton.setDisable(false);
        updateButton.setDisable(false);
        deleteButton.setDisable(false);
        zoomSlider.setDisable(false);
        bboxTable.setDisable(false);
        imageTable.setDisable(false);
        loadTrainingFolderButton.setDisable(true);
    }

    /**
     *
     */
    protected void updateGraph(){
        //Clear old data.
        chart.getData().clear();
        xAxis.getCategories().clear();

        xAxis.setCategories(FXCollections.<String>observableArrayList(types));
        XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
        int[] counts = new int[types.size()];
        for(int i = 0; i < counts.length; i++){
            counts[i]=0;
            for(BBox box: bboxList){
                if(box.type_int == i){
                    counts[i]++;
                }
            }
        }
        for(int i = 0; i < types.size(); i++){
            series.getData().add(new XYChart.Data<String, Number>(types.get(i), counts[i]));
        }
        chart.getData().add(series);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
    }


    /**
     * This method loads an image using zoom ratio to ensure image is displayed with representative size.
     */
    protected void loadImage(){

        //Loads the image selected in the table.
        try {
            File file = (File) imageTable.getSelectionModel().getSelectedItem();
            if(file == null || !file.exists()){
                return;
            }
            BufferedImage image = ImageIO.read(file);
            Image fxImage = SwingFXUtils.toFXImage(image, null);
            double imageWidth = image.getWidth(null);
            double imageHeight = image.getHeight(null);
            trainingImage.setFitHeight(imageHeight * zoomValue);
            trainingImage.setFitWidth(imageWidth * zoomValue);
            trainingImage.setPreserveRatio(true);
            trainingImage.setImage(fxImage);

            //Creates a ratio to ensure bounding boxes are relative to original image size.
            double displayWidth = trainingImage.boundsInParentProperty().get().getWidth();
            double displayHeight = trainingImage.boundsInParentProperty().get().getHeight();
            imageRatioHeight = imageHeight / displayHeight;
            imageRatioWidth = imageWidth / displayWidth;

            //Resets all drawing objects.
            rectNewObject.setTranslateX(0);
            rectNewObject.setTranslateY(0);
            rectNewObject.setWidth(0);
            rectNewObject.setHeight(0);
            circleStart.setTranslateX(0);
            circleStart.setTranslateY(0);
            circleEnd.setTranslateX(0);
            circleEnd.setTranslateY(0);

            //Removes all bounding boxes drawn so they can be redrawn then clears rectangle list.
            drawingSurface.getChildren().remove(rectNewObject);
            for (Rectangle rect : rectList) {
                drawingSurface.getChildren().remove(rect);
            }
            rectList.clear();

            //Builds a list of all boxes included in this image.
            filteredBBoxList.clear();
            for (BBox bbox : bboxList) {
                if (bbox.filename.equals(file.getName())) {
                    filteredBBoxList.add(bbox);
                }
            }
            bboxTable.setItems(filteredBBoxList);

            //Draws all bounding boxes from the filtered list.
            for (BBox bbox : filteredBBoxList) {
                Rectangle rect = new Rectangle();
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.RED);
                rect.setTranslateX(bbox.x1 * (1 / imageRatioWidth));
                rect.setTranslateY(bbox.y1 * (1 / imageRatioHeight));
                rect.setHeight(Math.abs((bbox.y2 * (1 / imageRatioHeight)) - (bbox.y1 * (1 / imageRatioHeight))));
                rect.setWidth(Math.abs((bbox.x2 * (1 / imageRatioWidth)) - (bbox.x1 * (1 / imageRatioWidth))));
                rectList.add(rect);
            }
            for (Rectangle rect : rectList) {
                drawingSurface.getChildren().add(rect);
            }
            drawingSurface.getChildren().add(rectNewObject);

        }catch (IOException e){
            sendAlert("During the loading process VisionForge encountered an error loading images. Please see the help manual to determine if the training.json, label.json, and images are stored correctly in the training folder.");
        }
    }


    /**
     * Adds a bounding box to the list of bounding boxes being saved for the training dataset. It will use the current
     */
    protected void addBBox(){
        File file = (File) imageTable.getSelectionModel().getSelectedItem();
        if(circleEnd.getTranslateX()-circleStart.getTranslateX()<=0 || circleEnd.getTranslateY()-circleStart.getTranslateY()<=0){
            return;
        }
        bboxList.add(new BBox(file.getName(),
                typeComboBox.getSelectionModel().getSelectedIndex(),
                types.get(typeComboBox.getSelectionModel().getSelectedIndex()),
                (int) (circleStart.getTranslateX() * imageRatioWidth),
                (int) (circleStart.getTranslateY() * imageRatioHeight),
                (int) (circleEnd.getTranslateX() * imageRatioWidth),
                (int) (circleEnd.getTranslateY() * imageRatioHeight)));
    }



    private void sendAlert(String message){
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setTitle("VisionForge: Warning");
        //URL style = getClass().getResource("/com/visonforge/visionforge/style.css");
        //warning.getDialogPane().getStylesheets().add(style.toString());
        warning.setWidth(700);
        warning.setHeight(700);
        warning.setContentText(message);
        warning.show();
    }



    protected void exportJSON(){
        JSONObject training_json = new JSONObject();

        JSONArray data_types = new JSONArray();
        data_types.addAll(types);
        training_json.put("LABELS", data_types);

        JSONArray features = new JSONArray();
        for(BBox bbox: bboxList) {
            JSONObject bbox_json_obj = new JSONObject();
            bbox_json_obj.put("IMAGE_ID", bbox.filename);
            bbox_json_obj.put("TYPE_ID", bbox.type_int + 1);

            JSONArray box_coords = new JSONArray();
            box_coords.add(bbox.x1);
            box_coords.add(bbox.y1);
            box_coords.add(bbox.x2);
            box_coords.add(bbox.y2);
            bbox_json_obj.put("BBOX", box_coords);
            features.add(bbox_json_obj);
        }
        training_json.put("FEATURES", features);

        try{
            FileWriter file_w = new FileWriter(json_file.getAbsoluteFile());
            file_w.write(training_json.toJSONString());
            file_w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VisionForgeController(){}
}