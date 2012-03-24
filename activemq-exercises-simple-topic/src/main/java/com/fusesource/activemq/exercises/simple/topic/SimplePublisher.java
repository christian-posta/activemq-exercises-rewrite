package com.fusesource.activemq.exercises.simple.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Created by IntelliJ IDEA.
 * User: cposta
 * Date: 3/23/12
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimplePublisher {

    private static final Log LOG = LogFactory.getLog(SimplePublisher.class);

    private static final Boolean NON_TRANSACTED = false;
    private static final int MESSAGE_DELAY_MILLISECONDS = 100;
    private static final int NUM_MESSAGES_TO_BE_SENT = 100;
    private static final String CONNECTION_FACTORY_NAME = "myJmsFactory";
    private static final String DESTINATION_NAME = "topic/simple";

    public static void main(String[] args) {
        Connection connection = null;
        try {
            Context context = new InitialContext();
            ConnectionFactory factory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_NAME);
            Destination destination = (Destination) context.lookup(DESTINATION_NAME);

            connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);

            for (int i = 1; i <= (NUM_MESSAGES_TO_BE_SENT / 10); i++) {
                for (int j = 1; j <= 10; j++) {
                    TextMessage message = session.createTextMessage(j + ". message sent");
                    LOG.info("Sending to destination: " + destination.toString() + " this text: '" + message.getText() + "'");
                    producer.send(message);
                    Thread.sleep(MESSAGE_DELAY_MILLISECONDS);
                }
                LOG.info("Send the Report Message");
                producer.send(session.createTextMessage("REPORT"));

            }

            LOG.info("Send the shutdown message");
            producer.send(session.createTextMessage("SHUTDOWN"));

            producer.close();
            session.close();

        } catch (Exception e) {
            LOG.error(e);
        }
        finally {
            // got to clean up the connections and other resources!
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    LOG.error(e);
                }
            }
        }
    }
}
