const {
    deck, baseSlide, titleBar, footer, codeBox, statCard,
    DARK, PANEL, ACCENT, MUTED, WHITE, CODE,
} = require('./make-episode1-deck.js');

// ---------- Slide 1: Title ----------
(function () {
    const s = baseSlide();
    s.addShape(deck.ShapeType.rect, { x: 0, y: 0, w: 13.333, h: 7.5, fill: { color: DARK } });
    s.addShape(deck.ShapeType.rect, { x: 0, y: 0, w: 0.25, h: 7.5, fill: { color: ACCENT } });
    s.addText('Episode 1', { x: 0.9, y: 2.1, w: 11.5, h: 0.6, fontSize: 20, color: ACCENT, fontFace: 'Arial', bold: true });
    s.addText('This App Skips Its Own Login Screen', { x: 0.9, y: 2.65, w: 11.5, h: 1.4, fontSize: 48, color: WHITE, fontFace: 'Arial', bold: true });
    s.addText('A JavaFX walkthrough of the Clash of Clans final project', { x: 0.9, y: 4.15, w: 11.5, h: 0.5, fontSize: 17, color: MUTED, fontFace: 'Arial' });
    s.addText('Trace main() \u2192 launch() \u2192 start() \u2014 then fix the entry point.', { x: 0.9, y: 4.75, w: 11.5, h: 0.45, fontSize: 14, color: MUTED, fontFace: 'Arial', italic: true });
    s.addText('Branch: new-gui  \u2022  \u224812 min  \u2022  Java devs new to JavaFX', { x: 0.9, y: 6.4, w: 11.5, h: 0.4, fontSize: 12, color: MUTED, fontFace: 'Arial' });
})();

// ---------- Slide 2: The hook ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'The app skips its own login screen', 'The problem');
    footer(s, 2);

    s.addText('There is a login screen in the repo \u2014 but nothing ever opens it.', {
        x: 0.7, y: 1.75, w: 6.4, h: 1.4, fontSize: 17, color: WHITE, fontFace: 'Arial', bold: true, lineSpacing: 1.1,
    });
    s.addText('Run the project and it boots straight into PlayerPanel: map, stats, everything \u2014 no username or password prompt.', {
        x: 0.7, y: 3.35, w: 6.4, h: 1.6, fontSize: 14, color: MUTED, fontFace: 'Arial', lineSpacing: 1.2,
    });

    codeBox(s, 7.5, 1.8, 5.1, 3.6, [
        'src/com/coc/GUI.java',
        'start(Stage stage) {',
        '    LoadData playerData = new LoadData();',
        '    Player player =',
        '        new LogInController(...).getPlayer("", "");',
        '    new PlayerPanel(player, ...).start(new Stage());',
        '}',
    ]);

    statCard(s, 0.7, 5.35, 3.0, 1.5, '0', 'screens shown', ACCENT);
    statCard(s, 3.9, 5.35, 3.0, 1.5, '1', 'fabricated Player', ACCENT);
    statCard(s, 7.1, 5.35, 3.0, 1.5, '0', 'logins required', ACCENT);
})();

