package com.a.eye.skywalking.storage.disruptor.ack;

import com.a.eye.skywalking.health.report.HealthCollector;
import com.a.eye.skywalking.health.report.HeathReading;
import com.a.eye.skywalking.logging.api.ILog;
import com.a.eye.skywalking.logging.api.LogManager;
import com.a.eye.skywalking.storage.config.Config;
import com.a.eye.skywalking.storage.data.file.DataFileWriter;
import com.a.eye.skywalking.storage.data.index.IndexMetaCollection;
import com.a.eye.skywalking.storage.data.index.IndexOperator;
import com.a.eye.skywalking.storage.data.index.IndexOperatorFactory;
import com.a.eye.skywalking.storage.data.spandata.AckSpanData;
import com.a.eye.skywalking.storage.data.spandata.SpanData;
import com.lmax.disruptor.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wusheng on 2016/11/24.
 */
public class StoreAckSpanEventHandler implements EventHandler<AckSpanData> {
    private static ILog logger = LogManager.getLogger(StoreAckSpanEventHandler.class);
    private DataFileWriter fileWriter;
    private IndexOperator  operator;
    private int            bufferSize;
    private List<SpanData> buffer;

    public StoreAckSpanEventHandler() {
        bufferSize = Config.Disruptor.FLUSH_SIZE;
        buffer = new ArrayList<>(bufferSize);
        fileWriter = new DataFileWriter();
        operator = IndexOperatorFactory.createIndexOperator();
    }

    @Override
    public void onEvent(AckSpanData event, long sequence, boolean endOfBatch) throws Exception {
        buffer.add(event);

        if (endOfBatch || buffer.size() == bufferSize) {
            try {
                IndexMetaCollection collection = fileWriter.write(buffer);

                operator.batchUpdate(collection);
                HealthCollector.getCurrentHeathReading("StoreAckSpanEventHandler").updateData(HeathReading.INFO, "Batch consume %s messages successfully.", buffer.size());
            } catch (Throwable e) {
                logger.error("Ack messages consume failure.", e);
                HealthCollector.getCurrentHeathReading("StoreAckSpanEventHandler").updateData(HeathReading.ERROR, "Batch consume %s messages failure.", buffer.size());
            } finally {
                buffer.clear();
            }
        }
    }
}
