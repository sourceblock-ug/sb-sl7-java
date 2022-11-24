package de.sourceblock.mhc.komserver.sl7;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SL7Part extends SL7Structure {
    private String type;
    public SL7Part(String content) {
        super(content);
    }

    public SL7Part(String content, SL7Structure parent) {
        super(content, parent);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null && type.length() == 3) {
            this.type = type;
        }
    }

    @Override
    protected int specialCharPosition() {
        return 0;
    }

    @Override
    protected SL7Structure createChildStructure(String part) {
        return new SL7FieldList(part, this);
    }

    @Override
    protected void parse(String content, Integer field, SL7Structure parent) {
//        System.out.println("\tParting new PART: " + teaserText(content));
        super.parse(content, field, parent);
    }

    @Override
    protected String[] parseParts(String content) {
        String[] parts = super.parseParts(content);
        if (parts.length>0 && parts[0].length()==3) {
            this.type = parts[0];
            if ("MSH".equals(this.type)) {
                final String[] copy = Arrays.copyOfRange(parts, 2, parts.length);
                if (copy.length==0) return new String[]{""};
                return copy;
            } else {
                return Arrays.copyOfRange(parts, 1, parts.length);
            }
        } else {
            this.type = "NTE";
        }
        return parts;
    }

    @Override
    public String key() {
        String r = "";
        if (this.parent()!=null) {
            r = this.parent().key();
            if (!r.equals("")) r+=".";

            List<SL7Structure> myChildren = this.parent().children;
            int found = 0;
            int p = -1;
            r += this.type;
            for (SL7Structure myChild : myChildren) {
                if (myChild instanceof SL7Part && ((SL7Part)myChild).type.equalsIgnoreCase(this.type)) {
                    found++;
                }
                if (myChild==this) {
                    p = found;
                    break;
                }
            }
            if (p>1) {
                r+="[" + p + "]";
            }
        }
        return r;
    }

    @Override
    public Optional<SL7Structure> fieldForKeyAutocreate(String key, boolean autocreate) {
        int item = 0;
        if (key.contains("[")) {
            item = Math.max(0, parseInt(key.substring(key.indexOf("[")+1, key.indexOf("]")))-1);
            key = key.substring(0, key.indexOf("["));
        }
        int keyIdx = parseInt(key);
        if ("MSH".equals(type)) {
            if (keyIdx<3) {
                throw new RuntimeException("Please Set Message-Chars on the SL7Message!");
            } else {
                keyIdx-=2;
            }
        }
        if (keyIdx > 0) {
            Optional<SL7Structure> child = this.fieldAtIndexAutocreate(keyIdx - 1, autocreate);
            if (child.isPresent()) {
                return child.get().fieldAtIndexAutocreate(item, autocreate);
            }
        }
        return Optional.empty();
    }

    @Override
    protected String internalRender(boolean doEscape) {
        String c =  super.internalRender(doEscape);
        return type + ("MSH".equals(type)?this.chars():"") + joinChar() + c;
    }
}
