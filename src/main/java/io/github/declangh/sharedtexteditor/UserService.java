package io.github.declangh.sharedtexteditor;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;

public class UserService {

    private static UserService instance;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;
    private final String TOPIC = "SharedTextEditor1";
    public final String USER_ID = UUID.randomUUID().toString();

    private final String GROUP_ID = String.valueOf(new Random().nextInt(20) + 1);

    // These will change upon instantiating UserService
    public long modValue = 1;
    BigInteger publicKey = BigInteger.valueOf(1);


    private UserService(){
        setupProducer();
        setupConsumer();
        setupEnvironment();
    }

    /*
     * We want one instance of our class.
     * if we do not have one, then a new instance shall be made.
     * We also want to return an instance such that it is not being called by multiple threads
     */
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private void setupProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "pi.cs.oswego.edu:26926");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);
    }

    private void setupConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "pi.cs.oswego.edu:26926");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Arrays.asList(TOPIC), new ConsumerGroupListener());

        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, byte[]> record : records) {
                    byte[] packet = record.value();
                    EditorClient.receivePacket(packet);
                }
            }
        }).start();
    }

    private void setupEnvironment() {
        Map<String, String> env = System.getenv();
        long seed = Long.parseLong(env.get("SEED"));
        modValue = SimpleSecurity.generateRandomNumber(seed);
        publicKey = BigInteger.valueOf(Long.parseLong(env.get("PUBLIC_KEY")));
    }

    // This class is used to implement a listener for when users join the group
    static class ConsumerGroupListener implements ConsumerRebalanceListener{
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions){
            System.out.println("USER HAS LEFT THE SESSION");
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            System.out.println("USER HAS JOINED THE SESSION");
        }
    }

    public void broadcast(byte[] packet) {

        // Send a message to each topic that is not the one your consumer is
        producer.send(new ProducerRecord<>(TOPIC, packet), (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent successfully to topic: " + metadata.topic() +
                        ", partition: " + metadata.partition() +
                        ", offset: " + metadata.offset());
            } else {
                System.err.println("Error sending message: " + exception.getMessage());
            }
        });
    }

    // This method is called when the editor client class closed
    public synchronized void close(){
        System.out.println("Closing producer and consumers");
        if(producer != null){
            producer.close();
        }
        if(consumer != null){
            synchronized (consumer) {
                consumer.close();
            }
        }
    }
}