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
    private final String BOOTSTRAP_SERVERS = "pi.cs.oswego.edu:26926,pi.cs.oswego.edu:26931";
    private final String USER_ID = UUID.randomUUID().toString();

    private KeysetHandle key;
    private final String associatedData;

    private UserService() throws GeneralSecurityException, IOException {
        setupProducer();
        setupConsumer();
        try {
            key = AEADEncryption.createKey();
            System.out.println("key" + key);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        this.associatedData = "Secret";
        //setupKeyConsumer();
    }

    /*
     * We want one instance of our class.
     * if we do not have one, then a new instance shall be made.
     * We also want to return an instance such that it is not being called by multiple threads
     */
    public static synchronized UserService getInstance() throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private void setupProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(properties);
    }

    private void setupConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", BOOTSTRAP_SERVERS);
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
                        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Packet received");
                    }
                }
            }
        }).start();
    }

    private void setupKeyConsumer() throws GeneralSecurityException, IOException {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("group.id", "key-distribution");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        boolean gotKey = false;

        KafkaConsumer<String, byte[]> keyConsumer = new KafkaConsumer<>(props);

        keyConsumer.subscribe(List.of("key-topic"), new ConsumerGroupListener());

        while (true) {
            ConsumerRecords<String, byte[]> records = keyConsumer.poll(100);
            for (ConsumerRecord<String, byte[]> record : records) {
                // Process the received key
                byte[] keyBytes = record.value();
                if (!Arrays.equals(keyBytes, AEADEncryption.keyToByteArray(key))) {
                    key = AEADEncryption.byteArrayToKey(keyBytes);
                    //gotKey = true;
                }
                System.out.println("Received key: " + key);
            }
            break;
        }
        keyConsumer.close();
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
            try {
                UserService service = UserService.getInstance();
                service.sendKeyToUser(service.key);
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void broadcast(byte[] packet) throws GeneralSecurityException, UnsupportedEncodingException {

        // Send message to the topic and register a callback
        List<PartitionInfo> partitions = producer.partitionsFor(TOPIC);
        // Send a message to each topic that is not the one your consumer is
        for(PartitionInfo partition : partitions) {
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
        }
    }

    private void sendKeyToUser(KeysetHandle key) throws GeneralSecurityException, IOException {
        //Send a key on the key topic, then call the method to set up key consumer
        List<PartitionInfo> partitions = producer.partitionsFor("key-topic");
        byte[] keyBytes = AEADEncryption.keyToByteArray(key);
        // Send a message to each topic that is not the one your consumer is
        for(PartitionInfo partition : partitions) {
            producer.send(new ProducerRecord<>(TOPIC, partition.partition(), USER_ID, keyBytes), (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Message sent successfully to topic: " + metadata.topic() +
                            ", partition: " + metadata.partition() +
                            ", offset: " + metadata.offset());
                } else {
                    System.err.println("Error sending message: " + exception.getMessage());
                }
            });
        }
        setupKeyConsumer();
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