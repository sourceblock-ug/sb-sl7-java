package de.sourceblock.mhc.komserver.sl7;

import java.time.LocalDateTime;
import java.util.Optional;

public class SL7Message extends SL7Structure {
    protected String messageChars;
    protected RuleSet msgRuleSet;

    public SL7Message() {
        this("MSH" + SL7Structure.DEFAULT_CHARS);
    }

    public SL7Message(String content) {
        this(content, null);
    }

    public SL7Message(String content, RuleSet msgRuleSet) {
        //super(content.startsWith("MSH")?content:"MSH"+SL7Structure.DEFAULT_CHARS + "\r" + content);
        if (content ==null || content.isEmpty()) content = "MSH" + SL7Structure.DEFAULT_CHARS;
        if(!content.startsWith("MSH")) throw new IllegalArgumentException("Given String does not start with 'MSH'");
        if (content.length()<4) {
            content += "|";
        }
        String split = content.substring(3,4);
        String otherChars = content.length()>4?content.substring(4, Math.min(content.length(), 8)):"";
        while (otherChars.length()<4) {
            otherChars += SL7Structure.DEFAULT_CHARS.substring(1 + otherChars.length(), 2+otherChars.length());
        }
        this.messageChars = split + otherChars;
        this.parse(content, null);
        this.msgRuleSet = msgRuleSet;
        if (!hasPart("MSH")) {
            final SL7Part part = new SL7Part("MSH|", this);
            this.children.add(0, part);
            part.setParent(this);
        }
        if (this.messageChars == null || this.messageChars.length()!=SL7Structure.DEFAULT_CHARS.length()) {
            this.messageChars = SL7Structure.DEFAULT_CHARS;
        }
    }

    public SL7Part msh() {
        for (SL7Structure child : children) {
            if (child instanceof SL7Part && ((SL7Part) child).getType().equalsIgnoreCase("MSH")) {
                return (SL7Part) child;
            }
        }
        SL7Part msh = new SL7Part("MSH|" + SL7Structure.DEFAULT_CHARS, this);
        children.add(0, msh);
        return msh;
    }

    public boolean hasPart(String part) {
        for (SL7Structure child : children) {
            if (child instanceof SL7Part &&
                    ((SL7Part) child).getType() != null &&
                    ((SL7Part) child).getType().equalsIgnoreCase(part)) {
                return true;
            }
        }
        return false;
    }

    public void addPart(SL7Part part) {
        children.add(part);
        part.setParent(this);
    }

    @Override
    public String chars() {
        return messageChars!=null && !messageChars.isEmpty()?messageChars:SL7Structure.DEFAULT_CHARS;
    }

    @Override
    public RuleSet ruleSet() {
        return this.msgRuleSet;
    }

    @Override
    protected char splitChar() {
        return '\r';
    }

    @Override
    protected char joinChar() {
        return '\r';
    }

    @Override
    protected String escapeChar() {
        return null;
    }

    @Override
    protected RemoveMode removeLastJoinChar() {
        return RemoveMode.NONE;
    }

    @Override
    protected void parse(String content, Integer field, SL7Structure parent) {
//        System.out.println("Parting new Message: " + teaserText(content));
        super.parse(content, field, parent);
    }


    @Override
    protected String[] parseParts(String content) {
        String[] parseParts = super.parseParts(content);
        // extract Message Chars in "MSH|..."
        for (String parsePart : parseParts) {
            if (parsePart.length()>9 && parsePart.toUpperCase().startsWith("MSH")) {
                int length = 5;
                String tc = parsePart.substring(3,4);
                String c = parsePart.substring(3,3+length);
                if (c.indexOf(tc)>0) {
                    // TODO:
                    c = c.substring(0, c.indexOf(tc)) + this.messageChars.charAt(c.indexOf(tc));
                }
                this.messageChars = c;
                break;
            }
        }
        if (messageChars == null || messageChars.isEmpty()) {
            messageChars = SL7Structure.DEFAULT_CHARS;
        }
        return parseParts;
    }


    @Override
    protected boolean addEmpty() {
        return false;
    }

    @Override
    protected SL7Structure createChildStructure(String part) {
        return new SL7Part(part, this);
    }

    @Override
    public String toString() {
        return render().replaceAll("\r", "\n");
    }

    @Override
    public Optional<SL7Structure> fieldForKeyAutocreate(String key, boolean autocreate) {
        int item = 0;
        if (key.contains("[")) {
            item = Math.max(0, parseInt(key.substring(key.indexOf("[")+1, key.indexOf("]")))-1);
            key = key.substring(0, key.indexOf("["));
        }
        int keyIdx = parseInt(key);
        if (keyIdx > 0) {
            return this.fieldAtIndexAutocreate(keyIdx - 1, autocreate);
        } else {
            int found = -1;
            for (SL7Structure child : children) {
                if (child instanceof SL7Part && ((SL7Part) child).getType().equalsIgnoreCase(key)) {
                    found++;
                    if (found == item) {
                        return Optional.of(child);
                    }
                }
            }
            SL7Part part = null;
            if (autocreate) {
                while (found < item) {
                    found++;
                    part = new SL7Part("", this);
                    part.setType(key.toUpperCase());
                    String position = RuleSet.segmentCounterField(key.toUpperCase());
                    if (!position.isEmpty()) {
                        part.set(position, String.valueOf(found + 1));
                    }
                    addPart(part);
                }
            }
            return Optional.ofNullable(part);
        }
    }

    public SL7Message createResponse(String code, String text) {
        return createResponseMessage(this, code, text);
    }

    public static SL7Message createResponseMessage(SL7Message msg, String code, String text) {
        if (code == null || code.length() != 2) {
            code = "AE";
        }
        text = text!=null?text:"";
        SL7Message ack = new SL7Message(msg.msh().render());
        String msgId = ack.get("MSH-10").render();
        String app = ack.get("MSH-3").render();
        String facc = ack.get("MSH-4").render();
        String dt = msg.formatDate(LocalDateTime.now());
        String nid = (dt + Math.random()).substring(0,22);
        ack.set("MSH-3", ack.get("MSH-5").render());
        ack.set("MSH-4", ack.get("MSH-6").render());
        ack.set("MSH-5", app);
        ack.set("MSH-6", facc);
        ack.set("MSH-7", dt);
        ack.set("MSH-9", "ACK");
        ack.set("MSH-10", nid);

        // AS
        ack.set("MSA-1", code);
        ack.set("MSA-2", msgId);
        ack.set("MSA-3", text);
        return ack;
    }
}
