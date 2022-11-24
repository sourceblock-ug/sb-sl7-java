package de.sourceblock.mhc.komserver.sl7;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;

public class SL7Wrapper {
    public static final Parser PARSER = buildParser();

    private static Parser buildParser() {
        HapiContext context = new DefaultHapiContext();
        context.getParserConfiguration().setValidating(false);
        return context.getPipeParser();
    }

    private final SL7Message message;

    public SL7Wrapper(ca.uhn.hl7v2.model.Message message) {
        try {
            this.message = new SL7Message(message.encode());
        } catch (HL7Exception e) {
            throw new RuntimeException("Not a valid Message", e);
        }
    }

    public String get(String key) {
        return message.get(key).render();
    }

    public SL7Structure getStructure(String key) {
        return message.get(key);
    }

    public void set(String key, String value) {
        message.set(key, value);
    }

    public ca.uhn.hl7v2.model.Message encode() {
        String theMessage = message.render();
        try {
            return PARSER.parse(theMessage);
        } catch (HL7Exception e) {
            throw new RuntimeException("Error Encoding Message:\n" + theMessage.replaceAll("\r", "\n"), e);
        }
    }

    public static void main(String[] args) throws Exception {
        ca.uhn.hl7v2.model.Message msg = PARSER.parse("MSH|^~\\&|COMDIS|CKISADT|medavis RIS|MEDAVIS|20210217152330||ADT^A08|1664560|T|2.5|||AL|NE|\r" +
                "EVN|A08|20200327113600|\r" +
                "PID|1||8004510||Neugebauer^Max^Zusatz1^^^Titel1|Springer|20010425000000|M|||Musterstasse 13^^Karlsruhe^^76185^D||03925888||deutsch||EV||max.neu@webadresse.com||||Goslar|N||D|Energieelektroniker|||\r" +
                "PV1|1|VS|Pall^^^Pall^Pall_Pall|stat||^^^|318146887^Bejeng^Mirabel Tengi^^^^^Harsefelder Str. 8^Stade^21680^DE^^^^^^Klinik Dr. Hancken^76325489^^1^0|^^^^^^^^^^^^^^|||||^1||||||9008613||1||0|1||0||||||||||01^1||||||||20200327113600|20200506152700|||||| |\r" +
                "PV2|||01^7||||||20200506113600|40||||\r" +
                "IN1|1|S||^^|^^^^|||||||||||^^^^^|||^^^^|||1||||||||||||||||||||||\r" +
                "IN2|||||P|\r" +
                "ZBE|12480^MYMEDIS|20200327113600||UPDATE|\r" +
                "OBX|1|NM|3137-7^Koerpergroesse^LN||186|cm|\r" +
                "OBX|2|NM|3141-9^Koerpergewicht^LN||84|kg|");
        SL7Wrapper SL7Wrapper = new SL7Wrapper(msg);
        System.out.println("----------------------------------------");
        String hl7Version = msg.encode();
        System.out.println(msg.encode().replaceAll("\r", "\n"));
        System.out.println("----------------------------------------");

        SL7Structure pid = SL7Wrapper.getStructure("PID");
        System.out.println(pid.render());
        SL7Structure pid3 = SL7Wrapper.getStructure("PID-3");
        SL7Structure pid42 = SL7Wrapper.getStructure("PV1-7");
        System.out.println("PID-3:   " + pid3.render());
        System.out.println("PPV1-7: " + pid42.toString());
        System.out.println(SL7Wrapper.message.toString());
        SL7Wrapper.set("PID-4.2", "DEMO DEMO DEMO");
        System.out.println("----------------------------------------");
        // System.out.println(hl7.encode().printStructure());
        // System.out.println("----------------------------------------");
        System.out.println("HL7:\n");
        System.out.println(hl7Version);
        System.out.println("----------------------------------------\nSL7\n");
        System.out.println(SL7Wrapper.message.toString());
        System.out.println("----------------------------------------");
        System.out.println(SL7Wrapper.encode().encode().replaceAll("\r", "\n"));

    }

}
