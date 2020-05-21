package com.axlor.predictionassistantanalyzer.gui;

import com.axlor.predictionassistantanalyzer.exception.NoSnapshotsInDatabaseException;
import com.axlor.predictionassistantanalyzer.model.Contract;
import com.axlor.predictionassistantanalyzer.model.Market;
import com.axlor.predictionassistantanalyzer.service.SnapshotService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Component
@FxmlView("AllMarketsScene.fxml")
public class AllMarketsSceneController {

    @Autowired
    SnapshotService snapshotService;

    @Autowired
    TrackedMarkets trackedMarkets;

    private List<Market> latestMarkets;

    private TableColumn marketIdColumn;
    private TableColumn contractIdColumn;
    private TableColumn nameColumn;
    private TableColumn buyYesColumn;
    private TableColumn buyNoColumn;
    private TableColumn sellYesColumn;
    private TableColumn sellNoColumn;

    ContextMenu allMarketsTableContextMenu;

    @FXML // fx:id="titleLabel"
    private Label titleLabel; // Value injected by FXMLLoader

    @FXML // fx:id="leftPane"
    private AnchorPane leftPane; // Value injected by FXMLLoader

    @FXML // fx:id="moversButton"
    private Button moversButton; // Value injected by FXMLLoader

    @FXML // fx:id="trackedMarketsButton"
    private Button trackedMarketsButton; // Value injected by FXMLLoader

    @FXML // fx:id="label_trackedMarkets1"
    private Label label_trackedMarkets1; // Value injected by FXMLLoader

    @FXML // fx:id="allMarketsTable"
    private TableView<DisplayableMC> allMarketsTable; // Value injected by FXMLLoader

    @FXML // fx:id="allMarketsButton"
    private Button allMarketsButton; // Value injected by FXMLLoader

    @FXML // fx:id="query_textfield_tooltip"
    private Tooltip query_textfield_tooltip; // Value injected by FXMLLoader

    @FXML // fx:id="menuBar"
    private MenuBar menuBar; // Value injected by FXMLLoader

    @FXML // fx:id="titlePane"
    private AnchorPane titlePane; // Value injected by FXMLLoader

    @FXML // fx:id="rightPane"
    private AnchorPane rightPane; // Value injected by FXMLLoader

    @FXML // fx:id="trackSelectedMarketButton"
    private Button trackSelectedMarketButton; // Value injected by FXMLLoader

    @FXML // fx:id="query_textField"
    private TextField query_textField; // Value injected by FXMLLoader

    @FXML // fx:id="label_query"
    private Label label_query; // Value injected by FXMLLoader

    @FXML
    void initialize() {

        setupColumns();
        setCurrentMarketsList();
        setupTableContextMenu();

        updateAllMarketsTableView();

    }