// ---------- Slide 3: Code anatomy ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'GUI.java: main() \u2192 launch() \u2192 start()', 'The entry point');
    footer(s, 3);

    codeBox(s, 0.7, 1.75, 7.4, 4.2, [
        'package com.coc;',
        'import controller.LogInController;',
        'import javafx.application.Application;',
        'import javafx.stage.Stage;',
        '',
        'public class GUI extends Application {',
        '    public static void main(String[] args) {',
        '        launch();',
        '    }',
        '    @Override',
        '    public void start(Stage stage) {',
        '        LoadData playerData = new LoadData();',
        '        Player player =',
        '            new LogInController(playerData.getPlayers())',
        '            .getPlayer("", "");',
        '        new PlayerPanel(player, playerData.getPlayers())',
        '            .start(new Stage());',
        '    }',
        '}',
    ]);

    s.addText('main() calls launch() \u2014 the JavaFX static entry point. The runtime starts the FX thread, instantiates GUI, and calls start(Stage).', {
        x: 0.7, y: 6.15, w: 7.4, h: 0.9, fontSize: 12.5, color: MUTED, fontFace: 'Arial', lineSpacing: 1.1,
    });

    s.addText('Concepts this episode:', { x: 8.5, y: 1.75, w: 4.1, h: 0.35, fontSize: 14, color: ACCENT, fontFace: 'Arial', bold: true });
    s.addText('\u2022 Application', { x: 8.5, y: 2.15, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 launch()', { x: 8.5, y: 2.55, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 Stage / Scene (first sight)', { x: 8.5, y: 2.95, w: 4.1, h: 0.55, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 JavaFX app lifecycle', { x: 8.5, y: 3.45, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('Why module-info needs javafx.controls', { x: 8.5, y: 4.0, w: 4.1, h: 0.55, fontSize: 14, color: WHITE, fontFace: 'Arial' });
})();

// ---------- Slide 4: The fabricated player ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'getPlayer("", "") invents a player', 'The bug');
    footer(s, 4);

    codeBox(s, 0.7, 1.75, 7.4, 4.4, [
        'public Player getPlayer(String idPlayer, String password) {',
        '    for (Player player : players) {',
        '        if (player.getIdPlayer().equals(idPlayer) &&',
        '            player.getPassword().equals(password)) {',
        '            return player;',
        '        }',
        '    }',
        '    return new Player(new MapLoader(4, "map4"), "", "");',  // <-- fabricated
        '    //throw new Exception("username or password is wrong!");',
        '}',
    ]);
    s.addText('src/controller/LogInController.java:21', {
        x: 0.7, y: 6.25, w: 7.4, h: 0.35, fontSize: 11, color: MUTED, fontFace: 'Consolas',
    });

    s.addText('With empty credentials, no real player matches. So line 21 returns a fabricated Player instead of throwing \u2014 and GUI.java hands that fake player straight to PlayerPanel. No login ever happens.', {
        x: 8.5, y: 1.85, w: 4.1, h: 2.6, fontSize: 13, color: MUTED, fontFace: 'Arial', lineSpacing: 1.2,
    });
    s.addText('Line 22 shows the real behavior was deliberately commented out.', {
        x: 8.5, y: 4.6, w: 4.1, h: 0.7, fontSize: 13, color: ACCENT, fontFace: 'Arial', bold: true, lineSpacing: 1.1,
    });
})();

// ---------- Slide 5: Module path ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'Running the app: module-info.java', 'The module path');
    footer(s, 5);

    codeBox(s, 0.7, 1.75, 7.4, 4.0, [
        'module com.coc {',
        '    requires javafx.controls;',
        '    requires javafx.fxml;',
        '    requires javafx.media;',
        '    requires javafx.base;',
        '    requires javafx.graphics;',
        '    requires assets;',
        '    requires org.yaml.snakeyaml;',
        '    requires cr0s.javara.main;',
        '}',
    ]);
    s.addText('src/module-info.java', { x: 0.7, y: 5.85, w: 7.4, h: 0.3, fontSize: 11, color: MUTED, fontFace: 'Consolas' });

    s.addText('The module system gates what JavaFX packages are available. Application, Stage, Scene all live in javafx.controls \u2014 without that require, the code won\u2019t compile on the module path. Ant run target: build.xml:61.', {
        x: 0.7, y: 6.25, w: 7.4, h: 0.9, fontSize: 12.5, color: MUTED, fontFace: 'Arial', lineSpacing: 1.1,
    });

    s.addText('What you see on screen', { x: 8.5, y: 1.85, w: 4.1, h: 0.35, fontSize: 14, color: ACCENT, fontFace: 'Arial', bold: true });
    s.addText('\u2022 module-info.java', { x: 8.5, y: 2.25, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 build.xml run target', { x: 8.5, y: 2.65, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 classpath vs module path', { x: 8.5, y: 3.05, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('Recurring thread: assets module + module-info requirements (returns in Ep 4)', { x: 8.5, y: 3.65, w: 4.1, h: 0.9, fontSize: 13, color: MUTED, fontFace: 'Arial', lineSpacing: 1.2, italic: true });
})();

// ---------- Slide 6: The refactor ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'Refactor: start at LogIn', 'The fix');
    footer(s, 6);

    s.addText('Replace GUI.start() so it opens the real LogIn screen instead of fabricating a player:', {
        x: 0.7, y: 1.75, w: 7.4, h: 0.75, fontSize: 14, color: WHITE, fontFace: 'Arial', lineSpacing: 1.1,
    });

    codeBox(s, 0.7, 2.6, 7.4, 3.4, [
        'public void start(Stage stage) {',
        '    LoadData playerData = new LoadData();',
        '    new LogIn(playerData.getPlayers()).start(stage);',
        '}',
    ]);
    s.addText('Reuses the given Stage \u2014 no new Stage per screen yet (that\u2019s Ep 2).', {
        x: 0.7, y: 6.1, w: 7.4, h: 0.8, fontSize: 12, color: MUTED, fontFace: 'Arial', lineSpacing: 1.1,
    });

    s.addText('Verify:', { x: 8.5, y: 1.85, w: 4.1, h: 0.35, fontSize: 14, color: ACCENT, fontFace: 'Arial', bold: true });
    s.addText('\u2022 App opens on LogIn', { x: 8.5, y: 2.25, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 PlayerPanel only after login', { x: 8.5, y: 2.65, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('\u2022 App still runnable', { x: 8.5, y: 3.05, w: 4.1, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial' });
    s.addText('Recurring thread: one Application per screen (\u201cnew X(...).start(new Stage())\u201d) \u2014 payoff in Ep 2 & 6.', { x: 8.5, y: 3.75, w: 4.1, h: 1.4, fontSize: 13, color: MUTED, fontFace: 'Arial', lineSpacing: 1.2, italic: true });
})();

