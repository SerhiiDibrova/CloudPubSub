package com.cloudpubsub.flow;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PubsubToBigquery {

    private static Logger LOG = LoggerFactory.getLogger(PubsubToBigquery.class);

    private static final String DATASET_NAME = "customer";
    private static final String SUBSCRIBER_NAME = "projects/project-cloud-314907/subscriptions/information-sub";
    private static final String TOPIC_NAME = "projects/project-cloud-314907/topics/information";

    public void insert(boolean isSubscriber) {

        // Build the table schema for the output table.
        List<TableFieldSchema> fields = new ArrayList<>();
        fields.add(new TableFieldSchema().setName("first_name").setType("STRING"));
        fields.add(new TableFieldSchema().setName("last_name").setType("STRING"));
        fields.add(new TableFieldSchema().setName("age").setType("INTEGER"));
        TableSchema schema = new TableSchema().setFields(fields);

        // Start by defining the options for the pipeline.
        Options options = PipelineOptionsFactory.create().as(Options.class);
        options.setInputSubscription(getValueProvider(SUBSCRIBER_NAME));
        options.setInputTopic(getValueProvider(TOPIC_NAME));
        options.setUseSubscription(isSubscriber);
//        options.setRunner(DataflowRunner.class);
        Pipeline pipeline = Pipeline.create(options);

        PubsubIO.Read<PubsubMessage> messages = options.getUseSubscription() ? PubsubIO.readMessagesWithAttributes()
                .fromSubscription(options.getInputSubscription()) : PubsubIO.readMessagesWithAttributes().fromTopic(options.getInputTopic());
        String messageLabel = options.getUseSubscription() ? "ReadPubSubSubscription" : "ReadPubSubTopic";

        pipeline.apply(messageLabel, messages)
                .apply("ConvertMessageToTableRow", ParDo.of(new DoFn<PubsubMessage, TableRow>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        LOG.info("Inside processor..");
                        PubsubMessage message = c.element();
                        String first_name = message.getAttribute("first_name");
                        String last_name = message.getAttribute("last_name");
                        int age = Integer.parseInt(message.getAttribute("age"));
                        LOG.info("Creating table row..");
                        LOG.info(first_name + " :: " + last_name + " :: " + age);
                        TableRow row = new TableRow()
                                .set("first_name", first_name)
                                .set("last_name", last_name)
                                .set("age", age);
                        c.output(row);
                    }
                }))
                .apply("InsertTableRowsToBigQuery",
                        BigQueryIO.writeTableRows().to(DATASET_NAME)
                                .withSchema(schema)
                                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));

        // Run the pipeline
        pipeline.run().waitUntilFinish();
    }

    private ValueProvider<String> getValueProvider(String value) {
        return new ValueProvider<>() {
            @Override
            public String get() {
                return value;
            }

            @Override
            public boolean isAccessible() {
                return true;
            }
        };
    }

}
