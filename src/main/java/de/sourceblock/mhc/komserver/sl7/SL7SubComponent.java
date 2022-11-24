package de.sourceblock.mhc.komserver.sl7;

public class SL7SubComponent extends SL7Structure {
    public String content;
    public SL7SubComponent(String content) {
        super(content);
    }

    public SL7SubComponent(String content, SL7Structure parent) {
        super(content, parent);
    }

    @Override
    protected String internalRender(boolean doEscape) {
        if (content != null) {
            return doEscape ? escapeSpecial(content, joinChar(), joinCharEscapedValue(), escapeChar()) : content;
        } else {
            return "";
        }
    }


    @Override
    protected void parse(String content, Integer field, SL7Structure parent) {
//        System.out.println("\t\t\t\t\tParsing new SUBcomponent: " + teaserText(content));
        if (parent != null) {
            this.setParent(parent);
        }
        this.content = (content + "")
                .replaceAll("\\\\X0A\\\\", "\n")
                .replaceAll("\\\\X0D\\\\", "\r")
        ;
    }
}
