package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class BracketController implements Initializable {

    @FXML private AnchorPane bracketPane;
    @FXML private VBox teamPool;
    @FXML private GridPane resultsGrid;

    private final List<List<VBox>> slotsByRound = new ArrayList<>();
    private final List<VBox> round0Slots = new ArrayList<>();
    private int nextSeedIndex = 0;
    private int numRounds;
    private final int numTeams = 16;

    private static final double TEAM_WIDTH = 190;
    private static final double TEAM_HEIGHT = 50;
    private static final double H_GAP = 115;
    private static final double LEAF_SPACING = 72;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        drawBracket(numTeams);
        populateTeamPool();
        initResultsGrid();
    }

    private void drawBracket(int participants) {
        bracketPane.getChildren().clear();
        slotsByRound.clear();
        round0Slots.clear();
        nextSeedIndex = 0;

        int rounds = (int) (Math.log(participants) / Math.log(2)) + 1;
        numRounds = rounds;

        for (int r = 0; r < rounds; r++) {
            slotsByRound.add(new ArrayList<>());
        }

        double x = 100;
        double yStart = 120;
        double spacing = LEAF_SPACING;

        for (int round = 0; round < rounds; round++) {
            int slotsCount = participants / (int) Math.pow(2, round);
            double colX = x + round * (TEAM_WIDTH + H_GAP);

            for (int i = 0; i < slotsCount; i++) {
                double y = yStart + i * spacing;

                StackPane stack = new StackPane();
                stack.setPrefSize(TEAM_WIDTH, TEAM_HEIGHT + 10);

                Label label = new Label("—");
                label.setPrefSize(TEAM_WIDTH, TEAM_HEIGHT);
                label.setAlignment(Pos.CENTER);
                label.setStyle("""
                    -fx-background-color: #ffffff;
                    -fx-border-color: #546e7a;
                    -fx-border-width: 2;
                    -fx-border-radius: 6;
                    -fx-background-radius: 6;
                    -fx-font-size: 15;
                    -fx-font-weight: bold;
                    -fx-text-fill: #263238;
                    """);

                stack.getChildren().add(label);

                VBox slot = new VBox(stack);
                slot.setAlignment(Pos.CENTER);
                slot.setUserData(new SlotData(round, i, label));

                // Обработчик клика вешается сразу при создании
                slot.setOnMouseClicked(ev -> advanceFromSlot(slot));

                AnchorPane.setLeftAnchor(slot, colX);
                AnchorPane.setTopAnchor(slot, y);
                bracketPane.getChildren().add(slot);

                slotsByRound.get(round).add(slot);
                if (round == 0) round0Slots.add(slot);

                // Линии
                if (round < rounds - 1) {
                    double centerY = y + TEAM_HEIGHT / 2.0;
                    double rightX = colX + TEAM_WIDTH + H_GAP / 2.0;

                    Line h = new Line(colX + TEAM_WIDTH, centerY, rightX, centerY);
                    h.setStroke(Color.rgb(80, 80, 100));
                    h.setStrokeWidth(2.5);
                    bracketPane.getChildren().add(h);

                    if (i % 2 == 0 && i + 1 < slotsCount) {
                        double nextCenterY = yStart + (i + 1) * spacing + TEAM_HEIGHT / 2.0;
                        Line v = new Line(rightX, centerY, rightX, nextCenterY);
                        v.setStroke(Color.rgb(80, 80, 100));
                        v.setStrokeWidth(2.5);
                        bracketPane.getChildren().add(v);

                        Line mid = new Line(rightX, (centerY + nextCenterY)/2, rightX + H_GAP/2, (centerY + nextCenterY)/2);
                        mid.setStroke(Color.rgb(80, 80, 100));
                        mid.setStrokeWidth(2.5);
                        bracketPane.getChildren().add(mid);
                    }
                }
            }

            yStart += spacing / 2.0;
            spacing *= 2;
        }
    }

    private void advanceFromSlot(VBox slot) {
        SlotData data = (SlotData) slot.getUserData();
        if (data.round >= numRounds - 1) return;

        Label label = data.label;
        String name = label.getText();
        if ("—".equals(name)) return;

        int nextRound = data.round + 1;
        int nextIndex = data.index / 2;

        VBox nextSlot = slotsByRound.get(nextRound).get(nextIndex);
        SlotData nextData = (SlotData) nextSlot.getUserData();
        nextData.label.setText(name);

        animateAdvance(nextSlot);
        updateResultsGrid(name, data.round, nextRound);
    }

    private void animateAdvance(VBox slot) {
        StackPane stack = (StackPane) slot.getChildren().get(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(500), stack);
        scale.setToX(1.18);
        scale.setToY(1.18);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);

        FadeTransition fade = new FadeTransition(Duration.millis(300), stack);
        fade.setFromValue(1.0);
        fade.setToValue(0.7);
        fade.setAutoReverse(true);
        fade.setCycleCount(2);

        scale.play();
        fade.play();
    }

    private void populateTeamPool() {
        teamPool.getChildren().removeIf(node -> node instanceof Label && !((Label) node).getText().startsWith("Команды"));

        List<Team> teams = generateExampleTeams(numTeams);
        Collections.shuffle(teams);

        for (Team t : teams) {
            Label l = new Label(t.getName());
            l.setPrefWidth(220);
            l.setPrefHeight(50);
            l.setAlignment(Pos.CENTER);
            l.setStyle("""
                -fx-background-color: #e3f2fd;
                -fx-border-color: #1976d2;
                -fx-border-width: 1.5;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-font-size: 15;
                -fx-font-weight: bold;
                -fx-padding: 8;
                """);

            l.setOnMouseClicked(e -> {
                if (nextSeedIndex < round0Slots.size()) {
                    placeTeam(t.getName(), l);
                }
            });

            teamPool.getChildren().add(l);
        }
    }

    private void placeTeam(String name, Label sourceLabel) {
        VBox target = round0Slots.get(nextSeedIndex);
        SlotData data = (SlotData) target.getUserData();
        data.label.setText(name);

        nextSeedIndex++;
        teamPool.getChildren().remove(sourceLabel);
        // НЕ перевешиваем обработчик — он уже есть!
    }

    private void initResultsGrid() {
        resultsGrid.getChildren().clear();

        Label header1 = new Label("Команда");
        Label header2 = new Label("Достигла");
        header1.setStyle("-fx-font-weight:bold; -fx-padding:6;");
        header2.setStyle("-fx-font-weight:bold; -fx-padding:6;");

        resultsGrid.add(header1, 0, 0);
        resultsGrid.add(header2, 1, 0);
    }

    private void updateResultsGrid(String teamName, int fromRound, int toRound) {
        int row = resultsGrid.getRowCount();

        Label team = new Label(teamName);
        Label stage = new Label(getStageName(toRound));

        team.setStyle("-fx-padding:6;");
        stage.setStyle("-fx-padding:6;");

        resultsGrid.add(team, 0, row);
        resultsGrid.add(stage, 1, row);
    }

    private String getStageName(int round) {
        return switch (round) {
            case 1 -> "1/8 финала";
            case 2 -> "1/4 финала";
            case 3 -> "Полуфинал";
            case 4 -> "Финал";
            default -> "Раунд " + (round + 1);
        };
    }

    @FXML
    private void resetBracket() {
        drawBracket(numTeams);
        populateTeamPool();
        initResultsGrid();
    }

    @FXML
    private void randomSeed() {
        resetBracket();
        List<Label> poolLabels = new ArrayList<>();
        for (int i = 1; i < teamPool.getChildren().size(); i++) {
            if (teamPool.getChildren().get(i) instanceof Label lbl) {
                poolLabels.add(lbl);
            }
        }
        Collections.shuffle(poolLabels);
        for (Label lbl : poolLabels) {
            if (nextSeedIndex < round0Slots.size()) {
                placeTeam(lbl.getText(), lbl);
            }
        }
    }

    private List<Team> generateExampleTeams(int count) {
        List<Team> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new Team("Команда " + i));
        }

        return list;
    }

    private static class SlotData {
        final int round;
        final int index;
        final Label label;

        SlotData(int round, int index, Label label) {
            this.round = round;
            this.index = index;
            this.label = label;
        }
    }
}