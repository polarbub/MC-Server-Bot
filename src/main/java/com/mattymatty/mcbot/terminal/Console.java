package com.mattymatty.mcbot.terminal;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.graphics.PropertyTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.menu.Menu;
import com.googlecode.lanterna.gui2.menu.MenuBar;
import com.googlecode.lanterna.gui2.menu.MenuItem;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.mattymatty.mcbot.Config;
import com.mattymatty.mcbot.Main;
import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.minecraft.Server;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public class Console {
    private final Server server;
    private final Bot bot;
    private final Config config;

    private final Theme log_theme = LanternaThemes.getRegisteredTheme("conqueror");
    private final Theme btn_theme;

    private TextBox chat_log;
    private TextBox console_log;
    private TextBox bot_log;
    private TextBox active_log;

    private Component log_component;
    private Component chat_component;
    private Component console_component;

    private Button log_btn;
    private Button chat_btn;
    private Button console_btn;

    public Console(Server server, Bot bot, Config config) {
        this.server = server;
        this.bot = bot;
        this.config = config;
        Properties properties = new Properties();
        try {
            ClassLoader classLoader = AbstractTextGUI.class.getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("local_theme.properties");
            if (resourceAsStream == null) {
                resourceAsStream = new FileInputStream("src/main/resources/local_theme.properties");
            }
            properties.load(resourceAsStream);
            resourceAsStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        btn_theme = new PropertyTheme(properties);
    }

    public void start() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createHeadlessTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLACK_BRIGHT));

        Window window = createWindow();
        Panel mainPanel = makeMainPannel(window);
        window.setComponent(mainPanel.withBorder(Borders.singleLine(" MC -> Discord ")));
        setup();
        gui.addWindowAndWait(window);
    }

    private void setup() {
        this.console_component.setVisible(false);
        this.chat_component.setVisible(false);
        this.log_btn.setEnabled(false);

        this.log_btn.addListener(l -> {
            this.active_log = this.bot_log;
            this.log_component.setVisible(true);
            this.console_component.setVisible(false);
            this.chat_component.setVisible(false);
            this.log_btn.setEnabled(false);
            this.chat_btn.setEnabled(true);
            this.console_btn.setEnabled(true);
        });

        this.chat_btn.addListener(l -> {
            this.active_log = this.chat_log;
            this.log_component.setVisible(false);
            this.chat_component.setVisible(true);
            this.console_component.setVisible(false);
            this.log_btn.setEnabled(true);
            this.chat_btn.setEnabled(false);
            this.console_btn.setEnabled(true);
        });

        this.console_btn.addListener(l -> {
            this.active_log = this.console_log;
            this.log_component.setVisible(false);
            this.chat_component.setVisible(false);
            this.console_component.setVisible(true);
            this.log_btn.setEnabled(true);
            this.chat_btn.setEnabled(true);
            this.console_btn.setEnabled(false);
        });

        server.addChatListener(s -> {
            while (this.chat_log.getLineCount() > this.active_log.getSize().getRows() - 1) {
                this.chat_log.removeLine(0);
            }
            this.chat_log.addLine(s);
        });

        server.addConsoleListener(s -> {
            while (this.console_log.getLineCount() > this.active_log.getSize().getRows() - 1) {
                this.console_log.removeLine(0);
            }
            this.console_log.addLine(s);
        });

        server.addErrorListener(s -> {
            String[] lines = s.split("\n");
            Arrays.stream(lines).skip(1).forEach(l->{
                while (this.console_log.getLineCount() > this.active_log.getSize().getRows() - 1) {
                    this.console_log.removeLine(0);
                }
                this.console_log.addLine(l);
            });
        });

        Main.LOG.addLogListener(s -> {
            while (this.bot_log.getLineCount() > this.active_log.getSize().getRows() - 1) {
                this.bot_log.removeLine(0);
            }
            this.bot_log.addLine(s);
        });

    }

    private Window createWindow() {
        BasicWindow window = new BasicWindow();
        window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
        return window;
    }

    private Panel makeMainPannel(Window window) {
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new BorderLayout());

        Panel tabPanel = makeTabPanel();
        tabPanel.setPreferredSize(new TerminalSize(5000, 1));
        tabPanel.setLayoutData(BorderLayout.Location.TOP);
        mainPanel.addComponent(tabPanel);

        Panel log_panel = makeLogPanel();
        log_panel.setLayoutData(BorderLayout.Location.CENTER);
        this.log_component = log_panel.withBorder(Borders.singleLine(" Log "));
        mainPanel.addComponent(this.log_component);

        Panel chat_panel = makeChatPanel();
        chat_panel.setLayoutData(BorderLayout.Location.CENTER);
        this.chat_component = chat_panel.withBorder(Borders.singleLine(" Chat "));
        mainPanel.addComponent(this.chat_component);

        Panel console_panel = makeConsolePanel();
        console_panel.setLayoutData(BorderLayout.Location.CENTER);
        this.console_component = console_panel.withBorder(Borders.singleLine(" Console "));
        mainPanel.addComponent(this.console_component);


        return mainPanel;
    }

    private Panel makeTabPanel() {
        Panel tabPanel = new Panel();
        tabPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));

        this.log_btn = new Button("Log");
        this.chat_btn = new Button("Chat");
        this.console_btn = new Button("Console");

        log_btn.setTheme(btn_theme);
        chat_btn.setTheme(btn_theme);
        console_btn.setTheme(btn_theme);

        tabPanel.addComponent(this.log_btn);
        tabPanel.addComponent(this.chat_btn);
        tabPanel.addComponent(this.console_btn);

        MenuBar menu = makeMenu();
        menu.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.End));

        EmptySpace filler = new EmptySpace();
        filler.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));
        tabPanel.addComponent(filler);
        tabPanel.addComponent(menu);

        return tabPanel;
    }

    private MenuBar makeMenu() {
        MenuBar menuBar = new MenuBar();

        Menu commands = new Menu("Commands");

        commands.add(new MenuItem("  Start  ", server::start));
        commands.add(new MenuItem("  Stop  ", server::stop));

        menuBar.add(commands);
        return menuBar;
    }

    private Panel makeChatPanel() {
        Panel chatPanel = new Panel();
        chatPanel.setLayoutManager(new BorderLayout());

        this.chat_log = new TextBox("Chat", TextBox.Style.MULTI_LINE) {
            @Override
            protected TextBoxRenderer createDefaultRenderer() {
                DefaultTextBoxRenderer def = new DefaultTextBoxRenderer();
                def.setHideScrollBars(true);
                return def;
            }
        };
        chat_log.setTheme(log_theme);
        chat_log.setReadOnly(true);
        chat_log.setLayoutData(BorderLayout.Location.CENTER);
        chatPanel.addComponent(chat_log);

        TextBox chat_input = new TextBox("input") {
            @Override
            public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
                if (keyStroke.getKeyType() != KeyType.Enter)
                    return super.handleKeyStroke(keyStroke);
                else {
                    String input = getText();
                    setText("");
                    server.command("say " + input);
                    return Result.HANDLED;
                }
            }
        };
        ;
        chat_input.setLayoutData(BorderLayout.Location.BOTTOM);
        chat_input.setPreferredSize(new TerminalSize(5000, 1));
        chatPanel.addComponent(chat_input.withBorder(Borders.singleLine("Input")));

        return chatPanel;
    }

    private Panel makeLogPanel() {
        Panel logPanel = new Panel();
        logPanel.setLayoutManager(new BorderLayout());

        this.bot_log = new TextBox("Log", TextBox.Style.MULTI_LINE) {
            @Override
            protected TextBoxRenderer createDefaultRenderer() {
                DefaultTextBoxRenderer def = new DefaultTextBoxRenderer();
                def.setHideScrollBars(true);
                return def;
            }
        };
        bot_log.setTheme(log_theme);
        bot_log.setReadOnly(true);
        bot_log.setLayoutData(BorderLayout.Location.CENTER);
        logPanel.addComponent(bot_log);

        return logPanel;
    }

    private Panel makeConsolePanel() {
        Panel consolePanel = new Panel();
        consolePanel.setLayoutManager(new BorderLayout());

        this.console_log = new TextBox("Console", TextBox.Style.MULTI_LINE) {
            @Override
            protected TextBoxRenderer createDefaultRenderer() {
                DefaultTextBoxRenderer def = new DefaultTextBoxRenderer();
                def.setHideScrollBars(true);
                return def;
            }
        };
        console_log.setTheme(log_theme);
        console_log.setReadOnly(true);
        console_log.setLayoutData(BorderLayout.Location.CENTER);
        consolePanel.addComponent(console_log);

        TextBox chat_input = new TextBox("input") {
            @Override
            public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
                if (keyStroke.getKeyType() != KeyType.Enter)
                    return super.handleKeyStroke(keyStroke);
                else {
                    String input = getText();
                    setText("");
                    server.command(input);
                    return Result.HANDLED;
                }
            }
        };
        chat_input.setLayoutData(BorderLayout.Location.BOTTOM);
        consolePanel.addComponent(chat_input.withBorder(Borders.singleLine("Input")));

        return consolePanel;
    }
}
