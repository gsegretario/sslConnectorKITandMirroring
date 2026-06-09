package com.comau.sslConnector;

import jakarta.jms.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.transport.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    //static String jmsUrl = "failover:(ssl://stg001eu1-msgbus.3dexperience.3ds.com:61617," + "ssl://stg001eu1-msgbus-1.3dexperience.3ds.com:61617)?randomize=false&nested.soTimeout=60000&nested.soWriteTimeout=60000"; 
    		//+ "&maxReconnectAttempts=-1"; 
    		/*+ "&initialReconnectDelay=1000"
    		+ "&maxReconnectDelay=60000" 
    		+ "&useExponentialBackOff=true"  
    		+ "&nested.wireFormat.maxInactivityDuration=30000" 
    		+ "&nested.wireFormat.maxInactivityDurationInitalDelay=10000" 
    		+ "&nested.keepAlive=true" 
    		+ "&nested.soTimeout=60000" 
    		+ "&nested.soWriteTimeout=60000";*/
    
    static String jmsUrl = "failover:(ssl://eu1-msgbus.3dexperience.3ds.com:61617,ssl://eu1-msgbus-1.3dexperience.3ds.com:61617)?randomize=false&nested.soTimeout=60000&nested.soWriteTimeout=60000";
            /*+ "?randomize=false"
            + "&maxReconnectAttempts=-1"
            + "&nested.wireFormat.maxInactivityDuration=0"
            + "&nested.soTimeout=60000"
            + "&nested.soWriteTimeout=60000";*/
    //static String tenantId = "OI000446636";
    //static String tenantId = "OI000000677";
    private static final String agentId = "df01e4e4-0d70-4b69-9d95-80c7f195346b";
    private static final String agentCredentials = "[U2`}P<at2e'<BM/Ck%^d+7L";
    //static String tenantId = "OI000446636";
    static String tenantId = "OI000000677";
    //private static final String agentId = "38ce7f09-a759-4d81-b410-b2f12b7dccf5";
    //private static final String agentCredentials = "F9z73*k@y_J2~qePTx!zX1Ag";
    
    
    static String userTopicName = "3dsevents." + tenantId + ".3DSpace.user";
    static String consumerName = "BatchUser-MirrorMBOM-and-KITManagement_QUALITY"; //deve essere diverso da BatchUser-8 usato nelle Integrazioni

    public static void main(String[] args) {
    	


        while (true) {

            ActiveMQConnection connection = null;
            Session session = null;
            MessageConsumer consumer = null;
            CountDownLatch broken = new CountDownLatch(1);

            try {
            	//System.out.println(agentCredentials);
                logger.info("🔌 Creo ConnectionFactory al broker con failover...");
                ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(jmsUrl);

                // Redelivery policy per NON perdere messaggi in caso di errori transitori
                RedeliveryPolicy rp = new RedeliveryPolicy();
                rp.setMaximumRedeliveries(-1);          // infinito
                /*rp.setInitialRedeliveryDelay(1000);     // 1s
                rp.setUseExponentialBackOff(true);
                rp.setBackOffMultiplier(2.0);
                rp.setMaximumRedeliveryDelay(60000); */   // 60s max tra redelivery
                factory.setRedeliveryPolicy(rp);

                // Crea connessione ActiveMQ (non la javax generica)
                connection = (ActiveMQConnection) factory.createConnection(agentId, agentCredentials);

                // Imposta un clientID stabile (obbligatorio per durable)
                connection.setClientID(consumerName);

                // Listener di trasporto (rete su/giu)
                connection.addTransportListener(new TransportListener() {
                    @Override public void onCommand(Object command) {
                    	logger.info("ActiveMQ command: {}", command);
                    }
                    @Override public void onException(IOException error) {
                        logger.error("❌ Transport exception: {}", "connection exception");
                        broken.countDown();
                    }
                    @Override public void transportInterupted() {
                        logger.warn("⚠️ Transport interrotto (rete giù?)");
                        broken.countDown();
                    }
                    @Override public void transportResumed() {
                        //logger.info("🔄 Transport ripreso (rete su). Ricreo le risorse comunque.");
                        //broken.countDown(); // forza rebuild totale
                    	logger.info("🔄 Transport ripreso (rete su)");
                    }
                });

                // JMS-level ExceptionListener
                connection.setExceptionListener(ex -> {
                    logger.error("❌ JMS ExceptionListener: {}", ex.toString(), ex);
                    broken.countDown();
                });

                connection.start();
                logger.info("✅ Connessione JMS attiva. clientID={}", consumerName);

                // Usa CLIENT_ACKNOWLEDGE per controllare tu l'ack
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

                Topic topic = new ActiveMQTopic(userTopicName);

                // Durable subscription (nome = consumerName, deve restare identico)
                consumer = session.createDurableConsumer(topic, consumerName, null, false);

                // Il tuo listener (vedi versione corretta sotto)
                consumer.setMessageListener(new EIFConsumerMessageListener());

                logger.info("📡 In ascolto su topic={} (durable={})", userTopicName, consumerName);

                // Attendo finché rete/connessione cadono.
                // Se cade, uno dei listener sopra fa countDown() e usciamo per ricostruire tutto.
                broken.await();

            } catch (Exception e) {
                logger.error("❌ Errore nel ciclo listener: {}", e.getMessage()+"connectivity exception");
            } finally {
                // Chiudi SEMPRE tutto per evitare stati 'stale'
                safeClose(consumer);
                safeClose(session);
                safeClose(connection);
            }

            //logger.warn("🔁 Riprovo a riconnettermi tra 3 minuti...");
            //try { TimeUnit.SECONDS.sleep(180); } catch (InterruptedException ignored) {}
            logger.warn("🔁 Riprovo a riconnettermi tra 30 secondi...");
            try { TimeUnit.SECONDS.sleep(30); } catch (InterruptedException ignored) {}
        }
    }
    
    private static void safeClose(AutoCloseable c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }
    
}
