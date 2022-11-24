package de.sourceblock.mhc.komserver.sl7;

import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SL7Structure {
    public static final String DEFAULT_CHARS = "|^~\\&";
    public static final DateTimeFormatter HL7_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    protected SL7Structure parent;
    protected final List<SL7Structure> children = new ArrayList<>();

    public SL7Structure() {
    }

    public SL7Structure(String content) {
        this(content, null);
    }

    public SL7Structure(String content, SL7Structure parent) {
        this.parse(content, parent);
    }


    protected void setParent(SL7Structure parent) {
        this.parent = parent;
    }

    public SL7Structure parent() {
        return parent;
    }

    public static AtomicInteger counter = new AtomicInteger(0);
    public String chars() {
        if (parent != null) {
            if (counter.incrementAndGet()> 130) {
                throw new RuntimeException("Loop?");
            }
            String chars = parent.chars();
            if (chars == null || chars.length()<5) {
                throw new RuntimeException("Loop Problem in chars!");
            }
            counter.decrementAndGet();
            return chars;
        }
        return DEFAULT_CHARS;
    }

    public RuleSet ruleSet() {
        return ruleSet(this);
    }

    private RuleSet ruleSet(SL7Structure forChild) {
        if (parent != null) return parent.ruleSet(forChild);
        return null;
    }

    protected SL7Structure createChildStructure(String part) {
        return new SL7Structure(part, this);
    }

    protected char splitChar() {
        String chars = chars();
        int index = specialCharPosition();
        if (index>=chars.length()) {
            throw new RuntimeException("UPS!");
        }
        return chars.charAt(index);
    }

    protected char joinChar() {
        return this.splitChar();
    }

    protected char joinCharEscapedValue() {
        int index = specialCharPosition();
        // |^~\&
        // 01234
        if (index == 0) return 'F';
        if (index == 1) return 'S';
        if (index == 2) return 'R';
        if (index == 3) return 'E';
        return 'T';
    }

    protected String escapeChar() {
        return String.valueOf(chars().charAt(3));
    }

    protected int specialCharPosition() {
        return 0;
    }

    ;


    public void parse(String content) {
        parse(content, null, null);
    }

    public void parse(String content, SL7Structure parent) {
        parse(content, null, parent);
    }

    public void parse(String content, int field) {
        parse(content, field, null);
    }

    protected void parse(String content, Integer field, SL7Structure parent) {
        int parseField = field != null ? field : -1;
        if (parent != null) {
            this.setParent(parent);
        }

        if (parseField < 0) {
            String[] parts = this.parseParts(content);
            if (parts.length==0) {
                parts = new String[]{content};
            }
            this.children.clear();
            for (String part : parts) {
                if (addEmpty() || !part.isEmpty()) {
                    SL7Structure com = this.createChildStructure(part);
                    if (com != null) {
                        this.children.add(com);
                    }
                }
            }
        } else {
            // Just parse one sub element
            while (this.children.size() <= parseField) {
                this.children.add(this.createChildStructure(null));
            }
            if (this.children.get(parseField) == null) {
                this.children.set(parseField, this.createChildStructure(content));
            } else {
                this.children.get(parseField).parse(content, this);
            }
        }
    }

    protected String[] parseParts(String content) {
        char myChar = this.splitChar();
        if (content == null) content = "";
        String regex;
        if (myChar == '\r') { // use R to split for all Line breaks!
            regex ="\r\n|\r|\n";
        } else {
            regex = makeRegExString(String.valueOf(myChar));

        }
        return content.split(regex,-1);
    }


    protected RemoveMode removeLastJoinChar() {
        return RemoveMode.LAST;
    }

    protected boolean addEmpty() {
        return true;
    }


    protected String escape(String str) {
        String escape = escapeChar();
        char myChar = joinChar();
        if (escape != null) {
            str = escapeSpecial(str, myChar, this.joinCharEscapedValue(), escape);
            /*str = str.replaceAll(escapeRegExp(String.valueOf(myChar)), escape + myChar);
            if (str.endsWith(escape)) {
                str += escape;
            }*/
        }
        return str;
    }

    final public String value() {
        return internalRender(false);
    }

    final public String render() {
        return internalRender(true);
    }

    protected String internalRender(boolean doEscape) {
        char myChar = joinChar();
        StringBuilder str = new StringBuilder("");

        for (SL7Structure child : children) {
            String t = child.internalRender(doEscape);
            if (doEscape) t = this.escape(t);
            str.append(t).append(myChar);
        }
        if (!removeLastJoinChar().equals(RemoveMode.NONE)) {
            int c = 0;
            while (str.length()>0 && str.charAt(str.length()-1)==myChar &&
                    (
                        (removeLastJoinChar().equals(RemoveMode.LAST) && c==0) ||
                        removeLastJoinChar().equals(RemoveMode.TAILING)
            )) {
                str.setLength(str.length() - 1);
                c++;
            }
        }
        return str.toString();
    }

    public final SL7Structure fieldAtIndex(int index) {
        return fieldAtIndexAutocreate(index, true).orElseThrow(() -> new RuntimeException("Field at Index did not exists an could not be created: " +key() + "." + (index+1)));
    }

    protected Optional<SL7Structure> fieldAtIndexAutocreate(int index, boolean autocreate) {
        if (index < 0) index = 0;
        if (children.size() <= index && autocreate) {
            while (children.size() <= index) {
                SL7Structure item = this.createChildStructure(null);
                item.setParent(this);
                children.add(item);
            }
        } else if (children.size() <= index) {
            return Optional.empty();
        }
        return Optional.ofNullable(children.get(index));
    }

    public String key() {
        String r = "";
        if (this.parent() != null) {
            r = this.parent().key();
            if (r != "") {
                r += ".";
            }
            r += (this.parent().children.indexOf(this) + 1);
        }
        return r;
    }

    public final SL7Structure fieldForKey(final String key) {
        return fieldForKeyAutocreate(key, true).orElseThrow(() -> {
            return new RuntimeException("Field at Index did not exists an could not be created: " +key() + "." + (key));
        });
    }

    protected Optional<SL7Structure> fieldForKeyAutocreate(String key, boolean autocreate) {
        int idx = parseInt(key);
        if (idx >= 0) return fieldAtIndexAutocreate(idx-1, autocreate);
        return Optional.empty();
    }

    public Optional<SL7Structure> fieldForKeyIfExists(String key) {
        return fieldForKeyAutocreate(key, false);
    }

    protected SL7Structure get(String[] selector, int idx) {
        if (selector.length > idx && selector[idx] != null && !selector[idx].isEmpty()) {
            SL7Structure field = this.fieldForKey(selector[idx]);
            if (field != null) {
                if (selector.length > idx + 1) {
                    return field.get(selector, idx + 1);
                } else {
                    return field;
                }
            }
        }
        return null;
    }

    public SL7Structure get(String selector) {
        String s = selector.replaceAll("-", ".");
        return get(s.split("\\."), 0);
    }

    public String text(String selector) {
        return get(selector).toString();
    }

    protected boolean has(String[] selector, int idx) {
        if (selector.length > idx && selector[idx] != null && !selector[idx].isEmpty()) {
            Optional<SL7Structure> field = this.fieldForKeyIfExists(selector[idx]);
            if (field.isPresent()) {
                if (selector.length > idx + 1) {
                    return field.get().has(selector, idx + 1);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean has(String selector) {
        String s = selector.replaceAll("-", ".");
        return has(s.split("\\."), 0);
    }

    public void set(String selector, String content) {
        SL7Structure field = this.get(selector);
        if (field != null) {
            field.parse(content);
        }
    }

    @Override
    public String toString() {
        return render();
    }

    ;


    public static int parseInt(String key) {
        try {
            return Integer.parseInt(key);
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static String escapeSpecial(String str, char toEscape, char replacement, String escapeChar) {
        String target = String.valueOf(toEscape);
        String dest = escapeChar + makeRegExString(replacement) + escapeChar;
        str = (str + "")
                .replace(target, dest)
                .replace("\n", escapeChar + "X0A" + escapeChar)
                .replace("\r", escapeChar+ "X0D" + escapeChar)
                ;
        return str;
    }

    public static String unescapeSpecial(String str, char toEscape, char replacement, String escapeChar) {
        String target = escapeChar + toEscape + escapeChar;
        String dest = String.valueOf(replacement);
        return (str + "")
                .replace(target, dest)
                .replace(escapeChar + "X0A" + escapeChar, "\n")
                .replace(escapeChar+ "X0D" + escapeChar, "\r")
                ;
    }

    public static String escapeCharset(String input, Charset charset, char esc) {
        if (charset.newEncoder().maxBytesPerChar()==1) {
            String prepared = StringEscapeUtils.escapeJava(input);
            prepared = prepared.replaceAll("(\\\\u00)([0-9A-F]{2})", SL7Structure.makeRegExString(esc) + "C$2" + SL7Structure.makeRegExString(esc));
            prepared = prepared.replaceAll("(\\\\u)([0-9A-F]{4})", SL7Structure.makeRegExString(esc) + "M$2" + SL7Structure.makeRegExString(esc));
            return prepared;
        }
        return input;
    }

    @Deprecated
    public static String escapeSpecial(String str) {
        return (str + "")
                .replace("\\", "\\E\\")
                .replace("|", "\\F\\")
                .replace("~", "\\R\\")
                .replace("^", "\\S\\")
                .replace("&", "\\T\\")

                ;
    }

    ;

    @Deprecated
    public static String unescapeSpecial(String str) {
        return (str + "")
                .replace("\\X0A\\", "\n")
                .replace("\\X0D\\", "\r")
                .replace("\\F\\", "|")
                .replace("\\R\\", "~")
                .replace("\\S\\", "^")
                .replace("\\T\\", "&")
                .replace("\\E\\", "\\")
                ;
    }

    ;

    public static LocalDateTime dateFromStringOrNow(String str) {
        if (str != null && str.length() >= 14) {
            return LocalDateTime.of(
                    LocalDate.of(
                            parseInt(str.substring(0, 4)),
                            parseInt(str.substring(4, 4 + 2)),
                            parseInt(str.substring(6, 6 + 2))
                    ),
                    LocalTime.of(
                            parseInt(str.substring(8, 8 + 2)),
                            parseInt(str.substring(10, 10 + 2)),
                            parseInt(str.substring(12, 12 + 2))
                    )
            );
        }
        return LocalDateTime.now();
    }

    public static Date dateFromStringOrNull(String str) {
        if (str != null && str.length() >= 14) {
            return java.sql.Timestamp.valueOf(LocalDateTime.of(
                    LocalDate.of(
                            parseInt(str.substring(0, 4)),
                            parseInt(str.substring(4, 4 + 2)),
                            parseInt(str.substring(6, 6 + 2))
                    ),
                    LocalTime.of(
                            parseInt(str.substring(8, 8 + 2)),
                            parseInt(str.substring(10, 10 + 2)),
                            parseInt(str.substring(12, 12 + 2))
                    )
            ));
        }
        return null;
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime.format(HL7_DATE_TIME_FORMATTER);
    }

    public static String makeRegExString(String string) {
        return string.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0"); // $& means the whole matched string
    }

    public static String makeRegExString(char myChar) {
        return makeRegExString(String.valueOf(myChar));
    }


    public static String teaserText(String content) {
        if (content==null) return "<null>";
        if (content.length()>100) {
            return content.substring(0, 100);
        } else {
            return content;
        }
    }


    public void forAll(String key, SturctureIterator iterator) {
        int i = 1;
        String structureString = "";
        while (this.has(key + "[" + i + "]")) {
            final SL7Structure sl7Structure = this.get(key + "[" + i + "]");
            iterator.iterate(sl7Structure);
            i++;
        }
    }

    public void clear() {
        children.clear();
    }

    public interface SturctureIterator {
        void iterate(SL7Structure structure);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SL7Structure that = (SL7Structure) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    public enum RemoveMode {
        NONE, LAST, TAILING
    }
}