// ---------- Slide 7: Demo result ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'Before \u2192 after', 'Live demo');
    footer(s, 7);

    s.addText('Before: launch \u2192 PlayerPanel (no login shown).', { x: 0.7, y: 1.8, w: 6.0, h: 0.5, fontSize: 14, color: MUTED, fontFace: 'Arial', bold: true });
    s.addText('After: launch \u2192 LogIn \u2192 (on success) PlayerPanel.', { x: 0.7, y: 2.4, w: 6.0, h: 0.5, fontSize: 14, color: ACCENT, fontFace: 'Arial', bold: true });

    s.addText('On-screen beats this episode:', { x: 0.7, y: 3.3, w: 6.0, h: 0.35, fontSize: 14, color: WHITE, fontFace: 'Arial', bold: true });
    const beats = [
        '1. Run the app \u2014 observe it lands on PlayerPanel, without LogIn.',
        '2. Inspect LogInController.getPlayer("", "") \u2014 fabricated Player at line 21.',
        '3. Walk module-info.java \u2014 read the requires lines.',
        '4. Edit GUI.java \u2014 redirect entry point to LogIn.',
        '5. Run again \u2014 confirm it now opens on LogIn.',
    ];
    beats.forEach((b, i) => {
        s.addText(b, {
            x: 0.7, y: 3.75 + i * 0.5, w: 6.0, h: 0.5,
            fontSize: 13, color: MUTED, fontFace: 'Arial',
        });
    });

    s.addText('Refactor beat: redirect the entry point to the real LogIn screen so the journey starts correctly.', {
        x: 7.2, y: 3.3, w: 5.4, h: 1.2, fontSize: 13, color: WHITE, fontFace: 'Arial', lineSpacing: 1.2,
    });
})();

// ---------- Slide 8: Next time ----------
(function () {
    const s = baseSlide();
    titleBar(s, 'Next time: Episode 2', 'Every Screen Is Its Own Application');
    footer(s, 8);

    s.addText('LogIn is itself a javafx.application.Application \u2014 and every screen in this project creates its own Stage with new Stage().', {
        x: 0.7, y: 2.2, w: 11.9, h: 1.0, fontSize: 16, color: WHITE, fontFace: 'Arial', lineSpacing: 1.2,
    });
    s.addText('That works, but it\u2019s a smell. Next episode we draw LogIn\u2019s scene graph by hand and replace one-Stage-per-screen with a single Screens.show(...) navigation helper.', {
        x: 0.7, y: 3.4, w: 11.9, h: 1.0, fontSize: 14, color: MUTED, fontFace: 'Arial', lineSpacing: 1.2,
    });
    s.addText('Coming up: scene graph \u2022 BorderPane / VBox / HBox \u2022 navigation refactor', { x: 0.7, y: 4.7, w: 11.9, h: 0.5, fontSize: 13, color: ACCENT, fontFace: 'Arial', bold: true });
})();

deck.writeFile('YouTube-Ep1-This-App-Skips-Its-Login-Screen.pptx')
    .then(() => console.log('Deck written.'));
