/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jackson.avro;

import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import org.apache.avro.Schema;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JacksonAvroMarshalUnmarshalPojoTest extends CamelTestSupport {

    @Test
    public void testMarshalUnmarshalPojo() throws Exception {
        MockEndpoint mock1 = getMockEndpoint("mock:serialized");
        mock1.expectedMessageCount(1);
        mock1.message(0).body().isInstanceOf(byte[].class);

        Pojo pojo = new Pojo("Hello");
        template.sendBody("direct:pojo", pojo);

        mock1.assertIsSatisfied();

        byte[] serialized = mock1.getReceivedExchanges().get(0).getIn().getBody(byte[].class);
        assertNotNull(serialized);
        assertEquals(6, serialized.length);

        MockEndpoint mock2 = getMockEndpoint("mock:pojo");
        mock2.expectedMessageCount(1);
        mock2.message(0).body().isInstanceOf(Pojo.class);

        template.sendBody("direct:serialized", serialized);
        mock2.assertIsSatisfied();

        Pojo back = mock2.getReceivedExchanges().get(0).getIn().getBody(Pojo.class);

        assertEquals(pojo.getText(), back.getText());
    }

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        String SCHEMA_JSON = "{\n"
                             + "\"type\": \"record\",\n"
                             + "\"name\": \"Pojo\",\n"
                             + "\"fields\": [\n"
                             + " {\"name\": \"text\", \"type\": \"string\"}\n"
                             + "]}";
        Schema raw = new Schema.Parser().setValidate(true).parse(SCHEMA_JSON);
        AvroSchema schema = new AvroSchema(raw);
        SchemaResolver resolver = ex -> schema;
        registry.bind("schema-resolver", SchemaResolver.class, resolver);

        JacksonAvroDataFormat df = new JacksonAvroDataFormat();
        df.setUnmarshalType(Pojo.class);
        registry.bind("custom-df", DataFormat.class, df);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:serialized").unmarshal().custom("custom-df").to("mock:pojo");
                from("direct:pojo").marshal().custom("custom-df").to("mock:serialized");
            }
        };
    }

    public static class Pojo {

        public Pojo() {
        }

        public Pojo(String text) {
            this.text = text;
        }

        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

}
