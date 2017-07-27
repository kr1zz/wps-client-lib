/*
 * ﻿Copyright (C) ${inceptionYear} - ${currentYear} 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.geoprocessing.wps.client.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.n52.geoprocessing.wps.client.WPSClientSession;
import org.n52.geoprocessing.wps.client.model.Format;
import org.n52.geoprocessing.wps.client.model.execution.ComplexInput;
import org.n52.geoprocessing.wps.client.model.execution.ComplexInputReference;
import org.n52.geoprocessing.wps.client.model.execution.Execute;
import org.n52.geoprocessing.wps.client.model.execution.ExecuteInput;
import org.n52.geoprocessing.wps.client.model.execution.ExecuteOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.wps.x20.DataDocument.Data;
import net.opengis.wps.x20.DataInputType;
import net.opengis.wps.x20.DataTransmissionModeType;
import net.opengis.wps.x20.ExecuteDocument;
import net.opengis.wps.x20.ExecuteRequestType;
import net.opengis.wps.x20.ExecuteRequestType.Response;
import net.opengis.wps.x20.OutputDefinitionType;
import net.opengis.wps.x20.ReferenceType;

public class WPS20ExecuteEncoder {

    private static Logger LOGGER = LoggerFactory.getLogger(WPS20ExecuteEncoder.class);

    public static ExecuteDocument encode(Execute execute) {

        ExecuteDocument executeDocument = ExecuteDocument.Factory.newInstance();

        ExecuteRequestType executeRequest = executeDocument.addNewExecute();

        executeRequest.setVersion(WPSClientSession.VERSION_200);

        executeRequest.addNewIdentifier().setStringValue(execute.getId());

        executeRequest.setResponse(ExecuteRequestType.Response.Enum.forString(execute.getResponseMode().name()));

        executeRequest.setMode(ExecuteRequestType.Mode.Enum.forString(execute.getExecutionMode().name()));

        addInputs(execute.getInputs(), executeRequest);

        addOutputs(execute.getOutputs(), executeRequest);

        return executeDocument;

    }

    private static void addInputs(List<ExecuteInput> inputs,
            ExecuteRequestType executeRequest) {

        for (ExecuteInput input : inputs) {
            addInput(input, executeRequest.addNewInput());
        }

    }

    private static void addInput(ExecuteInput input,
            DataInputType newInput) {

        newInput.setId(input.getId());

        if (input instanceof ComplexInput) {
            addComplexInput((ComplexInput) input, newInput);
        }

    }

    private static void addComplexInput(ComplexInput input,
            DataInputType newInput) {

        Format format = input.getFormat();

        String encoding = format.getEncoding();

        String schema = format.getSchema();

        if (input.isReference()) {

            ReferenceType reference = newInput.addNewReference();

            reference.setMimeType(format.getMimeType());

            if (encoding != null && !encoding.isEmpty()) {
                reference.setEncoding(encoding);
            }

            if (schema != null && !schema.isEmpty()) {
                reference.setSchema(schema);
            }

            ComplexInputReference complexInputReference = input.getReference();

            if (complexInputReference.getHref() != null) {
                reference.setHref(complexInputReference.getHref().toString());
            } else if (complexInputReference.getBody() != null) {
                reference.setBody(complexInputReference.getBody());
            } else if (complexInputReference.getBodyReference() != null) {
                reference.addNewBodyReference().setHref(complexInputReference.getBodyReference().toString());
            }
        } else {
            Data data = newInput.addNewData();

            data.setMimeType(format.getMimeType());

            if (encoding != null && !encoding.isEmpty()) {
                data.setEncoding(encoding);
            }

            if (schema != null && !schema.isEmpty()) {
                data.setSchema(schema);
            }

            Object value = input.getValue();

            if (value instanceof String) {

                String valueString = (String) value;

                try {
                    data.set(XmlObject.Factory.parse(valueString));
                } catch (XmlException e) {

                    LOGGER.warn("error parsing data String as xml node, trying to parse data as xs:string");

                    XmlString xml = XmlString.Factory.newInstance();

                    xml.setStringValue(valueString);

                    data.set(xml);

                }

            } else if (value instanceof InputStream) {

                InputStream stream = (InputStream) value;

                try {
                    data.set(XmlObject.Factory.parse(stream));
                } catch (XmlException e) {

                    LOGGER.warn("error parsing data stream as xml node, trying to parse data as xs:string");

                    String text = "";

                    int i = -1;

                    try {
                        while ((i = stream.read()) != -1) {
                            text = text + (char) i;
                        }
                    } catch (IOException e1) {
                        LOGGER.error("error parsing stream", e);
                    }

                    XmlString xml = XmlString.Factory.newInstance();

                    xml.setStringValue(text);

                    data.set(xml);
                } catch (IOException e) {
                    LOGGER.error("error parsing stream", e);
                }
            }
        }

    }

    private static void addOutputs(List<ExecuteOutput> outputs,
            ExecuteRequestType executeRequest) {

        for (ExecuteOutput executeOutput : outputs) {
            addOutput(executeOutput, executeRequest);
        }

    }

    private static void addOutput(ExecuteOutput executeOutput,
            ExecuteRequestType executeRequest) {

        OutputDefinitionType output = executeRequest.addNewOutput();

        Format format = executeOutput.getFormat();

        String encoding = format.getEncoding();

        String schema = format.getSchema();

        output.setMimeType(format.getMimeType());

        if (encoding != null && !encoding.isEmpty()) {
            output.setEncoding(encoding);
        }

        if (schema != null && !schema.isEmpty()) {
            output.setSchema(schema);
        }

        output.setTransmission(DataTransmissionModeType.Enum.forString(executeOutput.getTransmissionMode().name()));

    }

}