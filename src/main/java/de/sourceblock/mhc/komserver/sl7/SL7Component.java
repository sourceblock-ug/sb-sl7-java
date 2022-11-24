package de.sourceblock.mhc.komserver.sl7;

public class SL7Component extends SL7Structure {
    public SL7Component(String content) {
        super(content);
    }

    public SL7Component(String content, SL7Structure parent) {
        super(content, parent);
    }

    @Override
    protected int specialCharPosition() {
        return 4;
    }

    @Override
    protected void parse(String content, Integer field, SL7Structure parent) {
//        System.out.println("\t\t\t\tParting new Component: " + teaserText(content));
        super.parse(content, field, parent);
    }

    @Override
    protected SL7Structure createChildStructure(String part) {
        return new SL7SubComponent(part, this);
    }
}
