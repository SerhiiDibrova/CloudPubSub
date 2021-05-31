package com.cloudpubsub.flow;

import com.cloudpubsub.avro.model.CustomerMessage;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubToBigQueryMainAvro {

    private static Logger LOG = LoggerFactory.getLogger(PubSubToBigQueryMainAvro.class);

    private static final String DATASET_NAME = "customer";
    private static final String SUBSCRIBER_NAME = "projects/project-cloud-314907/subscriptions/information-sub";

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        run(options);
    }

    private static void run(Options options) {

        final TupleTag<CustomerMessage> adultTuple = new TupleTag<CustomerMessage>() {
        };
        final TupleTag<CustomerMessage> youngTuple = new TupleTag<CustomerMessage>() {
        };

        Pipeline pipeline = Pipeline.create(options);
        LOG.info("Start pipe");
        PCollectionTuple mixedCollection = pipeline
                .apply("ReadPubSubSubscription", PubsubIO.readAvros(CustomerMessage.class)
                        .fromSubscription(SUBSCRIBER_NAME))
                /*
                 * Filter messages by country
                 */
                .apply("Filtered by country Ukraine", Filter.by(PubSubToBigQueryMainAvro::isResident))
                /*
                 * Split customer by age
                 */
                .apply("Split output", ParDo.of(new DoFn<CustomerMessage, CustomerMessage>() {
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
        CustomerMessage customerMessage = (CustomerMessage) o;
        return "ukraine".equalsIgnoreCase(customerMessage.getCountry().toString());
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
            extends PTransform<PCollection<CustomerMessage>, PCollection<TableRow>> {

        @Override
        public PCollection<TableRow> expand(PCollection<CustomerMessage> input) {
            return
                    input
                            .apply(
                                    "Map name to table row",
                                    ParDo.of(new DoFn<CustomerMessage, TableRow>() {
                                        @ProcessElement
                                        public void processElement(ProcessContext context) {
                                            CustomerMessage customer = context.element();

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
