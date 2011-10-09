/**
 * This is part of the Java SyntaxHighlighter.
 * 
 * It is distributed under MIT license. See the file 'readme.txt' for
 * information on usage and redistribution of this file, and for a
 * DISCLAIMER OF ALL WARRANTIES.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
package syntaxhighlighter;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.IconView;
import javax.swing.text.JTextComponent;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import syntaxhighlighter.Parser.MatchResult;

/**
 * The text pane for the script text.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SyntaxHighlighterPane extends JTextPane {

    private static final long serialVersionUID = 1L;
    /**
     * The line number offset. E.g. set offset to 9 will make the first line number to appear equals 1 + 9 = 10
     */
    private int lineNumberOffset;
    /**
     * The background color of the highlighted line. Default is black.
     */
    private Color highlightedBackground;
    /**
     * Indicator that indicate to turn on the mouse-over-highlight effect or not. See {@link #setHighlightWhenMouseOver(boolean)}.
     */
    private boolean highlightWhenMouseOver;
    /**
     * The list of line numbers that needed to be highlighted.
     */
    protected final List<Integer> highlightedLineList;
    /**
     * The highlighter painter used to do the highlight line effect.
     */
    protected Highlighter.HighlightPainter highlightPainter;
    /**
     * The theme.
     */
    protected Theme theme;
    /**
     * The style list. see {@link #setStyle(java.util.Map)}.
     */
    protected Map<String, List<MatchResult>> styleList;
    /**
     * Record the mouse cursor is currently pointing at which line of the document. -1 means not any line.
     * It is used internally.
     */
    protected int mouseOnLine;

    public SyntaxHighlighterPane() {
        super();

        setEditable(false);
        //<editor-fold defaultstate="collapsed" desc="editor kit">
        setEditorKit(new StyledEditorKit() {

            private static final long serialVersionUID = 1L;

            @Override
            public ViewFactory getViewFactory() {
                return new ViewFactory() {

                    @Override
                    public View create(Element elem) {
                        String kind = elem.getName();
                        if (kind != null) {
                            if (kind.equals(AbstractDocument.ContentElementName)) {
                                return new LabelView(elem) {

                                    @Override
                                    public int getBreakWeight(int axis, float pos, float len) {
                                        return 0;
                                    }
                                };
                            } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                                return new ParagraphView(elem) {

                                    @Override
                                    public int getBreakWeight(int axis, float pos, float len) {
                                        return 0;
                                    }
                                };
                            } else if (kind.equals(AbstractDocument.SectionElementName)) {
                                return new BoxView(elem, View.Y_AXIS);
                            } else if (kind.equals(StyleConstants.ComponentElementName)) {
                                return new ComponentView(elem) {

                                    @Override
                                    public int getBreakWeight(int axis, float pos, float len) {
                                        return 0;
                                    }
                                };
                            } else if (kind.equals(StyleConstants.IconElementName)) {
                                return new IconView(elem);
                            }
                        }
                        return new LabelView(elem) {

                            @Override
                            public int getBreakWeight(int axis, float pos, float len) {
                                return 0;
                            }
                        };
                    }
                };
            }
        });
        //</editor-fold>

        lineNumberOffset = 0;

        //<editor-fold defaultstate="collapsed" desc="highlighter painter">
        highlightedBackground = Color.black;
        highlightWhenMouseOver = true;
        highlightedLineList = new ArrayList<Integer>();

        highlightPainter = new Highlighter.HighlightPainter() {

            @Override
            public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
                if (c.getParent() == null) {
                    return;
                }

                // get the Y-axis value of the visible area of the text component
                int startY = Math.abs(c.getY());
                int endY = startY + c.getParent().getHeight();

                FontMetrics textPaneFontMetrics = g.getFontMetrics(getFont());
                int textPaneFontHeight = textPaneFontMetrics.getHeight();

                int largerestLineNumber = c.getDocument().getDefaultRootElement().getElementCount();

                g.setColor(highlightedBackground);

                // draw the highlight background to the highlighted line
                synchronized (highlightedLineList) {
                    for (Integer lineNumber : highlightedLineList) {
                        if (lineNumber > largerestLineNumber + lineNumberOffset) {
                            // skip those line number that out of range
                            continue;
                        }
                        // get the Y-axis value of this highlighted line
                        int _y = Math.max(0, textPaneFontHeight * (lineNumber - lineNumberOffset - 1));
                        if (_y > endY || _y + textPaneFontHeight < startY) {
                            // this line is out of visible area, skip it
                            continue;
                        }
                        // draw the highlighted background
                        g.fillRect(0, _y, c.getWidth(), textPaneFontHeight);
                    }
                }

                // draw the mouse-over-highlight effect
                if (mouseOnLine != -1) {
                    if (mouseOnLine <= largerestLineNumber + lineNumberOffset) {
                        int _y = Math.max(0, textPaneFontHeight * (mouseOnLine - lineNumberOffset - 1));
                        if (_y < endY && _y + textPaneFontHeight > startY) {
                            // the line is within the range of visible area
                            g.fillRect(0, _y, c.getWidth(), textPaneFontHeight);
                        }
                    }
                }
            }
        };
        try {
            getHighlighter().addHighlight(0, 0, highlightPainter);
        } catch (BadLocationException ex) {
            Logger.getLogger(SyntaxHighlighterPane.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        mouseOnLine = -1;

        //<editor-fold defaultstate="collapsed" desc="mouse listener">
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                if (!highlightWhenMouseOver) {
                    return;
                }
                mouseOnLine = -1;
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!highlightWhenMouseOver) {
                    return;
                }

                Element defaultRootElement = getDocument().getDefaultRootElement();
                // get the position of the document the mouse cursor is pointing
                int documentOffsetStart = viewToModel(e.getPoint());

                // the line number that the mouse cursor is currently pointing
                int lineNumber = documentOffsetStart == -1 ? -1 : defaultRootElement.getElementIndex(documentOffsetStart) + 1 + lineNumberOffset;
                if (lineNumber == defaultRootElement.getElementCount()) {
                    // if the line number got is the last line, check if the cursor is actually on the line or already below the line
                    try {
                        Rectangle rectangle = modelToView(documentOffsetStart);
                        if (e.getY() > rectangle.y + rectangle.height) {
                            lineNumber = -1;
                        }
                    } catch (BadLocationException ex) {
                        Logger.getLogger(SyntaxHighlighterPane.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                // only repaint when the line number changed
                if (mouseOnLine != lineNumber) {
                    mouseOnLine = lineNumber;
                    repaint();
                }
            }
        });
        //</editor-fold>
    }

    @Override
    public void setHighlighter(Highlighter highlighter) {
        if (highlightPainter != null) {
            getHighlighter().removeHighlight(highlightPainter);
            try {
                highlighter.addHighlight(0, 0, highlightPainter);
            } catch (BadLocationException ex) {
                Logger.getLogger(SyntaxHighlighterPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        super.setHighlighter(highlighter);
    }

    /**
     * Set the content of the syntax highlighter. It is better to set other settings first and set this the last.
     * @param content the content to set
     */
    public void setContent(String content) {
        DefaultStyledDocument document = (DefaultStyledDocument) getDocument();

        try {
            document.remove(0, document.getLength());
            if (theme != null) {
                document.insertString(0, content, theme.getPlain().getAttributeSet());
            } else {
                document.insertString(0, content, new SimpleAttributeSet());
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(SyntaxHighlighterPane.class.getName()).log(Level.SEVERE, null, ex);
        }

        setCaretPosition(0);

        // clear the style list
        styleList = null;
    }

    /**
     * Apply the list of style to the script text pane. See {@link syntaxhighlighter.Parser#parse(syntaxhighlighter.Brush, boolean, char[], int, int)}.
     * @param styleList the style list
     */
    public void setStyle(Map<String, List<MatchResult>> styleList) {
        // not a deep copy
        this.styleList = new HashMap<String, List<MatchResult>>(styleList);

        if (theme == null) {
            return;
        }

        DefaultStyledDocument document = (DefaultStyledDocument) getDocument();
        // clear all the existing style
        document.setCharacterAttributes(0, document.getLength(), theme.getPlain().getAttributeSet(), true);

        // apply style according to the style list
        for (String key : styleList.keySet()) {
            List<MatchResult> posList = styleList.get(key);

            AttributeSet attributeSet = theme.getStyle(key).getAttributeSet();
            for (MatchResult pos : posList) {
                document.setCharacterAttributes(pos.getOffset(), pos.getLength(), attributeSet, true);
            }
        }

        repaint();
    }

    /**
     * Get current theme.
     * @return the current theme
     */
    public Theme getTheme() {
        return theme;
    }

    /**
     * Set the theme.
     * @param theme the theme
     */
    public void setTheme(Theme theme) {
        this.theme = theme;

        setFont(theme.getFont());
        setBackground(theme.getBackground());
        setHighlightedBackground(theme.getHighlightedBackground());

        if (styleList != null) {
            setStyle(styleList);
        }
    }

    /**
     * Get the line number offset
     * @return the offset
     */
    public int getLineNumberOffset() {
        return lineNumberOffset;
    }

    /**
     * Set the line number offset. E.g. set offset to 9 will make the first line number to appear equals 1 + 9 = 10
     * @param offset the offset
     */
    public void setLineNumberOffset(int offset) {
        lineNumberOffset = Math.max(lineNumberOffset, offset);
        repaint();
    }

    /**
     * Get the color of the highlighted background. Default is black.
     * @return the color
     */
    public Color getHighlightedBackground() {
        return highlightedBackground;
    }

    /**
     * Set the color of the highlighted background. Default is black.
     * @param highlightedBackground the color
     */
    public void setHighlightedBackground(Color highlightedBackground) {
        this.highlightedBackground = highlightedBackground;
        repaint();
    }

    /**
     * Get the status of the mouse-over-highlight effect. Default is turned on.
     * @return true if turned on, false if turned off
     */
    public boolean isHighlightWhenMouseOver() {
        return highlightWhenMouseOver;
    }

    /**
     * Set turn on the mouse-over-highlight effect or not. Default is turned on.
     * If set true, there will be a highlight line effect on the line the mouse cursor is pointing (on the script text panel only, not also the line number panel).
     * @param highlightWhenMouseOver true to turn on, false to turn off
     */
    public void setHighlightWhenMouseOver(boolean highlightWhenMouseOver) {
        this.highlightWhenMouseOver = highlightWhenMouseOver;
        if (!highlightWhenMouseOver) {
            mouseOnLine = -1;
        }
        repaint();
    }

    /**
     * Get the list of highlighted lines.
     * @return a copy of the list
     */
    public List<Integer> getHighlightedLineList() {
        List<Integer> returnList;
        synchronized (highlightedLineList) {
            returnList = new ArrayList<Integer>(highlightedLineList);
        }
        return returnList;
    }

    /**
     * Set highlighted lines. Note that this will clear all previous recorded highlighted lines.
     * @param highlightedLineList the list that contain the highlighted lines
     */
    public void setHighlightedLineList(List<Integer> highlightedLineList) {
        synchronized (this.highlightedLineList) {
            this.highlightedLineList.clear();
            this.highlightedLineList.addAll(highlightedLineList);
        }
        repaint();
    }

    /**
     * Add highlighted line.
     * @param lineNumber the line number to highlight
     */
    public void addHighlightedLine(int lineNumber) {
        highlightedLineList.add(lineNumber);
        repaint();
    }

    /**
     * Set the <code>font</code> according to <code>bold</code> and <code>italic</code>.
     * @param font the font to set
     * @param bold true to set bold, false not
     * @param italic true to set italic, false not
     * @return the font with bold and italic changed
     */
    protected static Font setFont(Font font, boolean bold, boolean italic) {
        if ((font.getStyle() & Font.ITALIC) != 0) {
            if (!bold) {
                return font.deriveFont(font.getStyle() ^ Font.BOLD);
            }
        } else {
            if (bold) {
                return font.deriveFont(font.getStyle() | Font.BOLD);
            }
        }
        if ((font.getStyle() & Font.ITALIC) != 0) {
            if (!italic) {
                return font.deriveFont(font.getStyle() ^ Font.ITALIC);
            }
        } else {
            if (italic) {
                return font.deriveFont(font.getStyle() | Font.ITALIC);
            }
        }
        return font;
    }
}
