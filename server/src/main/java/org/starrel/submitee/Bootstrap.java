package org.starrel.submitee;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class Bootstrap {
    public static void main(String[] args) {
        OptionSpec<String> mongoConnectionString, mongoDatabase, jdbcConnectionString, listen;

        OptionParser optionParser = new OptionParser();
        mongoConnectionString = optionParser.accepts("mongo-url",
                "connection string of mongodb, example: mongodb://localhost:27017")
                .withRequiredArg()
                .ofType(String.class);
        mongoDatabase = optionParser.accepts("mongo-database",
                "database of mongodb, defaults to \"submitee\"")
                .withOptionalArg().defaultsTo("submitee")
                .ofType(String.class);
        jdbcConnectionString = optionParser.accepts("jdbc-url",
                "connection string of jdbc, example: mysql://localhost/submitee")
                .withRequiredArg()
                .ofType(String.class);
        listen = optionParser.accepts("listen",
                "http server listen address and port, defaults to 0.0.0.0:8080, accepts multiple values")
                .withOptionalArg().defaultsTo("0.0.0.0:8080")
                .ofType(String.class);

        OptionSet parse;
        try {
            parse = optionParser.parse(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        if (!parse.has(mongoConnectionString) || !parse.has(jdbcConnectionString)) {
            System.err.println("mongo-url and jdbc-url are required parameters");
            try {
                optionParser.printHelpOn(System.err);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(-1);
            return;
        }

        MongoClient client = MongoClients.create(mongoConnectionString.value(parse));
        MongoDatabase database = client.getDatabase(mongoDatabase.value(parse));

        List<InetSocketAddress> listenAddresses = new LinkedList<>();
        try {
            for (String listenString : listen.values(parse)) {
                listenAddresses.add(parseListenAddress(listenString));
            }
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(Bootstrap.class).error("failed initializing listen addresses: " + e.getMessage());
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcConnectionString.value(parse));
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        SubmiteeServer server;
        try {
            server = new SubmiteeServer(database, dataSource, listenAddresses.toArray(new InetSocketAddress[0]));
        } catch (IOException e) {
            LoggerFactory.getLogger(Bootstrap.class).error("failed initializing server", e);
            System.exit(-1);
            return;
        }
        try {
            server.start();
            server.join();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static InetSocketAddress parseListenAddress(String listen) {
        if (listen.indexOf(":") != listen.lastIndexOf(":")) {
            throw new IllegalArgumentException("invalid listen address: " + listen);
        }
        String host;
        int port = 8443;
        if (listen.contains(":")) {
            host = listen.substring(0, listen.indexOf(":"));
            port = Integer.parseInt(listen.substring(listen.indexOf(":") + 1));
        } else {
            host = listen;
        }

        return new InetSocketAddress(host, port);
    }
}
