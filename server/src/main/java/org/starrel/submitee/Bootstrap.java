package org.starrel.submitee;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.net.InetSocketAddress;

public class Bootstrap {
    public static void main(String[] args) {
        OptionSpec<String> mongoConnectionString, mongoDatabase, listen;

        OptionParser optionParser = new OptionParser();
        mongoConnectionString = optionParser.accepts("mongo-uri",
                "connection string of mongodb, example: mongodb://host1:27017")
                .withRequiredArg()
                .ofType(String.class);
        mongoDatabase = optionParser.accepts("mongo-database",
                "database of mongodb, default value is \"submitee\"")
                .withOptionalArg().defaultsTo("submitee")
                .ofType(String.class);
        listen = optionParser.accepts("listen",
                "http server listen address and port, defaults to 0.0.0.0:8443")
                .withOptionalArg().defaultsTo("0.0.0.0:8443")
                .ofType(String.class);

        OptionSet parse = optionParser.parse(args);

        MongoClient client = MongoClients.create(mongoConnectionString.value(parse));
        MongoDatabase database = client.getDatabase(mongoDatabase.value(parse));

        InetSocketAddress listenAddress;
        try {
            listenAddress = parseListenAddress(listen.value(parse));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        SubmiteeServer server = new SubmiteeServer(database, listenAddress);
        try {
            server.start();
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        try {
            server.join();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static InetSocketAddress parseListenAddress(String listen) {
        if (listen.indexOf(":") != listen.indexOf(":")) {
            throw new IllegalArgumentException("invalid listen address: " + listen);
        }
        String host;
        int port = 8443;
        if (!listen.contains(":")) {
            host = listen.substring(0, listen.indexOf(":"));
            port = Integer.parseInt(listen.substring(listen.indexOf(":") + 1));
        } else {
            host = listen;
        }

        return new InetSocketAddress(host, port);
    }
}
