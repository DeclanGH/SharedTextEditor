# Shared Text Editor
This is a java application that lets users that are connected collaborate on a single text editor (just like google docs).

## Introduction
This software was made to showcase our knowledge of secure consistency/consensus protocols, by creating a group-based application. 
To accomplish this, our team made use of:
- Java Swing Toolkit: This was used to create the Graphical User Interface, as well as listen to changes and update accordingly.
- A Packet Class: This contained operations that served as packet headers for our various packets functions. These packets were necessary for holding information on updates, key exchange, user IDs and so on.
- Kafka Zookeper: This was used as our consensus protocol, and was responsible for managing connected users, sending packets and receiving packets.
- Encryption: This was used to ensure that packets sent were secure. We made use of diffie-hellman key exchange to generate keys and a simple bit XOR for encryption/decryption. We initially planned on using Google Tink for security, but fell back to the XOR when we noticed time was not on our side.

**We are open to contributions :)**

## Getting Started
Once you have the Master branch cloned on your system, there are only three major things to do.

1. Create a .env file in your root directory with the below fields
    ```
    # Change seed to some large number ranging from 1 to Long.MAX_VALUE and share with peers
    SEED=<Long Value>
    
    # We advice this should be a small number for better performance. Try numbers from 1 to 5
    PUBLIC_KEY=<Number>
    ```
    If you use VS code and you run into an error where the .env file cannot be found, move it to the resources folder.
2. In the UserService class, change the value in the line that says ```properties.put("bootstrap.servers", "pi.cs.oswego.edu:26921");``` to whatever server you'll be connected to.
3. Download Kafka on the server you and your peers will connect to.

<br>
<br>
<br>

Creators: [Declan Onunkwo](https://github.com/DeclanGH), [Nathan Moses](https://github.com/nmosOz), [Jack Wilcox](https://github.com/jwilcox5)
