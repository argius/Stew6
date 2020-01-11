package stew6.ui.swing;

import java.awt.*;
import java.util.Map.*;
import javax.swing.*;
import javax.swing.UIDefaults.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

final class FontControlLookAndFeel extends BasicLookAndFeel {

    private static final LookAndFeel BASE = UIManager.getLookAndFeel();

    private final RelativeFont relativeFont;

    FontControlLookAndFeel(String fontFamily, int fontStyleMask, double sizeRate) {
        this.relativeFont = new RelativeFont(fontFamily, fontStyleMask, sizeRate);
    }

    static void change(String fontFamily, int fontStyleMask, double sizeRate) {
        try {
            UIManager.setLookAndFeel(new FontControlLookAndFeel(fontFamily, fontStyleMask, sizeRate));
        } catch (UnsupportedLookAndFeelException ex) {
            throw new IllegalStateException(ex.toString());
        }
    }

    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = BASE.getDefaults();
        for (Entry<?, Object> entry : defaults.entrySet()) {
            Object key = entry.getKey();
            if (String.valueOf(key).endsWith("font")) {
                Object value = entry.getValue();
                if (value instanceof ActiveValue) {
                    entry.setValue(new FontControlActiveValue((ActiveValue)value, relativeFont));
                } else if (value instanceof LazyValue) {
                    entry.setValue(new FontControlLazyValue((LazyValue)value, relativeFont));
                }
            }
        }
        return defaults;
    }

    @Override
    public String getDescription() {
        return BASE.getDescription();
    }

    @Override
    public String getID() {
        return BASE.getID();
    }

    @Override
    public String getName() {
        return BASE.getName();
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return BASE.isNativeLookAndFeel();
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return BASE.isSupportedLookAndFeel();
    }

    private static final class RelativeFont {

        private final String fontFamily;
        private final int fontStyleMask;
        private final double sizeRate;

        RelativeFont(String fontFamily, int fontStyleMask, double sizeRate) {
            this.fontFamily = fontFamily;
            this.fontStyleMask = fontStyleMask;
            this.sizeRate = sizeRate;
        }

        Object wrapIfObjectIsFont(Object o) {
            if (o instanceof Font) {
                return createFontUIResource((Font)o);
            }
            return o;
        }

        FontUIResource createFontUIResource(Font font) {
            final int style = font.getStyle() & fontStyleMask;
            final int size = (int)(font.getSize() * sizeRate);
            return new FontUIResource(fontFamily, style, size);
        }

    }

    private static final class FontControlActiveValue implements ActiveValue {

        private final ActiveValue base;
        private final RelativeFont relativeFont;

        FontControlActiveValue(ActiveValue base, RelativeFont relativeFont) {
            this.base = base;
            this.relativeFont = relativeFont;
        }

        @Override
        public Object createValue(UIDefaults table) {
            return relativeFont.wrapIfObjectIsFont(base.createValue(table));
        }

    }

    private static final class FontControlLazyValue implements LazyValue {

        private final LazyValue base;
        private final RelativeFont relativeFont;

        FontControlLazyValue(LazyValue base, RelativeFont relativeFont) {
            this.base = base;
            this.relativeFont = relativeFont;
        }

        @Override
        public Object createValue(UIDefaults table) {
            return relativeFont.wrapIfObjectIsFont(base.createValue(table));
        }

    }

}
