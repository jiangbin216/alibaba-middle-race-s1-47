package com.alibaba.middleware.race.jstorm.blot;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import clojure.lang.IFn;
import com.alibaba.middleware.race.RaceUtils;
import com.alibaba.middleware.race.jstorm.spout.MqTuple;
import com.alibaba.middleware.race.model.OrderMessage;
import com.alibaba.middleware.race.model.PaymentMessage;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.codahale.metrics.RatioGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wangwenfeng on 6/29/16.
 */
public class RatioBolt implements IBasicBolt, Serializable {
    private static Logger LOG = LoggerFactory.getLogger(RatioBolt.class);
    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    public void prepare(Map stormConf, TopologyContext context) {

    }

    @Override
    public void execute(Tuple input, BasicOutputCollector collector) {
        try {
            MqTuple mqTuple = (MqTuple) input.getValue(0);
            List<MessageExt> list = mqTuple.getMsgList();
            byte[] body;
            MessageExt msg;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                msg = list.get(i);
                body = msg.getBody();
                if (body.length == 2 && body[0] == 0 && body[1] == 0) {
                    collector.emit(new Values("","",""));
                    return;
                }
                PaymentMessage paymentMessage = RaceUtils.readKryoObject(PaymentMessage.class, body);
                count.addAndGet(1);
                collector.emit(new Values(paymentMessage.getCreateTime(), paymentMessage.getPayPlatform(),paymentMessage.getPayAmount()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("createTime","payPlatform","payAmount"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
