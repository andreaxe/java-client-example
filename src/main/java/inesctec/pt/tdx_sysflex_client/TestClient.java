package inesctec.pt.tdx_sysflex_client;

import ch.iec.tc57._2011.schema.message.*;

import com.sun.xml.internal.ws.handler.HandlerException;
import es.ree.eemws.client.common.ParentClient;
import es.ree.eemws.core.utils.file.GZIPUtil;
import es.ree.eemws.core.utils.iec61968100.EnumMessageFormat;
import es.ree.eemws.core.utils.iec61968100.EnumVerb;
import es.ree.eemws.core.utils.security.CryptoException;
import es.ree.eemws.core.utils.security.CryptoManager;
import es.ree.eemws.core.utils.xml.XMLElementUtil;
import es.ree.eemws.core.utils.xml.XMLGregorianCalendarFactory;
import forecast.sysflexserver.inesctec.pt.*;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * @author Andre Garcia
 * @version 1.0
 * @apiNote this is a test client implementation
 */

class MessageHelper {

    public static ForecastRequest createForecastRequest(){

        ForecastRequest forecastRequest = new ForecastRequest();
        forecastRequest.setNetworkID("NetworkID");
        ResultType resultType = new ResultType();

        XMLGregorianCalendar datetime = XMLGregorianCalendarFactory.getGMTInstance(new Date());
        resultType.setDatetime(datetime);
        resultType.setRequest(datetime);
        DataType dataType = new DataType();
        EnergyConsumerType energyConsumerType = new EnergyConsumerType();
        EnergyConsumerNodeType energyConsumerNodeType = new EnergyConsumerNodeType();
        energyConsumerNodeType.setNodeID("NodeID");
        ValueUnitType valueUnitType = new ValueUnitType();
        valueUnitType.setUnit(UnitType.KVAR);
        valueUnitType.setValue(20.0);
        energyConsumerNodeType.setPowerFlowP(valueUnitType);
        valueUnitType.setValue(30.0);
        energyConsumerNodeType.setPowerFlowQ(valueUnitType);

        energyConsumerType.getEnergyConsumerNode().add(energyConsumerNodeType);
        dataType.setEnergyConsumer(energyConsumerType);
        resultType.setData(dataType);

        ResultsType resultsType = new ResultsType();
        resultsType.getResult().add(resultType);
        forecastRequest.setResults(resultsType);

        return forecastRequest;
    }
}

class Client extends ParentClient {

    Client(){}

    ResponseMessage send(RequestMessage requestMessage) throws HandlerException,
            es.ree.eemws.core.utils.operations.HandlerException {
        return sendMessage(requestMessage);
    }
}

public class TestClient {

    private static final String PRODUCTION_URL = "https://vcpes07.inesctec.pt/ws/request";

    public static void main(String[] args) throws IOException, JAXBException, CryptoException, InterruptedException,
            HandlerException, es.ree.eemws.core.utils.operations.HandlerException {

        // this should be verified to add to the keystore and truststore the allowed certificates
        Path client_trust_store = Paths.get("certificates", "client_truststore.ks");
        Path client_key_store = Paths.get("certificates", "client_keystore.ks");
        System.getProperties().put("javax.net.ssl.trustStore", client_trust_store.toString());
        System.getProperties().put("javax.net.ssl.trustStorePassword", "1234567");
        System.getProperties().put("javax.net.ssl.keyStore", client_key_store.toString());
        System.getProperties().put("javax.net.ssl.keyStorePassword", "1234567");

        ForecastRequest forecastRequest = MessageHelper.createForecastRequest();

        Client client = new Client();
        client.setEndPoint(new URL(PRODUCTION_URL));
        client.setVerifyResponse(false);
        client.setSignRequest(false);

        // Definition of the type of Forecast
        HeaderType headerType = new HeaderType();
        MessageProperty property = new MessageProperty();
        property.setName("network");
        property.setValue("Frades15");
        headerType.getProperties().add(property);

        // In the framework there is not a PUT Verb
        headerType.setVerb(EnumVerb.CREATE.toString());
        headerType.setNoun("ForecastCharges");

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setHeader(headerType);

        PayloadType payloadType = new PayloadType();
        payloadType.setFormat(EnumMessageFormat.BINARY.toString());

        String payloadString = XMLElementUtil.object2StringBuilder(forecastRequest).toString();

        // Step 1: ZIP Message
        byte[] compressed = GZIPUtil.compress(payloadString.getBytes(StandardCharsets.UTF_8));

        // Step 2: Encode Message
        String encodedString = Base64.getEncoder().encodeToString(compressed);

        // Step 3: Encrypt Message
        String encrytedMessage = CryptoManager.encrypt(encodedString);

        payloadType.setCompressed(encrytedMessage);
        requestMessage.setPayload(payloadType);

        // Test multiple sending messages
        for (int i=0 ; i<1; i++){
            Thread.sleep(1000);
            System.out.println(i);
            ResponseMessage responseMessage = client.send(requestMessage);
            // response
            System.out.println(XMLElementUtil.object2StringBuilder(responseMessage));
        }

    }

}