    private void setupTableContextMenu() {
        allMarketsTableContextMenu = new ContextMenu();
        MenuItem menuItem1 = new MenuItem("Open URL");
        MenuItem menuItem2 = new MenuItem("Track Selected Market");
        MenuItem menuItem3 = new MenuItem("Untrack Selected Market");

        allMarketsTableContextMenu.getItems().addAll(menuItem1, menuItem2, menuItem3);

        //--menuItem1-------------------------------------------------------------------------------------------------\\
        allMarketsTable.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if (event.getButton() == MouseButton.SECONDARY) { //right click
                allMarketsTableContextMenu.show(allMarketsTable, event.getScreenX(), event.getScreenY());
            }
        });
        menuItem1.setOnAction((ActionEvent event) -> {
            DisplayableMC displayableMC = allMarketsTable.getSelectionModel().getSelectedItem();

            if(displayableMC == null){
                System.out.println("displayableMC object is null, probably shouldn't be...");
            }
            if(!Desktop.isDesktopSupported()){
                System.out.println("Desktop is not supported for some reason... Unix system?");
            }
            if (Desktop.isDesktopSupported() && displayableMC != null && !displayableMC.getMarketUrl().equals("---")) {
                try {
                    try {
                        Desktop.getDesktop().browse(new URI(displayableMC.getMarketUrl()));
                    } catch (URISyntaxException ex) {
                        System.out.println("Failed to open market URL.");
                    }
                } catch (IOException ex) {
                    System.out.println("Failed to open market URL.");
                }
            }
        });
        //--------------------------------------------------------------------------------------------------------\\
        //menuItem2
        menuItem2.setOnAction((ActionEvent event) -> {
            DisplayableMC displayableMC = allMarketsTable.getSelectionModel().getSelectedItem();
            if(displayableMC != null && !displayableMC.getMarketId().equals("---")){
                trackedMarkets.track(Integer.parseInt(displayableMC.getMarketId()));
            }
        });
        //menuItem3
        menuItem3.setOnAction((ActionEvent event) -> {
            DisplayableMC displayableMC = allMarketsTable.getSelectionModel().getSelectedItem();
            if(displayableMC != null && !displayableMC.getMarketId().equals("---")){
                trackedMarkets.untrack(Integer.parseInt(displayableMC.getMarketId()));
            }
        });
    }

    private void setCurrentMarketsList() {
        try {
            latestMarkets = snapshotService.getLatestSnapshot().getMarkets();
        } catch (NoSnapshotsInDatabaseException e) {
            System.out.println("Caught NoSnapshotsInDatabaseException.");
            latestMarkets = null;
        }
    }

    public void updateAllMarketsTableView() {
        //create table data
        ObservableList<DisplayableMC> tableData = FXCollections.observableArrayList();
        if (latestMarkets != null) {
            for (Market market : latestMarkets) {
                if (matchesQuery(market)) {
                    //add market data to tableData
                    tableData.add(new DisplayableMC(
                            String.valueOf(market.getId()),
                            "---",
                            market.getName(),
                            "---",
                            "---",
                            "---",
                            "---",
                            market.getUrl()
                    ));

                    for (Contract contract : market.getContracts()) {
                        //add contract data for each contract in this 'market'

                        //for formatting, to make table look nicer
                        String nameToUse = "";
                        if (market.getName().equals(contract.getName())) {
                            nameToUse = "Yes / No";
                        } else {
                            nameToUse = contract.getName();
                        }

                        tableData.add(new DisplayableMC(
                                String.valueOf(market.getId()),
                                String.valueOf(contract.getId()),
                                "         " + nameToUse,
                                String.valueOf(contract.getBestBuyYesCost()),
                                String.valueOf(contract.getBestBuyNoCost()),
                                String.valueOf(contract.getBestSellYesCost()),
                                String.valueOf(contract.getBestSellNoCost()),
                                market.getUrl()
                        ));
                    }//for each contract
                }//if market's name matches query words
            }//for each market
            if (tableData.isEmpty()) {
                tableData.add(new DisplayableMC("---", "---", "Could not find any Market or Contract with any of the query terms.", "---", "---", "---", "---", "---"));
            }
            allMarketsTable.setItems(tableData);
        }//if latest!=null
        else {
            tableData.add(new DisplayableMC("---", "---", "Database error, could not get any Snaphot Market info from DB.", "---", "---", "---", "---", "---"));

        }

    }

    private boolean matchesQuery(Market market) {
        if (query_textField == null || query_textField.getText() == null || query_textField.getText().equals("")) {
            return true;
        }

        //ignore case difference
        String marketName = market.getName().toLowerCase();
        String queryText = query_textField.getText().toLowerCase();

        ArrayList<String> queryPhrases = getPhrases(queryText);
        //for each phrase, see if market name or any market contract contains that phrase
        for (String phrase : queryPhrases) {
            if (marketName.contains(phrase)) {
                return true;
            }
            for (Contract contract : market.getContracts()) {
                if (contract.getName().toLowerCase().contains(phrase)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ArrayList<String> getPhrases(String queryText) {
        ArrayList<String> searchTerms = new ArrayList();
        String[] terms = queryText.split(",");
        for (String term : terms) {
            String trimmedTerm = term.trim();
            if (!"".equals(trimmedTerm)) {
                searchTerms.add(trimmedTerm);
            }
        }
        return searchTerms;
    }

    private void setupColumns() {
        marketIdColumn = new TableColumn("MID");
        contractIdColumn = new TableColumn("CID");
        nameColumn = new TableColumn("Name");
        buyYesColumn = new TableColumn("BuyYes");
        buyNoColumn = new TableColumn("BuyNo");
        sellYesColumn = new TableColumn("SellYes");
        sellNoColumn = new TableColumn("SellNo");

        marketIdColumn.setCellValueFactory(new PropertyValueFactory<>("marketId"));
        contractIdColumn.setCellValueFactory(new PropertyValueFactory<>("contractId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        buyYesColumn.setCellValueFactory(new PropertyValueFactory<>("buyYes"));
        buyNoColumn.setCellValueFactory(new PropertyValueFactory<>("buyNo"));
        sellYesColumn.setCellValueFactory(new PropertyValueFactory<>("sellYes"));
        sellNoColumn.setCellValueFactory(new PropertyValueFactory<>("sellNo"));

        allMarketsTable.getColumns().addAll(marketIdColumn, contractIdColumn, nameColumn, buyYesColumn, buyNoColumn, sellYesColumn, sellNoColumn);
        allMarketsTable.getColumns().forEach((col) -> col.setSortable(false));//table gets messed up if columns are sorted in any way and cannot return to normal.
    }

    @FXML
    void trackedMarketsButtonClicked(MouseEvent event) {
        System.out.println("trackedMarketsButtonClicked clicked");
        //change scene
    }

    @FXML
    void moversButtonClicked(MouseEvent event) {
        System.out.println("moversButtonClicked clicked");
        //change scene
    }

    @FXML
    void query_TextFieldChanged(KeyEvent event) {
        System.out.println("Query TextField changed");
        updateAllMarketsTableView();
    }

    @FXML
    void trackSelectedMarketButton(MouseEvent event) {
        System.out.println("trackSelectedMarketButton clicked");
        trackedMarkets.track(
                Integer.parseInt(allMarketsTable.getSelectionModel().getSelectedItem().getMarketId())
        );
    }

}
