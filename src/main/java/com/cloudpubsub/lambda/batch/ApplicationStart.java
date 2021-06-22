package com.cloudpubsub.lambda.batch;

import com.cloudpubsub.protobuf.OrderMessageOuterClass;
import com.google.api.services.bigquery.model.TableRow;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

public class ApplicationStart {


    private static final Logger LOG = LoggerFactory.getLogger(ApplicationStart.class);

    private static final DateTimeFormatter dateF = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter hourF = DateTimeFormatter.ofPattern("HH:00")
            .withZone(ZoneOffset.UTC);

    /**
     * The {@link Options} class provides the custom execution options passed by the executor at the
     * command-line.
     */
    public interface Options extends StreamingOptions {

        @Description("The directory to read files from. Must end with a slash.")
        @Validation.Required
        ValueProvider<String> getInputDirectory();

        void setInputDirectory(ValueProvider<String> value);

        @Description("The table to write to")
        @Validation.Required
        ValueProvider<String> getOutputTable();

        void setOutputTable(ValueProvider<String> value);

    }

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
        options.setStreaming(true);

        PipelineResult.State state = run(options).waitUntilFinish();
        LOG.info("Pipeline state: {}", state);
    }

    public static PipelineResult run(Options options) {

        final Instant now = Instant.now();
        final String date = dateF.format(now);
        final String hour = hourF.format(now);

        Pipeline pipeline = Pipeline.create(options);
        pipeline.getCoderRegistry().registerCoderForClass(OrderMessageOuterClass.OrderMessage.Type.class, new Coder<OrderMessageOuterClass.OrderMessage.Type>() {
            @Override
            public void encode(OrderMessageOuterClass.OrderMessage.Type value, OutputStream outStream) throws IOException {
                outStream.write(value.getNumber());
            }

            @Override
            public OrderMessageOuterClass.OrderMessage.Type decode(InputStream inStream) throws IOException {
                return OrderMessageOuterClass.OrderMessage.Type.forNumber(inStream.read());
            }

            @Override
            public List<? extends Coder<?>> getCoderArguments() {
                return null;
            }

            @Override
            public void verifyDeterministic() throws NonDeterministicException {
            }
        });

        pipeline
                .apply(
                        "Read data from file(s)",
                        TextIO.read()
                                .from(options.getInputDirectory())
                )
                .apply(
                        "Map strings to OrderMessage",
                        MapElements.via(new SimpleFunction<String, OrderMessageOuterClass.OrderMessage>() {
                            @Override
                            public OrderMessageOuterClass.OrderMessage apply(String input) {
                                try {
                                    OrderMessageOuterClass.OrderMessage orderMessage = OrderMessageOuterClass.OrderMessage.parseFrom(Base64.getDecoder().decode(input));
                                    LOG.debug("Successfully parsed: {}", orderMessage);
                                    return orderMessage;
                                } catch (InvalidProtocolBufferException e) {
                                    LOG.error("Error while parsing OrderMessage: {}", e.getMessage());
                                    throw new RuntimeException(e);
                                }
                            }
                        })
                )
                .apply(
                        "Convert to KV<Type, Long>",
                        MapElements.via(new SimpleFunction<OrderMessageOuterClass.OrderMessage, OrderMessageOuterClass.OrderMessage.Type>() {
                            @Override
                            public OrderMessageOuterClass.OrderMessage.Type apply(OrderMessageOuterClass.OrderMessage input) {
                                return input.getType();
                            }
                        })
                )
                .apply(
                        "Count by type",
                        Count.perElement()
                )
                .apply("Map to TableRow", MapElements.via(new SimpleFunction<KV<OrderMessageOuterClass.OrderMessage.Type, Long>, TableRow>() {
                    @Override
                    public TableRow apply(KV<OrderMessageOuterClass.OrderMessage.Type, Long> input) {
                        return new TableRow()
                                .set("date", date)
                                .set("time", hour)
                                .set("count", input.getValue())
                                .set("type", input.getKey());
                    }
                }))
                .apply(
                        BigQueryIO.writeTableRows()
                                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
                                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                                .withMethod(BigQueryIO.Write.Method.FILE_LOADS)
                                .to(options.getOutputTable())
                );

        return pipeline.run();
    }
}
