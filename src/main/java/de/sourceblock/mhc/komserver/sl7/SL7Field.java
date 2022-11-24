package de.sourceblock.mhc.komserver.sl7;

public class SL7Field extends SL7Structure {
    public SL7Field(String content) {
        super(content);
    }

    public SL7Field(String content, SL7Structure parent) {
        super(content, parent);
    }

    @Override
    protected int specialCharPosition() {
        return 1;
    }

    @Override
    protected void parse(String content, Integer field, SL7Structure parent) {
//        System.out.println("\t\t\tParting new Field: " + teaserText(content));
        super.parse(content, field, parent);
    }

    @Override
    protected char joinCharEscapedValue() {
        return super.joinCharEscapedValue();
    }

    @Override
    protected RemoveMode removeLastJoinChar() {
        return RemoveMode.TAILING;
    }

    @Override
    public String key() {
        String r = "";
        if (this.parent!=null) {
            r = this.parent.key();
            if (this.parent.children.size()>1) {
                // if there is only one Field then no extra [...] for position
                r += "[" + (this.parent.children.indexOf(this)+1) + "]";
            }
        }
        return r;
    }

    @Override
    protected SL7Structure createChildStructure(String part) {
        return new SL7Component(part, this);
    }
}
