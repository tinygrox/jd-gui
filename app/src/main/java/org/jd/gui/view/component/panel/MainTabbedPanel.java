/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.view.component.panel;

import org.jd.gui.api.API;
import org.jd.gui.api.feature.*;
import org.jd.gui.service.platform.PlatformService;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MainTabbedPanel<T extends JComponent & UriGettable> extends TabbedPanel<T> implements UriOpenable, PreferencesChangeListener, PageChangeListener {
    protected ArrayList<PageChangeListener> pageChangedListeners = new ArrayList<>();
    // Flag to prevent the event cascades
    protected boolean pageChangedListenersEnabled = true;

    public MainTabbedPanel(API api) {
        super(api);
    }

    @Override
    public void create() {
        setLayout(cardLayout = new CardLayout());

        Color bg = darker(getBackground());

        if (PlatformService.getInstance().isWindows()) {
            setBackground(bg);
        }

        // panel //
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(bg);

        Color fontColor = panel.getBackground().darker();

        panel.add(Box.createHorizontalGlue());

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(panel.getBackground());
        box.add(Box.createVerticalGlue());

        JLabel title = newLabel("没有打开的文件", fontColor);//No files are open
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize()+8));

        box.add(title);
        box.add(newLabel("从菜单中打开一个文件 \"文件 > 打开文件...\"", fontColor));//Open a file with menu""File>Open File"
        box.add(newLabel("使用菜单打开最近使用的文件 \"文件 > 最近使用\"", fontColor));//Recent Files
        box.add(newLabel("从" + getFileManagerLabel() + "中拖拽文件到此", fontColor));//Drag and drop files from
        box.add(Box.createVerticalGlue());

        panel.add(box);
        panel.add(Box.createHorizontalGlue());
        add("panel", panel);

        // tabs //
        tabbedPane = createTabPanel();
        tabbedPane.addChangeListener(e -> {
            if (pageChangedListenersEnabled) {
                JComponent subPage = (JComponent)tabbedPane.getSelectedComponent();

                if (subPage == null) {
                    // Fire page changed event
                    for (PageChangeListener listener : pageChangedListeners) {
                        listener.pageChanged(null);
                    }
                } else {
                    T page = (T)subPage.getClientProperty("currentPage");

                    if (page == null) {
                        page = (T)tabbedPane.getSelectedComponent();
                    }
                    // Fire page changed event
                    for (PageChangeListener listener : pageChangedListeners) {
                        listener.pageChanged(page);
                    }
                    // Update current sub-page preferences
                    if (subPage instanceof PreferencesChangeListener) {
                        ((PreferencesChangeListener)subPage).preferencesChanged(preferences);
                    }
                }
            }
        });
		add("tabs", tabbedPane);

		setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, darker(darker(bg))));
	}

	protected String getFileManagerLabel() {
        switch (PlatformService.getInstance().getOs()) {
            case Linux:
                return "your file manager";
            case MacOSX:
                return "the Finder";
            default:
                return "资源管理器";//Explorer
        }
    }

	protected JLabel newLabel(String text, Color fontColor) {
        JLabel label = new JLabel(text);
        label.setForeground(fontColor);
        return label;
    }

    @Override
    public void addPage(String title, Icon icon, String tip, T page) {
        super.addPage(title, icon, tip, page);
        if (page instanceof PageChangeable) {
            ((PageChangeable)page).addPageChangeListener(this);
        }
    }

    public List<T> getPages() {
        int i = tabbedPane.getTabCount();
        ArrayList<T> pages = new ArrayList<>(i);
        while (i-- > 0) {
            pages.add((T)tabbedPane.getComponentAt(i));
        }
        return pages;
    }

    public ArrayList<PageChangeListener> getPageChangedListeners() {
        return pageChangedListeners;
    }

    // --- URIOpener --- //
    @Override
    public boolean openUri(URI uri) {
        try {
            // Disable page changed event
            pageChangedListenersEnabled = false;
            // Search & display main tab
            T page = showPage(uri);

            if (page != null) {
                if (page instanceof UriOpenable) {
                    // Enable page changed event
                    pageChangedListenersEnabled = true;
                    // Search & display sub tab
                    return ((UriOpenable)page).openUri(uri);
                }
                return true;
            }
        } finally {
            // Enable page changed event
            pageChangedListenersEnabled = true;
        }

        return false;
    }

    // --- PageChangedListener --- //
    @Override
    public <T extends JComponent & UriGettable> void pageChanged(T page) {
        // Store active page for current sub tabbed pane
        Component subPage = tabbedPane.getSelectedComponent();

        if (subPage != null) {
            ((JComponent)subPage).putClientProperty("currentPage", page);
        }

        if (page == null) {
            page = (T)subPage;
        }

        // Forward event
        for (PageChangeListener listener : pageChangedListeners) {
            listener.pageChanged(page);
        }
    }

    // --- PreferencesChangeListener --- //
    @Override
    public void preferencesChanged(Map<String, String> preferences) {
        super.preferencesChanged(preferences);

        // Update current sub-page preferences
        Component subPage = tabbedPane.getSelectedComponent();
        if (subPage instanceof PreferencesChangeListener) {
            ((PreferencesChangeListener)subPage).preferencesChanged(preferences);
        }
    }
}
