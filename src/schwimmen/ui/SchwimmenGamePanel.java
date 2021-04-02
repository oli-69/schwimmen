package schwimmen.ui;

import cardgame.CardGame;
import cardgame.Player;
import cardgame.messages.WebradioUrl;
import cardgame.ui.GamePanel;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import schwimmen.SchwimmenGame;
import schwimmen.SchwimmenGame.GAMEPHASE;
import schwimmen.SchwimmenGame.GAMERULE;

/**
 * Implements the main panel for the server side GUI. Created with NetBeans GUI
 * Editor.
 */
public class SchwimmenGamePanel extends GamePanel {

    private static final Logger LOGGER = LogManager.getLogger(SchwimmenGamePanel.class);
    private static final long serialVersionUID = 1L;

    DefaultListModel<Player> playerListModel;
    SchwimmenGame game;

    public SchwimmenGamePanel() {
        this(new SchwimmenGame());
    }

    public SchwimmenGamePanel(SchwimmenGame game) {
        this.game = game;
        initComponents();
        this.game.addPropertyChangeListener(this::gamePropertyChanged);
        playerList.setCellRenderer(new PlayerRenderer());
        cbWebradioPlaying.setSelected(game.isWebradioPlaying());
        cbRule789.setSelected(game.isGameRuleEnabled(GAMERULE.newCardsOn789));
        cbRulePassOnce.setSelected(game.isGameRuleEnabled(GAMERULE.passOnlyOncePerRound));
        cbRuleKnocking.setSelected(game.isGameRuleEnabled(GAMERULE.Knocking));
    }

    @Override
    public CardGame getGame() {
        return game;
    }

    private ListModel<Player> getListPlayerListModel() {
        if (playerListModel == null) {
            playerListModel = new DefaultListModel<>();
        }
        return playerListModel;
    }

    private ListCellRenderer<WebradioUrl> getRadioCBModelRenderer() {
        final BasicComboBoxRenderer delegateRenderer = new BasicComboBoxRenderer();
        return (JList<? extends WebradioUrl> list, WebradioUrl value, int index, boolean isSelected, boolean cellHasFocus) -> {
            return delegateRenderer.getListCellRendererComponent(
                    list, (value != null ? value.name : value), index, isSelected, cellHasFocus);
        };
    }

    private ComboBoxModel<WebradioUrl> getRadioCBModel() {
        List<WebradioUrl> radioList = game.getRadioList();
        return new DefaultComboBoxModel<>(radioList.toArray(new WebradioUrl[radioList.size()]));
    }

