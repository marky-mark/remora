
/*based on kafka.admin.ConsumerGroupCommand which gives an example of the below
*
* ➜  bin ./kafka-run-class.sh kafka.admin.ConsumerGroupCommand --new-consumer --describe --bootstrap-server localhost:9092 --group console-consumer-96416
GROUP                          TOPIC                          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             OWNER
console-consumer-96416         test                           0          3               3               0               consumer-1_/192.168.0.102
console-consumer-96416         test                           1          4               4               0               consumer-1_/192.168.0.102
*
* */
class RemoraKafkaConsumerGroupService(kafkaSettings: KafkaSettings) {

  def describeConsumerGroup(group: String) = ???
}
