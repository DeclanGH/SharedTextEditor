package io.github.declangh.sharedtexteditor;

import com.google.crypto.tink.KeysetHandle;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.*;

public class UserService {

    private static UserService instance;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;
    private final String TOPIC = "SharedTextEditor";
    public final String USER_ID = UUID.randomUUID().toString();

    private KeysetHandle key;
    private String associatedData;

    private UserService(){
        setupProducer();
        setupConsumer();
        try {
            key = AEADEncryption.createKey();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        setupKeyConsumer();

        this.associatedData = "Secret";
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
        properties.put("bootstrap.servers", "pi.cs.oswego.edu:26921");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);
    }

    private void setupConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "pi.cs.oswego.edu:26921");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "1");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Arrays.asList(TOPIC), new ConsumerGroupListener());

        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, byte[]> record : records) {
                    if (!USER_ID.equals(record.key())) {
                        byte[] encryptedPacket = record.value();
                        try {
                            byte[] packet = AEADEncryption.decrypt(encryptedPacket, associatedData, key);
                            EditorClient.receivePacket(packet);
                            System.out.println("Packet received");
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        } catch (GeneralSecurityException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }).start();
    }

    private void setupKeyConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "key-distribution");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        boolean gotKey = false;

        try (KafkaConsumer<String, String> keyConsumer = new KafkaConsumer<>(props)) {
            keyConsumer.subscribe(Collections.singletonList("key-topic"));

            while (!gotKey) {
                ConsumerRecords<String, String> records = keyConsumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    // Process the received key
                    byte[] keyBytes = record.value().getBytes();
                    if (!Arrays.equals(keyBytes, AEADEncryption.keyToByteArray(key))) {
                        key = AEADEncryption.byteArrayToKey(keyBytes);
                        gotKey = true;
                    }
                    System.out.println("Received key: " + key);
                }
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
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

        // Send message to the topic and register a callback
        List<PartitionInfo> partitions = producer.partitionsFor(TOPIC);
        // Send a message to each topic that is not the one your consumer is
        for(PartitionInfo partition : partitions) {
            try {
                byte[] encryptedPacket = AEADEncryption.encrypt(packet, associatedData, key);

                producer.send(new ProducerRecord<>(TOPIC, partition.partition(), USER_ID, encryptedPacket), (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Message sent successfully to topic: " + metadata.topic() +
                            ", partition: " + metadata.partition() +
                            ", offset: " + metadata.offset());
                    } else {
                        System.err.println("Error sending message: " + exception.getMessage());
                    }
                });
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
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