    private void gamePropertyChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case CardGame.PROP_PLAYERLIST:
                initPlayerList();
                break;
            case CardGame.PROP_GAMEPHASE:
                initGamePhase((GAMEPHASE) evt.getNewValue());
                break;
            case CardGame.PROP_PLAYER_ONLINE:
            case CardGame.PROP_ATTENDEESLIST:
                playerList.repaint();
                checkAttendeesCount();
            case CardGame.PROP_WEBRADIO_URL:
                cbRadio.setSelectedItem(game.getRadioUrl());
            default:
                break;
        }
    }

    private void initPlayerList() {
        SwingUtilities.invokeLater(() -> {
            List<Player> players = game.getPlayerList();
            playerListModel.clear();
            players.forEach(player -> {
                playerListModel.addElement(player);
            });
            checkAttendeesCount();
        });
    }

    private void checkAttendeesCount() {
        boolean enabled = game.getAttendeesCount() > 0 && game.getGamePhase() == GAMEPHASE.waitForAttendees;
        startGameBtn.setEnabled(enabled);
        shufflePlayerBtn.setEnabled(enabled);
    }

    private void initGamePhase(GAMEPHASE phase) {
        boolean changeRuleAllowd = phase == GAMEPHASE.waitForAttendees || phase == GAMEPHASE.shuffle || phase == GAMEPHASE.discover;
        cbRule789.setEnabled(changeRuleAllowd);
        cbRulePassOnce.setEnabled(changeRuleAllowd);
        cbRuleKnocking.setEnabled(changeRuleAllowd);
        startGameBtn.setEnabled(phase == GAMEPHASE.waitForAttendees);
        stopGameBtn.setEnabled(phase != GAMEPHASE.waitForAttendees);
        shufflePlayerBtn.setEnabled(phase == GAMEPHASE.waitForAttendees);
    }

    private class PlayerRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Player player = (Player) value;
            String text = player.getName().concat(String.format(" - %s", (game.isAttendee(player) ? "spielt mit" : "spielt nicht mit")));
            Component renderer = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            renderer.setEnabled(player.isOnline());
            return renderer;
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        bgWebRario = new javax.swing.ButtonGroup();
        pmPlayers = new javax.swing.JPopupMenu();
        miRemoveFromAttendees = new javax.swing.JMenuItem();
        miKick = new javax.swing.JMenuItem();
        bgRule789 = new javax.swing.ButtonGroup();
        bgRulePassOnce = new javax.swing.ButtonGroup();
        playerListScrollPanel = new javax.swing.JScrollPane();
        playerList = new javax.swing.JList<>();
        southPanel = new javax.swing.JPanel();
        buttonPanel = new javax.swing.JPanel();
        startGameBtn = new javax.swing.JButton();
        stopGameBtn = new javax.swing.JButton();
        shufflePlayerBtn = new javax.swing.JButton();
        settingsPanel = new javax.swing.JPanel();
        cbRule789 = new javax.swing.JCheckBox();
        cbRulePassOnce = new javax.swing.JCheckBox();
        cbRuleKnocking = new javax.swing.JCheckBox();
        cbWebradioPlaying = new javax.swing.JCheckBox();
        cbRadio = new javax.swing.JComboBox<>();
        layoutDummy = new javax.swing.JPanel();

        miRemoveFromAttendees.setText("Remove from Attendees");
        miRemoveFromAttendees.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRemoveFromAttendeesActionPerformed(evt);
            }
        });
        pmPlayers.add(miRemoveFromAttendees);

        miKick.setText("Kick");
        miKick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miKickActionPerformed(evt);
            }
        });
        pmPlayers.add(miKick);

        setLayout(new java.awt.BorderLayout());

        playerList.setModel(getListPlayerListModel());
        playerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        playerList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                playerListMouseClicked(evt);
            }
        });
        playerListScrollPanel.setViewportView(playerList);

        add(playerListScrollPanel, java.awt.BorderLayout.CENTER);

        buttonPanel.setLayout(new java.awt.GridLayout(1, 0));

        startGameBtn.setText("Start Game");
        startGameBtn.setEnabled(false);
        startGameBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startGameBtnActionPerformed(evt);
            }
        });
        buttonPanel.add(startGameBtn);

        stopGameBtn.setText("Stop Game");
        stopGameBtn.setEnabled(false);
        stopGameBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopGameBtnActionPerformed(evt);
            }
        });
        buttonPanel.add(stopGameBtn);

        shufflePlayerBtn.setText("Spieler umsetzen");
        shufflePlayerBtn.setEnabled(false);
        shufflePlayerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shufflePlayerBtnActionPerformed(evt);
            }
        });
        buttonPanel.add(shufflePlayerBtn);

        southPanel.add(buttonPanel);

        add(southPanel, java.awt.BorderLayout.PAGE_END);

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));
        settingsPanel.setLayout(new java.awt.GridBagLayout());

        cbRule789.setText("Rule 789");
        cbRule789.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbRule789ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(cbRule789, gridBagConstraints);

        cbRulePassOnce.setText("Rule Pass-Once");
        cbRulePassOnce.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbRulePassOnceActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(cbRulePassOnce, gridBagConstraints);

        cbRuleKnocking.setText("Rule Knocking");
        cbRuleKnocking.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbRuleKnockingActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(cbRuleKnocking, gridBagConstraints);

        cbWebradioPlaying.setText("Webradio");
        cbWebradioPlaying.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbWebradioPlayingActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(cbWebradioPlaying, gridBagConstraints);

        cbRadio.setModel(getRadioCBModel());
        cbRadio.setRenderer(getRadioCBModelRenderer());
        cbRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbRadioItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(cbRadio, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        settingsPanel.add(layoutDummy, gridBagConstraints);

        add(settingsPanel, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void startGameBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startGameBtnActionPerformed
        if (game.getGamePhase() == SchwimmenGame.GAMEPHASE.waitForAttendees) {
            game.startGame();
        }
    }//GEN-LAST:event_startGameBtnActionPerformed

    private void stopGameBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopGameBtnActionPerformed
        if (game.getGamePhase() != SchwimmenGame.GAMEPHASE.waitForAttendees) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                    this, "Soll das Spiel wirklich abgebrochen werden?", "Spiel abbrechen", JOptionPane.YES_NO_OPTION)) {
                game.stopGame();
            }
        }
    }//GEN-LAST:event_stopGameBtnActionPerformed

    private void playerListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playerListMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            Player selectedPlayer = playerList.getSelectedValue();
            if (selectedPlayer != null) {
                boolean isWaitForAttendees = game.getGamePhase() == GAMEPHASE.waitForAttendees;
                boolean isAttendee = game.isAttendee(selectedPlayer);
                miRemoveFromAttendees.setEnabled(isWaitForAttendees && isAttendee);
                pmPlayers.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_playerListMouseClicked

    private void miRemoveFromAttendeesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRemoveFromAttendeesActionPerformed
        game.removeAttendee(playerList.getSelectedValue());
    }//GEN-LAST:event_miRemoveFromAttendeesActionPerformed

    private void miKickActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miKickActionPerformed
        playerList.getSelectedValue().getSocket().close();
    }//GEN-LAST:event_miKickActionPerformed

    private void cbWebradioPlayingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbWebradioPlayingActionPerformed
        game.setWebRadioPlaying(cbWebradioPlaying.isSelected());
    }//GEN-LAST:event_cbWebradioPlayingActionPerformed

    private void cbRule789ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbRule789ActionPerformed
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, cbRule789.isSelected());
    }//GEN-LAST:event_cbRule789ActionPerformed

    private void cbRulePassOnceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbRulePassOnceActionPerformed
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, cbRulePassOnce.isSelected());
    }//GEN-LAST:event_cbRulePassOnceActionPerformed

    private void cbRuleKnockingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbRuleKnockingActionPerformed
        game.setGameRuleEnabled(GAMERULE.Knocking, cbRuleKnocking.isSelected());
    }//GEN-LAST:event_cbRuleKnockingActionPerformed

    private void cbRadioItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbRadioItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            game.setRadioUrl((WebradioUrl) cbRadio.getSelectedItem());
        }
    }//GEN-LAST:event_cbRadioItemStateChanged

    private void shufflePlayerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shufflePlayerBtnActionPerformed
        if (game.getGamePhase() == GAMEPHASE.waitForAttendees) {
            game.shufflePlayers();
        }
    }//GEN-LAST:event_shufflePlayerBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgRule789;
    private javax.swing.ButtonGroup bgRulePassOnce;
    private javax.swing.ButtonGroup bgWebRario;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JComboBox<WebradioUrl> cbRadio;
    private javax.swing.JCheckBox cbRule789;
    private javax.swing.JCheckBox cbRuleKnocking;
    private javax.swing.JCheckBox cbRulePassOnce;
    private javax.swing.JCheckBox cbWebradioPlaying;
    private javax.swing.JPanel layoutDummy;
    private javax.swing.JMenuItem miKick;
    private javax.swing.JMenuItem miRemoveFromAttendees;
    private javax.swing.JList<Player> playerList;
    private javax.swing.JScrollPane playerListScrollPanel;
    private javax.swing.JPopupMenu pmPlayers;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JButton shufflePlayerBtn;
    private javax.swing.JPanel southPanel;
    private javax.swing.JButton startGameBtn;
    private javax.swing.JButton stopGameBtn;
    // End of variables declaration//GEN-END:variables
}
