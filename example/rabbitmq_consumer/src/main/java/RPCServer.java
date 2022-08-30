import com.rabbitmq.client.*;
import ch.qos.logback.classic.util.ContextInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPCServer {

    private static final String RPC_QUEUE_NAME = "rpc_queue";

    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    public static void main(String[] argv) throws Exception {
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "config/logback.xml");
        Logger logger = LoggerFactory.getLogger("hieund.logback.rpc.server");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");


        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // create rpc_queue
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

            // only send 1 message to 1 consumer unless consumer is available
            channel.basicQos(1);

            System.out.println("Awaiting RPC requests");

            Object monitor = new Object();
            // Callback queue to receive a response
            // Queue pushing message to server is asynchronous, so we provide a callback in the form of an object
            // that will buffer the messages when server is ready to use
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                // read a response message from callback queue
                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    int n = Integer.parseInt(message);

                    logger.info("tinh ham fib(" + message + ")");
                    response += fib(n);
                } catch (RuntimeException e) {
                    logger.error("." + e.toString());
                } finally {
                    // publish fib result to default queue
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
            // Wait and be prepared to consume the message from RPC client.
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}