package cc.blynk.server;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.common.utils.PropertiesUtil;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.utils.JsonParser;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.server.workers.timer.TimerWorker;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/16/2015.
 */
public class Launcher {

    private static final Logger log = LogManager.getLogger(Launcher.class);

    public static void main(String[] args) throws Exception {
        //just to init mapper on server start and not first access
        JsonParser.check();

        // create Options object
        Options options = new Options();
        options.addOption("port", true, "Server port.");
        options.addOption("sslPort", true, "Server SSL port.");
        CommandLine cmd = new BasicParser().parse(options, args);

        String portString = cmd.getOptionValue("port");
        String sslPortString = cmd.getOptionValue("sslPort");

        Properties serverProperties = PropertiesUtil.loadProperties("server.properties");

        if (portString != null) {
            ParseUtil.parseInt(portString);
            serverProperties.put("server.default.port", portString);
        }
        if (sslPortString != null) {
            ParseUtil.parseInt(sslPortString);
            serverProperties.put("server.ssl.port", sslPortString);
        }

        boolean sslEnabled = PropertiesUtil.getBoolProperty(serverProperties, "app.ssl.enabled");


        FileManager fileManager = new FileManager(serverProperties.getProperty("data.folder"));
        SessionsHolder sessionsHolder = new SessionsHolder();

        log.debug("Reading user DB.");
        UserRegistry userRegistry = new UserRegistry(fileManager.deserialize());
        log.debug("Reading user DB finished.");
        GlobalStats stats = new GlobalStats();

        new TimerWorker(userRegistry, sessionsHolder).start();
        new ProfileSaverWorker(userRegistry, fileManager, PropertiesUtil.getIntProperty(serverProperties, "profile.save.worker.period"), stats).start();


        if (sslEnabled) {
            log.info("SSL for app. enabled.");
            new Thread(new SSLServer(serverProperties, fileManager, sessionsHolder, userRegistry, stats)).start();
            new Thread(new Server(serverProperties, fileManager, sessionsHolder, userRegistry, stats)).start();
        } else {
            new Thread(new Server(serverProperties, fileManager, sessionsHolder, userRegistry, stats)).start();
        }
    }

}