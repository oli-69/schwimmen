package schwimmen;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import schwimmen.SchwimmenGame.GAMERULE;
import schwimmen.ui.SchwimmenFrame;

/**
 * This class represents the SchwimmenServer. It implements the static main
 * entry point to start the application.
 */
public class SchwimmenServer {

    static {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.DEBUG);
    }
    private static final Logger LOGGER = LogManager.getLogger(SchwimmenServer.class);

    /**
     * Static main entry point.
     *
     * @param args the command line arguments. Accepts one argument which is an
     * alternate config path.
     * @throws java.lang.Exception in case of an Exception.
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting http server");
        String configPath = System.getProperty("user.dir");
        if (args.length > 0) {
            // try absolute path
            configPath = args[0];
            if (!new File(configPath + File.separator + "settings.properties").exists()) {
                // try relative path
                configPath = System.getProperty("user.dir") + File.separator + configPath + File.separator;
                if (!new File(configPath + File.separator + "settings.properties").exists()) {
                    LOGGER.error("No settings found in config path " + args[0]);
                }
            }
        }
        LOGGER.info("Using config path: " + configPath);
        Properties settings = new Properties();
        settings.load(new FileInputStream(configPath + File.separator + "settings.properties"));
        int port = Integer.parseInt(settings.getProperty("serverPort"));
        LOGGER.info("using port " + port);
        String confName = settings.getProperty("jitsiConference");
        LOGGER.info("using conference name '" + confName + "'");
//        confName = confName  + (System.currentTimeMillis() / 1000); // currently disabled, since Jitsi's iOS-App doesn't take the room name from the url.
        SchwimmenGame game = new SchwimmenGame(confName);
        Server httpServer = new Server(port);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setWelcomeFiles(new String[]{"index.html"});

        WebSocketUpgradeFilter filter = WebSocketUpgradeFilter.configureContext(context);
        filter.addMapping(new ServletPathSpec("/schwimmen/ws/*"), new GameWebSocketCreator(game, configPath));
        // add your own non-websocket servlets
//        context.addServlet(HelloServlet.class,"/hello");

        // Lastly, the default servlet for root content (serves up static content)
        // It is important that this is last.
        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
        holderPwd.setInitParameter("resourceBase", System.getProperty("user.dir").concat("/html"));
        holderPwd.setInitParameter("dirAllowed", "false");
        context.addServlet(holderPwd, "/");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{context, new DefaultHandler()});
        httpServer.setHandler(handlers);
        httpServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (httpServer.isStarted()) {
                try {
                    httpServer.stop();
                    LOGGER.info("Server stopped successfully.");
                } catch (Exception exception) {
                    LOGGER.error(exception);
                }
            }
        }, "Stop HttpServer Hook"));
        new PingWatchdog(game).start();

        game.setWebRadioPlaying(Boolean.parseBoolean(settings.getProperty("webradioEnabled", "false")));
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, Boolean.parseBoolean(settings.getProperty("rule789Enabled", "true")));
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, Boolean.parseBoolean(settings.getProperty("rulePassOnceEnabled", "true")));
        game.setGameRuleEnabled(GAMERULE.Knocking, Boolean.parseBoolean(settings.getProperty("ruleKnockingEnabled", "false")));
        if (Boolean.parseBoolean(settings.getProperty("startUI", "true"))) {
            installLookAndFeel();
            SwingUtilities.invokeLater(() -> new SchwimmenFrame(game).setVisible(true));
        }
    }

    private static void installLookAndFeel() {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            LOGGER.error(ex);
        }
    }

    private static class GameWebSocketCreator implements WebSocketCreator {

        private final SchwimmenGame game;
        private final String configPath;

        public GameWebSocketCreator(SchwimmenGame game, String configPath) {
            this.game = game;
            this.configPath = configPath;
        }

        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            return new SchwimmenSocket(game, configPath);
        }
    }

    private static final class PingWatchdog {

        private final SchwimmenGame game;
        private final long interval = 1000 * 60 * 3;
        private final Timer timer;

        public PingWatchdog(SchwimmenGame game) {
            this.game = game;
            timer = new Timer("ServerWatchdog");
        }

        public void start() {
            timer.schedule(getTask(), interval);
        }

        private TimerTask getTask() {
            return new TimerTask() {
                @Override
                public void run() {
                    game.sendPing();
                    timer.schedule(getTask(), interval);
                }
            };
        }
    }
}
