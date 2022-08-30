import ch.qos.logback.classic.util.ContextInitializer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmitLogTopic {

    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] argv) throws Exception {
        Logger logger = iNitLogs();
        ConnectionFactory factory = getConnectionFactory();
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            String routingKey = "dbc.abc";
            String message = "this is a message";
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            logger.info("Sent '" + routingKey + "':'" + message + "'");
        }
    }

    private static ConnectionFactory getConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        return factory;
    }

    private static Logger iNitLogs() {
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "config/logback.xml");
        Logger logger = LoggerFactory.getLogger("hieund.logback");
        return logger;
    }


}
