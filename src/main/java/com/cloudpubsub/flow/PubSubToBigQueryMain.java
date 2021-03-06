package com.cloudpubsub.flow;

import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubToBigQueryMain {

    private static Logger LOG = LoggerFactory.getLogger(PubSubToBigQueryMain.class);

    private static final String DATASET_NAME = "customer";
    private static final String SUBSCRIBER_NAME = "projects/project-cloud-314907/subscriptions/information-sub";

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        run(options);
    }

    private static void run(Options options) {

        final TupleTag<CustomerFlowData> adultTuple = new TupleTag<CustomerFlowData>() {
        };
        final TupleTag<CustomerFlowData> youngTuple = new TupleTag<CustomerFlowData>() {
        };

        Pipeline pipeline = Pipeline.create(options);
        LOG.info("Start pipe");
        PCollectionTuple mixedCollection = pipeline
                .apply("ReadPubSubSubscription", PubsubIO.readMessagesWithAttributes()
                        .fromSubscription(SUBSCRIBER_NAME))
                .apply("Extract attributes", MapElements.via(new SimpleFunction<PubsubMessage, CustomerFlowData>() {
                    @Override
                    public CustomerFlowData apply(PubsubMessage message) {
                        return new CustomerFlowData(message);
                    }
                }))
                /*
                 * Filter messages by country
                 */
                .apply("Filtered by country Ukraine", Filter.by(PubSubToBigQueryMain::isResident))
                /*
                 * Split customer by age
                 */
                .apply("Split output", ParDo.of(new DoFn<CustomerFlowData, CustomerFlowData>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        if (c.element().getAge() > 18) {
                            c.output(adultTuple, c.element());
                        } else {
                            c.output(youngTuple, c.element());
                        }
                    }
                }).withOutputTags(adultTuple, TupleTagList.of(youngTuple)));

        /*
         * Convert adult to TableRAW and write to BigQuery
         */
        mixedCollection.get(adultTuple)
                .apply("Convert adult to TableRow", new CustomerToTableRowTransform())
                .apply("Write adult to BigQuery", insert(createTableByName("adult")));

        /*
         * Convert young to TableRAW and write to BigQuery
         */
        mixedCollection.get(youngTuple)
                .apply("Convert adult to TableRow", new CustomerToTableRowTransform())
                .apply("Write young to BigQuery", insert(createTableByName("young")));

        LOG.info("END pipeline");
        // Run the pipeline
        pipeline.run();
    }

    private static boolean isResident(Object o) {
        CustomerFlowData customerFlowData = (CustomerFlowData) o;
        return "ukraine".equalsIgnoreCase(customerFlowData.getCountry());
    }

    private static BigQueryIO.Write<TableRow> insert(TableReference outputProvider) {
        return BigQueryIO.writeTableRows()
                .withoutValidation()
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .withMethod(BigQueryIO.Write.Method.STREAMING_INSERTS)
                .to(outputProvider);
    }

    private static TableReference createTableByName(String tableName) {
        return new TableReference()
                .setProjectId("project-cloud-314907")
                .setDatasetId(DATASET_NAME)
                .setTableId(tableName);
    }

    static class CustomerToTableRowTransform
            extends PTransform<PCollection<CustomerFlowData>, PCollection<TableRow>> {

        @Override
        public PCollection<TableRow> expand(PCollection<CustomerFlowData> input) {
            return
                    input
                            .apply(
                                    "Map name to table row",
                                    ParDo.of(new DoFn<CustomerFlowData, TableRow>() {
                                        @ProcessElement
                                        public void processElement(ProcessContext context) {
                                            CustomerFlowData customer = context.element();

                                            TableRow tableRow = new TableRow()
                                                    .set("first_name", customer.getFirstName())
                                                    .set("last_name", customer.getLastName())
                                                    .set("country", customer.getLastName())
                                                    .set("age", customer.getAge());

                                            LOG.info("Row: {}", tableRow);

                                            context.output(tableRow);
                                        }
                                    })
                            );
        }
    }


}
