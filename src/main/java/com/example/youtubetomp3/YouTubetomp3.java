package com.example.youtubetomp3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.*;
import javafx.beans.binding.Bindings;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;

public class YouTubetomp3 extends Application
{
    @FXML
    Scene scene;
    @FXML
    HBox HBox1linkinsertion;
    @FXML
    TableView<Link> tableview1mp3;

    Dragboard db;
    static TableView<Link> tableview1;

    @Override
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(YouTubetomp3.class.getResource("YouTubetomp3.fxml"));
        System.setProperty("prism.lcdtext", "false");
        scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add("/tooltip.css");
        scene.setOnMouseClicked(e -> tableview1.getSelectionModel().clearSelection());
        stage.setScene(scene);
        stage.getIcons().add(new Image("/youtubetomp3icon.png"));
        stage.setTitle("YouTube to mp3");
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);
        stage.setOpacity(0);
        PauseTransition pt = new PauseTransition(Duration.seconds(0.1));
        pt.setOnFinished(e ->
        {
            stage.centerOnScreen();
            stage.setOpacity(1);
        });
        pt.play();
        stage.show();
    }

    public static class Link
    {
        String mp3link, mp3metadat;
        DoubleProperty perc = new SimpleDoubleProperty(0);
        StringProperty col = new SimpleStringProperty("#000000");
        BooleanProperty vis = new SimpleBooleanProperty(false), download = new SimpleBooleanProperty(false);
        Process p;
        Timeline tl;

        public Link(String link, String metadat)
        {
            mp3link = link;
            mp3metadat = metadat;
        }
    }

    AtomicInteger id = new AtomicInteger();
    ObservableList<Link> mp3list = FXCollections.observableArrayList();
    HashSet<String> linkset = new HashSet<>();

    public void initialize()
    {
        tableview1 = tableview1mp3;

        HBox1linkinsertion.setOnKeyReleased(ke ->
        {
            Clipboard cb = Clipboard.getSystemClipboard();
            if (ke.isControlDown() && ke.getCode() == KeyCode.V)
                if (cb.hasString() && cb.getString().contains("www.youtube.com") && linkset.add(cb.getString()))
                    linkinsertion(cb.getString());
        });

        HBox1linkinsertion.setOnDragOver(deo ->
        {
            deo.acceptTransferModes(TransferMode.COPY);
            HBox1linkinsertion.setOnDragDropped(de ->
            {
                db = de.getDragboard();
                de.setDropCompleted(true);
                de.consume();

                if (db.hasString() && db.getString().contains("www.youtube.com") && linkset.add(db.getString()))
                    linkinsertion(db.getString());
            });
        });

        TableColumn<Link, String> first = new TableColumn<>();
        TableColumn<Link, Void> second = new TableColumn<>();
        tableview1mp3.getColumns().add(first);
        tableview1mp3.getColumns().add(second);
        first.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().mp3metadat));
        second.setCellFactory(v -> new TableCell<>()
        {
            final Button button1download = new Button("\u279C");
            {
                button1download.setRotate(90);
                button1download.setStyle("-fx-background-color: transparent; -fx-background-insets: 8; -fx-padding: 0; -fx-font-family: Times New Roman; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #4B0000");
                button1download.setOnMouseEntered(e -> button1download.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-font-family: Times New Roman; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #FF0000"));
                button1download.setOnMouseExited(e ->
                {
                    if (getTableRow().isSelected())
                        button1download.setStyle("-fx-background-color: transparent; -fx-background-insets: 8; -fx-padding: 0; -fx-font-family: Times New Roman; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #FF0000");
                    else
                        button1download.setStyle("-fx-background-color: transparent; -fx-background-insets: 8; -fx-padding: 0; -fx-font-family: Times New Roman; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #4B0000");
                });
                button1download.setVisible(false);
                tableRowProperty().addListener((obs, or, nr) ->
                {
                    if (nr != null)
                    {
                        nr.itemProperty().addListener((obslin, ol, nl) ->
                        {
                            if (nl != null)
                                button1download.visibleProperty().bind(nl.vis);
                        });
                        nr.selectedProperty().addListener((obsbool, ob, nb) ->
                        {
                            if (nb)
                                button1download.setStyle("-fx-background-color: transparent; -fx-background-insets: 8; -fx-padding: 0; -fx-font-family: Times New Roman; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #FF0000");
                            else
                                button1download.setStyle("-fx-background-color: transparent; -fx-background-insets: 8; -fx-padding: 0; -fx-font-family: Times New Roman; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #4B0000");
                        });
                    }
                });
                button1download.setOnAction(e ->
                {
                    getTableView().getItems().get(getIndex()).col.set("#4B0000");
                    try
                    {
                        getTableView().getItems().get(getIndex()).vis.set(false);
                        getTableView().getItems().get(getIndex()).download.set(true);
                        downloading(getTableView().getItems().get(getIndex()));
                    }
                    catch (IOException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                });
            }

            @Override
            protected void updateItem(Void v, boolean empt)
            {
                super.updateItem(v, empt);
                if (empt)
                    setGraphic(null);
                else
                    setGraphic(button1download);
            }
        });
        first.setPrefWidth(275);
        second.setPrefWidth(25);

        tableview1mp3.setRowFactory(rf -> new TableRow<>()
        {
            @Override
            protected void updateItem(Link item, boolean empt)
            {
                super.updateItem(item, empt);

                styleProperty().unbind();
                if (empt || item == null)
                    setStyle("");
                else
                    styleProperty().bind(Bindings.createStringBinding(() ->
                    {
                        if (isSelected())
                        {
                            if (item.perc.get() == 0)
                                getTableView().getItems().get(getIndex()).col.set("#FF0000");
                            if (item.perc.get() > 0)
                                return "-fx-background-color: linear-gradient(to right, #4B0000 " + item.perc.get() + "%, transparent 0%)";
                            else if (getItem().mp3metadat.isEmpty() || getItem().mp3metadat.equals(".") || getItem().mp3metadat.equals("..") || getItem().mp3metadat.equals("..."))
                                return "-fx-background-color: transparent";
                            else
                                return "-fx-background-color: #4B0000";
                        }
                        else
                        {
                            if (item.perc.get() == 0)
                                getTableView().getItems().get(getIndex()).col.set("#000000");
                            return "-fx-background-color: linear-gradient(to right, #4B0000 " + item.perc.get() + "%, transparent 0%)";
                        }
                    }, item.perc));
            }
        });

        tableview1mp3.setOnKeyPressed(e ->
        {
            Link lin;

            if (e.getCode().equals(KeyCode.DELETE))
                if (tableview1mp3.getSelectionModel().getSelectedItem() != null)
                {
                    lin = tableview1mp3.getSelectionModel().getSelectedItem();
                    linkset.remove(lin.mp3link);
                    mp3list.remove(lin);
                    id.decrementAndGet();
                    if (id.get() == -1)
                        id.set(0);
                    if (lin.p != null)
                        lin.p.destroyForcibly();
                    tableview1mp3.getSelectionModel().clearSelection();
                }
        });

        first.setCellFactory(c -> new TableCell<>()
        {
            final Tooltip tt = new Tooltip();

            @Override
            protected void updateItem(String item, boolean empt)
            {
                super.updateItem(item, empt);

                styleProperty().unbind();
                if (empt || item == null)
                {
                    setText(null);
                    setStyle("");
                }
                else
                {
                    setText(item);
                    if (getTableView().getItems().get(getIndex()).mp3metadat.isEmpty() || getTableView().getItems().get(getIndex()).mp3metadat.equals(".") || getTableView().getItems().get(getIndex()).mp3metadat.equals("..") || getTableView().getItems().get(getIndex()).mp3metadat.equals("..."))
                        setStyle("-fx-text-fill: #000000");
                    else
                        styleProperty().bind(Bindings.concat("-fx-text-fill:", getTableView().getItems().get(getIndex()).col));
                }

                tt.setMaxWidth(Screen.getPrimary().getBounds().getWidth());
                tt.setStyle("-fx-text-fill: #000000");
                tt.setWrapText(true);
                tt.setShowDuration(javafx.util.Duration.INDEFINITE);
                setText(item);
                Runnable run = () ->
                {
                    Text itemtext = new Text(item);

                    itemtext.setFont(getFont());
                    if ((itemtext.getLayoutBounds().getWidth() > (getWidth() - getPadding().getLeft() - getPadding().getRight()) && getWidth() > 0) || (getTableRow().getItem() != null && getTableRow().getItem().download.get()))
                    {
                        tt.setText(item);
                        setTooltip(tt);
                    }
                    else
                        setTooltip(null);
                };
                Platform.runLater(run);
                if (getTableRow().getItem() != null)
                    getTableRow().getItem().download.addListener((obsbool, ob, nb) -> Platform.runLater(run));
            }
        });
    }

    public void linkinsertion(String link)
    {
        AtomicInteger i = new AtomicInteger();
        String[] loadarr = new String[]{"", ".", "..", "..."};

        mp3list.add(new Link(link, ""));
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(0.3), e ->
        {
            mp3list.set(id.intValue(), new Link(link, loadarr[i.intValue()]));
            i.incrementAndGet();
            if (i.intValue() >= loadarr.length)
                i.set(0);
        }));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();

        tableview1mp3.setItems(mp3list);
        try
        {
            metadata(link, tl);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void metadata(String link, Timeline tl) throws IOException
    {
        Process metadatproc = Runtime.getRuntime().exec(new String[]{"bash", "-c", "yt-dlp --no-playlist --print \"%(artist)s - %(title)s (%(duration_string)s)\" \"" + link + "\""});
        BufferedReader br = new BufferedReader(new InputStreamReader(metadatproc.getInputStream()));

        metadatproc.onExit().thenRun(() ->
        {
            Link lin;
            String metadat;

            tl.stop();
            try
            {
                metadat = br.readLine();
                if (metadat != null)
                {
                    lin = new Link(link, metadat.replace("NA - ", "").replace("\"", "").replace("/", ""));
                    lin.vis.set(true);
                }
                else
                    lin = new Link(link, "Video unavailable");
                mp3list.set(id.intValue(), lin);
                id.incrementAndGet();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public void loading(int arr, Link track)
    {
        double[] percarr;

        if (arr == 1)
        {
            percarr = new double[50];
            for (int i = 0; i < 50; i++)
                percarr[i] = i + 1;
        }
        else if (arr == 2)
        {
            percarr = new double[20];
            for (int i = 0; i < 20; i++)
                percarr[i] = i + 51;
        }
        else
        {
            percarr = new double[29];
            for (int i = 0; i < 29; i++)
                percarr[i] = i + 71;
        }
        if (track.tl != null)
            track.tl.stop();
        AtomicInteger i = new AtomicInteger();
        track.tl = new Timeline(new KeyFrame(Duration.seconds(0.075), e ->
        {
            if (i.intValue() < percarr.length)
            {
                track.perc.set(percarr[i.intValue()]);
                i.incrementAndGet();
            }
        }));
        track.tl.setCycleCount(Animation.INDEFINITE);
        track.tl.play();
    }

    public void downloading(Link track) throws IOException
    {
        track.p = Runtime.getRuntime().exec(new String[]{"bash", "-c", "yt-dlp --no-playlist -x --audio-format mp3 --audio-quality 320K --parse-metadata \"%(title)s:%(artist)s - %(track)s%\" --embed-metadata --postprocessor-args \"-ar 44100 -ac 2 -metadata album= -metadata genre= -metadata composer= -metadata date= -metadata comment= -metadata description= -metadata synopsis= -metadata purl=\" -o \"~/Downloads/" + track.mp3metadat.substring(0, track.mp3metadat.indexOf("(")-1) + "\" \"" + track.mp3link + "\""});
        BufferedReader br = new BufferedReader(new InputStreamReader(track.p.getInputStream()));

        new Thread(() ->
        {
            String lin;

            loading(1, track);
            try
            {
                while ((lin = br.readLine()) != null)
                {
                    if (lin.contains(" 0.0%"))
                        loading(2, track);
                    if (lin.contains("[ExtractAudio]"))
                        loading(3, track);
                    if (lin.contains("[Metadata]"))
                    {
                        track.tl.stop();
                        track.perc.set(100);
                    }
                }
                if (track.p.waitFor() == 0)
                {
                    linkset.remove(track.mp3link);
                    mp3list.remove(track);
                    if (id.intValue() > 0)
                        id.decrementAndGet();
                }
            }
            catch (Exception e)
            {
                track.p.destroyForcibly();
            }
        }).start();
    }

    public void stop()
    {
        System.exit(0);
    }
}