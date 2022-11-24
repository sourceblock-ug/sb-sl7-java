package de.sourceblock.mhc.komserver.sl7;

public class RuleSet {
    public static String segmentCounterField(String type) {
        switch (type) {
            case "PID":
            case "PV1":
            case "PV2":
            case "IN1":
            case "IN2":
            case "IN3":
                return "1";
        }
        return "";
    }
}
