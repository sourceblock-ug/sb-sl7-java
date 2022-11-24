package de.sourceblock.mhc.komserver.sl7;

public class SL7FieldList extends SL7Structure {
    public SL7FieldList(String content) {
        super(content);
    }

    public SL7FieldList(String part, SL7Structure parent) {
        super(part, parent);
    }

    @Override
    protected boolean addEmpty() {
        return false;
    }

    @Override
    protected RemoveMode removeLastJoinChar() {
        return RemoveMode.TAILING;
    }

    @Override
    protected int specialCharPosition() {
        return 2;
    }

    @Override
    protected SL7Structure createChildStructure(String part) {
        return new SL7Field(part, this);
    }

    @Override
    protected void parse(String content, Integer field, SL7Structure parent) {
//        System.out.println("\t\tParsing new Field List: " + teaserText(content));
        super.parse(content, field, parent);
    }

    @Override
    public String key() {
        String r = "";
        if (this.parent()!=null) {
            r = this.parent().key();
            if (!r.equals("")) r+="-";
            r += (this.parent().children.indexOf(this) + 1);
        }
        return r;
    }
}
